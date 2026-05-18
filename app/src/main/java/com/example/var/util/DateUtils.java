package com.example.var.util;

import android.content.Context;

import com.example.var.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * DateUtils - Tarih formatlama ve dönüştürme yardımcı sınıfı.
 * Uygulama genelinde tarih işlemleri bu sınıf üzerinden yapılır.
 *
 * Önemli: API unix timestamp kullanır (saniye cinsinden).
 * Java/Android milisaniye kullanır, bu yüzden x1000 dönüşümü gerekir.
 */
public class DateUtils {

    /** API tarih formatı */
    private static final String API_DATE_FORMAT = "yyyy-MM-dd";

    /** Gösterim tarih formatı */
    private static final String DISPLAY_DATE_FORMAT = "dd MMM";

    /** Saat formatı */
    private static final String TIME_FORMAT = "HH:mm";

    /**
     * Calendar nesnesini API formatına dönüştürür.
     * @param calendar Dönüştürülecek tarih
     * @return "yyyy-MM-dd" formatında tarih string'i
     */
    public static String formatForApi(Calendar calendar) {
        SimpleDateFormat sdf = new SimpleDateFormat(API_DATE_FORMAT, Locale.US);
        return sdf.format(calendar.getTime());
    }

    /**
     * API formatındaki tarih string'ini Calendar'a dönüştürür.
     * @param dateStr "yyyy-MM-dd" formatında tarih
     * @return Calendar nesnesi, parse hatasında bugünün tarihi
     */
    public static Calendar parseApiDate(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(API_DATE_FORMAT, Locale.US);
            Date date = sdf.parse(dateStr);
            Calendar cal = Calendar.getInstance();
            if (date != null) cal.setTime(date);
            return cal;
        } catch (Exception e) {
            return Calendar.getInstance();
        }
    }

    /**
     * Unix timestamp'i saat formatına dönüştürür.
     * API'den gelen matchTime (saniye cinsinden) -> "HH:mm" formatı.
     *
     * @param timestamp Unix timestamp (saniye)
     * @return "HH:mm" formatında saat string'i
     */
    public static String formatMatchTime(long timestamp) {
        if (timestamp == 0) return "--:--";
        SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT, Locale.US);
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(new Date(timestamp * 1000));
    }

    /**
     * Calendar nesnesini kullanıcıya gösterilecek formata dönüştürür.
     * @param calendar Dönüştürülecek tarih
     * @return "dd MMM" formatında tarih string'i (örn: "06 May")
     */
    public static String formatForDisplay(Calendar calendar) {
        SimpleDateFormat sdf = new SimpleDateFormat(DISPLAY_DATE_FORMAT, Locale.US);
        return sdf.format(calendar.getTime());
    }

    /**
     * Verilen tarihin bugün olup olmadığını kontrol eder.
     * @param calendar Kontrol edilecek tarih
     * @return Bugün ise true
     */
    public static boolean isToday(Calendar calendar) {
        Calendar today = Calendar.getInstance();
        return calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                && calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * Haftanın gün adını kısa formatta döndürür.
     * @param context Context (string kaynaklarına erişim için)
     * @param calendar Tarih
     * @return Kısa gün adı (örn: "Pzt", "Mon")
     */
    public static String getDayShortName(Context context, Calendar calendar) {
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        switch (dayOfWeek) {
            case Calendar.MONDAY: return context.getString(R.string.day_mon);
            case Calendar.TUESDAY: return context.getString(R.string.day_tue);
            case Calendar.WEDNESDAY: return context.getString(R.string.day_wed);
            case Calendar.THURSDAY: return context.getString(R.string.day_thu);
            case Calendar.FRIDAY: return context.getString(R.string.day_fri);
            case Calendar.SATURDAY: return context.getString(R.string.day_sat);
            case Calendar.SUNDAY: return context.getString(R.string.day_sun);
            default: return "";
        }
    }

    /**
     * Gün numarasını string olarak döndürür.
     * @param calendar Tarih
     * @return Gün numarası string (örn: "06", "15")
     */
    public static String getDayNumber(Calendar calendar) {
        return String.format(Locale.US, "%02d", calendar.get(Calendar.DAY_OF_MONTH));
    }

    /**
     * Ayın kısa adını döndürür.
     * @param context Context
     * @param calendar Tarih
     * @return Kısa ay adı (örn: "May", "Oca")
     */
    public static String getMonthShortName(Context context, Calendar calendar) {
        int month = calendar.get(Calendar.MONTH);
        int[] monthResIds = {
                R.string.month_jan, R.string.month_feb, R.string.month_mar,
                R.string.month_apr, R.string.month_may, R.string.month_jun,
                R.string.month_jul, R.string.month_aug, R.string.month_sep,
                R.string.month_oct, R.string.month_nov, R.string.month_dec
        };
        return context.getString(monthResIds[month]);
    }

    /**
     * Maç durumunu (status) okunabilir metne dönüştürür.
     * @param context Context
     * @param status  API'den gelen status kodu
     * @return Okunabilir durum metni
     */
    public static String getStatusText(Context context, int status) {
        switch (status) {
            case 0: return context.getString(R.string.status_not_started);
            case 1: return context.getString(R.string.status_first_half);
            case 2: return context.getString(R.string.status_half_time);
            case 3: return context.getString(R.string.status_second_half);
            case 4: return context.getString(R.string.status_extra_time);
            case 5: return context.getString(R.string.status_penalties);
            case -1: return context.getString(R.string.status_finished);
            case -10: return context.getString(R.string.status_cancelled);
            case -11: return context.getString(R.string.status_tbd);
            case -12: return context.getString(R.string.status_finished);
            case -13: return context.getString(R.string.status_interrupted);
            case -14: return context.getString(R.string.status_postponed);
            default: return context.getString(R.string.status_not_started);
        }
    }
}
