package com.example.var.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;

import androidx.core.app.NotificationCompat;

import com.example.var.R;
import com.example.var.ui.MainActivity;

/**
 * NotificationHelper — Bildirim kanallarını oluşturur ve bildirimleri gösterir.
 *
 * Kanallar:
 *  - CHANNEL_LIVE         : Kırmızı kart, devre arası, kadro, VAR, penaltı bildirimleri
 *  - CHANNEL_GOAL         : Gol bildirimleri (gol.mp3)
 *  - CHANNEL_MATCH_START  : Maç başlangıç bildirimleri (macbaslama.mp3)
 *  - CHANNEL_MATCH_END    : Maç sonu bildirimleri (macbitis.mp3)
 *  - CHANNEL_REMINDER     : 1 saat öncesi maç hatırlatmaları (machatirlatma.mp3)
 */
public class NotificationHelper {

    public static final String CHANNEL_LIVE        = "varscore_live";
    public static final String CHANNEL_GOAL        = "varscore_goal";
    public static final String CHANNEL_MATCH_START = "varscore_match_start";
    public static final String CHANNEL_MATCH_END   = "varscore_match_end";
    public static final String CHANNEL_REMINDER    = "varscore_reminder";

    private static AudioAttributes notifAudioAttrs() {
        return new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build();
    }

    /** Uygulama ilk açıldığında çağrılır — kanalları oluşturur. */
    public static void createChannels(Context ctx) {
        SoundPreferencesManager sp = new SoundPreferencesManager(ctx);
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        AudioAttributes attrs = notifAudioAttrs();

        // Diğer canlı bildirimler (kırmızı kart, devre, kadro, VAR, penaltı)
        NotificationChannel live = new NotificationChannel(
                CHANNEL_LIVE,
                ctx.getString(R.string.channel_live_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        live.setDescription(ctx.getString(R.string.channel_live_desc));
        live.enableVibration(true);

        // Gol
        NotificationChannel goal = new NotificationChannel(
                CHANNEL_GOAL,
                ctx.getString(R.string.channel_live_name) + " (Gol)",
                NotificationManager.IMPORTANCE_HIGH
        );
        goal.setSound(sp.getSoundUri(SoundPreferencesManager.KEY_GOAL, R.raw.gol), attrs);
        goal.enableVibration(true);

        // Maç başlangıcı
        NotificationChannel matchStart = new NotificationChannel(
                CHANNEL_MATCH_START,
                ctx.getString(R.string.channel_live_name) + " (Maç Başlangıcı)",
                NotificationManager.IMPORTANCE_HIGH
        );
        matchStart.setSound(sp.getSoundUri(SoundPreferencesManager.KEY_MATCH_START, R.raw.macbaslama), attrs);
        matchStart.enableVibration(true);

        // Maç sonu
        NotificationChannel matchEnd = new NotificationChannel(
                CHANNEL_MATCH_END,
                ctx.getString(R.string.channel_live_name) + " (Maç Sonu)",
                NotificationManager.IMPORTANCE_HIGH
        );
        matchEnd.setSound(sp.getSoundUri(SoundPreferencesManager.KEY_MATCH_END, R.raw.macbitis), attrs);
        matchEnd.enableVibration(true);

        // Hatırlatma
        NotificationChannel reminder = new NotificationChannel(
                CHANNEL_REMINDER,
                ctx.getString(R.string.channel_reminder_name),
                NotificationManager.IMPORTANCE_DEFAULT
        );
        reminder.setDescription(ctx.getString(R.string.channel_reminder_desc));
        reminder.setSound(sp.getSoundUri(SoundPreferencesManager.KEY_REMINDER, R.raw.machatirlatma), attrs);

        nm.createNotificationChannel(live);
        nm.createNotificationChannel(goal);
        nm.createNotificationChannel(matchStart);
        nm.createNotificationChannel(matchEnd);
        nm.createNotificationChannel(reminder);
    }

    /**
     * Belirli bir kanalı siler ve güncel ses tercihiyle yeniden oluşturur.
     * Kullanıcı ses değiştirdiğinde çağrılır.
     */
    public static void updateChannelSound(Context ctx, String channelId) {
        ctx.getSystemService(NotificationManager.class).deleteNotificationChannel(channelId);
        createChannels(ctx);
    }

    // ── Canlı Maç Bildirimleri ──────────────────────────────────────────

    public static void showMatchStart(Context ctx, String home, String away) {
        show(ctx, CHANNEL_MATCH_START,
                ctx.getString(R.string.notification_match_started),
                home + " - " + away,
                id(home + away + "start"));
    }

    public static void showGoal(Context ctx, String home, String away, int hs, int as) {
        show(ctx, CHANNEL_GOAL,
                ctx.getString(R.string.notification_goal),
                home + " " + hs + " - " + as + " " + away,
                id(home + away + "goal" + hs + as));
    }

    public static void showRedCard(Context ctx, String home, String away, boolean isHome) {
        String team = isHome ? home : away;
        show(ctx, CHANNEL_LIVE,
                ctx.getString(R.string.notification_red_card),
                team + " | " + home + " - " + away,
                id(home + away + "red" + isHome));

    }

    public static void showHalfTime(Context ctx, String home, String away, int hs, int as) {
        show(ctx, CHANNEL_LIVE,
                ctx.getString(R.string.notification_half_time),
                ctx.getString(R.string.notification_half_time_body, home, hs, as, away),
                id(home + away + "half"));

    }

    public static void showMatchEnd(Context ctx, String home, String away, int hs, int as) {
        show(ctx, CHANNEL_MATCH_END,
                ctx.getString(R.string.notification_match_ended),
                home + " " + hs + " - " + as + " " + away,
                id(home + away + "end"));
    }

    public static void showLineupReady(Context ctx, String home, String away) {
        show(ctx, CHANNEL_LIVE,
                ctx.getString(R.string.notification_lineups_ready),
                ctx.getString(R.string.notification_lineups_ready_body, home, away),
                id(home + away + "lineup"));

    }

    public static void showVar(Context ctx, String home, String away) {
        show(ctx, CHANNEL_LIVE,
                ctx.getString(R.string.notification_var),
                home + " - " + away,
                id(home + away + "var"));

    }

    public static void showPenalty(Context ctx, String home, String away) {
        show(ctx, CHANNEL_LIVE,
                ctx.getString(R.string.notification_penalty),
                ctx.getString(R.string.notification_penalty_body, home, away),
                id(home + away + "penalty"));

    }

    // ── Hatırlatma Bildirimleri ─────────────────────────────────────────

    public static void showReminder(Context ctx, String home, String away, String matchTime) {
        show(ctx, CHANNEL_REMINDER,
                ctx.getString(R.string.notification_reminder),
                ctx.getString(R.string.notification_reminder_body, home, away, matchTime),
                id(home + away + "reminder"));

    }

    // ── Yardımcı Metodlar ──────────────────────────────────────────────

    private static void show(Context ctx, String channel, String title, String text, int notifId) {
        Intent intent = new Intent(ctx, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(ctx, notifId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notif = new NotificationCompat.Builder(ctx, channel)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build();

        ctx.getSystemService(NotificationManager.class).notify(notifId, notif);
    }

    private static int id(String key) {
        return key.hashCode();
    }
}
