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
import com.example.var.data.remote.FootballApiService;
import com.example.var.data.remote.RetrofitClient;

import retrofit2.Call;

/**
 * MatchRepository - Maç verilerine erişim katmanı (Repository Pattern).
 */
public class MatchRepository {

    private final FootballApiService apiService;
    private final String apiKey;

    public MatchRepository(String apiKey) {
        this.apiService = RetrofitClient.getInstance().getApiService();
        this.apiKey = apiKey;
    }

    public Call<ApiResponse<MatchModel>> getMatchesByDate(String date) {
        return apiService.getSchedule(apiKey, date);
    }

    public Call<ApiResponse<MatchModel>> getDetailedMatchesByDate(String date) {
        return apiService.getSchedule(apiKey, date);
    }

    public Call<ApiResponse<MatchModel>> getMatchDetail(String matchId) {
        return apiService.getMatchDetail(apiKey, matchId);
    }

    public Call<ApiResponse<MatchModel>> getLiveScores() {
        return apiService.getLiveScores(apiKey);
    }

    public Call<ApiResponse<MatchModel>> getLiveScoreChanges() {
        return apiService.getLiveScoreChanges(apiKey);
    }

    public Call<ApiResponse<LeagueModel>> getLeagues() {
        return apiService.getLeagues(apiKey);
    }

    public Call<ApiResponse<MatchModel>> getLeagueMatches(String leagueId) {
        return apiService.getLeagueSchedule(apiKey, leagueId);
    }

    public Call<ApiResponse<EventModel>> getEvents(String matchId) {
        return apiService.getEvents(apiKey, matchId);
    }

    public Call<ApiResponse<StatModel>> getStatistics(String matchId) {
        return apiService.getStatistics(apiKey, matchId);
    }

    public Call<AnalysisResponse> getAnalysis(String matchId) {
        return apiService.getAnalysis(apiKey, matchId);
    }

    public Call<ApiResponse<MatchStatsResponse>> getStats(String matchId) {
        return apiService.getStats(apiKey, matchId);
    }

    public Call<ApiResponse<LineupModel>> getLineup(String matchId) {
        return apiService.getLineup(apiKey, matchId);
    }
}
