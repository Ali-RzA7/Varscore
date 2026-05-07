package com.example.var.ui.fragment;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.var.R;
import com.example.var.data.model.UserModel;
import com.example.var.databinding.FragmentProfileBinding;
import com.example.var.ui.dialog.SettingsDialogFragment;
import com.example.var.util.FirebaseManager;
import com.example.var.util.PreferencesManager;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

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

        setupClickListeners();
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
                    displayFavorites(user.getFavoriteTeams(), user.getFavoriteLeagues());
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

    /**
     * Favori takım ve lig listelerini ekranda gösterir.
     *
     * @param favoriteTeams   Favori takım ID listesi (sadece sayıyı gösteriyoruz)
     * @param favoriteLeagues Favori lig ID listesi
     */
    private void displayFavorites(List<String> favoriteTeams, List<String> favoriteLeagues) {
        // Favori takım sayısı
        if (favoriteTeams == null || favoriteTeams.isEmpty()) {
            binding.tvFavoriteTeams.setText(getString(R.string.no_favorites));
        } else {
            binding.tvFavoriteTeams.setText(
                    getString(R.string.favorite_count, favoriteTeams.size()));
        }

        // Favori lig sayısı
        if (favoriteLeagues == null || favoriteLeagues.isEmpty()) {
            binding.tvFavoriteLeagues.setText(getString(R.string.no_favorites));
        } else {
            binding.tvFavoriteLeagues.setText(
                    getString(R.string.favorite_count, favoriteLeagues.size()));
        }
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
