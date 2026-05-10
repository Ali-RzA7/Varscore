package com.example.var.ui.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.var.R;
import com.example.var.data.model.UserModel;
import com.example.var.databinding.FragmentRegisterBinding;
import com.example.var.util.FirebaseManager;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

/**
 * RegisterFragment - Yeni kullanıcı kayıt ekranı.
 *
 * Kullanıcıdan şu bilgileri alır:
 * - Görünen ad (displayName)
 * - E-posta adresi
 * - Şifre (en az 6 karakter)
 * - Şifre tekrar (doğrulama için)
 *
 * Kayıt işlemi adımları:
 * 1. Form validasyonu (boş alan, e-posta formatı, şifre eşleşmesi)
 * 2. Firebase Authentication ile hesap oluşturma
 * 3. Firebase'de displayName güncelleme
 * 4. Firestore'a kullanıcı profili kaydetme
 * 5. AccountFragment güncelleme (ProfileFragment gösterilsin)
 *
 * Bağlantılar:
 * - "Giriş Yap" linki → LoginFragment
 */
public class RegisterFragment extends Fragment {

    private static final String TAG = "RegisterFragment";

    /** ViewBinding referansı */
    private FragmentRegisterBinding binding;

    /** Firebase kimlik doğrulama instance */
    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();

        // Kayıt Ol butonu
        binding.btnRegister.setOnClickListener(v -> register());

        // Giriş Yap linki → back stack ile LoginFragment'a dön
        binding.tvLogin.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );
    }

    /**
     * Formdaki verileri doğrular ve Firebase Authentication ile kayıt işlemini başlatır.
     *
     * Validasyon kuralları:
     * - Ad: Boş olamaz
     * - E-posta: Boş olamaz ve geçerli format içermeli
     * - Şifre: En az 6 karakter
     * - Şifre tekrar: Şifre ile eşleşmeli
     */
    private void register() {
        String name = binding.etName.getText() != null
                ? binding.etName.getText().toString().trim() : "";
        String email = binding.etEmail.getText() != null
                ? binding.etEmail.getText().toString().trim() : "";
        String password = binding.etPassword.getText() != null
                ? binding.etPassword.getText().toString() : "";
        String confirmPassword = binding.etConfirmPassword.getText() != null
                ? binding.etConfirmPassword.getText().toString() : "";

        // Ad validasyonu
        if (TextUtils.isEmpty(name)) {
            binding.nameLayout.setError(getString(R.string.name_required));
            return;
        }
        binding.nameLayout.setError(null);

        // E-posta validasyonu
        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.setError(getString(R.string.email_invalid));
            return;
        }
        binding.emailLayout.setError(null);

        // Şifre uzunluk validasyonu
        if (password.length() < 6) {
            binding.passwordLayout.setError(getString(R.string.password_too_short));
            return;
        }
        binding.passwordLayout.setError(null);

        // Şifre eşleşme validasyonu
        if (!password.equals(confirmPassword)) {
            binding.confirmPasswordLayout.setError(getString(R.string.passwords_not_match));
            return;
        }
        binding.confirmPasswordLayout.setError(null);

        showLoading(true);

        // Firebase Authentication ile yeni hesap oluştur
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    if (!isAdded()) return;
                    // Hesap oluşturuldu - displayName güncelle
                    updateUserProfile(authResult.getUser(), name, email);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    showLoading(false);
                    // Firebase hata kodunu logla (debug için)
                    String errorMsg = e.getMessage();
                    Log.e(TAG, "Kayıt hatası: " + errorMsg, e);
                    if (e instanceof FirebaseAuthException) {
                        String code = ((FirebaseAuthException) e).getErrorCode();
                        Log.e(TAG, "Firebase hata kodu: " + code);
                        // Kullanıcıya anlamlı hata mesajı göster
                        switch (code) {
                            case "ERROR_EMAIL_ALREADY_IN_USE":
                                showSnackbar(getString(R.string.error_email_already_in_use));
                                break;
                            case "ERROR_INVALID_EMAIL":
                                showSnackbar(getString(R.string.error_invalid_email_firebase));
                                break;
                            case "ERROR_WEAK_PASSWORD":
                                showSnackbar(getString(R.string.error_weak_password));
                                break;
                            case "ERROR_OPERATION_NOT_ALLOWED":
                                showSnackbar(getString(R.string.error_operation_not_allowed));
                                break;
                            default:
                                showSnackbar(getString(R.string.register_failed) + " (" + code + ")");
                        }
                    } else {
                        showSnackbar(getString(R.string.register_failed) + ": " + errorMsg);
                    }
                });
    }

    /**
     * Firebase Authentication profilinde displayName'i günceller.
     * Ardından Firestore'a kullanıcı belgesi oluşturur.
     *
     * @param firebaseUser Yeni oluşturulan Firebase kullanıcısı
     * @param name         Kullanıcının girdiği görünen ad
     * @param email        Kullanıcının e-posta adresi
     */
    private void updateUserProfile(FirebaseUser firebaseUser, String name, String email) {
        // Firebase Auth profilinde adı güncelle
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();

        firebaseUser.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    // Profil güncelleme başarılı olsa da olmasa da Firestore'a kaydet
                    saveToFirestore(firebaseUser, name, email);
                });
    }

    /**
     * Firestore'a yeni kullanıcı profili belgesi oluşturur.
     * Başarılı olunca e-posta doğrulaması gönderilir.
     *
     * @param firebaseUser Yeni oluşturulan Firebase kullanıcısı
     * @param name   Görünen ad
     * @param email  E-posta
     */
    private void saveToFirestore(FirebaseUser firebaseUser, String name, String email) {
        UserModel user = new UserModel(firebaseUser.getUid(), name, email);

        FirebaseManager.saveUserProfile(user,
                unused -> {
                    if (!isAdded()) return;
                    sendVerificationAndSignOut(firebaseUser);
                },
                e -> {
                    if (!isAdded()) return;
                    sendVerificationAndSignOut(firebaseUser);
                }
        );
    }

    /**
     * Kullanıcıya doğrulama e-postası gönderir, oturumunu kapatır ve Login ekranına geri döndürür.
     */
    private void sendVerificationAndSignOut(FirebaseUser firebaseUser) {
        firebaseUser.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (!isAdded()) return;
                    showLoading(false);
                    
                    if (task.isSuccessful()) {
                        showSnackbar(getString(R.string.register_success_verify));
                    } else {
                        showSnackbar(getString(R.string.register_success_no_verify));

                    }
                    
                    // Kullanıcıyı doğrulamadan uygulamaya sokmamak için hemen çıkış yapıyoruz
                    auth.signOut();
                    
                    // Mesajın okunabilmesi için 1.5 saniye bekleyip Login sayfasına dön
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        if (isAdded()) {
                            requireActivity().getSupportFragmentManager().popBackStack();
                        }
                    }, 1500);
                });
    }

    /**
     * Yükleniyor durumunu gösterir veya gizler.
     *
     * @param loading true ise yükleniyor aktif
     */
    private void showLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnRegister.setEnabled(!loading);
    }

    private void showSnackbar(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
