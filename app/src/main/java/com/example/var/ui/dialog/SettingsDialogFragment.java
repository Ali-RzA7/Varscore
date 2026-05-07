package com.example.var.ui.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.DialogFragment;

import com.example.var.R;
import com.example.var.util.PreferencesManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

/**
 * SettingsDialogFragment - Ayarlar dialog'u.
 *
 * Kullanıcının tema (koyu/açık) ve dil (TR/EN) tercihlerini yönetir.
 * Tercihler SharedPreferences'a anlık kaydedilir, ancak Activity'nin
 * yeniden oluşturulması (recreate) dialog KAPATILDIĞINDA yapılır.
 *
 * Bu yaklaşım, dialog açıkken sayfanın kaybolması sorununu çözer:
 * - Tema: dialog kapanınca AppCompatDelegate.setDefaultNightMode() çağrılır
 * - Dil: dialog kapanınca getActivity().recreate() çağrılır
 * Back stack (addToBackStack) ile kullanıcı doğru sayfaya döner.
 */
public class SettingsDialogFragment extends DialogFragment {

    /** Tema değişikliği olup olmadığını izler */
    private boolean darkModeChanged = false;

    /** Dil değişikliği olup olmadığını izler */
    private boolean langChanged = false;

    /** Yeni koyu tema değeri (dialog kapanınca uygulanır) */
    private boolean newDarkMode;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_settings, null);

        PreferencesManager prefs = new PreferencesManager(requireContext());

        // Başlangıç değerlerini kaydet (karşılaştırma için)
        newDarkMode = prefs.isDarkMode();

        // ===== Koyu Tema Switch =====
        // Mevcut tema tercihine göre switch durumunu ayarla
        SwitchMaterial switchDarkMode = view.findViewById(R.id.switchDarkMode);
        switchDarkMode.setChecked(prefs.isDarkMode());

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Değişikliği prefs'e KAYDET (henüz uygulamaya geçirme)
            prefs.saveDarkMode(isChecked);
            newDarkMode = isChecked;
            darkModeChanged = true;
        });

        // ===== Dil Seçimi Radio Group =====
        RadioGroup radioGroup = view.findViewById(R.id.radioGroupLanguage);
        MaterialRadioButton radioTurkish = view.findViewById(R.id.radioTurkish);
        MaterialRadioButton radioEnglish = view.findViewById(R.id.radioEnglish);

        // Mevcut dil tercihine göre seçili radio butonunu ayarla
        if ("en".equals(prefs.getLanguage())) {
            radioEnglish.setChecked(true);
        } else {
            radioTurkish.setChecked(true);
        }

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            // Yeni dil tercihini kaydet (henüz recreate etme)
            if (checkedId == R.id.radioTurkish) {
                prefs.setLanguage("tr");
            } else if (checkedId == R.id.radioEnglish) {
                prefs.setLanguage("en");
            }
            langChanged = true;
        });

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .create();
    }

    /**
     * Dialog kapandığında tema ve dil değişikliklerini uygular.
     *
     * Değişiklikler buradan uygulanarak:
     * 1. Dialog açıkken sayfanın yeniden yüklenmesi engellenir
     * 2. Kullanıcı, dialog kapandıktan sonra doğru sayfaya döner
     * 3. Fragment back stack korunduğu için yönlendirme sorunu yaşanmaz
     */
    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);

        // Tema değiştiyse AppCompatDelegate üzerinden anında uygula
        // Bu çağrı Activity'yi yeniden oluşturur, ancak artık dialog kapalıdır
        if (darkModeChanged) {
            AppCompatDelegate.setDefaultNightMode(
                    newDarkMode
                            ? AppCompatDelegate.MODE_NIGHT_YES
                            : AppCompatDelegate.MODE_NIGHT_NO
            );
        }

        // Dil değiştiyse Activity'yi yeniden başlat
        // Dil değişikliği için Context yeniden oluşturulmalıdır
        if (langChanged && !darkModeChanged && getActivity() != null) {
            getActivity().recreate();
        }
    }
}
