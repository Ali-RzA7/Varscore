package com.example.var.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * TeamProfileModel - Takım profil bilgilerini temsil eden veri sınıfı.
 * /sport/football/team?teamId=X endpoint'inden gelen veri.
 */
public class TeamProfileModel {

    @SerializedName("teamId")
    private String teamId;

    @SerializedName("leagueId")
    private String leagueId;

    @SerializedName("name")
    private String name;

    @SerializedName("logo")
    private String logo;

    @SerializedName("foundingDate")
    private String foundingDate;

    @SerializedName("address")
    private String address;

    @SerializedName("area")
    private String area;

    @SerializedName("venue")
    private String venue;

    @SerializedName("capacity")
    private String capacity;

    @SerializedName("coach")
    private String coach;

    @SerializedName("website")
    private String website;

    @SerializedName("isNational")
    private boolean isNational;

    @SerializedName("country_logo")
    private String countryLogo;

    // ===== Getters =====

    public String getTeamId() { return teamId; }
    public String getLeagueId() { return leagueId; }
    public String getName() { return name; }
    public String getLogo() { return logo; }
    public String getFoundingDate() { return foundingDate; }
    public String getAddress() { return address; }
    public String getArea() { return area; }
    public String getVenue() { return venue; }
    public String getCapacity() { return capacity; }
    public String getCoach() { return coach; }
    public String getWebsite() { return website; }
    public boolean isNational() { return isNational; }
    public String getCountryLogo() { return countryLogo; }
}
