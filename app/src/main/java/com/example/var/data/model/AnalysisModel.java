package com.example.var.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

/**
 * AnalysisModel - /analysis endpoint'inden dönen verileri karşılar.
 */
public class AnalysisModel implements Serializable {

    @SerializedName("headToHead")
    private List<String> headToHead;

    @SerializedName("homeDataVs")
    private FormModel homeDataVs;

    @SerializedName("awayDataVs")
    private FormModel awayDataVs;

    public List<String> getHeadToHead() { return headToHead; }
    public FormModel getHomeDataVs() { return homeDataVs; }
    public FormModel getAwayDataVs() { return awayDataVs; }

    public static class FormModel implements Serializable {
        @SerializedName("win") public int win;
        @SerializedName("draw") public int draw;
        @SerializedName("lose") public int lose;
        @SerializedName("scored") public int scored;
        @SerializedName("conceded") public int conceded;
    }
}
