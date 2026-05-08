package com.example.var.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * StandingModel - Lig puan tablosundaki bir takımın verilerini temsil eder.
 *
 * iSportsAPI /league/table endpoint'inden dönen JSON nesnesiyle eşleşir.
 * Puan tablosunda gösterilecek tüm bilgileri içerir:
 * sıra, takım adı, oynanan maç sayısı, galibiyet/beraberlik/mağlubiyet,
 * atılan/yenilen gol ve puan bilgileri.
 *
 * Kullanım:
 * - LeagueStandingsFragment'ta RecyclerView'da listelenir
 * - StandingsTableAdapter tarafından görüntüye bağlanır
 */
public class StandingModel {

    /** Takımın ligdeki sırası (1'den başlar) */
    @SerializedName("rank")
    private int rank;

    /** Takımın benzersiz kimliği - takım maç listesi için kullanılır */
    @SerializedName("teamId")
    private String teamId;

    /** Takımın tam adı ve logosu - leagueInfo.teamInfos'tan manuel set edilir, JSON'da bu field yok */
    private String teamName;
    private String logoUrl;

    /** Oynanan toplam maç sayısı */
    @SerializedName("totalCount")
    private int played;

    /** Kazanılan maç sayısı */
    @SerializedName("winCount")
    private int won;

    /** Beraberlik sayısı */
    @SerializedName("drawCount")
    private int drawn;

    /** Kaybedilen maç sayısı */
    @SerializedName("loseCount")
    private int lost;

    /** Atılan gol sayısı */
    @SerializedName("getScore")
    private int goalsFor;

    /** Yenilen gol sayısı */
    @SerializedName("loseScore")
    private int goalsAgainst;

    /** Kazanılan toplam puan */
    @SerializedName("integral")
    private int points;

    /**
     * Son 6 maç formu: 0=Galibiyet, 1=Beraberlik, 2=Mağlubiyet, 3=Boş
     * (iSportsAPI /standing/league yanıtından gelir)
     */
    @SerializedName("recentFirstResult")  private int recentFirst;
    @SerializedName("recentSecondResult") private int recentSecond;
    @SerializedName("recentThirdResult")  private int recentThird;
    @SerializedName("recentFourthResult") private int recentFourth;
    @SerializedName("recentFifthResult")  private int recentFifth;
    @SerializedName("recentSixthResult")  private int recentSixth;

    // ===== Getter Metodları =====

    /** @return Takımın ligteki sırası */
    public int getRank() { return rank; }

    /** @return Takımın benzersiz ID'si */
    public String getTeamId() { return teamId; }

    /** @return Takımın tam adı */
    public String getTeamName() { return teamName; }

    public void setTeamName(String teamName) { this.teamName = teamName; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    /** @return Oynanan toplam maç sayısı */
    public int getPlayed() { return played; }

    /** @return Kazanılan maç sayısı */
    public int getWon() { return won; }

    /** @return Beraberlik sayısı */
    public int getDrawn() { return drawn; }

    /** @return Kaybedilen maç sayısı */
    public int getLost() { return lost; }

    /** @return Atılan gol sayısı */
    public int getGoalsFor() { return goalsFor; }

    /** @return Yenilen gol sayısı */
    public int getGoalsAgainst() { return goalsAgainst; }

    /** @return Kazanılan toplam puan */
    public int getPoints() { return points; }

    /**
     * Gol farkını hesaplar (atılan - yenilen).
     * Puan tablosunda GF sütunu için kullanılır.
     *
     * @return Pozitif sayı → gol avantajı, negatif → gol açığı
     */
    public int getGoalDifference() { return goalsFor - goalsAgainst; }

    /** Son 6 maç formu (en yeni = recentFirst). 0=G, 1=B, 2=M, 3=Boş */
    public int[] getRecentForm() {
        return new int[]{recentFirst, recentSecond, recentThird,
                         recentFourth, recentFifth, recentSixth};
    }
}
