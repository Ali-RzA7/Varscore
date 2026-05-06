package com.example.var.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * ExtraExplain - Uzatma, penaltı gibi ekstra maç bilgilerini tutan model.
 */
public class ExtraExplain implements Serializable {

    @SerializedName("kickOff")
    private int kickOff;

    @SerializedName("minute")
    private int minute;

    @SerializedName("homeScore")
    private int homeScore;

    @SerializedName("awayScore")
    private int awayScore;

    @SerializedName("extraTimeStatus")
    private int extraTimeStatus;

    @SerializedName("extraHomeScore")
    private int extraHomeScore;

    @SerializedName("extraAwayScore")
    private int extraAwayScore;

    @SerializedName("penHomeScore")
    private int penHomeScore;

    @SerializedName("penAwayScore")
    private int penAwayScore;

    @SerializedName("twoRoundsHomeScore")
    private int twoRoundsHomeScore;

    @SerializedName("twoRoundsAwayScore")
    private int twoRoundsAwayScore;

    @SerializedName("winner")
    private int winner;

    public int getKickOff() { return kickOff; }
    public int getMinute() { return minute; }
    public int getHomeScore() { return homeScore; }
    public int getAwayScore() { return awayScore; }
    public int getExtraTimeStatus() { return extraTimeStatus; }
    public int getExtraHomeScore() { return extraHomeScore; }
    public int getExtraAwayScore() { return extraAwayScore; }
    public int getPenHomeScore() { return penHomeScore; }
    public int getPenAwayScore() { return penAwayScore; }
    public int getTwoRoundsHomeScore() { return twoRoundsHomeScore; }
    public int getTwoRoundsAwayScore() { return twoRoundsAwayScore; }
    public int getWinner() { return winner; }
}
