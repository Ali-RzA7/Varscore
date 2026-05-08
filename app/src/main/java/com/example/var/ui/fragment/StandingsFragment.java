package com.example.var.ui.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.var.BuildConfig;
import com.example.var.R;
import com.example.var.data.model.ApiResponse;
import com.example.var.data.model.LeagueModel;
import com.example.var.data.repository.MatchRepository;
import com.example.var.databinding.FragmentStandingsBinding;
import com.example.var.ui.adapter.LeagueListAdapter;
import com.example.var.util.FirebaseManager;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * StandingsFragment - Puan Durumu ana ekranı.
 *
 * Alt navigasyondan "Puan Durumu" sekmesine gelindiğinde gösterilir.
 * Tüm liglerin listesini çeker ve kullanıcıya sunar.
 *
 * Özellikler:
 * - Lig listesi (LeagueListAdapter ile)
 * - Lig adına göre anlık arama/filtreleme
 * - Lig favoriye ekleme/çıkarma (Firebase Firestore)
 * - Lig satırına tıklayınca LeagueStandingsFragment açma
 * - Pull-to-refresh ile yenileme
 * - Yükleniyor / Boş / Hata durumu gösterimi
 *
 * Navigasyon:
 * - Lig seçilince → LeagueStandingsFragment.newInstance(leagueId, leagueName)
 */
public class StandingsFragment extends Fragment
        implements LeagueListAdapter.OnLeagueClickListener,
        LeagueListAdapter.OnFavoriteClickListener {

    /** ViewBinding referansı */
    private FragmentStandingsBinding binding;

    /** Lig listesi adaptörü */
    private LeagueListAdapter leagueAdapter;

    /** Veri erişim katmanı */
    private MatchRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentStandingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new MatchRepository(BuildConfig.API_KEY);

        setupRecyclerView();
        setupSearchBox();
        setupSwipeRefresh();
        loadLeagues();
    }

    /**
     * RecyclerView ve adaptörü başlatır.
     * LeagueListAdapter'a callback olarak kendini atar.
     */
    private void setupRecyclerView() {
        leagueAdapter = new LeagueListAdapter(this, this);
        binding.rvLeagues.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvLeagues.setAdapter(leagueAdapter);
    }

    /**
     * Arama kutusuna metin girişini dinler ve anlık filtreleme yapar.
     */
    private void setupSearchBox() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {
            }

            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Her tuş vuruşunda adaptörün filtre metodunu çağır
                leagueAdapter.filter(s.toString());
            }
        });
    }

    /**
     * SwipeRefreshLayout yapılandırması.
     * Aşağı çekme ile lig listesini yeniden yükler.
     */
    private void setupSwipeRefresh() {
        binding.swipeRefreshLayout.setColorSchemeResources(R.color.primary, R.color.secondary);
        binding.swipeRefreshLayout.setOnRefreshListener(this::loadLeagues);
    }

    /**
     * İSportsAPI'den tüm lig listesini çeker.
     * Yükleme, başarı, boş ve hata durumlarını yönetir.
     */
    private void loadLeagues() {
        showLoading();

        repository.getLeagues().enqueue(new Callback<ApiResponse<LeagueModel>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<LeagueModel>> call,
                    @NonNull Response<ApiResponse<LeagueModel>> response) {
                if (!isAdded())
                    return;
                binding.swipeRefreshLayout.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    List<LeagueModel> leagues = response.body().getData();
                    if (leagues != null && !leagues.isEmpty()) {
                        // Ligleri adaptöre aktar
                        leagueAdapter.setLeagues(leagues);
                        showContent();
                        // Eğer giriş yapılmışsa favori lig ID'lerini yükle
                        loadFavoriteLeagues();
                    } else {
                        showEmpty();
                    }
                } else {
                    showError(getString(R.string.error_loading));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<LeagueModel>> call,
                    @NonNull Throwable t) {
                if (!isAdded())
                    return;
                binding.swipeRefreshLayout.setRefreshing(false);
                showError(getString(R.string.error_loading));
            }
        });
    }

    /**
     * Giriş yapılmışsa Firestore'dan favori lig listesini çeker.
     * Adaptörü favori ID'leri ile günceller (yıldız ikonları güncellenir).
     */
    private void loadFavoriteLeagues() {
        if (!FirebaseManager.isLoggedIn())
            return;

        String userId = FirebaseManager.getCurrentUser().getUid();
        FirebaseManager.getUserProfile(userId,
                user -> {
                    if (!isAdded())
                        return;
                    leagueAdapter.setFavoriteLeagueIds(user.getFavoriteLeagues());
                },
                e -> {
                    /* Sessizce başarısız ol */ });
    }

    // ===== Callback Uygulamaları =====

    /**
     * Lig satırına tıklandığında LeagueStandingsFragment'ı açar.
     * Animasyonlu geçiş ve back stack kullanılır.
     */
    @Override
    public void onLeagueClick(LeagueModel league) {
        Fragment target = LeagueStandingsFragment.newInstance(
                league.getLeagueId(),
                league.getName());
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                        R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.fragmentContainer, target)
                .addToBackStack(null)
                .commit();
    }

    /**
     * Favori yıldız butonuna tıklandığında Firebase'e ekler veya çıkarır.
     * Giriş yapılmamışsa uyarı gösterir.
     *
     * @param league       Favoriye eklenecek/çıkarılacak lig
     * @param currentState Mevcut favori durumu
     */
    @Override
    public void onFavoriteClick(LeagueModel league, boolean currentState) {
        if (!FirebaseManager.isLoggedIn()) {
            // Giriş yapılmamış - kullanıcıyı uyar
            Snackbar.make(binding.getRoot(),
                    getString(R.string.login_required),
                    Snackbar.LENGTH_LONG)
                    .setAction(getString(R.string.login), v -> navigateToAccount())
                    .show();
            return;
        }

        if (currentState) {
            // Favoriden çıkar
            FirebaseManager.removeFavoriteLeague(league.getLeagueId(),
                    unused -> {
                        if (!isAdded())
                            return;
                        Snackbar.make(binding.getRoot(),
                                getString(R.string.removed_from_favorites),
                                Snackbar.LENGTH_SHORT).show();
                        loadFavoriteLeagues();
                    },
                    e -> showSnackbar(getString(R.string.error_loading)));
        } else {
            // Favoriye ekle
            FirebaseManager.addFavoriteLeague(league.getLeagueId(), league.getName(),
                    unused -> {
                        if (!isAdded())
                            return;
                        Snackbar.make(binding.getRoot(),
                                getString(R.string.added_to_favorites),
                                Snackbar.LENGTH_SHORT).show();
                        loadFavoriteLeagues();
                    },
                    e -> showSnackbar(getString(R.string.error_loading)));
        }
    }

    /**
     * Hesap sekmesine (Giriş/Profil) yönlendirme yapar.
     * BottomNavigationView üzerinden Hesap tabı seçilir.
     */
    private void navigateToAccount() {
        if (getActivity() instanceof com.example.var.ui.MainActivity) {
            ((com.example.var.ui.MainActivity) getActivity()).navigateToAccount();
        }
    }

    // ===== UI Durum Metodları =====

    private void showLoading() {
        binding.loadingContainer.setVisibility(View.VISIBLE);
        binding.emptyContainer.setVisibility(View.GONE);
        binding.errorContainer.setVisibility(View.GONE);
        binding.swipeRefreshLayout.setVisibility(View.GONE);
    }

    private void showContent() {
        binding.loadingContainer.setVisibility(View.GONE);
        binding.emptyContainer.setVisibility(View.GONE);
        binding.errorContainer.setVisibility(View.GONE);
        binding.swipeRefreshLayout.setVisibility(View.VISIBLE);
    }

    private void showEmpty() {
        binding.loadingContainer.setVisibility(View.GONE);
        binding.emptyContainer.setVisibility(View.VISIBLE);
        binding.errorContainer.setVisibility(View.GONE);
        binding.swipeRefreshLayout.setVisibility(View.GONE);
    }

    private void showError(String message) {
        binding.loadingContainer.setVisibility(View.GONE);
        binding.emptyContainer.setVisibility(View.GONE);
        binding.errorContainer.setVisibility(View.VISIBLE);
        binding.swipeRefreshLayout.setVisibility(View.GONE);
        binding.tvErrorMessage.setText(message);
        binding.btnRetry.setOnClickListener(v -> loadLeagues());
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
