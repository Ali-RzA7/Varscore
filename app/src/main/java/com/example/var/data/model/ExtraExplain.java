package com.example.var.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * ExtraExplain - Uzatma, penaltı gibi ekstra maç bilgilerini tutan model.
 * MatchModel içinde gömülü nesne olarak kullanılır.
 * API yanıtındaki "extraExplain" alanını temsil eder.
 */
public class ExtraExplain {

    /** Başlangıç vuruşu: 1=Ev sahibi, 2=Deplasman */
    @SerializedName("kickOff")
    private int kickOff;

    /** Normal sürede oynanan dakika */
    @SerializedName("minute")
    private int minute;

    /** Normal süre ev sahibi skoru */
    @SerializedName("homeScore")
    private int homeScore;

    /** Normal süre deplasman skoru */
    @SerializedName("awayScore")
    private int awayScore;

    /** Uzatma durumu: 1=Normal uzatma bitti, 2=Özel maç uzatma, 3=Uzatma devam ediyor */
    @SerializedName("extraTimeStatus")
    private int extraTimeStatus;

    /** Uzatma ev sahibi skoru */
    @SerializedName("extraHomeScore")
    private int extraHomeScore;

    /** Uzatma deplasman skoru */
    @SerializedName("extraAwayScore")
    private int extraAwayScore;

    /** Penaltı ev sahibi skoru */
    @SerializedName("penHomeScore")
    private int penHomeScore;

    /** Penaltı deplasman skoru */
    @SerializedName("penAwayScore")
    private int penAwayScore;

    /** Çift maç toplam ev sahibi skoru */
    @SerializedName("twoRoundsHomeScore")
    private int twoRoundsHomeScore;

    /** Çift maç toplam deplasman skoru */
    @SerializedName("twoRoundsAwayScore")
    private int twoRoundsAwayScore;

    /** Kazanan: 1=Ev sahibi, 2=Deplasman */
    @SerializedName("winner")
    private int winner;

    // ===== Getter ve Setter Metotları =====

    public int getKickOff() { return kickOff; }
    public void setKickOff(int kickOff) { this.kickOff = kickOff; }

    public int getMinute() { return minute; }
    public void setMinute(int minute) { this.minute = minute; }

    public int getHomeScore() { return homeScore; }
    public void setHomeScore(int homeScore) { this.homeScore = homeScore; }

    public int getAwayScore() { return awayScore; }
    public void setAwayScore(int awayScore) { this.awayScore = awayScore; }

    public int getExtraTimeStatus() { return extraTimeStatus; }
    public void setExtraTimeStatus(int extraTimeStatus) { this.extraTimeStatus = extraTimeStatus; }

    public int getExtraHomeScore() { return extraHomeScore; }
    public void setExtraHomeScore(int extraHomeScore) { this.extraHomeScore = extraHomeScore; }

    public int getExtraAwayScore() { return extraAwayScore; }
    public void setExtraAwayScore(int extraAwayScore) { this.extraAwayScore = extraAwayScore; }

    public int getPenHomeScore() { return penHomeScore; }
    public void setPenHomeScore(int penHomeScore) { this.penHomeScore = penHomeScore; }

    public int getPenAwayScore() { return penAwayScore; }
    public void setPenAwayScore(int penAwayScore) { this.penAwayScore = penAwayScore; }

    public int getTwoRoundsHomeScore() { return twoRoundsHomeScore; }
    public void setTwoRoundsHomeScore(int twoRoundsHomeScore) { this.twoRoundsHomeScore = twoRoundsHomeScore; }

    public int getTwoRoundsAwayScore() { return twoRoundsAwayScore; }
    public void setTwoRoundsAwayScore(int twoRoundsAwayScore) { this.twoRoundsAwayScore = twoRoundsAwayScore; }

    public int getWinner() { return winner; }
    public void setWinner(int winner) { this.winner = winner; }
}
