package com.example.var.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.example.var.R;
import com.example.var.ui.MainActivity;

/**
 * NotificationHelper — Bildirim kanallarını oluşturur ve bildirimleri gösterir.
 *
 * Kanallar:
 *  - CHANNEL_LIVE     : Gol, kart, durum değişikliği gibi canlı maç bildirimleri
 *  - CHANNEL_REMINDER : 1 saat öncesi maç hatırlatmaları
 */
public class NotificationHelper {

    public static final String CHANNEL_LIVE     = "varscore_live";
    public static final String CHANNEL_REMINDER = "varscore_reminder";

    /** Uygulama ilk açıldığında çağrılır — kanalları oluşturur. */
    public static void createChannels(Context ctx) {
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);

        NotificationChannel live = new NotificationChannel(
                CHANNEL_LIVE,
                "Canlı Maç Bildirimleri",
                NotificationManager.IMPORTANCE_HIGH
        );
        live.setDescription("Gol, kırmızı kart, maç başlangıcı/sonu bildirimleri");
        live.enableVibration(true);

        NotificationChannel reminder = new NotificationChannel(
                CHANNEL_REMINDER,
                "Maç Hatırlatmaları",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        reminder.setDescription("Maç başlamadan 1 saat önce hatırlatma");

        nm.createNotificationChannel(live);
        nm.createNotificationChannel(reminder);
    }

    // ── Canlı Maç Bildirimleri ──────────────────────────────────────────

    public static void showMatchStart(Context ctx, String home, String away) {
        show(ctx, CHANNEL_LIVE,
                "🔴 Maç Başladı",
                home + " - " + away,
                id(home + away + "start"));
    }

    public static void showGoal(Context ctx, String home, String away, int hs, int as) {
        show(ctx, CHANNEL_LIVE,
                "⚽ Gol!",
                home + " " + hs + " - " + as + " " + away,
                id(home + away + "goal" + hs + as));
    }

    public static void showRedCard(Context ctx, String home, String away, boolean isHome) {
        String team = isHome ? home : away;
        show(ctx, CHANNEL_LIVE,
                "🟥 Kırmızı Kart",
                team + " | " + home + " - " + away,
                id(home + away + "red" + isHome));
    }

    public static void showHalfTime(Context ctx, String home, String away, int hs, int as) {
        show(ctx, CHANNEL_LIVE,
                "⏸ Devre Arası",
                "İlk yarı: " + home + " " + hs + " - " + as + " " + away,
                id(home + away + "half"));
    }

    public static void showMatchEnd(Context ctx, String home, String away, int hs, int as) {
        show(ctx, CHANNEL_LIVE,
                "🏁 Maç Sona Erdi",
                home + " " + hs + " - " + as + " " + away,
                id(home + away + "end"));
    }

    public static void showLineupReady(Context ctx, String home, String away) {
        show(ctx, CHANNEL_LIVE,
                "📋 Kadrolar Açıklandı",
                home + " - " + away + " kadroları belli oldu",
                id(home + away + "lineup"));
    }

    public static void showVar(Context ctx, String home, String away) {
        show(ctx, CHANNEL_LIVE,
                "📺 VAR İncelemesi",
                home + " - " + away,
                id(home + away + "var"));
    }

    public static void showPenalty(Context ctx, String home, String away) {
        show(ctx, CHANNEL_LIVE,
                "🥅 Penaltı Atışları",
                home + " - " + away + " penaltıya gidildi",
                id(home + away + "penalty"));
    }

    // ── Hatırlatma Bildirimleri ─────────────────────────────────────────

    public static void showReminder(Context ctx, String home, String away, String matchTime) {
        show(ctx, CHANNEL_REMINDER,
                "⏰ Maç Hatırlatması",
                home + " - " + away + " 1 saat sonra başlıyor! (" + matchTime + ")",
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
