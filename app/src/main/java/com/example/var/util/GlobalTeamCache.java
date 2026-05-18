package com.example.var.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.var.data.model.MatchModel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GlobalTeamCache — Tarihten bağımsız, kapsamlı takım önbelleği.
 *
 * Maç verileri yüklendikçe (HomeFragment vb.) merge() ile otomatik birikir.
 * TTL: 30 gün. Süresi dolarsa bir sonraki merge() TTL'yi sıfırlar.
 *
 * Kullanım:
 *  - HomeFragment: her maç yüklemesinde merge() çağır
 *  - SearchDialogFragment: loadAny() ile tüm takımları oku
 */
public class GlobalTeamCache {

    private static final String PREF_NAME  = "global_team_cache";
    private static final String KEY_TEAMS  = "teams";
    private static final String KEY_TIME   = "saved_at";
    private static final long   TTL_MS     = 30L * 24 * 60 * 60 * 1000; // 30 gün

    /** Takım kaydı */
    public static class TeamEntry {
        public String teamId;
        public String teamName;
        public String leagueId;
        public String leagueName;

        public TeamEntry() {} // Gson için

        public TeamEntry(String teamId, String teamName, String leagueId, String leagueName) {
            this.teamId    = teamId;
            this.teamName  = teamName;
            this.leagueId  = leagueId;
            this.leagueName = leagueName;
        }
    }

    /** TTL içindeyse döner, süresi dolmuşsa null döner. */
    public static List<TeamEntry> load(Context ctx) {
        long savedAt = prefs(ctx).getLong(KEY_TIME, 0);
        if (System.currentTimeMillis() - savedAt > TTL_MS) return null;
        return loadAny(ctx);
    }

    /** TTL'den bağımsız olarak her zaman döner (arama için uygundur). */
    public static List<TeamEntry> loadAny(Context ctx) {
        String json = prefs(ctx).getString(KEY_TEAMS, null);
        if (json == null) return null;
        Type type = new TypeToken<List<TeamEntry>>() {}.getType();
        try {
            return new Gson().fromJson(json, type);
        } catch (Exception e) {
            return null;
        }
    }

    /** Cache boş veya TTL dolmuşsa true döner. */
    public static boolean needsRefresh(Context ctx) {
        SharedPreferences sp = prefs(ctx);
        if (sp.getString(KEY_TEAMS, null) == null) return true;
        long savedAt = sp.getLong(KEY_TIME, 0);
        return System.currentTimeMillis() - savedAt > TTL_MS;
    }

    /**
     * Maç listesinden takımları çıkarıp mevcut cache'e ekler (merge, overwrite değil).
     * TTL dolmuşsa saved_at sıfırlanır (30 günlük sayaç yeniden başlar).
     * Aynı teamId tekrar eklenmez.
     */
    public static void merge(Context ctx, List<MatchModel> matches) {
        if (matches == null || matches.isEmpty()) return;

        SharedPreferences sp = prefs(ctx);
        long savedAt = sp.getLong(KEY_TIME, 0);
        boolean expired = savedAt == 0 || System.currentTimeMillis() - savedAt > TTL_MS;

        // Mevcut veriyi haritaya al
        Map<String, TeamEntry> map = new HashMap<>();
        List<TeamEntry> existing = loadAny(ctx);
        if (existing != null) {
            for (TeamEntry e : existing) {
                if (e.teamId != null) map.put(e.teamId, e);
            }
        }

        boolean changed = false;
        for (MatchModel m : matches) {
            if (m.getHomeId() != null && m.getHomeName() != null
                    && !map.containsKey(m.getHomeId())) {
                map.put(m.getHomeId(), new TeamEntry(
                        m.getHomeId(), m.getHomeName(),
                        m.getLeagueId(), m.getLeagueName()));
                changed = true;
            }
            if (m.getAwayId() != null && m.getAwayName() != null
                    && !map.containsKey(m.getAwayId())) {
                map.put(m.getAwayId(), new TeamEntry(
                        m.getAwayId(), m.getAwayName(),
                        m.getLeagueId(), m.getLeagueName()));
                changed = true;
            }
        }

        if (!changed && !expired) return;

        // TTL dolmuşsa sıfırla; değilse mevcut saved_at koru
        long newSavedAt = expired ? System.currentTimeMillis() : savedAt;

        sp.edit()
                .putString(KEY_TEAMS, new Gson().toJson(new ArrayList<>(map.values())))
                .putLong(KEY_TIME, newSavedAt)
                .apply();
    }

    /**
     * Tüm takımları bulk olarak kaydeder ve TTL'yi sıfırlar.
     * İlk yükleme veya periyodik yenileme için kullanılır.
     */
    public static void save(Context ctx, List<TeamEntry> teams) {
        prefs(ctx).edit()
                .putString(KEY_TEAMS, new Gson().toJson(teams))
                .putLong(KEY_TIME, System.currentTimeMillis())
                .apply();
    }

    public static void clear(Context ctx) {
        prefs(ctx).edit().clear().apply();
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}
