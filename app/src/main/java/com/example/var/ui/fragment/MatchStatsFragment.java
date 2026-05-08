package com.example.var.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.var.BuildConfig;
import com.example.var.ui.adapter.MatchStatAdapter;
import com.example.var.data.model.ApiResponse;
import com.example.var.data.model.StatModel;
import com.example.var.data.repository.MatchRepository;
import com.example.var.databinding.FragmentMatchStatsBinding;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.var.data.model.MatchModel;
import com.example.var.data.model.MatchStatsResponse;

/**
 * MatchStatsFragment - Maç istatistikleri sekmesi.
 */
public class MatchStatsFragment extends Fragment {

    private FragmentMatchStatsBinding binding;
    private MatchRepository repository;
    private String matchId;
    private MatchModel match;
    private MatchStatAdapter adapter;

    public static MatchStatsFragment newInstance(MatchModel match) {
        MatchStatsFragment fragment = new MatchStatsFragment();
        Bundle args = new Bundle();
        args.putSerializable("match_data", match);
        args.putString("match_id", match.getMatchId());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            matchId = getArguments().getString("match_id");
            match = getArguments().getSerializable("match_data", MatchModel.class);
        }
        repository = new MatchRepository(BuildConfig.API_KEY);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMatchStatsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter = new MatchStatAdapter();
        binding.rvStats.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvStats.setAdapter(adapter);
        loadStats();
    }

    private void loadStats() {
        if (matchId == null) return;

        // Python'daki gibi /stats endpoint'ini kullanıyoruz
        repository.getStats(matchId).enqueue(new Callback<ApiResponse<MatchStatsResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<MatchStatsResponse>> call, Response<ApiResponse<MatchStatsResponse>> response) {
                List<StatModel> finalStats = new java.util.ArrayList<>();
                
                // 1. Önce temel verileri ekle (Kart/Korner - MatchModel'den gelenler)
                if (match != null) {
                    addBasicStats(finalStats);
                }

                if (response.isSuccessful() && response.body() != null && response.body().getData() != null && !response.body().getData().isEmpty()) {
                    // /stats endpoint'i [ { matchId, stats: [] } ] yapısında döner
                    List<StatModel> detailedStats = response.body().getData().get(0).getStats();
                    if (detailedStats != null) {
                        for (StatModel s : detailedStats) {
                            if (!isAlreadyAdded(finalStats, s)) {
                                finalStats.add(s);
                            }
                        }
                    }
                }

                if (!finalStats.isEmpty()) {
                    adapter.setStats(finalStats);
                    if (binding != null) {
                        binding.tvEmptyStats.setVisibility(View.GONE);
                        binding.rvStats.setVisibility(View.VISIBLE);
                    }
                } else {
                    // Eğer /stats boşsa eski yöntemi (analysis/statistics) dene (Yedek Plan)
                    loadBackupStats(finalStats);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<MatchStatsResponse>> call, Throwable t) {
                List<StatModel> finalStats = new java.util.ArrayList<>();
                if (match != null) addBasicStats(finalStats);
                loadBackupStats(finalStats);
            }
        });
    }

    private void loadBackupStats(List<StatModel> initialList) {
        repository.getStatistics(matchId).enqueue(new Callback<ApiResponse<StatModel>>() {
            @Override
            public void onResponse(Call<ApiResponse<StatModel>> call, Response<ApiResponse<StatModel>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    for (StatModel s : response.body().getData()) {
                        if (!isAlreadyAdded(initialList, s)) {
                            initialList.add(s);
                        }
                    }
                }
                updateUI(initialList);
            }

            @Override
            public void onFailure(Call<ApiResponse<StatModel>> call, Throwable t) {
                updateUI(initialList);
            }
        });
    }

    private void updateUI(List<StatModel> list) {
        if (binding == null) return;
        if (!list.isEmpty()) {
            adapter.setStats(list);
            binding.tvEmptyStats.setVisibility(View.GONE);
            binding.rvStats.setVisibility(View.VISIBLE);
        } else {
            binding.tvEmptyStats.setVisibility(View.VISIBLE);
            binding.rvStats.setVisibility(View.GONE);
        }
    }

    private void addBasicStats(List<StatModel> list) {
        // Kornerler (Type 6)
        if (match.getHomeCorner() > 0 || match.getAwayCorner() > 0) {
            StatModel s = new StatModel(6, String.valueOf(match.getHomeCorner()), String.valueOf(match.getAwayCorner()));
            if (s.getTypeName() != null) list.add(s);
        }
        // Sarı Kartlar (Type 11 - Python/Stats endpoint standardı tercih edildi)
        if (match.getHomeYellow() > 0 || match.getAwayYellow() > 0) {
            StatModel s = new StatModel(11, String.valueOf(match.getHomeYellow()), String.valueOf(match.getAwayYellow()));
            if (s.getTypeName() != null) list.add(s);
        }
        // Kırmızı Kartlar (Type 13)
        if (match.getHomeRed() > 0 || match.getAwayRed() > 0) {
            StatModel s = new StatModel(13, String.valueOf(match.getHomeRed()), String.valueOf(match.getAwayRed()));
            if (s.getTypeName() != null) list.add(s);
        }
    }

    private boolean isAlreadyAdded(List<StatModel> list, StatModel newStat) {
        String newName = newStat.getTypeName();
        if (newName == null) return true; // İsmi olmayanı zaten listeye almayacağız, o yüzden eklenmiş gibi davran (atla)
        for (StatModel s : list) {
            if (newName.equals(s.getTypeName())) return true;
        }
        return false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
