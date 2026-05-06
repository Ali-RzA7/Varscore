package com.example.var.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * StatModel - Maç istatistiklerini (şut, korner, topla oynama vb.) temsil eder.
 */
public class StatModel {
    @SerializedName("type")
    private int type; // 3: Topla Oynama, 4: Şut, 5: İsabetli Şut, 6: Korner, 7: Ofsayt, 8: Faul...

    @SerializedName("home")
    private String homeValue;

    @SerializedName("away")
    private String awayValue;

    public StatModel() {
    }

    public StatModel(int type, String homeValue, String awayValue) {
        this.type = type;
        this.homeValue = homeValue;
        this.awayValue = awayValue;
    }

    public int getType() {
        return type;
    }

    public String getHomeValue() {
        return homeValue;
    }

    public String getAwayValue() {
        return awayValue;
    }

    /**
     * İstatistik tipinin adını döndürür.
     */
    public String getTypeName() {
        switch (type) {
            case 3:
                return "Toplam Şut";
            case 4:
                return "İsabetli Şut";
            case 5:
                return "Faul";
            case 6:
                return "Korner";
            case 9:
            case 11:
                return "Sarı Kart";
            case 10:
            case 13:
                return "Kırmızı Kart";
            case 14:
                return "Topla Oynama %";
            case 19:
                return "Başarılı Top Kapma";
            case 20:
                return "Pas Arası";
            case 34:
                return "İsabetsiz Şut";
            case 37:
                return "Engellenen Şut";
            case 39:
                return "Başarılı Çalım";
            case 40:
                return "Taç Atışı";
            case 41:
                return "Toplam Pas";
            case 42:
                return "İsabetli Pas Oranı";
            case 43:
                return "Atak";
            case 44:
                return "Tehlikeli Atak";
            case 45:
                return "Serbest Vuruş";
            case 46:
                return "Birinci Yarı Topla Oynama";
            case 48:
                return "Kaçan Net Pozisyon";
            case 51:
                return "Kazanılan İkili Mücadele";
            case 52:
                return "Gol Beklentisi";
            case 57:
                return "Rakip Ceza Sahasında Topla Buluşma";
            case 60:
                return "Kazanılan Hava Topu";
            case 61:
                return "Uzaklaştırma";
            default:
                return null;
        }
    }

    /**
     * Progress bar için yüzde değeri hesaplar.
     */
    public int getHomePercentage() {
        try {
            float h = Float.parseFloat(homeValue.replace("%", ""));
            float a = Float.parseFloat(awayValue.replace("%", ""));
            if (h + a == 0)
                return 50;
            return Math.round((h / (h + a)) * 100);
        } catch (Exception e) {
            return 50;
        }
    }
}
