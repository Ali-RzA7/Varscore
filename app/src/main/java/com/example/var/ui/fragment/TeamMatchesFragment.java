package com.example.var.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.var.BuildConfig;
import com.example.var.R;
import com.example.var.data.model.ApiResponse;
import com.example.var.data.model.MatchModel;
import com.example.var.data.repository.MatchRepository;
import com.example.var.databinding.FragmentTeamMatchesBinding;
import com.example.var.ui.adapter.MatchAdapter;
import com.example.var.util.FirebaseManager;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * TeamMatchesFragment - Bir takımın geçmiş ve gelecek maçlarını gösteren ekran.
 *
 * Açılma yolları:
 * 1. Arama sonucunda takıma tıklandığında (SearchDialogFragment)
 * 2. Puan tablosunda takıma tıklandığında (LeagueStandingsFragment)
 *
 * Özellikler:
 * - iSportsAPI /schedule/basic?teamId endpoint'inden maçları çeker
 * - Mevcut MatchAdapter ile ligelere göre gruplanmış liste gösterir
 * - Takımı favoriye ekle/çıkar (Firebase, toolbar yıldız butonu)
 * - Maça tıklanınca MatchDetailFragment açılır
 * - Pull-to-refresh ile yenileme
 *
 * Parametreler (Bundle üzerinden):
 * - ARG_TEAM_ID: Maçları çekilecek takımın ID'si
 * - ARG_TEAM_NAME: Toolbar başlığı
 * - ARG_LEAGUE_ID: Lig favorisi kontrolü için (opsiyonel)
 */
public class TeamMatchesFragment extends Fragment
        implements MatchAdapter.OnMatchClickListener {

    /** Bundle argüman anahtarları */
    private static final String ARG_TEAM_ID = "team_id";
    private static final String ARG_TEAM_NAME = "team_name";
    private static final String ARG_LEAGUE_ID = "league_id";
    private static final String ARG_LOGO_MAP = "logo_map";

    /** ViewBinding referansı */
    private FragmentTeamMatchesBinding binding;

    /** Maç listesi adaptörü */
    private MatchAdapter matchAdapter;

    /** Veri erişim katmanı */
    private MatchRepository repository;

    /** Takım ID'si (API isteği için) */
    private String teamId;

    /** Takım adı (toolbar başlığı için) */
    private String teamName;

    /** Ligin ID'si (lig favori kontrolü için, boş olabilir) */
    private String leagueId;

    /** Takım favori durumu */
    private boolean isFavorite = false;

    /**
     * Fragment oluşturma fabrika metodu.
     *
     * @param teamId    Maçları çekilecek takımın ID'si
     * @param teamName  Toolbar'da gösterilecek takım adı
     * @param leagueId  Takımın bağlı olduğu ligin ID'si (null olabilir)
     * @return Argümanları yüklenmiş TeamMatchesFragment örneği
     */
    public static TeamMatchesFragment newInstance(String teamId, String teamName, String leagueId) {
        return newInstance(teamId, teamName, leagueId, null);
    }

    public static TeamMatchesFragment newInstance(String teamId, String teamName, String leagueId,
            HashMap<String, String> logoMap) {
        TeamMatchesFragment fragment = new TeamMatchesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TEAM_ID, teamId);
        args.putString(ARG_TEAM_NAME, teamName);
        args.putString(ARG_LEAGUE_ID, leagueId != null ? leagueId : "");
        if (logoMap != null) args.putSerializable(ARG_LOGO_MAP, logoMap);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            teamId = getArguments().getString(ARG_TEAM_ID, "");
            teamName = getArguments().getString(ARG_TEAM_NAME, "");
            leagueId = getArguments().getString(ARG_LEAGUE_ID, "");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentTeamMatchesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new MatchRepository(BuildConfig.API_KEY);

        setupToolbar();
        setupRecyclerView();
        setupSwipeRefresh();
        checkFavoriteStatus();
        loadTeamMatches();
    }

    /**
     * Toolbar'ı yapılandırır.
     * Başlık olarak takım adını, geri butonu ve favori yıldız butonunu gösterir.
     */
    private void setupToolbar() {
        binding.toolbar.setTitle(teamName);
        binding.toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        // Favori yıldız butonuna tıklanınca favoriye ekle/çıkar
        binding.btnFavorite.setOnClickListener(v -> toggleFavorite());
    }

    /**
     * RecyclerView ve MatchAdapter'ı başlatır.
     * Mevcut MatchAdapter yeniden kullanılır (lig bazında gruplama özelliği ile).
     */
    @SuppressWarnings("unchecked")
    private void setupRecyclerView() {
        matchAdapter = new MatchAdapter(requireContext(), this);
        if (getArguments() != null) {
            HashMap<String, String> logoMap = (HashMap<String, String>)
                    getArguments().getSerializable(ARG_LOGO_MAP);
            if (logoMap != null) matchAdapter.setTeamLogoMap(logoMap);
        }
        binding.rvMatches.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvMatches.setAdapter(matchAdapter);
    }

    /**
     * SwipeRefreshLayout yapılandırması.
     */
    private void setupSwipeRefresh() {
        binding.swipeRefreshLayout.setColorSchemeResources(R.color.primary, R.color.secondary);
        binding.swipeRefreshLayout.setOnRefreshListener(this::loadTeamMatches);
    }

    /**
     * Firebase Firestore'dan takımın favori durumunu kontrol eder.
     * Favori ise yıldız dolu, değilse boş gösterilir.
     */
    private void checkFavoriteStatus() {
        if (!FirebaseManager.isLoggedIn()) return;

        FirebaseManager.isTeamOrLeagueFavorite(teamId, leagueId, isFav -> {
            if (!isAdded()) return;
            isFavorite = isFav;
            updateFavoriteIcon();
        });
    }

    /**
     * Favori butonunun ikonunu mevcut duruma göre günceller.
     */
    private void updateFavoriteIcon() {
        binding.btnFavorite.setIconResource(
                isFavorite ? R.drawable.ic_star_filled : R.drawable.ic_star
        );
    }

    /**
     * Takımı favoriye ekler veya çıkarır.
     * Giriş yapılmamışsa uyarı gösterir.
     */
    private void toggleFavorite() {
        if (!FirebaseManager.isLoggedIn()) {
            Snackbar.make(binding.getRoot(),
                    getString(R.string.login_required),
                    Snackbar.LENGTH_LONG).show();
            return;
        }

        if (isFavorite) {
            // Favoriden çıkar
            FirebaseManager.removeFavoriteTeam(teamId,
                    unused -> {
                        if (!isAdded()) return;
                        isFavorite = false;
                        updateFavoriteIcon();
                        Snackbar.make(binding.getRoot(),
                                getString(R.string.removed_from_favorites),
                                Snackbar.LENGTH_SHORT).show();
                    },
                    e -> showSnackbar(getString(R.string.error_loading))
            );
        } else {
            // Favoriye ekle
            FirebaseManager.addFavoriteTeam(teamId,
                    unused -> {
                        if (!isAdded()) return;
                        isFavorite = true;
                        updateFavoriteIcon();
                        Snackbar.make(binding.getRoot(),
                                getString(R.string.added_to_favorites),
                                Snackbar.LENGTH_SHORT).show();
                    },
                    e -> showSnackbar(getString(R.string.error_loading))
            );
        }
    }

    /**
     * Takımın maçlarını çeker.
     * API'de teamId filtresi olmadığı için leagueId ile tüm lig maçları alınır,
     * client tarafında homeId/awayId == teamId filtresiyle elenir.
     */
    private void loadTeamMatches() {
        showLoading();

        // leagueId varsa lig maçlarını çek ve filtrele, yoksa hata göster
        if (leagueId == null || leagueId.isEmpty()) {
            showEmpty(getString(R.string.no_matches));
            return;
        }

        repository.getLeagueMatches(leagueId).enqueue(new Callback<ApiResponse<MatchModel>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<MatchModel>> call,
                    @NonNull Response<ApiResponse<MatchModel>> response) {
                if (!isAdded()) return;
                binding.swipeRefreshLayout.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    List<MatchModel> all = response.body().getData();
                    List<MatchModel> teamMatches = new ArrayList<>();
                    if (all != null) {
                        for (MatchModel m : all) {
                            if (teamId.equals(m.getHomeId()) || teamId.equals(m.getAwayId())) {
                                teamMatches.add(m);
                            }
                        }
                    }
                    if (!teamMatches.isEmpty()) {
                        matchAdapter.setMatches(teamMatches);
                        showContent();
                    } else {
                        showEmpty(getString(R.string.no_matches));
                    }
                } else {
                    showEmpty(getString(R.string.error_loading));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<MatchModel>> call,
                    @NonNull Throwable t) {
                if (!isAdded()) return;
                binding.swipeRefreshLayout.setRefreshing(false);
                showEmpty(getString(R.string.error_loading));
            }
        });
    }

    /**
     * Maç listesindeki bir maça tıklandığında MatchDetailFragment'ı açar.
     * Mevcut HomeFragment akışıyla aynı navigasyon kullanılır.
     */
    @Override
    public void onMatchClick(MatchModel match) {
        MatchDetailFragment detailFragment = MatchDetailFragment.newInstance(match);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                                     R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.fragmentContainer, detailFragment)
                .addToBackStack(null)
                .commit();
    }

    // ===== UI Durum Metodları =====

    private void showLoading() {
        binding.loadingContainer.setVisibility(View.VISIBLE);
        binding.emptyContainer.setVisibility(View.GONE);
        binding.swipeRefreshLayout.setVisibility(View.GONE);
    }

    private void showContent() {
        binding.loadingContainer.setVisibility(View.GONE);
        binding.emptyContainer.setVisibility(View.GONE);
        binding.swipeRefreshLayout.setVisibility(View.VISIBLE);
    }

    private void showEmpty(String message) {
        binding.loadingContainer.setVisibility(View.GONE);
        binding.emptyContainer.setVisibility(View.VISIBLE);
        binding.swipeRefreshLayout.setVisibility(View.GONE);
        binding.tvEmptyMessage.setText(message);
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
