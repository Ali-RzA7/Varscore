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
import com.example.var.util.GlobalTeamCache;
import com.example.var.util.LeagueCache;
import com.example.var.util.MatchCache;
import com.google.android.material.button.MaterialButton;
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
 * SearchDialogFragment — Takım ve lig arama diyaloğu.
 *
 * İki mod:
 *  - Günlük (Daily): Yalnızca seçili günün maçlarından takım/lig arar.
 *  - Genel  (Global): Tüm ligler (LeagueCache, 30 gün) + GlobalTeamCache (tarihten bağımsız).
 *
 * GlobalTeamCache: HomeFragment her maç yüklediğinde otomatik birikir.
 * İlk açılışta boşsa selectedDate API'den çekilerek seed oluşturulur.
 */
public class SearchDialogFragment extends DialogFragment
        implements SearchResultAdapter.OnSearchResultClickListener {

    private static final String ARG_SELECTED_DATE = "selected_date";
    private static final long DEBOUNCE_MS = 300;
    private static final String API_KEY = BuildConfig.API_KEY;

    private EditText etSearch;
    private RecyclerView rvSearchResults;
    private TextView tvNoResults;
    private TextView tvScopeHint;
    private MaterialButton btnSearchScope;
    private LinearProgressIndicator progressBar;
    private SearchResultAdapter adapter;
    private MatchRepository repo;

    private final List<SearchResultAdapter.SearchResultItem> allResults = new ArrayList<>();
    private final Set<String> addedTeamIds = new HashSet<>();
    private int pendingLoads = 0;
    private boolean isGlobalMode = false;
    private String selectedDate;

    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;

    // ================================================================
    // Factory
    // ================================================================

    public static SearchDialogFragment newInstance(String selectedDate) {
        SearchDialogFragment f = new SearchDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SELECTED_DATE, selectedDate);
        f.setArguments(args);
        return f;
    }

    // ================================================================
    // Oluşturma
    // ================================================================

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            selectedDate = getArguments().getString(ARG_SELECTED_DATE);
        }
        if (selectedDate == null) {
            selectedDate = DateUtils.formatForApi(Calendar.getInstance());
        }

        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_search, null);

        etSearch = view.findViewById(R.id.etSearch);
        rvSearchResults = view.findViewById(R.id.rvSearchResults);
        tvNoResults = view.findViewById(R.id.tvNoResults);
        tvScopeHint = view.findViewById(R.id.tvSearchScopeHint);
        btnSearchScope = view.findViewById(R.id.btnSearchScope);
        progressBar = view.findViewById(R.id.progressBarSearch);

        repo = new MatchRepository(API_KEY);

        adapter = new SearchResultAdapter(this);
        rvSearchResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSearchResults.setAdapter(adapter);

        setupScopeButton();
        setupSearchInput();
        loadForCurrentMode();

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .create();
    }

    // ================================================================
    // Kapsam Butonu
    // ================================================================

    private void setupScopeButton() {
        updateScopeUI();
        btnSearchScope.setOnClickListener(v -> {
            isGlobalMode = !isGlobalMode;
            updateScopeUI();
            resetAndReload();
        });
    }

    /** Buton rengini ve açıklama metnini moda göre günceller. */
    private void updateScopeUI() {
        if (isGlobalMode) {
            btnSearchScope.setText(getString(R.string.search_scope_global));
            btnSearchScope.setBackgroundTintList(
                    requireContext().getColorStateList(R.color.secondary));
            btnSearchScope.setTextColor(requireContext().getColor(R.color.on_secondary));
            btnSearchScope.setStrokeColorResource(R.color.secondary);
            tvScopeHint.setText(getString(R.string.search_scope_global_hint));
        } else {
            btnSearchScope.setText(getString(R.string.search_scope_daily));
            btnSearchScope.setBackgroundTintList(
                    requireContext().getColorStateList(android.R.color.transparent));
            btnSearchScope.setTextColor(requireContext().getColor(R.color.primary));
            btnSearchScope.setStrokeColorResource(R.color.primary);
            tvScopeHint.setText(getString(R.string.search_scope_daily_hint));
        }
    }

    // ================================================================
    // Arama Girişi
    // ================================================================

    private void setupSearchInput() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
                String q = s.toString().trim();
                debounceRunnable = () -> filterResults(q);
                debounceHandler.postDelayed(debounceRunnable, DEBOUNCE_MS);
            }
        });
    }

    // ================================================================
    // Veri Yükleme
    // ================================================================

    /** Modu sıfırla ve yeniden yükle (mod değiştiğinde çağrılır). */
    private void resetAndReload() {
        allResults.clear();
        addedTeamIds.clear();
        adapter.setResults(new ArrayList<>());
        tvNoResults.setVisibility(View.GONE);
        rvSearchResults.setVisibility(View.GONE);
        loadForCurrentMode();
    }

    private void loadForCurrentMode() {
        if (isGlobalMode) {
            loadGlobal();
        } else {
            loadDaily();
        }
    }

    // ---- Günlük Mod ----

    /**
     * Günlük mod: sadece selectedDate'in maçlarından takım/lig toplar.
     * Ligler de yalnızca o günkü maçlardan çıkarılır.
     */
    private void loadDaily() {
        List<MatchModel> cached = MatchCache.load(requireContext(), selectedDate);
        if (cached != null) {
            extractLeaguesFromMatches(cached);
            extractTeams(cached);
        } else {
            pendingLoads++;
            updateLoadingState();
            repo.getMatchesByDate(selectedDate).enqueue(new Callback<ApiResponse<MatchModel>>() {
                @Override
                public void onResponse(@NonNull Call<ApiResponse<MatchModel>> call,
                        @NonNull Response<ApiResponse<MatchModel>> response) {
                    if (!isAdded()) return;
                    pendingLoads--;
                    if (response.isSuccessful() && response.body() != null
                            && response.body().getData() != null) {
                        List<MatchModel> matches = response.body().getData();
                        MatchCache.save(requireContext(), selectedDate, matches);
                        extractLeaguesFromMatches(matches);
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
    }

    // ---- Genel Mod ----

    /**
     * Genel mod:
     *  1. TÜM ligler: LeagueCache (30 gün TTL) → /league/basic
     *  2. TÜM takımlar: GlobalTeamCache (tarihten bağımsız) → birikimli cache
     */
    private void loadGlobal() {
        loadAllLeagues();
        loadAllTeamsGlobal();
    }

    /** Tüm lig listesini LeagueCache'ten veya API'den yükler. */
    private void loadAllLeagues() {
        List<LeagueModel> cached = LeagueCache.load(requireContext());
        if (cached != null) { addLeaguesToResults(cached); return; }

        List<LeagueModel> stale = LeagueCache.loadAny(requireContext());
        if (stale != null) {
            addLeaguesToResults(stale);
            fetchLeaguesFromApi();
            return;
        }
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

    /**
     * GlobalTeamCache'ten tüm takımları yükler (tarihten tamamen bağımsız).
     * Cache boşsa selectedDate API'den çekilerek seed oluşturulur.
     */
    private void loadAllTeamsGlobal() {
        List<GlobalTeamCache.TeamEntry> teams = GlobalTeamCache.loadAny(requireContext());
        if (teams != null && !teams.isEmpty()) {
            addGlobalTeamsToResults(teams);
        } else {
            // Cache tamamen boş — selectedDate API'den çekerek seed oluştur
            seedGlobalTeamCache();
        }
    }

    /** GlobalTeamCache girişlerini sonuç listesine ekler. */
    private void addGlobalTeamsToResults(List<GlobalTeamCache.TeamEntry> teams) {
        for (GlobalTeamCache.TeamEntry team : teams) {
            String subtitle = team.leagueName != null
                    ? team.leagueName : getString(R.string.search_teams);
            allResults.add(new SearchResultAdapter.SearchResultItem(
                    "👕", team.teamName, subtitle,
                    team.teamId, "team", team.leagueId));
        }
        refreshFilter();
    }

    /**
     * GlobalTeamCache boşken çağrılır: selectedDate'i API'den çekip cache'i başlatır.
     * Yanıt gelince takımlar sonuç listesine eklenir.
     */
    private void seedGlobalTeamCache() {
        pendingLoads++;
        updateLoadingState();
        repo.getMatchesByDate(selectedDate).enqueue(new Callback<ApiResponse<MatchModel>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<MatchModel>> call,
                    @NonNull Response<ApiResponse<MatchModel>> response) {
                if (!isAdded()) return;
                pendingLoads--;
                if (response.isSuccessful() && response.body() != null
                        && response.body().getData() != null) {
                    List<MatchModel> matches = response.body().getData();
                    MatchCache.save(requireContext(), selectedDate, matches);
                    GlobalTeamCache.merge(requireContext(), matches);
                    List<GlobalTeamCache.TeamEntry> seeded = GlobalTeamCache.loadAny(requireContext());
                    if (seeded != null) addGlobalTeamsToResults(seeded);
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

    // ================================================================
    // Veri Çıkarma
    // ================================================================

    /** Maç listesinden benzersiz ligleri çıkarır (günlük mod için). */
    private void extractLeaguesFromMatches(List<MatchModel> matches) {
        Set<String> addedLeagueIds = new HashSet<>();
        for (MatchModel match : matches) {
            if (match.getLeagueId() == null || match.getLeagueName() == null) continue;
            if (addedLeagueIds.contains(match.getLeagueId())) continue;
            String icon = match.getLeagueType() == 2 ? "🏆" : "⚽";
            String subtitle = match.getLeagueType() == 2
                    ? getString(R.string.search_cup) : getString(R.string.search_leagues);
            allResults.add(new SearchResultAdapter.SearchResultItem(
                    icon, match.getLeagueName(), subtitle,
                    match.getLeagueId(), "league"));
            addedLeagueIds.add(match.getLeagueId());
        }
        refreshFilter();
    }

    /** LeagueModel listesinden sonuç öğeleri oluşturur (genel mod için). */
    private void addLeaguesToResults(List<LeagueModel> leagues) {
        for (LeagueModel league : leagues) {
            if (league.getLeagueId() == null || league.getName() == null) continue;
            String icon = league.getType() == 2 ? "🏆" : "⚽";
            String subtitle = league.getType() == 2
                    ? getString(R.string.search_cup) : getString(R.string.search_leagues);
            allResults.add(new SearchResultAdapter.SearchResultItem(
                    icon, league.getName(), subtitle,
                    league.getLeagueId(), "league"));
        }
        refreshFilter();
    }

    /** Maç listesinden benzersiz takımları çıkarır. */
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
    // Filtreleme
    // ================================================================

    private void refreshFilter() {
        if (etSearch != null) filterResults(etSearch.getText().toString().trim());
    }

    /**
     * Sorguyu küçük harfe çevirip tüm sonuçları filtreler.
     * Ligler önce, takımlar arkada gösterilir.
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

        List<SearchResultAdapter.SearchResultItem> merged =
                new ArrayList<>(leagues.size() + teams.size());
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
    // Sonuç Tıklaması
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
