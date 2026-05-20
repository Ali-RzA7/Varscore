package com.example.var.data.model;

import com.google.gson.annotations.SerializedName;

public class EventModel {

    @SerializedName("eventId")
    private String eventId;

    @SerializedName("type")
    private int type;

    // Yeni API: "minute" (int). Eski API: "time" (String) — her ikisi korunuyor.
    @SerializedName("minute")
    private int minute;

    @SerializedName("time")
    private String time;

    // Uzatma dakikası (örn. 45+3 → minute=45, overtime=3)
    @SerializedName("overtime")
    private int overtime;

    // Yeni API boolean; eski API int (1=ev). Gson boolean JSON'u okur.
    @SerializedName("homeEvent")
    private boolean homeEvent;

    @SerializedName("playerId")
    private String playerId;

    @SerializedName("playerName")
    private String playerName;

    @SerializedName("assistPlayerId")
    private String assistPlayerId;

    // Değişiklik olaylarında giren/çıkan oyuncu
    @SerializedName("playerNameIn")
    private String playerNameIn;

    @SerializedName("playerNameOut")
    private String playerNameOut;

    @SerializedName("content")
    private String content;

    // --- Getters ---

    public String getEventId() { return eventId; }
    public int getType() { return type; }
    public int getMinute() { return minute; }
    public String getTime() { return time; }
    public int getOvertime() { return overtime; }
    public boolean isHomeEvent() { return homeEvent; }
    public String getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public String getAssistPlayerId() { return assistPlayerId; }
    public String getPlayerNameIn() { return playerNameIn; }
    public String getPlayerNameOut() { return playerNameOut; }
    public String getContent() { return content; }

    /** "45'" veya "45+3'" formatında dakika döner. */
    public String getMinuteDisplay() {
        int min = minute > 0 ? minute : parseTimeSafe();
        if (overtime > 0) return min + "+" + overtime + "'";
        return min + "'";
    }

    private int parseTimeSafe() {
        if (time == null || time.isEmpty()) return 0;
        try { return Integer.parseInt(time.replaceAll("[^0-9]", "")); }
        catch (NumberFormatException e) { return 0; }
    }
}
