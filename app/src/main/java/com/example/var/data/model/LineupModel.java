package com.example.var.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * LineupModel - Maç kadrolarını temsil eder.
 */
public class LineupModel {
    @SerializedName("homeFormation")
    private String homeFormation;

    @SerializedName("awayFormation")
    private String awayFormation;

    @SerializedName("homeLineup")
    private List<PlayerModel> homeLineup;

    @SerializedName("awayLineup")
    private List<PlayerModel> awayLineup;

    @SerializedName("homeBackup")
    private List<PlayerModel> homeBackup;

    @SerializedName("awayBackup")
    private List<PlayerModel> awayBackup;

    public String getHomeFormation() { return homeFormation; }
    public String getAwayFormation() { return awayFormation; }
    public List<PlayerModel> getHomeLineup() { return homeLineup; }
    public List<PlayerModel> getAwayLineup() { return awayLineup; }
    public List<PlayerModel> getHomeBackup() { return homeBackup; }
    public List<PlayerModel> getAwayBackup() { return awayBackup; }

    public static class PlayerModel {
        @SerializedName("playerId")
        private String playerId;

        @SerializedName(value = "playerName", alternate = {"name"})
        private String playerName;

        @SerializedName(value = "number", alternate = {"shirtNumber"})
        private int number;

        @SerializedName("position")
        private String position;

        @SerializedName("rating")
        private String rating;

        public String getPlayerName() { return playerName; }
        public int getNumber() { return number; }
        public String getPosition() { return position; }
        public String getRating() { return rating; }
    }
}
