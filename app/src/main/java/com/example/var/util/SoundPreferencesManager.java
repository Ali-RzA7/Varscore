package com.example.var.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import com.example.var.R;

import java.io.File;

public class SoundPreferencesManager {

    private static final String PREFS_NAME = "notification_sounds";

    public static final String KEY_GOAL        = "sound_goal";
    public static final String KEY_MATCH_START = "sound_match_start";
    public static final String KEY_MATCH_END   = "sound_match_end";
    public static final String KEY_REMINDER    = "sound_reminder";

    private final SharedPreferences prefs;
    private final Context ctx;

    public SoundPreferencesManager(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        this.prefs = this.ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Verilen key için ses URI'sini döner.
     * Özel ses varsa ve dosya mevcutsa onu kullanır, yoksa default raw kaynağa döner.
     */
    public Uri getSoundUri(String key, int defaultRawRes) {
        String customPath = prefs.getString(key, null);
        if (customPath != null) {
            File file = new File(customPath);
            if (file.exists()) {
                return Uri.fromFile(file);
            }
            prefs.edit().remove(key).apply();
        }
        return defaultRawUri(defaultRawRes);
    }

    public Uri defaultRawUri(int rawRes) {
        return Uri.parse("android.resource://" + ctx.getPackageName() + "/" + rawRes);
    }

    /** Verilen key için özel ses ayarlanmış ve dosya mevcut mu? */
    public boolean isCustomSound(String key) {
        String path = prefs.getString(key, null);
        if (path == null) return false;
        File file = new File(path);
        if (!file.exists()) {
            prefs.edit().remove(key).apply();
            return false;
        }
        return true;
    }

    public void setCustomSoundPath(String key, String filePath) {
        prefs.edit().putString(key, filePath).apply();
    }

    /** Özel sesi siler ve tercihi temizler. */
    public void resetToDefault(String key) {
        String customPath = prefs.getString(key, null);
        if (customPath != null) {
            new File(customPath).delete();
        }
        prefs.edit().remove(key).apply();
    }

    /**
     * Özel seslerin saklandığı klasör.
     * Harici uygulama depolaması kullanılır; sistem bildirim servisi
     * (NotificationManagerService) bu dizindeki dosyaları file:// URI
     * üzerinden okuyabilir. getFilesDir() (private) ile bu mümkün değildir.
     */
    public File getCustomSoundsDir() {
        File dir = ctx.getExternalFilesDir("custom_sounds");
        if (dir == null) {
            // Harici depolama yoksa iç depoya geri dön
            dir = new File(ctx.getFilesDir(), "custom_sounds");
        }
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    /** key → varsayılan ham kaynak ID eşlemesi. */
    public static int defaultRawRes(String key) {
        switch (key) {
            case KEY_GOAL:        return R.raw.gol;
            case KEY_MATCH_START: return R.raw.macbaslama;
            case KEY_MATCH_END:   return R.raw.macbitis;
            case KEY_REMINDER:    return R.raw.machatirlatma;
            default:              return R.raw.gol;
        }
    }

    /** key → bildirim kanalı ID eşlemesi. */
    public static String channelId(String key) {
        switch (key) {
            case KEY_GOAL:        return NotificationHelper.CHANNEL_GOAL;
            case KEY_MATCH_START: return NotificationHelper.CHANNEL_MATCH_START;
            case KEY_MATCH_END:   return NotificationHelper.CHANNEL_MATCH_END;
            case KEY_REMINDER:    return NotificationHelper.CHANNEL_REMINDER;
            default:              return NotificationHelper.CHANNEL_LIVE;
        }
    }
}
