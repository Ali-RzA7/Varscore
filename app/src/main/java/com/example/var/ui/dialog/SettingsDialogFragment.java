package com.example.var.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.var.R;
import com.example.var.util.PreferencesManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

/**
 * SettingsDialogFragment - Ayarlar dialog'u.
 * Koyu tema ve dil tercihlerini yönetir.
 * Değişiklikler SharedPreferences'a kaydedilir ve anında uygulanır.
 */
public class SettingsDialogFragment extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_settings, null);

        PreferencesManager prefs = new PreferencesManager(requireContext());

        // === Koyu Tema Switch ===
        SwitchMaterial switchDarkMode = view.findViewById(R.id.switchDarkMode);
        switchDarkMode.setChecked(prefs.isDarkMode());
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setDarkMode(isChecked);
        });

        // === Dil Seçimi Radio Group ===
        RadioGroup radioGroup = view.findViewById(R.id.radioGroupLanguage);
        MaterialRadioButton radioTurkish = view.findViewById(R.id.radioTurkish);
        MaterialRadioButton radioEnglish = view.findViewById(R.id.radioEnglish);

        // Mevcut dil tercihine göre radio butonunu seç
        if ("en".equals(prefs.getLanguage())) {
            radioEnglish.setChecked(true);
        } else {
            radioTurkish.setChecked(true);
        }

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioTurkish) {
                prefs.setLanguage("tr");
            } else if (checkedId == R.id.radioEnglish) {
                prefs.setLanguage("en");
            }
            // Activity'yi yeniden başlat (dil değişikliği uygulanması için)
            if (getActivity() != null) {
                getActivity().recreate();
            }
        });

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .create();
    }
}
