package com.example.var.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

/**
 * MatchStatsResponse - /stats endpoint'inden dönen her bir maçın istatistik konteyneri.
 */
public class MatchStatsResponse implements Serializable {

    @SerializedName("matchId")
    private String matchId;

    @SerializedName("stats")
    private List<StatModel> stats;

    public String getMatchId() { return matchId; }
    public List<StatModel> getStats() { return stats; }
}
