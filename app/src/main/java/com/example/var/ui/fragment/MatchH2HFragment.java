package com.example.var.ui.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.var.data.model.AnalysisResponse;
import com.example.var.data.model.AnalysisModel;
import com.example.var.data.repository.MatchRepository;
import com.example.var.databinding.FragmentMatchH2hBinding;
import com.example.var.ui.adapter.MatchH2HAdapter;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * MatchH2HFragment - Geçmiş rekabet (H2H) verilerini gösteren sekme.
 */
public class MatchH2HFragment extends Fragment {

    private static final String TAG = "MatchH2H";
    private FragmentMatchH2hBinding binding;
    private MatchRepository repository;
    private String matchId;
    private MatchH2HAdapter adapter;

    public static MatchH2HFragment newInstance(String matchId) {
        MatchH2HFragment fragment = new MatchH2HFragment();
        Bundle args = new Bundle();
        args.putString("match_id", matchId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            matchId = getArguments().getString("match_id");
        }
        repository = new MatchRepository("BuildConfig.API_KEY");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMatchH2hBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecyclerView();
        loadH2HData();
    }

    private void setupRecyclerView() {
        adapter = new MatchH2HAdapter();
        binding.rvH2h.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvH2h.setAdapter(adapter);
    }

    private void loadH2HData() {
        if (matchId == null) return;

        Log.d(TAG, "Fetching H2H data for matchId: " + matchId);

        repository.getAnalysis(matchId).enqueue(new Callback<AnalysisResponse>() {
            @Override
            public void onResponse(Call<AnalysisResponse> call, Response<AnalysisResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonElement dataElement = response.body().getData();
                    Log.d(TAG, "Raw data element received: " + (dataElement != null ? "Not Null" : "Null"));
                    
                    if (dataElement != null) {
                        try {
                            JsonElement actualData = null;
                            
                            // 1. ADIM: Liste mi Obje mi kontrolü (Python'daki isinstance kontrolü)
                            if (dataElement.isJsonArray() && dataElement.getAsJsonArray().size() > 0) {
                                actualData = dataElement.getAsJsonArray().get(0);
                                Log.d(TAG, "Data is an array, took first element");
                            } else if (dataElement.isJsonObject()) {
                                actualData = dataElement;
                                Log.d(TAG, "Data is an object");
                            }

                            // 2. ADIM: headToHead alanını manuel ara
                            if (actualData != null && actualData.isJsonObject()) {
                                JsonElement h2hElement = actualData.getAsJsonObject().get("headToHead");
                                
                                if (h2hElement != null && h2hElement.isJsonArray()) {
                                    List<String> h2hList = new ArrayList<>();
                                    for (JsonElement item : h2hElement.getAsJsonArray()) {
                                        if (item.isJsonPrimitive()) {
                                            h2hList.add(item.getAsString());
                                        }
                                    }

                                    if (!h2hList.isEmpty()) {
                                        Log.d(TAG, "H2H data extracted successfully: " + h2hList.size());
                                        adapter.setH2HData(h2hList);
                                        if (binding != null) {
                                            binding.tvEmptyH2h.setVisibility(View.GONE);
                                            binding.rvH2h.setVisibility(View.VISIBLE);
                                        }
                                        return; // Başarılı çıkış
                                    }
                                }
                            }
                            Log.w(TAG, "headToHead field not found or empty in data");
                        } catch (Exception e) {
                            Log.e(TAG, "Manual parsing error: " + e.getMessage());
                        }
                    }
                } else {
                    Log.e(TAG, "API error: " + response.code());
                }
                showEmptyState();
            }

            @Override
            public void onFailure(Call<AnalysisResponse> call, Throwable t) {
                Log.e(TAG, "Network failure: " + t.getMessage());
                showEmptyState();
            }
        });
    }

    private void showEmptyState() {
        if (binding != null) {
            binding.tvEmptyH2h.setVisibility(View.VISIBLE);
            binding.rvH2h.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
