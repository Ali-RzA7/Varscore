package com.example.var.data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * ApiResponse - iSportsAPI'nin genel yanıt sarmalayıcı (wrapper) sınıfı.
 * Tüm API endpoint'leri bu formatta yanıt döner:
 * {
 *   "code": 0,        -> Başarı durumu (0 = başarılı)
 *   "message": "...",  -> Durum mesajı
 *   "data": [...]      -> Veri listesi
 * }
 *
 * @param <T> API yanıtındaki "data" listesinin eleman tipi
 */
public class ApiResponse<T> {

    /** API yanıt kodu: 0 = başarılı */
    @SerializedName("code")
    private int code;

    /** API yanıt mesajı */
    @SerializedName("message")
    private String message;

    /** API yanıt verisi (liste) */
    @SerializedName("data")
    private List<T> data;

    /**
     * API yanıtının başarılı olup olmadığını kontrol eder.
     * @return code == 0 ise true
     */
    public boolean isSuccess() {
        return code == 0;
    }

    // ===== Getter ve Setter Metotları =====

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<T> getData() { return data; }
    public void setData(List<T> data) { this.data = data; }
}
