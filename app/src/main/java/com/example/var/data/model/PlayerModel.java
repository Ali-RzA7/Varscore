package com.example.var.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.Calendar;

/**
 * PlayerModel - Oyuncu bilgilerini temsil eden veri sınıfı.
 * /sport/football/player?teamId=X endpoint'inden gelen veri.
 */
public class PlayerModel {

    @SerializedName("playerId")
    private String playerId;

    @SerializedName("name")
    private String name;

    @SerializedName("birthday")
    private String birthday;

    @SerializedName("height")
    private String height;

    @SerializedName("country")
    private String country;

    @SerializedName("feet")
    private String feet;

    @SerializedName("weight")
    private String weight;

    @SerializedName("photo")
    private String photo;

    @SerializedName("value")
    private String value;

    @SerializedName("teamId")
    private String teamId;

    @SerializedName("position")
    private String position;

    @SerializedName("number")
    private int number;

    @SerializedName("PAC")
    private int pac;

    @SerializedName("SHO")
    private int sho;

    @SerializedName("PAS")
    private int pas;

    @SerializedName("DRI")
    private int dri;

    @SerializedName("DEF")
    private int def;

    @SerializedName("PHY")
    private int phy;

    /**
     * Doğum tarihinden yaşı hesaplar.
     * @return Yaş (yıl olarak), hesaplanamıyorsa 0
     */
    public int getAge() {
        if (birthday == null || birthday.isEmpty()) return 0;
        try {
            String[] parts = birthday.split("-");
            if (parts.length < 1) return 0;
            int birthYear = Integer.parseInt(parts[0]);
            int birthMonth = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
            int birthDay = parts.length > 2 ? Integer.parseInt(parts[2]) : 1;

            Calendar today = Calendar.getInstance();
            int age = today.get(Calendar.YEAR) - birthYear;
            if (today.get(Calendar.MONTH) + 1 < birthMonth ||
                    (today.get(Calendar.MONTH) + 1 == birthMonth &&
                     today.get(Calendar.DAY_OF_MONTH) < birthDay)) {
                age--;
            }
            return age;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ===== Getters =====

    public String getPlayerId() { return playerId; }
    public String getName() { return name; }

    // ===== Setters (lineup fallback için) =====

    public void setPlayerId(String v) { this.playerId = v; }
    public void setName(String v) { this.name = v; }
    public void setNumber(int v) { this.number = v; }
    public void setPosition(String v) { this.position = v; }
    public void setPhoto(String v) { this.photo = v; }
    public String getBirthday() { return birthday; }
    public String getHeight() { return height; }
    public String getCountry() { return country; }
    public String getFeet() { return feet; }
    public String getWeight() { return weight; }
    public String getPhoto() { return photo; }
    public String getValue() { return value; }
    public String getTeamId() { return teamId; }
    public String getPosition() { return position; }
    public int getNumber() { return number; }
    public int getPac() { return pac; }
    public int getSho() { return sho; }
    public int getPas() { return pas; }
    public int getDri() { return dri; }
    public int getDef() { return def; }
    public int getPhy() { return phy; }
}
