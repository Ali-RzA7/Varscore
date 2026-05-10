package com.example.var.data.model;

import com.example.var.R;
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
     * İstatistik tipinin adının string kaynak ID'sini döndürür.
     */
    public int getTypeNameResId() {
        switch (type) {
            case 3:
                return R.string.stat_total_shots;
            case 4:
                return R.string.stat_shots_on_goal;
            case 5:
                return R.string.stat_fouls;
            case 6:
                return R.string.stat_corners;
            case 9:
            case 11:
                return R.string.stat_yellow_cards;
            case 10:
            case 13:
                return R.string.stat_red_cards;
            case 14:
                return R.string.stat_possession;
            case 19:
                return R.string.stat_tackles;
            case 20:
                return R.string.stat_interceptions;
            case 34:
                return R.string.stat_shots_off_goal;
            case 37:
                return R.string.stat_blocked_shots;
            case 39:
                return R.string.stat_dribbles;
            case 40:
                return R.string.stat_throw_ins;
            case 41:
                return R.string.stat_total_passes;
            case 42:
                return R.string.stat_pass_accuracy;
            case 43:
                return R.string.stat_attacks;
            case 44:
                return R.string.stat_dangerous_attacks;
            case 45:
                return R.string.stat_free_kicks;
            case 46:
                return R.string.stat_possession_1h;
            case 48:
                return R.string.stat_big_chances_missed;
            case 51:
                return R.string.stat_duels_won;
            case 52:
                return R.string.stat_xg;
            case 57:
                return R.string.stat_touches_in_box;
            case 60:
                return R.string.stat_aerials_won;
            case 61:
                return R.string.stat_clearances;
            default:
                return 0;
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
