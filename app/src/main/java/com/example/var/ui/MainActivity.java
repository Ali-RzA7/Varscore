package com.example.var.ui;

import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.var.R;
import com.example.var.ui.fragment.AccountFragment;
import com.example.var.ui.fragment.HomeFragment;
import com.example.var.ui.fragment.StandingsFragment;
import com.example.var.util.MatchMonitorWorker;
import com.example.var.util.NotificationHelper;
import com.example.var.util.PreferencesManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.concurrent.TimeUnit;

/**
 * MainActivity - Uygulamanın tek Activity'si.
 *
 * Fragment container olarak çalışır. Tüm ekranlar Fragment olarak gösterilir.
 * BottomNavigationView ile 3 ana sekme arasında geçiş yapılır:
 *
 * 1. nav_home      → HomeFragment   (Maçlar, canlı skor, tarih seçici)
 * 2. nav_standings → StandingsFragment (Puan tabloları, lig listesi)
 * 3. nav_account   → AccountFragment  (Giriş/Profil - auth durumuna göre)
 *
 * Özellikler:
 * - Uygulama açılışında tema ve dil tercihleri uygulanır
 * - Bottom nav'dan sekme geçişinde animasyon kullanılmaz (hızlı geçiş)
 * - MatchDetailFragment, TeamMatchesFragment vb. Back Stack üzerinden açılır
 * - navigateToAccount() metodu, diğer fragment'lardan Hesap sekmesine yönlendirme sağlar
 */
public class MainActivity extends AppCompatActivity {

    /** Kullanıcı tercihleri yöneticisi */
    private PreferencesManager preferencesManager;

    /** Alt navigasyon çubuğu */
    private BottomNavigationView bottomNav;

    @Override
    protected void attachBaseContext(Context newBase) {
        // Dil tercihini Activity oluşturulmadan önce uygula
        // Bu sayede tüm string kaynakları doğru dille yüklenir
        PreferencesManager prefs = new PreferencesManager(newBase);
        super.attachBaseContext(prefs.applyLanguage(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Tema tercihini Content View'dan önce uygula
        preferencesManager = new PreferencesManager(this);
        preferencesManager.applyTheme();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottomNav);

        NotificationHelper.createChannels(this);
        scheduleMatchMonitor();

        setupBottomNavigation();

        // İlk açılışta (savedInstanceState null ise) HomeFragment'ı göster
        // Activity yeniden oluşturulduğunda (tema/dil değişikliği) mevcut state korunur
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), false);
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    /**
     * Alt navigasyon çubuğunu yapılandırır.
     * Her sekme için ilgili Fragment yüklenir.
     * Geçiş sırasında animasyon kullanılmaz (sekme geçişleri anlık olmalı).
     */
    private void setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                // Maçlar sekmesi
                loadFragment(new HomeFragment(), false);
                return true;

            } else if (itemId == R.id.nav_standings) {
                // Puan Durumu sekmesi
                loadFragment(new StandingsFragment(), false);
                return true;

            } else if (itemId == R.id.nav_account) {
                // Hesap sekmesi (login/profil durumuna göre AccountFragment yönetir)
                AccountFragment accountFragment = new AccountFragment();
                loadFragment(accountFragment, false);
                return true;
            }
            return false;
        });
    }

    /**
     * Fragment'ı fragmentContainer'a yükler.
     * Back Stack'e EKLEMEZ - bottom nav sekme geçişleri back stack'e eklenmez.
     * İçerik fragment'ları (MatchDetail, TeamMatches) ise addToBackStack ile açılır.
     *
     * @param fragment Yüklenecek fragment
     * @param animate  Animasyon kullanılsın mı (bottom nav için false)
     */
    private void loadFragment(Fragment fragment, boolean animate) {
        // Back stack'i temizle (yeni sekmeye geçince önceki stack silinir)
        getSupportFragmentManager().popBackStack(null,
                androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);

        androidx.fragment.app.FragmentTransaction transaction =
                getSupportFragmentManager().beginTransaction();

        if (animate) {
            transaction.setCustomAnimations(
                    R.anim.slide_in_right, R.anim.slide_out_left,
                    R.anim.slide_in_left, R.anim.slide_out_right
            );
        }

        transaction.replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    /**
     * WorkManager ile arka planda periyodik maç izlemeyi başlatır.
     * Her 15 dakikada bir favori takımların maçlarını kontrol eder.
     * Unique work kullanıldığı için birden fazla kez çalıştırılmaz.
     */
    private void scheduleMatchMonitor() {
        PeriodicWorkRequest work = new PeriodicWorkRequest.Builder(
                MatchMonitorWorker.class, 15, TimeUnit.MINUTES)
                .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "match_monitor",
                ExistingPeriodicWorkPolicy.KEEP,
                work
        );
    }

    /**
     * Programatik olarak Hesap sekmesine yönlendirir.
     * StandingsFragment'ta "Giriş Yap" SnackBar aksiyonundan çağrılır.
     */
    public void navigateToAccount() {
        bottomNav.setSelectedItemId(R.id.nav_account);
    }
}
