package com.example.var.data.model;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * AnalysisResponse - Ham JSON yapısını yakalamak için basitleştirilmiş sınıf.
 */
public class AnalysisResponse implements Serializable {

    @SerializedName("code")
    private int code;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private JsonElement data;

    public int getCode() { return code; }
    public String getMessage() { return message; }
    public JsonElement getData() { return data; }
}
