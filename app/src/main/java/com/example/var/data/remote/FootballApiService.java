package com.example.var.data.remote;

import com.example.var.data.model.ApiResponse;
import com.example.var.data.model.LeagueModel;
import com.example.var.data.model.MatchModel;

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
     * Tüm liglerin temel bilgilerini getirir.
     * Arama fonksiyonunda lig araması için kullanılır.
     *
     * @param apiKey API anahtarı (zorunlu)
     * @return Lig bilgileri listesi
     */
    @GET("league/basic")
    Call<ApiResponse<LeagueModel>> getLeagues(
            @Query("api_key") String apiKey
    );
}
