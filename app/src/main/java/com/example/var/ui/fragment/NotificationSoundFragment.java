package com.example.var.ui.fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.var.R;
import com.example.var.databinding.FragmentNotificationSoundBinding;
import com.example.var.databinding.ItemNotificationSoundBinding;
import com.example.var.util.NotificationHelper;
import com.example.var.util.SoundPreferencesManager;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class NotificationSoundFragment extends Fragment {

    private FragmentNotificationSoundBinding binding;
    private SoundPreferencesManager soundPrefs;
    private MediaPlayer mediaPlayer;

    // Hangi sesin değiştirileceğini tutar
    private String currentEditingKey;

    // Her ses öğesi için binding referansları
    private ItemNotificationSoundBinding bindingGoal;
    private ItemNotificationSoundBinding bindingMatchStart;
    private ItemNotificationSoundBinding bindingMatchEnd;
    private ItemNotificationSoundBinding bindingReminder;

    private final ActivityResultLauncher<String> audioPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null && currentEditingKey != null) {
                    processSelectedAudio(uri);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentNotificationSoundBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        soundPrefs = new SoundPreferencesManager(requireContext());

        binding.toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        buildSoundItems();
    }

    // ── Ses öğelerini oluştur ──────────────────────────────────────────

    private void buildSoundItems() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        bindingGoal = buildItem(inflater,
                getString(R.string.sound_goal),
                SoundPreferencesManager.KEY_GOAL);

        bindingMatchStart = buildItem(inflater,
                getString(R.string.sound_match_start),
                SoundPreferencesManager.KEY_MATCH_START);

        bindingMatchEnd = buildItem(inflater,
                getString(R.string.sound_match_end),
                SoundPreferencesManager.KEY_MATCH_END);

        bindingReminder = buildItem(inflater,
                getString(R.string.sound_reminder),
                SoundPreferencesManager.KEY_REMINDER);
    }

    private ItemNotificationSoundBinding buildItem(LayoutInflater inflater, String name, String key) {
        ItemNotificationSoundBinding b = ItemNotificationSoundBinding.inflate(
                inflater, binding.llSoundsContainer, false);

        b.tvSoundName.setText(name);
        updateStatusLabel(b, key);

        b.btnPlay.setOnClickListener(v -> playSound(key));
        b.btnChange.setOnClickListener(v -> showChangeDialog(key, b));

        binding.llSoundsContainer.addView(b.getRoot());
        return b;
    }

    private void updateStatusLabel(ItemNotificationSoundBinding b, String key) {
        if (soundPrefs.isCustomSound(key)) {
            b.tvSoundStatus.setText(getString(R.string.sound_custom));
            b.tvSoundStatus.setTextColor(requireContext().getColor(R.color.primary));
        } else {
            b.tvSoundStatus.setText(getString(R.string.sound_default));
            b.tvSoundStatus.setTextColor(requireContext().getColor(android.R.color.darker_gray));
        }
    }

    // ── Değiştir dialog ───────────────────────────────────────────────

    private void showChangeDialog(String key, ItemNotificationSoundBinding itemBinding) {
        boolean hasCustom = soundPrefs.isCustomSound(key);

        String[] options = hasCustom
                ? new String[]{getString(R.string.sound_select_file), getString(R.string.sound_reset)}
                : new String[]{getString(R.string.sound_select_file)};

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(itemBinding.tvSoundName.getText())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        currentEditingKey = key;
                        audioPickerLauncher.launch("audio/*");
                    } else {
                        resetSound(key, itemBinding);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // ── Ses çalma ─────────────────────────────────────────────────────

    private void playSound(String key) {
        stopCurrentPlayback();

        Uri uri = soundPrefs.getSoundUri(key, SoundPreferencesManager.defaultRawRes(key));

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(requireContext(), uri);
            mediaPlayer.setOnCompletionListener(mp -> stopCurrentPlayback());
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            stopCurrentPlayback();
        }
    }

    private void stopCurrentPlayback() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    // ── Dosya işleme ──────────────────────────────────────────────────

    private void processSelectedAudio(Uri uri) {
        // MIME tipi kontrolü
        String mimeType = requireContext().getContentResolver().getType(uri);
        if (mimeType == null || !mimeType.startsWith("audio/")) {
            showSnackbar(getString(R.string.sound_invalid_format));
            return;
        }

        // Süre kontrolü (max 5 saniye)
        long durationMs = getAudioDurationMs(uri);
        if (durationMs < 0 || durationMs > 5000) {
            showSnackbar(getString(R.string.sound_too_long));
            return;
        }

        // İç depolamaya kopyala
        try {
            File dest = copyAudioToInternal(uri, currentEditingKey);
            soundPrefs.setCustomSoundPath(currentEditingKey, dest.getAbsolutePath());

            // Bildirim kanalını sıfırla
            String channelId = SoundPreferencesManager.channelId(currentEditingKey);
            NotificationHelper.updateChannelSound(requireContext(), channelId);

            refreshItem(currentEditingKey);
            showSnackbar(getString(R.string.sound_changed));
        } catch (IOException e) {
            showSnackbar(getString(R.string.error_loading));
        }
    }

    private long getAudioDurationMs(Uri uri) {
        try (MediaMetadataRetriever retriever = new MediaMetadataRetriever()) {
            retriever.setDataSource(requireContext(), uri);
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            return durationStr != null ? Long.parseLong(durationStr) : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    private File copyAudioToInternal(Uri uri, String key) throws IOException {
        File dest = new File(soundPrefs.getCustomSoundsDir(), key + ".audio");
        try (InputStream in = requireContext().getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(dest)) {
            if (in == null) throw new IOException("Cannot open input stream");
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
        }
        return dest;
    }

    // ── Sıfırlama ─────────────────────────────────────────────────────

    private void resetSound(String key, ItemNotificationSoundBinding itemBinding) {
        soundPrefs.resetToDefault(key);
        String channelId = SoundPreferencesManager.channelId(key);
        NotificationHelper.updateChannelSound(requireContext(), channelId);
        updateStatusLabel(itemBinding, key);
        showSnackbar(getString(R.string.sound_reset_done));
    }

    // ── UI güncelleme ─────────────────────────────────────────────────

    private void refreshItem(String key) {
        ItemNotificationSoundBinding b = bindingForKey(key);
        if (b != null) updateStatusLabel(b, key);
    }

    private ItemNotificationSoundBinding bindingForKey(String key) {
        switch (key) {
            case SoundPreferencesManager.KEY_GOAL:        return bindingGoal;
            case SoundPreferencesManager.KEY_MATCH_START: return bindingMatchStart;
            case SoundPreferencesManager.KEY_MATCH_END:   return bindingMatchEnd;
            case SoundPreferencesManager.KEY_REMINDER:    return bindingReminder;
            default: return null;
        }
    }

    private void showSnackbar(String msg) {
        if (getView() != null) {
            Snackbar snackbar = Snackbar.make(getView(), msg, Snackbar.LENGTH_SHORT);
            View bottomNav = requireActivity().findViewById(R.id.bottomNav);
            if (bottomNav != null) snackbar.setAnchorView(bottomNav);
            snackbar.show();
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopCurrentPlayback();
        binding = null;
    }
}
