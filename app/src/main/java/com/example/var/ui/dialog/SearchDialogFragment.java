package com.example.var.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
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

import com.example.var.R;
import com.example.var.data.model.ApiResponse;
import com.example.var.data.model.MatchModel;
import com.example.var.data.repository.MatchRepository;
import com.example.var.ui.adapter.SearchResultAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * SearchDialogFragment - Arama dialog'u.
 * Kullanıcı takım veya lig adı arayabilir.
 * Günün maçlarından takım ve lig bilgileri çekilerek filtrelenir.
 */
public class SearchDialogFragment extends DialogFragment
        implements SearchResultAdapter.OnSearchResultClickListener {

    private EditText etSearch;
    private RecyclerView rvSearchResults;
    private TextView tvNoResults;
    private SearchResultAdapter adapter;

    /** Tüm aranabilir sonuçlar (maçlardan çıkarılan takım ve lig listesi) */
    private final List<SearchResultAdapter.SearchResultItem> allResults = new ArrayList<>();

    private static final String API_KEY = "BuildConfig.API_KEY";

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_search, null);

        etSearch = view.findViewById(R.id.etSearch);
        rvSearchResults = view.findViewById(R.id.rvSearchResults);
        tvNoResults = view.findViewById(R.id.tvNoResults);

        // RecyclerView kurulumu
        adapter = new SearchResultAdapter(this);
        rvSearchResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSearchResults.setAdapter(adapter);

        // Arama metni değiştiğinde filtreleme yap
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {
            }

            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                filterResults(s.toString().trim());
            }
        });

        // Bugünün maçlarından aranabilir verileri yükle
        loadSearchableData();

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .create();
    }

    /**
     * Bugünün maçlarından takım ve lig isimlerini çeker.
     */
    private void loadSearchableData() {
        MatchRepository repo = new MatchRepository(API_KEY);
        String today = com.example.var.util.DateUtils.formatForApi(Calendar.getInstance());

        repo.getMatchesByDate(today).enqueue(new Callback<ApiResponse<MatchModel>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<MatchModel>> call,
                    @NonNull Response<ApiResponse<MatchModel>> response) {
                if (!isAdded())
                    return;
                if (response.isSuccessful() && response.body() != null
                        && response.body().getData() != null) {
                    extractSearchItems(response.body().getData());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<MatchModel>> call,
                    @NonNull Throwable t) {
                // Sessizce başarısız ol
            }
        });
    }

    /**
     * Maç listesinden benzersiz takım ve lig bilgilerini çıkarır.
     */
    private void extractSearchItems(List<MatchModel> matches) {
        Set<String> addedTeams = new HashSet<>();
        Set<String> addedLeagues = new HashSet<>();

        for (MatchModel match : matches) {
            // Takımları ekle
            if (match.getHomeName() != null && !addedTeams.contains(match.getHomeName())) {
                allResults.add(new SearchResultAdapter.SearchResultItem(
                        "⚽", match.getHomeName(),
                        getString(R.string.search_teams),
                        match.getHomeId(), "team"));
                addedTeams.add(match.getHomeName());
            }
            if (match.getAwayName() != null && !addedTeams.contains(match.getAwayName())) {
                allResults.add(new SearchResultAdapter.SearchResultItem(
                        "⚽", match.getAwayName(),
                        getString(R.string.search_teams),
                        match.getAwayId(), "team"));
                addedTeams.add(match.getAwayName());
            }
            // Ligleri ekle
            if (match.getLeagueName() != null && !addedLeagues.contains(match.getLeagueName())) {
                allResults.add(new SearchResultAdapter.SearchResultItem(
                        "🏆", match.getLeagueName(),
                        getString(R.string.search_leagues),
                        match.getLeagueId(), "league"));
                addedLeagues.add(match.getLeagueName());
            }
        }
    }

    /**
     * Arama metnine göre sonuçları filtreler.
     */
    private void filterResults(String query) {
        if (query.isEmpty()) {
            adapter.setResults(allResults);
            tvNoResults.setVisibility(allResults.isEmpty() ? View.VISIBLE : View.GONE);
            rvSearchResults.setVisibility(allResults.isEmpty() ? View.GONE : View.VISIBLE);
            return;
        }

        List<SearchResultAdapter.SearchResultItem> filtered = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        for (SearchResultAdapter.SearchResultItem item : allResults) {
            if (item.name.toLowerCase().contains(lowerQuery)) {
                filtered.add(item);
            }
        }

        adapter.setResults(filtered);
        tvNoResults.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        rvSearchResults.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onSearchResultClick(SearchResultAdapter.SearchResultItem item) {
        // Sonuca tıklanınca snackbar göster (ileride detay sayfasına yönlendirme)
        if (getView() != null) {
            Snackbar.make(getView(), item.name, Snackbar.LENGTH_SHORT).show();
        }
        dismiss();
    }
}
