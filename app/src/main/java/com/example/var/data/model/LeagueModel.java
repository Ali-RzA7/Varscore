package com.example.var.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * LeagueModel - Lig/kupa bilgisini temsil eden veri sınıfı.
 * iSportsAPI /league/basic endpoint'inden dönen verileri tutar.
 * Arama fonksiyonunda lig araması için kullanılır.
 */
public class LeagueModel {

    /** Lig benzersiz kimliği */
    @SerializedName("leagueId")
    private String leagueId;

    /** Lig tam adı (örn: "Turkey Super League") */
    @SerializedName("name")
    private String name;

    /** Lig kısa adı (örn: "TUR SL") */
    @SerializedName("shortName")
    private String shortName;

    /** Lig tipi: 1=Lig, 2=Kupa */
    @SerializedName("type")
    private int type;

    /** Alt lig adı (örn: "Play Off") */
    @SerializedName("subLeagueName")
    private String subLeagueName;

    // ===== Getter ve Setter Metotları =====

    public String getLeagueId() { return leagueId; }
    public void setLeagueId(String leagueId) { this.leagueId = leagueId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getShortName() { return shortName; }
    public void setShortName(String shortName) { this.shortName = shortName; }

    public int getType() { return type; }
    public void setType(int type) { this.type = type; }

    public String getSubLeagueName() { return subLeagueName; }
    public void setSubLeagueName(String subLeagueName) { this.subLeagueName = subLeagueName; }
}
