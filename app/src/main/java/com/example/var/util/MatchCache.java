package com.example.var.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.var.data.model.MatchModel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

/**
 * MatchCache - Belirli bir tarihteki maç listesini önbellekler.
 *
 * Rate limit'i aşmamak ve hızlı açılış sağlamak için kullanılır.
 */
public class MatchCache {

    private static final String PREF_NAME = "match_cache";
    private static final String KEY_PREFIX = "matches_";
    private static final String TIME_PREFIX = "time_";
    
    // Canlı maçlar sık değiştiği için TTL kısa tutulmalı (örn: 1 dakika)
    // Geçmiş veya gelecek maçlar için daha uzun olabilir.
    private static final long TTL_MS = 60_000L; // 1 dakika

    public static void save(Context ctx, String date, List<MatchModel> matches) {
        String json = new Gson().toJson(matches);
        prefs(ctx).edit()
                .putString(KEY_PREFIX + date, json)
                .putLong(TIME_PREFIX + date, System.currentTimeMillis())
                .apply();
    }

    public static List<MatchModel> load(Context ctx, String date) {
        SharedPreferences sp = prefs(ctx);
        long savedAt = sp.getLong(TIME_PREFIX + date, 0);
        if (System.currentTimeMillis() - savedAt > TTL_MS) return null;

        String json = sp.getString(KEY_PREFIX + date, null);
        if (json == null) return null;

        Type type = new TypeToken<List<MatchModel>>() {}.getType();
        return new Gson().fromJson(json, type);
    }

    /**
     * Tarih bazlı maç verisini TTL kontrolü olmadan yükler.
     * Arama gibi hafif bayatlığın sorun olmadığı durumlarda kullanılır.
     */
    public static List<MatchModel> loadIgnoreTTL(Context ctx, String date) {
        String json = prefs(ctx).getString(KEY_PREFIX + date, null);
        if (json == null) return null;
        Type type = new TypeToken<List<MatchModel>>() {}.getType();
        return new Gson().fromJson(json, type);
    }

    public static void clear(Context ctx) {
        prefs(ctx).edit().clear().apply();
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}
