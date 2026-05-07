package com.example.var.data.remote;

import com.example.var.data.model.ApiResponse;
import com.example.var.data.model.EventModel;
import com.example.var.data.model.LeagueModel;
import com.example.var.data.model.LineupModel;
import com.example.var.data.model.MatchModel;
import com.example.var.data.model.StatModel;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * FootballApiService - iSportsAPI ile iletişim kuran Retrofit servis arayüzü.
 * Tüm API endpoint tanımlamaları bu arayüzde bulunur.
 *
 * Base URL: http://api.isportsapi.com/sport/football/
 *
 * Kullanılan Endpoint'ler:
 * 1. /schedule/basic  -> Temel fikstür bilgileri (liste görünümü)
 * 2. /schedule        -> Detaylı maç bilgileri (maç detay sayfası)
 * 3. /livescores      -> Günün canlı maç verileri
 * 4. /league/basic    -> Lig listesi (arama fonksiyonu)
 */
public interface FootballApiService {

    /**
     * Belirtilen tarihteki maçların temel bilgilerini getirir.
     * Ana sayfadaki maç listesi için kullanılır (hafif veri).
     *
     * @param apiKey API anahtarı (zorunlu)
     * @param date   Tarih formatı: yyyy-MM-dd (isteğe bağlı, boş ise bugün)
     * @return Temel maç bilgileri listesi
     */
    @GET("schedule/basic")
    Call<ApiResponse<MatchModel>> getScheduleBasic(
            @Query("api_key") String apiKey,
            @Query("date") String date
    );

    /**
     * Belirtilen tarihteki maçların detaylı bilgilerini getirir.
     * Maç detay sayfası veya istatistik görünümü için kullanılır.
     *
     * @param apiKey API anahtarı (zorunlu)
     * @param date   Tarih formatı: yyyy-MM-dd (isteğe bağlı)
     * @return Detaylı maç bilgileri listesi
     */
    @GET("schedule")
    Call<ApiResponse<MatchModel>> getSchedule(
            @Query("api_key") String apiKey,
            @Query("date") String date
    );

    /**
     * Belirli bir maçın detaylı bilgilerini getirir.
     *
     * @param apiKey  API anahtarı (zorunlu)
     * @param matchId Maç kimliği
     * @return Detaylı maç bilgisi
     */
    @GET("schedule")
    Call<ApiResponse<MatchModel>> getMatchDetail(
            @Query("api_key") String apiKey,
            @Query("matchId") String matchId
    );

    /**
     * Belirtilen ligin maçlarını getirir.
     *
     * @param apiKey   API anahtarı (zorunlu)
     * @param leagueId Lig kimliği
     * @return Lig maç bilgileri listesi
     */
    @GET("schedule/basic")
    Call<ApiResponse<MatchModel>> getLeagueSchedule(
            @Query("api_key") String apiKey,
            @Query("leagueId") String leagueId
    );

    /**
     * Günün tüm canlı skorlarını getirir.
     * Uygulama açıldığında veya günde 1 kez çağrılır.
     * Tüm maç ID'leri ve takım bilgileri çekilip lokal veritabanına kaydedilir.
     *
     * @param apiKey API anahtarı (zorunlu)
     * @return Günün canlı maç verileri
     */
    @GET("livescores")
    Call<ApiResponse<MatchModel>> getLiveScores(
            @Query("api_key") String apiKey
    );

    /**
     * Son 20 saniye içindeki skor değişikliklerini getirir.
     * Polling mekanizmasıyla her 15 saniyede bir çağrılır.
     * Sadece değişen maçları döner, değişiklik yoksa boş liste döner.
     *
     * @param apiKey API anahtarı (zorunlu)
     * @return Son değişiklikleri içeren maç verileri
     */
    @GET("livescores/changes")
    Call<ApiResponse<MatchModel>> getLiveScoreChanges(
            @Query("api_key") String apiKey
    );

    /**
     * Maç olaylarını (gol, kart, oyuncu değişikliği) getirir.
     *
     * @param apiKey  API anahtarı
     * @param matchId Maç kimliği
     * @return Olay listesi
     */
    @GET("analysis/event")
    Call<ApiResponse<EventModel>> getEvents(
            @Query("api_key") String apiKey,
            @Query("matchId") String matchId
    );

    /**
     * Maç istatistiklerini (şut, korner, topla oynama vb.) getirir.
     * Python kodundaki /stats endpoint'ini kullanır.
     *
     * @param apiKey  API anahtarı
     * @param matchId Maç kimliği (isteğe bağlı)
     * @return İstatistik listesi içeren yanıt
     */
    @GET("stats")
    Call<ApiResponse<com.example.var.data.model.MatchStatsResponse>> getStats(
            @Query("api_key") String apiKey,
            @Query("matchId") String matchId
    );

    /**
     * Maç analiz verilerini (H2H, Form, Gol Zamanlaması vb.) getirir.
     *
     * @param apiKey  API anahtarı
     * @param matchId Maç kimliği
     * @return Analiz verilerini içeren yanıt
     */
    @GET("analysis")
    Call<com.example.var.data.model.AnalysisResponse> getAnalysis(
            @Query("api_key") String apiKey,
            @Query("matchId") String matchId
    );

    @GET("analysis/statistics")
    Call<ApiResponse<com.example.var.data.model.StatModel>> getStatistics(
            @Query("api_key") String apiKey,
            @Query("matchId") String matchId
    );

    /**
     * Maç kadrolarını getirir.
     *
     * @param apiKey  API anahtarı
     * @param matchId Maç kimliği
     * @return Kadro bilgisi
     */
    @GET("analysis/lineup")
    Call<ApiResponse<LineupModel>> getLineup(
            @Query("api_key") String apiKey,
            @Query("matchId") String matchId
    );

    /**
     * Tüm liglerin temel bilgilerini getirir.
     * Arama fonksiyonunda ve Puan Durumu ekranında lig listesi için kullanılır.
     *
     * @param apiKey API anahtarı (zorunlu)
     * @return Lig bilgileri listesi
     */
    @GET("league/basic")
    Call<ApiResponse<LeagueModel>> getLeagues(
            @Query("api_key") String apiKey
    );

    /**
     * Belirli bir ligin puan tablosunu getirir.
     * Puan Durumu ekranında lig seçildiğinde kullanılır.
     * Tabloda: sıra, takım, oynanan, galibiyet, beraberlik, mağlubiyet, gol farkı, puan.
     *
     * @param apiKey   API anahtarı (zorunlu)
     * @param leagueId Lig kimliği (zorunlu)
     * @return Puan tablosu listesi
     */
    @GET("league/table")
    Call<ApiResponse<com.example.var.data.model.StandingModel>> getLeagueTable(
            @Query("api_key") String apiKey,
            @Query("leagueId") String leagueId
    );

    /**
     * Belirli bir takımın geçmiş ve gelecek maçlarını getirir.
     * Arama'da takıma tıklandığında ve Profil ekranındaki favori takımlar için kullanılır.
     *
     * @param apiKey API anahtarı (zorunlu)
     * @param teamId Takım kimliği (zorunlu)
     * @return Takıma ait maç listesi
     */
    @GET("schedule/basic")
    Call<ApiResponse<MatchModel>> getTeamMatches(
            @Query("api_key") String apiKey,
            @Query("teamId") String teamId
    );
}
