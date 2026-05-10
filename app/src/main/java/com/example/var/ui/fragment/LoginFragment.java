package com.example.var.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.var.R;
import com.example.var.data.model.UserModel;
import com.example.var.databinding.FragmentLoginBinding;
import com.example.var.util.FirebaseManager;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

/**
 * LoginFragment - Kullanıcı giriş ekranı.
 *
 * AccountFragment tarafından giriş yapılmamış kullanıcılar için gösterilir.
 *
 * Giriş yöntemleri:
 * 1. E-posta ve şifre (Firebase Authentication)
 * 2. Google ile giriş (GoogleSignInClient + Firebase credential)
 *
 * Başarılı girişten sonra:
 * - Firestore'da kullanıcı profili oluşturulur veya güncellenir
 * - AccountFragment, ProfileFragment'ı gösterecek şekilde güncellenir
 *
 * Bağlantılar:
 * - "Şifremi Unuttum" → ForgotPasswordFragment
 * - "Kayıt Ol" → RegisterFragment
 */
public class LoginFragment extends Fragment {

    private static final String TAG = "LoginFragment";

    /** ViewBinding referansı */
    private FragmentLoginBinding binding;

    /** Firebase kimlik doğrulama instance */
    private FirebaseAuth auth;

    /** Google Sign-In istemcisi */
    private GoogleSignInClient googleSignInClient;

    /**
     * Google Sign-In sonucunu işlemek için ActivityResultLauncher.
     * Eski startActivityForResult'ın modern ve güvenli alternatifidir.
     */
    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        // Google Sign-In intent'inin dönüş verisi
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(
                                result.getData());
                        handleGoogleSignInResult(task);
                    });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();

        setupGoogleSignIn();
        setupClickListeners();
    }

    /**
     * Google Sign-In istemcisini yapılandırır.
     * GoogleSignInOptions'ta istek yapılandırılır ve client oluşturulur.
     * requestIdToken: Firebase ile entegrasyon için gerekli
     */
    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);
    }

    /**
     * Tüm buton ve link tıklama olaylarını yapılandırır.
     */
    private void setupClickListeners() {
        // E-posta/şifre ile giriş butonu
        binding.btnLogin.setOnClickListener(v -> loginWithEmail());

        // Google ile giriş butonu
        binding.btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());

        // Şifremi Unuttum linki
        binding.tvForgotPassword.setOnClickListener(v -> navigateToForgotPassword());

        // Kayıt Ol linki
        binding.tvRegister.setOnClickListener(v -> navigateToRegister());
    }

    /**
     * E-posta ve şifre ile Firebase Authentication üzerinden giriş yapar.
     * Form validasyonu yapar, ardından API isteği gönderir.
     */
    private void loginWithEmail() {
        String email = binding.etEmail.getText() != null
                ? binding.etEmail.getText().toString().trim() : "";
        String password = binding.etPassword.getText() != null
                ? binding.etPassword.getText().toString() : "";

        // Form validasyonu
        if (TextUtils.isEmpty(email)) {
            binding.emailLayout.setError(getString(R.string.email_required));
            return;
        }
        binding.emailLayout.setError(null);

        if (TextUtils.isEmpty(password)) {
            binding.passwordLayout.setError(getString(R.string.password_required));
            return;
        }
        binding.passwordLayout.setError(null);

        showLoading(true);

        // Firebase Authentication ile giriş yap
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    if (!isAdded()) return;
                    showLoading(false);
                    
                    FirebaseUser user = authResult.getUser();
                    if (user != null && !user.isEmailVerified()) {
                        auth.signOut();
                        showSnackbar(getString(R.string.error_verify_email));

                        return;
                    }
                    
                    onLoginSuccess(user);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    showLoading(false);
                    Log.e(TAG, "Giriş hatası: " + e.getMessage(), e);
                    if (e instanceof FirebaseAuthException) {
                        String code = ((FirebaseAuthException) e).getErrorCode();
                        Log.e(TAG, "Firebase hata kodu: " + code);
                        if ("ERROR_OPERATION_NOT_ALLOWED".equals(code)) {
                            showSnackbar(getString(R.string.error_operation_not_allowed));

                        } else {
                            showSnackbar(getString(R.string.login_failed) + " (" + code + ")");
                        }
                    } else {
                        showSnackbar(getString(R.string.login_failed));
                    }
                });
    }

    /**
     * Google ile giriş akışını başlatır.
     * Önce mevcut Google hesabından çıkış yapılır (hesap seçim ekranı için).
     * Ardından Google Sign-In intent'i launcher üzerinden başlatılır.
     */
    private void signInWithGoogle() {
        showLoading(true);
        // Önceki oturumu kapat ki kullanıcı hesap seçebilsin
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    /**
     * Google Sign-In akışının sonucunu işler.
     * Başarılı ise Google credential'ı Firebase'e iletir.
     *
     * @param task Google hesap verilerini içeren Task
     */
    private void handleGoogleSignInResult(Task<GoogleSignInAccount> task) {
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            // Google ID token'ı ile Firebase credential oluştur
            AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
            firebaseAuthWithCredential(credential, account.getDisplayName(), account.getEmail());
        } catch (ApiException e) {
            if (!isAdded()) return;
            showLoading(false);
            Log.e(TAG, "Google Sign In failed. Status Code: " + e.getStatusCode(), e);
            showSnackbar(getString(R.string.google_sign_in_failed) + " (Hata Kodu: " + e.getStatusCode() + ")");
        }
    }

    /**
     * Firebase Authentication'a Google credential'ı ile giriş yapar.
     *
     * @param credential  Google'dan alınan Firebase credential
     * @param displayName Kullanıcının Google'daki görünen adı
     * @param email       Kullanıcının e-posta adresi
     */
    private void firebaseAuthWithCredential(AuthCredential credential,
            String displayName, String email) {
        auth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    if (!isAdded()) return;
                    showLoading(false);
                    onLoginSuccess(authResult.getUser());
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    showLoading(false);
                    showSnackbar(getString(R.string.login_failed));
                });
    }

    /**
     * Başarılı giriş sonrası işlemleri yapar.
     * Firestore'a kullanıcı profilini kaydeder/günceller,
     * ardından AccountFragment'ı bildirir (ProfileFragment gösterilsin).
     *
     * @param firebaseUser Giriş yapmış Firebase kullanıcısı
     */
    private void onLoginSuccess(FirebaseUser firebaseUser) {
        if (firebaseUser == null) return;

        // Firestore'a kullanıcı profilini kaydet (ilk girişte oluşturur, sonrakinde günceller)
        UserModel user = new UserModel(
                firebaseUser.getUid(),
                firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "",
                firebaseUser.getEmail() != null ? firebaseUser.getEmail() : ""
        );
        
        // Eğer FirebaseUser'da (örneğin Google login) fotoğraf URL'si varsa onu da ekle
        if (firebaseUser.getPhotoUrl() != null) {
            user.setPhotoUrl(firebaseUser.getPhotoUrl().toString());
        }

        FirebaseManager.saveUserProfile(user,
                unused -> {
                    if (!isAdded()) return;
                    // AccountFragment'ı güncelle (ProfileFragment gösterilsin)
                    refreshAccountFragment();
                },
                e -> {
                    // Profil kaydedilemese de giriş başarılıdır, devam et
                    if (!isAdded()) return;
                    refreshAccountFragment();
                }
        );
    }

    /**
     * Ebeveyn AccountFragment'ını Auth durumu değiştiğini bildirir.
     * AccountFragment'ın kendisi giriş durumuna göre Profile/Login seçimini yeniden yapar.
     */
    private void refreshAccountFragment() {
        // Bu fragment'ın ebeveyni AccountFragment - onu güncelle
        Fragment parent = getParentFragment();
        if (parent instanceof AccountFragment) {
            ((AccountFragment) parent).onAuthStateChanged();
        }
    }

    /** ForgotPasswordFragment'a yönlendirir */
    private void navigateToForgotPassword() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                                     R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.fragmentContainer, new ForgotPasswordFragment())
                .addToBackStack(null)
                .commit();
    }

    /** RegisterFragment'a yönlendirir */
    private void navigateToRegister() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                                     R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.fragmentContainer, new RegisterFragment())
                .addToBackStack(null)
                .commit();
    }

    /**
     * Yükleniyor göstergesini gösterir veya gizler.
     * Giriş butonu ve yükleniyor çubuğu durumunu yönetir.
     *
     * @param loading true ise yükleniyor durumu aktif
     */
    private void showLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!loading);
        binding.btnGoogleSignIn.setEnabled(!loading);
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
