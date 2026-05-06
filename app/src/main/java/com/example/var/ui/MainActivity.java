package com.example.var.ui;

import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.var.R;
import com.example.var.ui.fragment.HomeFragment;
import com.example.var.util.PreferencesManager;

/**
 * MainActivity - Uygulamanın ana Activity'si.
 * Fragment container olarak çalışır ve HomeFragment'ı barındırır.
 * Uygulama açılışında tema ve dil tercihlerini uygular.
 */
public class MainActivity extends AppCompatActivity {

    private PreferencesManager preferencesManager;

    @Override
    protected void attachBaseContext(Context newBase) {
        // Dil tercihini uygula (Activity oluşturulmadan önce)
        PreferencesManager prefs = new PreferencesManager(newBase);
        super.attachBaseContext(prefs.applyLanguage(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Tema tercihini uygula
        preferencesManager = new PreferencesManager(this);
        preferencesManager.applyTheme();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // İlk açılışta HomeFragment'ı yükle
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new HomeFragment())
                    .commit();
        }
    }
}
