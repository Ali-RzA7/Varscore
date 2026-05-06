package com.example.var.data.repository;

import com.example.var.data.model.ApiResponse;
import com.example.var.data.model.LeagueModel;
import com.example.var.data.model.MatchModel;
import com.example.var.data.remote.FootballApiService;
import com.example.var.data.remote.RetrofitClient;

import retrofit2.Call;

/**
 * MatchRepository - Maç verilerine erişim katmanı (Repository Pattern).
 * Activity/Fragment ile API servisi arasında soyutlama sağlar.
 * Tüm veri çekme işlemleri bu sınıf üzerinden yapılır.
 *
 * Avantajları:
 * - Veri kaynağı soyutlaması (API veya lokal DB)
 * - Test edilebilirlik
 * - Tek sorumluluk prensibi (Single Responsibility)
 */
public class MatchRepository {

    /** API servis referansı */
    private final FootballApiService apiService;

    /** API anahtarı */
    private final String apiKey;

    /**
     * Repository constructor.
     * @param apiKey iSportsAPI anahtarı
     */
    public MatchRepository(String apiKey) {
        this.apiService = RetrofitClient.getInstance().getApiService();
        this.apiKey = apiKey;
    }

    /**
     * Belirtilen tarihteki maçların temel bilgilerini çeker.
     * Ana sayfada maç listesi için kullanılır.
     *
     * @param date Tarih (yyyy-MM-dd formatında)
     * @return API çağrısı (asenkron)
     */
    public Call<ApiResponse<MatchModel>> getMatchesByDate(String date) {
        return apiService.getSchedule(apiKey, date);
    }

    /**
     * Belirtilen tarihteki maçların detaylı bilgilerini çeker.
     * Maç detay sayfası için kullanılır.
     *
     * @param date Tarih (yyyy-MM-dd formatında)
     * @return API çağrısı (asenkron)
     */
    public Call<ApiResponse<MatchModel>> getDetailedMatchesByDate(String date) {
        return apiService.getSchedule(apiKey, date);
    }

    /**
     * Belirli bir maçın detaylı bilgilerini çeker.
     *
     * @param matchId Maç kimliği
     * @return API çağrısı (asenkron)
     */
    public Call<ApiResponse<MatchModel>> getMatchDetail(String matchId) {
        return apiService.getMatchDetail(apiKey, matchId);
    }

    /**
     * Günün tüm canlı skorlarını çeker.
     * Günde 1 kez çağrılarak matchId'ler lokal DB'ye kaydedilir.
     *
     * @return API çağrısı (asenkron)
     */
    public Call<ApiResponse<MatchModel>> getLiveScores() {
        return apiService.getLiveScores(apiKey);
    }

    /**
     * Son 20 saniyedeki skor değişikliklerini çeker.
     * Canlı sekme aktifken her 15 saniyede bir çağrılır.
     *
     * @return API çağrısı (asenkron)
     */
    public Call<ApiResponse<MatchModel>> getLiveScoreChanges() {
        return apiService.getLiveScoreChanges(apiKey);
    }

    /**
     * Tüm lig bilgilerini çeker.
     * Arama fonksiyonunda lig araması için kullanılır.
     *
     * @return API çağrısı (asenkron)
     */
    public Call<ApiResponse<LeagueModel>> getLeagues() {
        return apiService.getLeagues(apiKey);
    }

    /**
     * Belirli bir ligin maçlarını çeker.
     *
     * @param leagueId Lig kimliği
     * @return API çağrısı (asenkron)
     */
    public Call<ApiResponse<MatchModel>> getLeagueMatches(String leagueId) {
        return apiService.getLeagueSchedule(apiKey, leagueId);
    }
}
