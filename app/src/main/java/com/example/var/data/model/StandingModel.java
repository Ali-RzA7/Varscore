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

    /** Takımın tam adı */
    @SerializedName("teamName")
    private String teamName;

    /** Oynanan toplam maç sayısı */
    @SerializedName("played")
    private int played;

    /** Kazanılan maç sayısı */
    @SerializedName("won")
    private int won;

    /** Beraberlik sayısı */
    @SerializedName("drawn")
    private int drawn;

    /** Kaybedilen maç sayısı */
    @SerializedName("lost")
    private int lost;

    /** Atılan gol sayısı */
    @SerializedName("goalsFor")
    private int goalsFor;

    /** Yenilen gol sayısı */
    @SerializedName("goalsAgainst")
    private int goalsAgainst;

    /** Kazanılan toplam puan */
    @SerializedName("points")
    private int points;

    // ===== Getter Metodları =====

    /** @return Takımın ligteki sırası */
    public int getRank() { return rank; }

    /** @return Takımın benzersiz ID'si */
    public String getTeamId() { return teamId; }

    /** @return Takımın tam adı */
    public String getTeamName() { return teamName; }

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
    public int getGoalDifference() {
        return goalsFor - goalsAgainst;
    }
}
