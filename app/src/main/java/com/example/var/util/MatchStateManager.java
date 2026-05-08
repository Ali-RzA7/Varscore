package com.example.var.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.var.data.model.MatchModel;

/**
 * MatchStateManager — Maç durumlarını SharedPreferences'ta saklar.
 *
 * WorkManager her çalıştığında önceki durumla karşılaştırarak
 * gol, kırmızı kart gibi değişiklikleri tespit eder.
 *
 * Durum formatı: "homeScore,awayScore,status,homeRed,awayRed,hasLineup,var,homeScore_ht,awayScore_ht"
 */
public class MatchStateManager {

    private static final String PREFS = "match_states";
    private static final String KEY_STATE   = "state_";
    private static final String KEY_REMINDED = "reminded_";

    private final SharedPreferences prefs;

    public MatchStateManager(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Daha önce kaydedilmiş durumu döndürür. null ise daha önce görülmemiş demektir. */
    public String getState(String matchId) {
        return prefs.getString(KEY_STATE + matchId, null);
    }

    /** Yeni durumu kaydeder. */
    public void saveState(String matchId, String state) {
        prefs.edit().putString(KEY_STATE + matchId, state).apply();
    }

    /** MatchModel'den durum string'i oluşturur. */
    public static String buildState(MatchModel m) {
        int htHome = 0, htAway = 0;
        if (m.getExtraExplain() != null) {
            htHome = m.getExtraExplain().getHomeScore();
            htAway = m.getExtraExplain().getAwayScore();
        }
        return m.getHomeScore() + "," + m.getAwayScore() + ","
                + m.getStatus() + ","
                + m.getHomeRed() + "," + m.getAwayRed() + ","
                + (m.isHasLineup() ? 1 : 0) + ","
                + (m.getVar() != null ? m.getVar().hashCode() : 0) + ","
                + htHome + "," + htAway;
    }

    /** Bu maç için hatırlatma zaten gönderildi mi? */
    public boolean isReminderSent(String matchId) {
        return prefs.getBoolean(KEY_REMINDED + matchId, false);
    }

    /** Hatırlatma gönderildi olarak işaretle. */
    public void markReminderSent(String matchId) {
        prefs.edit().putBoolean(KEY_REMINDED + matchId, true).apply();
    }

    /** Eski maç durumlarını temizler (bitmişler). */
    public void clearState(String matchId) {
        prefs.edit()
                .remove(KEY_STATE + matchId)
                .remove(KEY_REMINDED + matchId)
                .apply();
    }
}
