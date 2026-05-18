package com.example.var.ui.fragment;

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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ProfileFragment - Giriş yapmış kullanıcının profil ekranı.
 *
 * AccountFragment tarafından kullanıcı giriş yapmış ise gösterilir.
 */
public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    /** ViewBinding referansı */
    private FragmentProfileBinding binding;

    /** Kullanıcı tercihleri yöneticisi */
    private PreferencesManager prefs;

    /** Mevcut kullanıcı profil verisi */
    private UserModel currentUser;

    /** Ligleri adlandırmak için kullanılan tüm lig listesi */
    private Map<String, String> leagueNameMap = new HashMap<>();

    private MatchRepository repository;

    /**
     * Galeriden fotoğraf seçmek için ActivityResultLauncher.
     */
    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    uploadProfilePhoto(uri);
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
        if (firebaseUser == null)
            return;

        binding.tvDisplayName.setText(
                firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "");
        binding.tvEmail.setText(
                firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "");

        updateLanguageDisplay();

        // Önce Auth'daki fotoğrafı dene (hızlı yükleme)
        if (firebaseUser.getPhotoUrl() != null) {
            loadProfilePhoto(firebaseUser.getPhotoUrl().toString());
        }

        // Firestore'dan detaylı profil yükle
        FirebaseManager.getUserProfile(firebaseUser.getUid(),
                user -> {
                    if (!isAdded())
                        return;
                    currentUser = user;
                    // Firestore'daki fotoğraf URL'si daha güncel olabilir
                    if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
                        loadProfilePhoto(user.getPhotoUrl());
                    }
                    displayFavorites(user.getFavoriteTeams(), user.getFavoriteLeagues(),
                            user.getFavoriteTeamNames());
                },
                e -> {
                    /* Sessizce başarısız ol */ });
    }

    private void loadProfilePhoto(String photoUrl) {
        Object loadTarget = (photoUrl == null || photoUrl.isEmpty()) ? R.drawable.ic_person : photoUrl;

        Glide.with(this)
                .load(loadTarget)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .circleCrop()
                .into(binding.ivProfilePhoto);
    }

    private void loadLeagueNames() {
        repository.getLeagues().enqueue(new Callback<ApiResponse<LeagueModel>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<LeagueModel>> call,
                    @NonNull Response<ApiResponse<LeagueModel>> response) {
                if (!isAdded() || binding == null)
                    return;
                if (!response.isSuccessful() || response.body() == null
                        || response.body().getData() == null)
                    return;
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
            public void onFailure(@NonNull Call<ApiResponse<LeagueModel>> call, @NonNull Throwable t) {
            }
        });
    }

    private void displayFavorites(List<String> favoriteTeams, List<String> favoriteLeagues,
            Map<String, String> teamNames) {
        Map<String, String> teamLeagues = currentUser != null
                ? currentUser.getFavoriteTeamLeagues()
                : new HashMap<>();

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
                ? currentUser.getFavoriteLeagueNames()
                : new HashMap<>();

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
                if (!isAdded() || binding == null)
                    return;
                if (!response.isSuccessful() || response.body() == null || response.body().getData() == null)
                    return;

                List<com.example.var.data.model.StandingLeagueResponse.TeamInfo> infos = response.body().getData()
                        .getTeamInfos();
                if (infos == null)
                    return;

                for (com.example.var.data.model.StandingLeagueResponse.TeamInfo ti : infos) {
                    if (teamId.equals(ti.getTeamId()) && ti.getName() != null) {
                        final String resolvedName = ti.getName();
                        ((TextView) row.findViewById(R.id.tvFavoriteName)).setText(resolvedName);
                        row.setOnClickListener(v -> openTeam(teamId, resolvedName, leagueId));
                        FirebaseManager.addFavoriteTeam(teamId, resolvedName, leagueId, u -> {
                        }, e -> {
                        });
                        return;
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<com.example.var.data.model.StandingLeagueResponse> call,
                    @NonNull Throwable t) {
            }
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

    /**
     * Seçilen fotoğrafı Firebase Storage'a yükler ve download URL'sini alır.
     * "Object does not exist" hatasını önlemek için modern task chaining kullanır.
     */
    private void uploadProfilePhoto(Uri imageUri) {
        FirebaseUser user = FirebaseManager.getCurrentUser();
        if (user == null)
            return;

        showSnackbar(getString(R.string.uploading_photo));

        final StorageReference photoRef = FirebaseStorage.getInstance().getReference()
                .child("profile_photos")
                .child(user.getUid() + ".jpg");

        // Yükleme işlemini başlat
        UploadTask uploadTask = photoRef.putFile(imageUri);

        // Task zincirleme: Yükleme bitince URL almayı dene
        uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) {
                if (task.getException() != null)
                    throw task.getException();
            }
            // Yükleme bitti, şimdi URL'yi iste
            return photoRef.getDownloadUrl();
        }).addOnSuccessListener(uri -> {
            if (!isAdded())
                return;
            String downloadUrl = uri.toString();

            // Firestore'da güncelle
            updatePhotoUrlInFirestore(downloadUrl);

            // UI'da göster
            loadProfilePhoto(downloadUrl);

        }).addOnFailureListener(e -> {
            if (!isAdded())
                return;
            Log.e(TAG, "Yükleme hatası: " + e.getMessage(), e);

            // "Object does not exist" hatası genelde bucket veya kurallar kaynaklıdır
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("does not exist")) {
                showSnackbar(
                        getString(R.string.upload_failed) + ": Depolama alanı (Bucket) bulunamadı veya yetki yok.");
            } else {
                showSnackbar(
                        getString(R.string.upload_failed) + ": " + (errorMsg != null ? errorMsg : "Bilinmeyen hata"));
            }
        });
    }

    private void updatePhotoUrlInFirestore(String photoUrl) {
        FirebaseUser user = FirebaseManager.getCurrentUser();
        if (user == null)
            return;

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .update("photoUrl", photoUrl)
                .addOnSuccessListener(unused -> showSnackbar(getString(R.string.photo_updated)))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore güncellenemedi", e);
                });
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
            Snackbar snackbar = Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT);
            View bottomNav = requireActivity().findViewById(R.id.bottomNav);
            if (bottomNav != null) {
                snackbar.setAnchorView(bottomNav);
            }
            snackbar.show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
