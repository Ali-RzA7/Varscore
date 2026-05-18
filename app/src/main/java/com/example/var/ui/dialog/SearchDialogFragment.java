package com.example.var.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.var.BuildConfig;
import com.example.var.R;
import com.example.var.data.model.ApiResponse;
import com.example.var.data.model.LeagueModel;
import com.example.var.data.model.MatchModel;
import com.example.var.data.repository.MatchRepository;
import com.example.var.ui.adapter.SearchResultAdapter;
import com.example.var.util.DateUtils;
import com.example.var.util.LeagueCache;
import com.example.var.util.MatchCache;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * SearchDialogFragment - Genel arama dialog'u.
 *
 * İki veri kaynağı kullanır:
 *  1. Lig arama: LeagueCache → /league/basic tüm lig listesi (yüzlerce lig anında aranabilir)
 *  2. Takım arama: dün+bugün+yarın maç penceresi → geniş takım indexi
 *
 * Ligler önce sıralanır; takım satırlarında ait oldukları lig gösterilir.
 * Minimum 2 karakter girildikten sonra sonuçlar listelenir.
 * Debounce (300ms) ile gereksiz filtreleme engellenir.
 */
public class SearchDialogFragment extends DialogFragment
        implements SearchResultAdapter.OnSearchResultClickListener {

    private static final long DEBOUNCE_MS = 300;
    private static final String API_KEY = BuildConfig.API_KEY;

    private EditText etSearch;
    private RecyclerView rvSearchResults;
    private TextView tvNoResults;
    private LinearProgressIndicator progressBar;
    private SearchResultAdapter adapter;

    /** Tüm aranabilir sonuçlar (lig + takım karışık) */
    private final List<SearchResultAdapter.SearchResultItem> allResults = new ArrayList<>();

    /** Mükerrer takım girişini önlemek için teamId seti */
    private final Set<String> addedTeamIds = new HashSet<>();

    /** Uçuşta olan asenkron yükleme sayısı */
    private int pendingLoads = 0;

    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;

    private MatchRepository repo;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_search, null);

        etSearch = view.findViewById(R.id.etSearch);
        rvSearchResults = view.findViewById(R.id.rvSearchResults);
        tvNoResults = view.findViewById(R.id.tvNoResults);
        progressBar = view.findViewById(R.id.progressBarSearch);

        repo = new MatchRepository(API_KEY);

        adapter = new SearchResultAdapter(this);
        rvSearchResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSearchResults.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
                String query = s.toString().trim();
                debounceRunnable = () -> filterResults(query);
                debounceHandler.postDelayed(debounceRunnable, DEBOUNCE_MS);
            }
        });

        loadSearchableData();

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .create();
    }

    // ================================================================
    // Veri Yükleme
    // ================================================================

    private void loadSearchableData() {
        loadLeagues();
        loadTeamsFromMultipleDays();
    }

    /**
     * Tüm lig listesini LeagueCache'ten yükler.
     * Cache boşsa /league/basic API'sinden çeker ve cache'e kaydeder.
     * Bu sayede yüzlerce lig anında aranabilir hale gelir.
     */
    private void loadLeagues() {
        // Geçerli cache varsa hemen kullan
        List<LeagueModel> cached = LeagueCache.load(requireContext());
        if (cached != null) {
            addLeaguesToResults(cached);
            return;
        }

        // Süresi geçmiş eski cache varsa geçici olarak kullan
        List<LeagueModel> stale = LeagueCache.loadAny(requireContext());
        if (stale != null) {
            addLeaguesToResults(stale);
            // Arka planda taze veriyi çek
            fetchLeaguesFromApi();
            return;
        }

        // Cache yok — API'den çek
        fetchLeaguesFromApi();
    }

    private void fetchLeaguesFromApi() {
        pendingLoads++;
        updateLoadingState();

        repo.getLeagues().enqueue(new Callback<ApiResponse<LeagueModel>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<LeagueModel>> call,
                    @NonNull Response<ApiResponse<LeagueModel>> response) {
                if (!isAdded()) return;
                pendingLoads--;
                if (response.isSuccessful() && response.body() != null
                        && response.body().getData() != null) {
                    List<LeagueModel> leagues = response.body().getData();
                    LeagueCache.save(requireContext(), leagues);
                    addLeaguesToResults(leagues);
                }
                updateLoadingState();
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<LeagueModel>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                pendingLoads--;
                updateLoadingState();
            }
        });
    }

    private void addLeaguesToResults(List<LeagueModel> leagues) {
        for (LeagueModel league : leagues) {
            if (league.getLeagueId() == null || league.getName() == null) continue;
            String icon = league.getType() == 2 ? "🏆" : "⚽";
            String subtitle = league.getType() == 2
                    ? getString(R.string.search_cup)
                    : getString(R.string.search_leagues);
            allResults.add(new SearchResultAdapter.SearchResultItem(
                    icon, league.getName(), subtitle,
                    league.getLeagueId(), "league"));
        }
        refreshFilter();
    }

    /**
     * Dün, bugün ve yarınki maçlardan takımları toplar.
     * Birden fazla günden veri gelmesi, sadece bugün oynamayan takımları da
     * aranabilir kılar. Cache'te olmayanlar için sadece bugünü API'den çeker.
     */
    private void loadTeamsFromMultipleDays() {
        Calendar base = Calendar.getInstance();
        for (int offset = -1; offset <= 1; offset++) {
            Calendar day = (Calendar) base.clone();
            day.add(Calendar.DAY_OF_YEAR, offset);
            String dateStr = DateUtils.formatForApi(day);

            List<MatchModel> cached = MatchCache.load(requireContext(), dateStr);
            if (cached != null) {
                extractTeams(cached);
            } else if (offset == 0) {
                // Sadece bugünü API'den çek (rate limit aşımını önlemek için)
                fetchTeamsForDate(dateStr);
            }
        }
    }

    private void fetchTeamsForDate(String date) {
        pendingLoads++;
        updateLoadingState();

        repo.getMatchesByDate(date).enqueue(new Callback<ApiResponse<MatchModel>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<MatchModel>> call,
                    @NonNull Response<ApiResponse<MatchModel>> response) {
                if (!isAdded()) return;
                pendingLoads--;
                if (response.isSuccessful() && response.body() != null
                        && response.body().getData() != null) {
                    List<MatchModel> matches = response.body().getData();
                    MatchCache.save(requireContext(), date, matches);
                    extractTeams(matches);
                }
                updateLoadingState();
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<MatchModel>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                pendingLoads--;
                updateLoadingState();
            }
        });
    }

    private void extractTeams(List<MatchModel> matches) {
        boolean changed = false;
        for (MatchModel match : matches) {
            if (match.getHomeId() != null && match.getHomeName() != null
                    && !addedTeamIds.contains(match.getHomeId())) {
                allResults.add(new SearchResultAdapter.SearchResultItem(
                        "👕", match.getHomeName(),
                        match.getLeagueName() != null ? match.getLeagueName() : getString(R.string.search_teams),
                        match.getHomeId(), "team", match.getLeagueId()));
                addedTeamIds.add(match.getHomeId());
                changed = true;
            }
            if (match.getAwayId() != null && match.getAwayName() != null
                    && !addedTeamIds.contains(match.getAwayId())) {
                allResults.add(new SearchResultAdapter.SearchResultItem(
                        "👕", match.getAwayName(),
                        match.getLeagueName() != null ? match.getLeagueName() : getString(R.string.search_teams),
                        match.getAwayId(), "team", match.getLeagueId()));
                addedTeamIds.add(match.getAwayId());
                changed = true;
            }
        }
        if (changed) refreshFilter();
    }

    // ================================================================
    // Filtreleme ve UI
    // ================================================================

    /** Mevcut arama metni ile filtreyi yeniden uygular. */
    private void refreshFilter() {
        if (etSearch != null) {
            filterResults(etSearch.getText().toString().trim());
        }
    }

    /**
     * Arama metnine göre sonuçları filtreler.
     * Ligler önce, takımlar arkada sıralanır.
     * 2 karakterden az girişte liste gizlenir.
     */
    private void filterResults(String query) {
        if (query.length() < 2) {
            adapter.setResults(new ArrayList<>());
            tvNoResults.setVisibility(View.GONE);
            rvSearchResults.setVisibility(View.GONE);
            return;
        }

        String lower = query.toLowerCase(Locale.getDefault());
        List<SearchResultAdapter.SearchResultItem> leagues = new ArrayList<>();
        List<SearchResultAdapter.SearchResultItem> teams = new ArrayList<>();

        for (SearchResultAdapter.SearchResultItem item : allResults) {
            if (!item.name.toLowerCase(Locale.getDefault()).contains(lower)) continue;
            if ("league".equals(item.category)) leagues.add(item);
            else teams.add(item);
        }

        List<SearchResultAdapter.SearchResultItem> merged = new ArrayList<>(leagues.size() + teams.size());
        merged.addAll(leagues);
        merged.addAll(teams);

        adapter.setResults(merged);

        boolean empty = merged.isEmpty();
        tvNoResults.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvSearchResults.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void updateLoadingState() {
        if (progressBar != null) {
            progressBar.setVisibility(pendingLoads > 0 ? View.VISIBLE : View.GONE);
        }
    }

    // ================================================================
    // Tıklama: Seçilen sonuca yönlendir
    // ================================================================

    @Override
    public void onSearchResultClick(SearchResultAdapter.SearchResultItem item) {
        dismiss();

        androidx.fragment.app.Fragment target;
        if ("team".equals(item.category)) {
            target = com.example.var.ui.fragment.TeamMatchesFragment.newInstance(
                    item.id, item.name, item.leagueId);
        } else {
            target = com.example.var.ui.fragment.LeagueStandingsFragment.newInstance(
                    item.id, item.name);
        }

        getParentFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        R.anim.slide_in_right, R.anim.slide_out_left,
                        R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.fragmentContainer, target)
                .addToBackStack(null)
                .commit();
    }

    // ================================================================
    // Lifecycle
    // ================================================================

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        debounceHandler.removeCallbacksAndMessages(null);
    }
}
