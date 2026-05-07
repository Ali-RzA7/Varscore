package com.example.var.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class StandingLeagueResponse {

    @SerializedName("code")
    private int code;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private StandingData data;

    public boolean isSuccess() { return code == 0; }
    public int getCode() { return code; }
    public StandingData getData() { return data; }

    public static class StandingData {

        @SerializedName("leagueInfo")
        private LeagueInfo leagueInfo;

        @SerializedName("teamInfos")
        private List<TeamInfo> teamInfos;

        @SerializedName("totalStandings")
        private List<StandingModel> totalStandings;

        public LeagueInfo getLeagueInfo() { return leagueInfo; }
        public List<TeamInfo> getTeamInfos() { return teamInfos; }
        public List<StandingModel> getTotalStandings() { return totalStandings; }
    }

    public static class LeagueInfo {

        @SerializedName("leagueId")
        private String leagueId;

        @SerializedName("name")
        private String name;

        public String getLeagueId() { return leagueId; }
        public String getName() { return name; }
    }

    public static class TeamInfo {

        @SerializedName("teamId")
        private String teamId;

        @SerializedName("name")
        private String name;

        @SerializedName("logo")
        private String logo;

        public String getTeamId() { return teamId; }
        public String getName() { return name; }
        public String getLogo() { return logo; }
    }
}
