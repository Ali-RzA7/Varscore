package com.example.var.ui.fragment;

import android.net.Uri;
import android.os.Bundle;
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
 *
 * Gösterilen bilgiler:
 * - Profil fotoğrafı (Firebase Storage'dan Glide ile yüklenir)
 * - Görünen ad ve e-posta
 * - Tema ayarı (dark/light mode switch)
 * - Dil ayarı (mevcut dil gösterilir, SettingsDialog açar)
 * - Favori takımlar listesi (Firestore'dan)
 * - Favori ligler listesi (Firestore'dan)
 * - Lig favorisi açıklaması
 * - Çıkış Yap butonu
 *
 * Fotoğraf değiştirme:
 * - Galeriden seçim (ActivityResultLauncher ile)
 * - Firebase Storage'a yükleme
 * - Firestore'da photoUrl güncelleme
 */
public class ProfileFragment extends Fragment {

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
     * Seçilen URI'yi Firebase Storage'a yükler.
     */
    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(
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

    /**
     * Tüm buton ve tıklama olaylarını yapılandırır.
     */
    private void setupClickListeners() {
        // Profil fotoğrafına tıklayınca galeriden seçim başlat
        binding.cardPhoto.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        binding.tvChangePhoto.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        // Tema switch'i
        binding.switchDarkMode.setChecked(prefs.isDarkMode());
        binding.switchDarkMode.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.setDarkMode(isChecked); // setDarkMode anında AppCompatDelegate'i çağırır
        });

        // Dil ayarına tıklanınca SettingsDialogFragment aç (sadece dil seçimi için)
        binding.layoutLanguage.setOnClickListener(v -> {
            SettingsDialogFragment dialog = new SettingsDialogFragment();
            dialog.show(getParentFragmentManager(), "settings_dialog");
        });

        // Çıkış Yap butonu
        binding.btnSignOut.setOnClickListener(v -> signOut());
    }

    /**
     * Firebase'den kullanıcı profilini ve favori listelerini yükler.
     * Profil fotoğrafını, adı ve e-postayı günceller.
     */
    private void loadUserProfile() {
        FirebaseUser firebaseUser = FirebaseManager.getCurrentUser();
        if (firebaseUser == null) return;

        // Temel bilgileri anında göster (Firestore beklerken)
        binding.tvDisplayName.setText(
                firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "");
        binding.tvEmail.setText(
                firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "");

        // Mevcut dil tercihini göster
        updateLanguageDisplay();

        // Profil fotoğrafını göster (Firebase Auth URL'si varsa)
        if (firebaseUser.getPhotoUrl() != null) {
            loadProfilePhoto(firebaseUser.getPhotoUrl().toString());
        }

        // Firestore'dan detaylı profil yükle (favori listeler için)
        FirebaseManager.getUserProfile(firebaseUser.getUid(),
                user -> {
                    if (!isAdded()) return;
                    currentUser = user;
                    // Firestore'dan gelen fotoğraf URL'si varsa güncelle
                    if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
                        loadProfilePhoto(user.getPhotoUrl());
                    }
                    // Favori listelerini göster
                    displayFavorites(user.getFavoriteTeams(), user.getFavoriteLeagues(),
                            user.getFavoriteTeamNames());
                },
                e -> { /* Sessizce başarısız ol, temel bilgiler zaten gösteriliyor */ }
        );
    }

    /**
     * Profil fotoğrafını Glide ile yükler.
     * Placeholder olarak ic_person ikonu kullanılır.
     *
     * @param photoUrl Firebase Storage veya Google hesabından gelen fotoğraf URL'si
     */
    private void loadProfilePhoto(String photoUrl) {
        Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .circleCrop()
                .into(binding.ivProfilePhoto);
    }

    /** Tüm ligleri API'den çekip leagueId → leagueName haritasını doldurur. */
    private void loadLeagueNames() {
        repository.getLeagues().enqueue(new Callback<ApiResponse<LeagueModel>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<LeagueModel>> call,
                    @NonNull Response<ApiResponse<LeagueModel>> response) {
                if (!isAdded() || binding == null) return;
                if (!response.isSuccessful() || response.body() == null
                        || response.body().getData() == null) return;
                for (LeagueModel l : response.body().getData()) {
                    if (l.getLeagueId() != null && l.getName() != null) {
                        leagueNameMap.put(l.getLeagueId(), l.getName());
                    }
                }
                // Kullanıcı profili zaten yüklendiyse lig adlarıyla ligleri yeniden çiz
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

    /**
     * Favori takım ve ligleri clickable satırlar olarak gösterir.
     */
    private void displayFavorites(List<String> favoriteTeams, List<String> favoriteLeagues,
            Map<String, String> teamNames) {
        Map<String, String> teamLeagues = currentUser != null
                ? currentUser.getFavoriteTeamLeagues() : new HashMap<>();

        // ── Takımlar ──
        binding.llFavoriteTeams.removeAllViews();
        if (favoriteTeams == null || favoriteTeams.isEmpty()) {
            addEmptyRow(binding.llFavoriteTeams);
        } else {
            for (String teamId : favoriteTeams) {
                final String storedLeagueId = teamLeagues.containsKey(teamId)
                        ? teamLeagues.get(teamId) : null;
                boolean hasName = teamNames != null && teamNames.containsKey(teamId)
                        && teamNames.get(teamId) != null && !teamNames.get(teamId).isEmpty();
                final String name = hasName ? teamNames.get(teamId) : "";
                View row = addFavoriteRow(binding.llFavoriteTeams,
                        hasName ? name : "Yükleniyor…",
                        v -> openTeam(teamId, name, storedLeagueId));
                if (!hasName && storedLeagueId != null) {
                    resolveTeamNameFromStandings(teamId, storedLeagueId, row);
                }
            }
        }

        // ── Ligler ──
        Map<String, String> leagueNames = currentUser != null
                ? currentUser.getFavoriteLeagueNames() : new HashMap<>();

        binding.llFavoriteLeagues.removeAllViews();
        if (favoriteLeagues == null || favoriteLeagues.isEmpty()) {
            addEmptyRow(binding.llFavoriteLeagues);
        } else {
            for (String leagueId : favoriteLeagues) {
                String name = leagueId;
                if (leagueNames.containsKey(leagueId) && leagueNames.get(leagueId) != null && !leagueNames.get(leagueId).isEmpty()) {
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

    /**
     * leagueId'nin standings/teamInfos listesinden teamId'ye karşılık gelen takım adını bulur.
     * Bulunca satırı günceller ve Firestore'a kaydeder.
     */
    private void resolveTeamNameFromStandings(String teamId, String leagueId, View row) {
        repository.getLeagueTable(leagueId).enqueue(new Callback<com.example.var.data.model.StandingLeagueResponse>() {
            @Override
            public void onResponse(@NonNull Call<com.example.var.data.model.StandingLeagueResponse> call,
                    @NonNull Response<com.example.var.data.model.StandingLeagueResponse> response) {
                if (!isAdded() || binding == null) return;
                if (!response.isSuccessful() || response.body() == null
                        || !response.body().isSuccess()
                        || response.body().getData() == null) return;

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
            public void onFailure(@NonNull Call<com.example.var.data.model.StandingLeagueResponse> call,
                    @NonNull Throwable t) { }
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

    /**
     * Mevcut dil ayarını gösterir (Türkçe/İngilizce).
     */
    private void updateLanguageDisplay() {
        String lang = prefs.getLanguage();
        binding.tvCurrentLanguage.setText("tr".equals(lang)
                ? getString(R.string.language_turkish)
                : getString(R.string.language_english));
    }

    /**
     * Seçilen fotoğrafı Firebase Storage'a yükler ve Firestore'da URL'yi günceller.
     * Yükleme sırasında SnackBar gösterilir.
     *
     * @param imageUri Galeriden seçilen fotoğrafın URI'si
     */
    private void uploadProfilePhoto(Uri imageUri) {
        FirebaseUser user = FirebaseManager.getCurrentUser();
        if (user == null) return;

        showSnackbar(getString(R.string.uploading_photo));

        // Firebase Storage'da kullanıcı klasörüne kaydet
        StorageReference photoRef = FirebaseStorage.getInstance().getReference()
                .child("profile_photos")
                .child(user.getUid() + ".jpg");

        photoRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Yükleme başarılı - download URL'si al
                    photoRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                if (!isAdded()) return;
                                // Firestore'da photoUrl güncelle
                                updatePhotoUrlInFirestore(uri.toString());
                                // Fotoğrafı hemen göster
                                loadProfilePhoto(uri.toString());
                            });
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    showSnackbar(getString(R.string.upload_failed));
                });
    }

    /**
     * Firestore'daki kullanıcı belgesinde photoUrl alanını günceller.
     *
     * @param photoUrl Firebase Storage'dan alınan yeni fotoğraf URL'si
     */
    private void updatePhotoUrlInFirestore(String photoUrl) {
        FirebaseUser user = FirebaseManager.getCurrentUser();
        if (user == null) return;

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .update("photoUrl", photoUrl)
                .addOnSuccessListener(unused -> showSnackbar(getString(R.string.photo_updated)))
                .addOnFailureListener(e -> { /* Sessizce başarısız ol */ });
    }

    /**
     * Kullanıcıyı Firebase'den çıkarır ve AccountFragment'a LoginFragment'ı göstermesini söyler.
     */
    private void signOut() {
        FirebaseManager.signOut();

        // Ebeveyn AccountFragment'ı güncelle (LoginFragment gösterilsin)
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
