package com.example.var.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class EventsResponse {

    @SerializedName("matchId")
    private String matchId;

    @SerializedName("events")
    private List<EventModel> events;

    public String getMatchId() { return matchId; }
    public List<EventModel> getEvents() { return events; }
}
