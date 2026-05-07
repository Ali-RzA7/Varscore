package com.example.var.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.var.R;
import com.example.var.util.FirebaseManager;

/**
 * AccountFragment - Hesap sekmesinin yönlendirici fragment'ı.
 *
 * Alt navigasyondan "Hesap" sekmesine gelindiğinde her zaman bu fragment gösterilir.
 * Kullanıcının giriş durumuna göre içerik dinamik olarak belirlenir:
 *
 * - Giriş yapılmamış → LoginFragment gösterilir
 * - Giriş yapılmış   → ProfileFragment gösterilir
 *
 * Bu yaklaşım sayesinde MainActivity, kimlik doğrulama durumunu takip etmek zorunda kalmaz.
 * Giriş/çıkış işlemlerinden sonra onAuthStateChanged() çağrılarak içerik güncellenir.
 *
 * Kullanım:
 * - MainActivity: BottomNavigationView'dan "Hesap" seçilince AccountFragment gösterilir
 * - LoginFragment başarılı girişte: onAuthStateChanged() çağırır
 * - ProfileFragment çıkışta: onAuthStateChanged() çağırır
 */
public class AccountFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        // Kendine ait layout yok - sadece bir container olarak çalışır
        // İçeriği çocuk fragment belirler
        return inflater.inflate(R.layout.fragment_account, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Başlangıçta auth durumuna göre doğru fragment'ı göster
        showAppropriateFragment(false);
    }

    /**
     * Firebase Authentication durumuna göre LoginFragment veya ProfileFragment gösterir.
     * Bu metot hem başlangıçta hem de auth durumu değiştiğinde çağrılır.
     *
     * @param animate Geçiş animasyonu kullanılsın mı?
     *                false: İlk açılışta animasyon istenmiyor
     *                true: Giriş/çıkış sonrası animasyonlu geçiş
     */
    public void showAppropriateFragment(boolean animate) {
        if (!isAdded()) return;

        // Auth durumunu kontrol et
        Fragment targetFragment = FirebaseManager.isLoggedIn()
                ? new ProfileFragment()    // Giriş yapılmış → Profil göster
                : new LoginFragment();     // Giriş yapılmamış → Giriş formu göster

        FragmentTransaction transaction = getChildFragmentManager()
                .beginTransaction();

        // Animasyonlu geçiş isteğe bağlı
        if (animate) {
            transaction.setCustomAnimations(
                    R.anim.slide_in_right, R.anim.slide_out_left,
                    R.anim.slide_in_left, R.anim.slide_out_right
            );
        }

        // Çocuk fragment container'ına yükle
        // Not: child fragment manager kullanımı, AccountFragment'ın kendi back stack'ini yönetir
        transaction.replace(R.id.accountContainer, targetFragment)
                .commit();
    }

    /**
     * Kimlik doğrulama durumu değiştiğinde çağrılır.
     * LoginFragment'tan (başarılı giriş) veya ProfileFragment'tan (çıkış) tetiklenir.
     * Animasyonlu geçiş ile doğru fragment'a geçer.
     */
    public void onAuthStateChanged() {
        showAppropriateFragment(true);
    }
}
