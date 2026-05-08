package com.example.var.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * MatchReminderReceiver — AlarmManager tarafından tetiklenen hatırlatma alıcısı.
 *
 * Intent extras:
 *   - "home" : Ev sahibi takım adı
 *   - "away" : Deplasman takım adı
 *   - "time" : Maç saati (metin)
 */
public class MatchReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String home = intent.getStringExtra("home");
        String away = intent.getStringExtra("away");
        String time = intent.getStringExtra("time");
        if (home == null || away == null) return;
        NotificationHelper.showReminder(context, home, away, time != null ? time : "");
    }
}
