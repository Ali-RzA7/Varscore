package com.example.var.ui.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.var.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

/**
 * ForgotPasswordFragment - Şifre sıfırlama ekranı.
 *
 * Kullanıcı e-posta adresini girer.
 * Firebase Authentication, bu adrese şifre sıfırlama bağlantısı gönderir.
 *
 * Akış:
 * LoginFragment → "Şifremi Unuttum" → ForgotPasswordFragment
 * Başarılı e-posta gönderiminden sonra geri dönme imkânı sunulur.
 */
public class ForgotPasswordFragment extends Fragment {

    /** E-posta giriş alanı container'ı */
    private TextInputLayout emailLayout;

    /** E-posta giriş alanı */
    private TextInputEditText etEmail;

    /** Gönder butonu */
    private MaterialButton btnSend;

    /** Yükleniyor göstergesi */
    private View progressBar;

    /** Başarı mesajı container'ı (e-posta gönderildikten sonra gösterilir) */
    private LinearLayout successContainer;

    /** Firebase kimlik doğrulama instance */
    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        // Not: Basit bir ekran olduğundan ViewBinding yerine manuel inflate kullanılabilir
        // Ancak tutarlılık için layout XML'i genişlet
        return inflater.inflate(R.layout.fragment_forgot_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();

        // View referansları
        emailLayout = view.findViewById(R.id.emailLayout);
        etEmail = view.findViewById(R.id.etEmail);
        btnSend = view.findViewById(R.id.btnSend);
        progressBar = view.findViewById(R.id.progressBar);
        successContainer = view.findViewById(R.id.successContainer);

        // Toolbar geri butonu
        view.findViewById(R.id.toolbar);
        com.google.android.material.appbar.MaterialToolbar toolbar =
                view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        // Gönder butonu
        btnSend.setOnClickListener(v -> sendResetEmail());

        // Geri dön linki (başarı durumunda gösterilir)
        TextView tvBackToLogin = view.findViewById(R.id.tvBackToLogin);
        if (tvBackToLogin != null) {
            tvBackToLogin.setOnClickListener(v ->
                    requireActivity().getSupportFragmentManager().popBackStack()
            );
        }
    }

    /**
     * E-posta adresini doğrular ve Firebase üzerinden şifre sıfırlama e-postası gönderir.
     *
     * Başarılı gönderim sonrası:
     * - Form gizlenir
     * - Başarı mesajı gösterilir
     * - Kullanıcı e-postasını kontrol etmesi için bilgilendirilir
     */
    private void sendResetEmail() {
        String email = etEmail.getText() != null
                ? etEmail.getText().toString().trim() : "";

        // E-posta validasyonu
        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError(getString(R.string.email_invalid));
            return;
        }
        emailLayout.setError(null);

        showLoading(true);

        // Firebase Authentication şifre sıfırlama e-postası gönder
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> {
                    if (!isAdded()) return;
                    showLoading(false);
                    // Başarı durumunu göster
                    showSuccessState();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    showLoading(false);
                    showSnackbar(getString(R.string.reset_email_failed));
                });
    }

    /**
     * E-posta gönderim başarılı durumunu gösterir.
     * Form alanları gizlenir ve başarı mesajı gösterilir.
     */
    private void showSuccessState() {
        emailLayout.setVisibility(View.GONE);
        btnSend.setVisibility(View.GONE);
        if (successContainer != null) {
            successContainer.setVisibility(View.VISIBLE);
        } else {
            showSnackbar(getString(R.string.reset_email_sent));
        }
    }

    private void showLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        btnSend.setEnabled(!loading);
    }

    private void showSnackbar(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
        }
    }
}
