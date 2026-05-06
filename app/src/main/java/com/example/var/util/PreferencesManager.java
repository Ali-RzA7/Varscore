package com.example.var.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import java.util.Locale;

/**
 * PreferencesManager - SharedPreferences yardımcı sınıfı.
 * Kullanıcı tercihlerini (tema, dil) kalıcı olarak saklar ve yönetir.
 *
 * Saklanan veriler:
 * - Koyu/açık tema tercihi
 * - Dil tercihi (TR/EN)
 */
public class PreferencesManager {

    /** SharedPreferences dosya adı */
    private static final String PREF_NAME = "varscore_preferences";

    /** Tema anahtarı */
    private static final String KEY_DARK_MODE = "dark_mode";

    /** Dil anahtarı */
    private static final String KEY_LANGUAGE = "language";

    /** SharedPreferences referansı */
    private final SharedPreferences preferences;

    /**
     * Constructor - Context üzerinden SharedPreferences'a erişir.
     * @param context Uygulama veya Activity context'i
     */
    public PreferencesManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ===== Tema Yönetimi =====

    /**
     * Koyu tema durumunu kaydeder ve hemen uygular.
     * @param isDarkMode true ise koyu tema, false ise açık tema
     */
    public void setDarkMode(boolean isDarkMode) {
        preferences.edit().putBoolean(KEY_DARK_MODE, isDarkMode).apply();
        // Temayı anında uygula
        AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    /**
     * Koyu tema tercihini döndürür.
     * @return true ise koyu tema aktif, false ise açık tema (varsayılan: false)
     */
    public boolean isDarkMode() {
        return preferences.getBoolean(KEY_DARK_MODE, false);
    }

    /**
     * Kayıtlı tema tercihini uygular.
     * Uygulama açılışında çağrılmalıdır.
     */
    public void applyTheme() {
        AppCompatDelegate.setDefaultNightMode(
                isDarkMode() ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    // ===== Dil Yönetimi =====

    /**
     * Dil tercihini kaydeder.
     * @param languageCode Dil kodu ("tr" veya "en")
     */
    public void setLanguage(String languageCode) {
        preferences.edit().putString(KEY_LANGUAGE, languageCode).apply();
    }

    /**
     * Kayıtlı dil tercihini döndürür.
     * @return Dil kodu (varsayılan: "tr")
     */
    public String getLanguage() {
        return preferences.getString(KEY_LANGUAGE, "tr");
    }

    /**
     * Kayıtlı dil tercihini uygulamaya uygular.
     * Activity'nin onCreate metodunda çağrılmalıdır.
     *
     * @param context Activity context'i
     * @return Dil ayarlanmış Context
     */
    public Context applyLanguage(Context context) {
        String language = getLanguage();
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        android.content.res.Configuration config = new android.content.res.Configuration(
                context.getResources().getConfiguration()
        );
        config.setLocale(locale);

        return context.createConfigurationContext(config);
    }
}
