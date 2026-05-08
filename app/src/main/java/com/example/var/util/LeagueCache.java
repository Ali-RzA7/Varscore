package com.example.var.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.var.data.model.LeagueModel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

/**
 * LeagueCache - Lig listesini hem bellekte hem SharedPreferences'te önbellekler.
 *
 * /league/basic endpoint'inin rate limit'ini aşmamak için:
 * - Oturum içi erişimler için in-memory cache
 * - Uygulama yeniden başlatıldığında SharedPreferences'ten yükleme
 * - TTL: 1 saat (rate limit 1800s olduğu için güvenli aralık)
 */
public class LeagueCache {

    private static final String PREF_NAME  = "league_cache";
    private static final String KEY_JSON   = "leagues_json";
    private static final String KEY_TIME   = "cache_time";
    private static final long   TTL_MS     = 1_800_000L; // 30 dakika (API rate limit perioduna eşit)

    private static List<LeagueModel> memory;

    /** Ligleri hem memory hem SharedPreferences'e yazar. */
    public static void save(Context ctx, List<LeagueModel> leagues) {
        memory = leagues;
        String json = new Gson().toJson(leagues);
        prefs(ctx).edit()
                .putString(KEY_JSON, json)
                .putLong(KEY_TIME, System.currentTimeMillis())
                .apply();
    }

    /**
     * Geçerli önbellekten ligleri döner.
     * Önce memory, sonra SharedPreferences kontrol edilir.
     * TTL süresi aşılmışsa null döner.
     */
    public static List<LeagueModel> load(Context ctx) {
        if (memory != null && !memory.isEmpty()) return memory;

        SharedPreferences sp = prefs(ctx);
        long savedAt = sp.getLong(KEY_TIME, 0);
        if (System.currentTimeMillis() - savedAt > TTL_MS) return null;

        String json = sp.getString(KEY_JSON, null);
        if (json == null) return null;

        Type type = new TypeToken<List<LeagueModel>>() {}.getType();
        memory = new Gson().fromJson(json, type);
        return memory;
    }

    /**
     * TTL'dan bağımsız, kayıtlı herhangi bir veri var mı?
     * API başarısız olduğunda eski veriyi göstermek için kullanılır.
     */
    public static List<LeagueModel> loadAny(Context ctx) {
        if (memory != null && !memory.isEmpty()) return memory;
        String json = prefs(ctx).getString(KEY_JSON, null);
        if (json == null) return null;
        Type type = new TypeToken<List<LeagueModel>>() {}.getType();
        memory = new Gson().fromJson(json, type);
        return memory;
    }

    /** Cache'i tamamen temizler (swipe-refresh ile zorla yenileme için). */
    public static void clear(Context ctx) {
        memory = null;
        prefs(ctx).edit().remove(KEY_JSON).remove(KEY_TIME).apply();
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}
