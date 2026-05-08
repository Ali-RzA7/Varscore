package com.example.var.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.var.BuildConfig;
import com.example.var.data.model.ApiResponse;
import com.example.var.data.model.MatchModel;
import com.example.var.data.remote.FootballApiService;
import com.example.var.data.remote.RetrofitClient;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import retrofit2.Response;

/**
 * MatchMonitorWorker — Arka planda maçları izleyen WorkManager Worker'ı.
 *
 * Her 15 dakikada bir çalışır:
 * 1. Favori takım/lig ID'lerini Firebase'den okur
 * 2. Canlı skorları API'den çeker
 * 3. Favori takımların maçlarındaki değişiklikleri tespit eder
 * 4. İlgili bildirim türünü gönderir
 * 5. Bugünkü maçlar için 1 saatlik hatırlatma planlar
 *
 * NOT: Worker senkron çalışır; Firebase için Tasks.await() kullanılır.
 */
public class MatchMonitorWorker extends Worker {

    private static final String API_KEY = BuildConfig.API_KEY;

    public MatchMonitorWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();

        // Kullanıcı giriş yapmamışsa çalışmaya gerek yok
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return Result.success();

        try {
            // ── 1. Firebase'den favorileri al ────────────────────────────
            DocumentSnapshot doc = Tasks.await(
                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(user.getUid())
                            .get(),
                    10, TimeUnit.SECONDS
            );
            if (!doc.exists()) return Result.success();

            List<String> favTeams  = castList(doc.get("favoriteTeams"));
            List<String> favLeagues = castList(doc.get("favoriteLeagues"));

            Set<String> teamSet   = new HashSet<>(favTeams);
            Set<String> leagueSet = new HashSet<>(favLeagues);

            if (teamSet.isEmpty() && leagueSet.isEmpty()) return Result.success();

            // ── 2. Canlı maçları çek ─────────────────────────────────────
            FootballApiService api = RetrofitClient.getInstance().getApiService();
            Response<ApiResponse<MatchModel>> liveResp = api.getLiveScores(API_KEY).execute();

            if (liveResp.isSuccessful() && liveResp.body() != null
                    && liveResp.body().getData() != null) {

                MatchStateManager stateManager = new MatchStateManager(ctx);

                for (MatchModel match : liveResp.body().getData()) {
                    if (!isFavorite(match, teamSet, leagueSet)) continue;
                    processLiveMatch(ctx, match, stateManager);
                }
            }

            // ── 3. Bugünkü maçlar için hatırlatma planla ─────────────────
            String today = com.example.var.util.DateUtils.formatForApi(Calendar.getInstance());
            Response<ApiResponse<MatchModel>> todayResp = api.getSchedule(API_KEY, today).execute();

            if (todayResp.isSuccessful() && todayResp.body() != null
                    && todayResp.body().getData() != null) {

                MatchStateManager stateManager = new MatchStateManager(ctx);

                for (MatchModel match : todayResp.body().getData()) {
                    if (!isFavorite(match, teamSet, leagueSet)) continue;
                    scheduleReminderIfNeeded(ctx, match, stateManager);
                }
            }

        } catch (Exception e) {
            // Sessiz hata — bir sonraki periyotta tekrar denenir
            return Result.retry();
        }

        return Result.success();
    }

    // ── Canlı Maç İşleme ──────────────────────────────────────────────

    private void processLiveMatch(Context ctx, MatchModel match, MatchStateManager sm) {
        String matchId = match.getMatchId();
        if (matchId == null) return;

        String prevState = sm.getState(matchId);
        String newState  = MatchStateManager.buildState(match);

        if (prevState == null) {
            // İlk kez görüldü → maç başlangıcı bildirimi
            if (match.isLive()) {
                NotificationHelper.showMatchStart(ctx, match.getHomeName(), match.getAwayName());
            }
            sm.saveState(matchId, newState);
            return;
        }

        if (prevState.equals(newState)) return; // Değişiklik yok

        // Önceki ve yeni değerleri parse et
        String[] prev = prevState.split(",");
        String[] next = newState.split(",");

        if (prev.length < 9 || next.length < 9) {
            sm.saveState(matchId, newState);
            return;
        }

        int prevHomeScore = parseInt(prev[0]);
        int prevAwayScore = parseInt(prev[1]);
        int prevStatus    = parseInt(prev[2]);
        int prevHomeRed   = parseInt(prev[3]);
        int prevAwayRed   = parseInt(prev[4]);
        int prevLineup    = parseInt(prev[5]);
        int prevVar       = parseInt(prev[6]);

        int newHomeScore  = parseInt(next[0]);
        int newAwayScore  = parseInt(next[1]);
        int newStatus     = parseInt(next[2]);
        int newHomeRed    = parseInt(next[3]);
        int newAwayRed    = parseInt(next[4]);
        int newLineup     = parseInt(next[5]);
        int newVar        = parseInt(next[6]);

        String home = match.getHomeName();
        String away = match.getAwayName();

        // Gol
        if (newHomeScore > prevHomeScore || newAwayScore > prevAwayScore) {
            NotificationHelper.showGoal(ctx, home, away, newHomeScore, newAwayScore);
        }

        // Kırmızı kart
        if (newHomeRed > prevHomeRed) {
            NotificationHelper.showRedCard(ctx, home, away, true);
        }
        if (newAwayRed > prevAwayRed) {
            NotificationHelper.showRedCard(ctx, home, away, false);
        }

        // Devre arası (status = 2)
        if (prevStatus != 2 && newStatus == 2) {
            NotificationHelper.showHalfTime(ctx, home, away, newHomeScore, newAwayScore);
        }

        // Maç sonu (status = -1)
        if (prevStatus != -1 && newStatus == -1) {
            NotificationHelper.showMatchEnd(ctx, home, away, newHomeScore, newAwayScore);
            sm.clearState(matchId);
            return;
        }

        // Penaltı atışları (status = 5)
        if (prevStatus != 5 && newStatus == 5) {
            NotificationHelper.showPenalty(ctx, home, away);
        }

        // Kadro açıklandı
        if (prevLineup == 0 && newLineup == 1) {
            NotificationHelper.showLineupReady(ctx, home, away);
        }

        // VAR
        if (prevVar != newVar && newVar != 0) {
            NotificationHelper.showVar(ctx, home, away);
        }

        sm.saveState(matchId, newState);
    }

    // ── 1 Saatlik Hatırlatma ──────────────────────────────────────────

    private void scheduleReminderIfNeeded(Context ctx, MatchModel match, MatchStateManager sm) {
        if (!match.isNotStarted()) return;
        String matchId = match.getMatchId();
        if (matchId == null) return;
        if (sm.isReminderSent(matchId)) return;

        long matchMs  = match.getMatchTime() * 1000L;
        long reminderMs = matchMs - 3600_000L; // 1 saat önce
        long nowMs    = System.currentTimeMillis();

        // Hatırlatma zamanı geçmişte veya 3 dakikadan az kaldıysa atla
        if (reminderMs < nowMs + 3 * 60_000L) return;

        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        String timeStr = com.example.var.util.DateUtils.formatMatchTime(match.getMatchTime());

        Intent intent = new Intent(ctx, MatchReminderReceiver.class);
        intent.putExtra("home", match.getHomeName());
        intent.putExtra("away", match.getAwayName());
        intent.putExtra("time", timeStr);

        int reqCode = matchId.hashCode();
        PendingIntent pi = PendingIntent.getBroadcast(ctx, reqCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderMs, pi);
        sm.markReminderSent(matchId);
    }

    // ── Yardımcı Metodlar ────────────────────────────────────────────

    private boolean isFavorite(MatchModel match, Set<String> teamSet, Set<String> leagueSet) {
        return teamSet.contains(match.getHomeId())
                || teamSet.contains(match.getAwayId())
                || leagueSet.contains(match.getLeagueId());
    }

    @SuppressWarnings("unchecked")
    private List<String> castList(Object obj) {
        if (obj instanceof List) {
            try { return (List<String>) obj; } catch (ClassCastException ignored) {}
        }
        return new ArrayList<>();
    }

    private int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return 0; }
    }
}
