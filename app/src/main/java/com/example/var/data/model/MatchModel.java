package com.example.var.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * MatchModel - API'den gelen maç verisini temsil eden veri sınıfı.
 * iSportsAPI /schedule ve /livescores endpoint'lerinden dönen
 * JSON verilerini Java nesnesine dönüştürmek için kullanılır.
 *
 * API Yanıt Alanları:
 * - matchId: Maçın benzersiz kimliği
 * - leagueId/leagueName: Lig bilgileri
 * - status: Maç durumu (-1: bitti, 0: başlamadı, 1: ilk yarı, vb.)
 * - homeScore/awayScore: Ev sahibi ve deplasman skorları
 * - homeRed/awayRed: Kırmızı kart sayıları
 * - homeYellow/awayYellow: Sarı kart sayıları
 * - homeCorner/awayCorner: Korner sayıları
 */
public class MatchModel {

    // ===== Maç Kimliği =====
    @SerializedName("matchId")
    private String matchId;

    // ===== Lig Bilgileri =====
    @SerializedName("leagueId")
    private String leagueId;

    @SerializedName("leagueType")
    private int leagueType;

    @SerializedName("leagueName")
    private String leagueName;

    @SerializedName("leagueShortName")
    private String leagueShortName;

    @SerializedName("leagueColor")
    private String leagueColor;

    // ===== Maç Zamanı =====
    @SerializedName("matchTime")
    private long matchTime;

    @SerializedName("halfStartTime")
    private long halfStartTime;

    // ===== Maç Durumu =====
    /** status: 0=Başlamadı, 1=İlk Yarı, 2=Devre Arası, 3=İkinci Yarı, 4=Uzatma, 5=Penaltı, -1=Bitti */
    @SerializedName("status")
    private int status;

    // ===== Takım Bilgileri =====
    @SerializedName("homeId")
    private String homeId;

    @SerializedName("homeName")
    private String homeName;

    @SerializedName("awayId")
    private String awayId;

    @SerializedName("awayName")
    private String awayName;

    // ===== Skor Bilgileri =====
    @SerializedName("homeScore")
    private int homeScore;

    @SerializedName("awayScore")
    private int awayScore;

    @SerializedName("homeHalfScore")
    private int homeHalfScore;

    @SerializedName("awayHalfScore")
    private int awayHalfScore;

    // ===== Kart Bilgileri =====
    @SerializedName("homeRed")
    private int homeRed;

    @SerializedName("awayRed")
    private int awayRed;

    @SerializedName("homeYellow")
    private int homeYellow;

    @SerializedName("awayYellow")
    private int awayYellow;

    // ===== Korner Bilgileri =====
    @SerializedName("homeCorner")
    private int homeCorner;

    @SerializedName("awayCorner")
    private int awayCorner;

    // ===== Takım Sıralama =====
    @SerializedName("homeRank")
    private String homeRank;

    @SerializedName("awayRank")
    private String awayRank;

    // ===== Sezon ve Tur Bilgileri =====
    @SerializedName("season")
    private String season;

    @SerializedName("round")
    private String round;

    @SerializedName("group")
    private String group;

    // ===== Mekan ve Hava Durumu =====
    @SerializedName("location")
    private String location;

    @SerializedName("weather")
    private String weather;

    @SerializedName("temperature")
    private String temperature;

    // ===== Ek Bilgiler =====
    @SerializedName("explain")
    private String explain;

    @SerializedName("extraExplain")
    private ExtraExplain extraExplain;

    @SerializedName("hasLineup")
    private boolean hasLineup;

    @SerializedName("neutral")
    private boolean neutral;

    @SerializedName("injuryTime")
    private int injuryTime;

    @SerializedName("updateTime")
    private long updateTime;

    // ===== Yardımcı Metotlar =====

    /**
     * Maçın canlı olup olmadığını kontrol eder.
     * status 1-5 arası ise maç devam ediyordur.
     */
    public boolean isLive() {
        return status >= 1 && status <= 5;
    }

    /**
     * Maçın bitip bitmediğini kontrol eder.
     */
    public boolean isFinished() {
        return status == -1;
    }

    /**
     * Maçın henüz başlamamış olduğunu kontrol eder.
     */
    public boolean isNotStarted() {
        return status == 0;
    }

    /**
     * Maçın iptal/ertelenmiş/yarıda kesilmiş olup olmadığını kontrol eder.
     */
    public boolean isCancelled() {
        return status <= -10;
    }

    // ===== Getter ve Setter Metotları =====

    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }

    public String getLeagueId() { return leagueId; }
    public void setLeagueId(String leagueId) { this.leagueId = leagueId; }

    public int getLeagueType() { return leagueType; }
    public void setLeagueType(int leagueType) { this.leagueType = leagueType; }

    public String getLeagueName() { return leagueName; }
    public void setLeagueName(String leagueName) { this.leagueName = leagueName; }

    public String getLeagueShortName() { return leagueShortName; }
    public void setLeagueShortName(String leagueShortName) { this.leagueShortName = leagueShortName; }

    public String getLeagueColor() { return leagueColor; }
    public void setLeagueColor(String leagueColor) { this.leagueColor = leagueColor; }

    public long getMatchTime() { return matchTime; }
    public void setMatchTime(long matchTime) { this.matchTime = matchTime; }

    public long getHalfStartTime() { return halfStartTime; }
    public void setHalfStartTime(long halfStartTime) { this.halfStartTime = halfStartTime; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getHomeId() { return homeId; }
    public void setHomeId(String homeId) { this.homeId = homeId; }

    public String getHomeName() { return homeName; }
    public void setHomeName(String homeName) { this.homeName = homeName; }

    public String getAwayId() { return awayId; }
    public void setAwayId(String awayId) { this.awayId = awayId; }

    public String getAwayName() { return awayName; }
    public void setAwayName(String awayName) { this.awayName = awayName; }

    public int getHomeScore() { return homeScore; }
    public void setHomeScore(int homeScore) { this.homeScore = homeScore; }

    public int getAwayScore() { return awayScore; }
    public void setAwayScore(int awayScore) { this.awayScore = awayScore; }

    public int getHomeHalfScore() { return homeHalfScore; }
    public void setHomeHalfScore(int homeHalfScore) { this.homeHalfScore = homeHalfScore; }

    public int getAwayHalfScore() { return awayHalfScore; }
    public void setAwayHalfScore(int awayHalfScore) { this.awayHalfScore = awayHalfScore; }

    public int getHomeRed() { return homeRed; }
    public void setHomeRed(int homeRed) { this.homeRed = homeRed; }

    public int getAwayRed() { return awayRed; }
    public void setAwayRed(int awayRed) { this.awayRed = awayRed; }

    public int getHomeYellow() { return homeYellow; }
    public void setHomeYellow(int homeYellow) { this.homeYellow = homeYellow; }

    public int getAwayYellow() { return awayYellow; }
    public void setAwayYellow(int awayYellow) { this.awayYellow = awayYellow; }

    public int getHomeCorner() { return homeCorner; }
    public void setHomeCorner(int homeCorner) { this.homeCorner = homeCorner; }

    public int getAwayCorner() { return awayCorner; }
    public void setAwayCorner(int awayCorner) { this.awayCorner = awayCorner; }

    public String getHomeRank() { return homeRank; }
    public void setHomeRank(String homeRank) { this.homeRank = homeRank; }

    public String getAwayRank() { return awayRank; }
    public void setAwayRank(String awayRank) { this.awayRank = awayRank; }

    public String getSeason() { return season; }
    public void setSeason(String season) { this.season = season; }

    public String getRound() { return round; }
    public void setRound(String round) { this.round = round; }

    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getWeather() { return weather; }
    public void setWeather(String weather) { this.weather = weather; }

    public String getTemperature() { return temperature; }
    public void setTemperature(String temperature) { this.temperature = temperature; }

    public String getExplain() { return explain; }
    public void setExplain(String explain) { this.explain = explain; }

    public ExtraExplain getExtraExplain() { return extraExplain; }
    public void setExtraExplain(ExtraExplain extraExplain) { this.extraExplain = extraExplain; }

    public boolean isHasLineup() { return hasLineup; }
    public void setHasLineup(boolean hasLineup) { this.hasLineup = hasLineup; }

    public boolean isNeutral() { return neutral; }
    public void setNeutral(boolean neutral) { this.neutral = neutral; }

    public int getInjuryTime() { return injuryTime; }
    public void setInjuryTime(int injuryTime) { this.injuryTime = injuryTime; }

    public long getUpdateTime() { return updateTime; }
    public void setUpdateTime(long updateTime) { this.updateTime = updateTime; }
}
