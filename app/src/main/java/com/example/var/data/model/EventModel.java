package com.example.var.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * EventModel - Maç olaylarını (gol, kart, oyuncu değişikliği) temsil eder.
 */
public class EventModel {
    @SerializedName("type")
    private int type; 

    @SerializedName("time")
    private String time; 

    @SerializedName("homeEvent")
    private int homeEvent; // 1: Ev, 0: Deplasman (Genelde int gelir API'den)

    @SerializedName("playerName")
    private String playerName;

    @SerializedName("playerNameIn")
    private String playerNameIn;

    @SerializedName("playerNameOut")
    private String playerNameOut;

    @SerializedName("content")
    private String content;

    public int getType() { return type; }
    public String getTime() { return time; }
    public boolean isHomeEvent() { return homeEvent == 1; }
    public String getPlayerName() { return playerName; }
    public String getPlayerNameIn() { return playerNameIn; }
    public String getPlayerNameOut() { return playerNameOut; }
    public String getContent() { return content; }
}
