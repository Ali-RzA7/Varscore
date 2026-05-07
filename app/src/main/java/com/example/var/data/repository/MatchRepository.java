package com.example.var.data.repository;

import com.example.var.data.model.AnalysisResponse;
import com.example.var.data.model.AnalysisModel;
import com.example.var.data.model.ApiResponse;
import com.example.var.data.model.EventModel;
import com.example.var.data.model.LeagueModel;
import com.example.var.data.model.LineupModel;
import com.example.var.data.model.MatchStatsResponse;
import com.example.var.data.model.MatchModel;
import com.example.var.data.model.StatModel;
import com.example.var.data.model.StandingModel;
import com.example.var.data.remote.FootballApiService;
import com.example.var.data.remote.RetrofitClient;

import retrofit2.Call;

/**
 * MatchRepository - Maç ve Lig verilerine erişim katmanı (Repository Pattern).
 *
 * Bu sınıf, FootballApiService üzerindeki tüm API çağrılarını sarmalar.
 * Fragment'lar veri erişimini bu sınıf üzerinden yapar; API anahtarı
 * burada merkezi olarak yönetilir.
 *
 * Desteklenen işlemler:
 * - Tarih bazlı ve canlı maç listeleri
 * - Maç detayı, olaylar, istatistikler, kadrolar
 * - Lig listesi, lig puan tablosu
 * - Takım geçmiş maçları
 */
public class MatchRepository {

    /** iSportsAPI Retrofit arayüzü */
    private final FootballApiService apiService;

    /** API kimlik doğrulama anahtarı */
    private final String apiKey;

    /**
     * Repository'yi oluşturur ve Retrofit servisini başlatır.
     *
     * @param apiKey İsportsAPI erişim anahtarı (BuildConfig.API_KEY)
     */
    public MatchRepository(String apiKey) {
        this.apiService = RetrofitClient.getInstance().getApiService();
        this.apiKey = apiKey;
    }

    // ===== Maç Listeleri =====

    /** Belirtilen tarihteki maçları getirir (/schedule endpoint) */
    public Call<ApiResponse<MatchModel>> getMatchesByDate(String date) {
        return apiService.getSchedule(apiKey, date);
    }

    /** Belirtilen tarihteki maçları temel bilgilerle getirir (/schedule/basic) */
    public Call<ApiResponse<MatchModel>> getDetailedMatchesByDate(String date) {
        return apiService.getSchedule(apiKey, date);
    }

    /** Tek bir maçın detaylı verisini getirir (matchId ile /schedule) */
    public Call<ApiResponse<MatchModel>> getMatchDetail(String matchId) {
        return apiService.getMatchDetail(apiKey, matchId);
    }

    /** Günün tüm canlı maçlarını getirir (/livescores) */
    public Call<ApiResponse<MatchModel>> getLiveScores() {
        return apiService.getLiveScores(apiKey);
    }

    /** Son 20 saniyedeki skor değişikliklerini getirir (/livescores/changes) */
    public Call<ApiResponse<MatchModel>> getLiveScoreChanges() {
        return apiService.getLiveScoreChanges(apiKey);
    }

    // ===== Lig İşlemleri =====

    /** Tüm liglerin temel listesini getirir (/league/basic) */
    public Call<ApiResponse<LeagueModel>> getLeagues() {
        return apiService.getLeagues(apiKey);
    }

    /** Belirli bir ligin maçlarını getirir (/schedule/basic?leagueId) */
    public Call<ApiResponse<MatchModel>> getLeagueMatches(String leagueId) {
        return apiService.getLeagueSchedule(apiKey, leagueId);
    }

    /**
     * Belirli bir ligin puan tablosunu getirir (/league/table).
     * Puan Durumu ekranında kullanılır.
     *
     * @param leagueId Puan tablosu istenilen ligin ID'si
     */
    public Call<ApiResponse<StandingModel>> getLeagueTable(String leagueId) {
        return apiService.getLeagueTable(apiKey, leagueId);
    }

    /**
     * Belirli bir takımın maçlarını getirir (/schedule/basic?teamId).
     * Arama ekranında takıma tıklandığında ve favori takımlar için kullanılır.
     *
     * @param teamId Maçları istenilen takımın ID'si
     */
    public Call<ApiResponse<MatchModel>> getTeamMatches(String teamId) {
        return apiService.getTeamMatches(apiKey, teamId);
    }

    // ===== Maç Detay Verileri =====

    /** Maç olay listesini getirir: gol, kart, değişiklik (/analysis/event) */
    public Call<ApiResponse<EventModel>> getEvents(String matchId) {
        return apiService.getEvents(apiKey, matchId);
    }

    /** Maç istatistiklerini getirir: şut, topla oynama vb. (/analysis/statistics) */
    public Call<ApiResponse<StatModel>> getStatistics(String matchId) {
        return apiService.getStatistics(apiKey, matchId);
    }

    /** H2H ve form analizini getirir (/analysis) */
    public Call<AnalysisResponse> getAnalysis(String matchId) {
        return apiService.getAnalysis(apiKey, matchId);
    }

    /** Detaylı maç istatistik paketini getirir (/stats) */
    public Call<ApiResponse<MatchStatsResponse>> getStats(String matchId) {
        return apiService.getStats(apiKey, matchId);
    }

    /** Maç kadro bilgisini getirir: ilk 11, yedekler (/analysis/lineup) */
    public Call<ApiResponse<LineupModel>> getLineup(String matchId) {
        return apiService.getLineup(apiKey, matchId);
    }
}
