package com.example.var.ui.fragment;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.var.BuildConfig;
import com.example.var.R;
import com.example.var.data.model.ApiResponse;
import com.example.var.data.model.LeagueModel;
import com.example.var.data.model.UserModel;
import com.example.var.data.repository.MatchRepository;
import com.example.var.databinding.FragmentProfileBinding;
import com.example.var.ui.dialog.SettingsDialogFragment;
import com.example.var.util.FirebaseManager;
import com.example.var.util.PreferencesManager;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ProfileFragment - Yerel depolama destekli profil ekranı.
 * Fotoğraflar sunucuya yüklenmez, cihazın yerel hafızasında saklanır.
 */
public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    private FragmentProfileBinding binding;
    private PreferencesManager prefs;
    private UserModel currentUser;
    private Map<String, String> leagueNameMap = new HashMap<>();
    private MatchRepository repository;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            saveProfilePhotoLocally(uri);
                        }
                    });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = new PreferencesManager(requireContext());
        repository = new MatchRepository(BuildConfig.API_KEY);

        setupClickListeners();
        loadLeagueNames();
        loadUserProfile();
    }

    private void setupClickListeners() {
        binding.cardPhoto.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        binding.tvChangePhoto.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        binding.switchDarkMode.setChecked(prefs.isDarkMode());
        binding.switchDarkMode.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.setDarkMode(isChecked);
        });

        binding.layoutLanguage.setOnClickListener(v -> {
            SettingsDialogFragment dialog = new SettingsDialogFragment();
            dialog.show(getParentFragmentManager(), "settings_dialog");
        });

        binding.btnSignOut.setOnClickListener(v -> signOut());
    }

    private void loadUserProfile() {
        FirebaseUser firebaseUser = FirebaseManager.getCurrentUser();
        if (firebaseUser == null) return;

        binding.tvDisplayName.setText(
                firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "");
        binding.tvEmail.setText(
                firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "");

        updateLanguageDisplay();

        // 1. Önce yerel cihazda bir fotoğraf var mı bak
        File localFile = getLocalProfileFile(firebaseUser.getUid());
        if (localFile.exists()) {
            loadProfilePhoto(localFile.getAbsolutePath());
        } 
        // 2. Yerelde yoksa ve Google hesabıysa, Google URL'sini dene
        else if (firebaseUser.getPhotoUrl() != null) {
            loadProfilePhoto(firebaseUser.getPhotoUrl().toString());
        }

        FirebaseManager.getUserProfile(firebaseUser.getUid(),
                user -> {
                    if (!isAdded()) return;
                    currentUser = user;
                    // Eğer yerel dosya yoksa ama Firestore'da bir URL (Google vb.) varsa onu yükle
                    if (!localFile.exists() && user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
                        loadProfilePhoto(user.getPhotoUrl());
                    }
                    displayFavorites(user.getFavoriteTeams(), user.getFavoriteLeagues(),
                            user.getFavoriteTeamNames());
                },
                e -> { /* Sessizce başarısız ol */ }
        );
    }

    private void loadProfilePhoto(Object source) {
        Glide.with(this)
                .load(source)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .circleCrop()
                .into(binding.ivProfilePhoto);
    }

    /**
     * Seçilen fotoğrafı cihazın yerel hafızasına kaydeder.
     */
    private void saveProfilePhotoLocally(Uri imageUri) {
        FirebaseUser user = FirebaseManager.getCurrentUser();
        if (user == null) return;

        try {
            // Görseli oku ve sıkıştır (yer tasarrufu için)
            InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            
            // Dosya yolunu belirle: /data/user/0/com.example.var/files/profile_[uid].jpg
            File file = getLocalProfileFile(user.getUid());
            
            FileOutputStream outputStream = new FileOutputStream(file);
            // %80 kalite ile JPEG olarak kaydet
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
            
            outputStream.flush();
            outputStream.close();
            
            // UI'da hemen göster
            loadProfilePhoto(file.getAbsolutePath());
            showSnackbar(getString(R.string.photo_updated));
            
        } catch (Exception e) {
            Log.e(TAG, "Yerel kaydetme hatası", e);
            showSnackbar(getString(R.string.error_loading));
        }
    }

    /**
     * Kullanıcıya özel yerel profil fotoğrafı dosyasını döndürür.
     */
    private File getLocalProfileFile(String uid) {
        return new File(requireContext().getFilesDir(), "profile_" + uid + ".jpg");
    }

    private void loadLeagueNames() {
        repository.getLeagues().enqueue(new Callback<ApiResponse<LeagueModel>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<LeagueModel>> call,
                    @NonNull Response<ApiResponse<LeagueModel>> response) {
                if (!isAdded() || binding == null) return;
                if (!response.isSuccessful() || response.body() == null || response.body().getData() == null) return;
                for (LeagueModel l : response.body().getData()) {
                    if (l.getLeagueId() != null && l.getName() != null) {
                        leagueNameMap.put(l.getLeagueId(), l.getName());
                    }
                }
                if (currentUser != null) {
                    displayFavorites(currentUser.getFavoriteTeams(),
                            currentUser.getFavoriteLeagues(),
                            currentUser.getFavoriteTeamNames());
                }
            }
            @Override
            public void onFailure(@NonNull Call<ApiResponse<LeagueModel>> call, @NonNull Throwable t) { }
        });
    }

    private void displayFavorites(List<String> favoriteTeams, List<String> favoriteLeagues,
            Map<String, String> teamNames) {
        Map<String, String> teamLeagues = currentUser != null
                ? currentUser.getFavoriteTeamLeagues() : new HashMap<>();

        binding.llFavoriteTeams.removeAllViews();
        if (favoriteTeams == null || favoriteTeams.isEmpty()) {
            addEmptyRow(binding.llFavoriteTeams);
        } else {
            for (String teamId : favoriteTeams) {
                final String storedLeagueId = teamLeagues.get(teamId);
                boolean hasName = teamNames != null && teamNames.containsKey(teamId);
                final String name = hasName ? teamNames.get(teamId) : "";
                View row = addFavoriteRow(binding.llFavoriteTeams,
                        hasName ? name : getString(R.string.loading),
                        v -> openTeam(teamId, name, storedLeagueId));
                if (!hasName && storedLeagueId != null) {
                    resolveTeamNameFromStandings(teamId, storedLeagueId, row);
                }
            }
        }

        Map<String, String> leagueNames = currentUser != null
                ? currentUser.getFavoriteLeagueNames() : new HashMap<>();

        binding.llFavoriteLeagues.removeAllViews();
        if (favoriteLeagues == null || favoriteLeagues.isEmpty()) {
            addEmptyRow(binding.llFavoriteLeagues);
        } else {
            for (String leagueId : favoriteLeagues) {
                String name = leagueId;
                if (leagueNames.containsKey(leagueId)) {
                    name = leagueNames.get(leagueId);
                } else if (leagueNameMap.containsKey(leagueId)) {
                    name = leagueNameMap.get(leagueId);
                }
                final String finalName = name;
                addFavoriteRow(binding.llFavoriteLeagues, finalName,
                        v -> openLeague(leagueId, finalName));
            }
        }
    }

    private void resolveTeamNameFromStandings(String teamId, String leagueId, View row) {
        repository.getLeagueTable(leagueId).enqueue(new Callback<com.example.var.data.model.StandingLeagueResponse>() {
            @Override
            public void onResponse(@NonNull Call<com.example.var.data.model.StandingLeagueResponse> call,
                    @NonNull Response<com.example.var.data.model.StandingLeagueResponse> response) {
                if (!isAdded() || binding == null) return;
                if (!response.isSuccessful() || response.body() == null || response.body().getData() == null) return;

                List<com.example.var.data.model.StandingLeagueResponse.TeamInfo> infos =
                        response.body().getData().getTeamInfos();
                if (infos == null) return;

                for (com.example.var.data.model.StandingLeagueResponse.TeamInfo ti : infos) {
                    if (teamId.equals(ti.getTeamId()) && ti.getName() != null) {
                        final String resolvedName = ti.getName();
                        ((TextView) row.findViewById(R.id.tvFavoriteName)).setText(resolvedName);
                        row.setOnClickListener(v -> openTeam(teamId, resolvedName, leagueId));
                        FirebaseManager.addFavoriteTeam(teamId, resolvedName, leagueId, u -> {}, e -> {});
                        return;
                    }
                }
            }
            @Override
            public void onFailure(@NonNull Call<com.example.var.data.model.StandingLeagueResponse> call, @NonNull Throwable t) { }
        });
    }

    private View addFavoriteRow(LinearLayout container, String label, View.OnClickListener onClick) {
        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_favorite_row, container, false);
        ((TextView) row.findViewById(R.id.tvFavoriteName)).setText(label);
        row.setOnClickListener(onClick);
        container.addView(row);
        return row;
    }

    private void addEmptyRow(LinearLayout container) {
        TextView tv = new TextView(requireContext());
        tv.setText(getString(R.string.no_favorites));
        tv.setTextColor(requireContext().getColor(android.R.color.darker_gray));
        int pad = (int) (8 * getResources().getDisplayMetrics().density);
        tv.setPadding(0, pad, 0, pad);
        container.addView(tv);
    }

    private void openLeague(String leagueId, String leagueName) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                        R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.fragmentContainer, LeagueStandingsFragment.newInstance(leagueId, leagueName))
                .addToBackStack(null)
                .commit();
    }

    private void openTeam(String teamId, String teamName, String leagueId) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                        R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.fragmentContainer,
                        TeamMatchesFragment.newInstance(teamId, teamName, leagueId))
                .addToBackStack(null)
                .commit();
    }

    private void updateLanguageDisplay() {
        String lang = prefs.getLanguage();
        binding.tvCurrentLanguage.setText("tr".equals(lang)
                ? getString(R.string.language_turkish)
                : getString(R.string.language_english));
    }

    private void signOut() {
        FirebaseManager.signOut();
        Fragment parent = getParentFragment();
        if (parent instanceof AccountFragment) {
            ((AccountFragment) parent).onAuthStateChanged();
        }
    }

    private void showSnackbar(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
