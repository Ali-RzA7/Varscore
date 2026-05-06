package com.example.var.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.var.ui.adapter.MatchEventAdapter;
import com.example.var.data.model.ApiResponse;
import com.example.var.data.model.EventModel;
import com.example.var.data.model.MatchModel;
import com.example.var.data.repository.MatchRepository;
import com.example.var.databinding.FragmentMatchSummaryBinding;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.util.Log;
import android.widget.Toast;

/**
 * MatchSummaryFragment - Maç özeti ve olay akışı sekmesi.
 */
public class MatchSummaryFragment extends Fragment {

    private static final String TAG = "MatchSummary";
    private FragmentMatchSummaryBinding binding;
    private MatchRepository repository;
    private String matchId;

    public static MatchSummaryFragment newInstance(String matchId) {
        MatchSummaryFragment fragment = new MatchSummaryFragment();
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
        // TODO: Centralize API Key
        repository = new MatchRepository("BuildConfig.API_KEY");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMatchSummaryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecyclerView();
        loadEvents();
    }

    private MatchEventAdapter adapter;

    private void setupRecyclerView() {
        adapter = new MatchEventAdapter();
        binding.rvEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvEvents.setAdapter(adapter);
    }

    private void loadEvents() {
        if (matchId == null) {
            Log.e(TAG, "matchId is null!");
            return;
        }
        
        Log.d(TAG, "Loading events for matchId: " + matchId);
        
        repository.getEvents(matchId).enqueue(new Callback<ApiResponse<EventModel>>() {
            @Override
            public void onResponse(Call<ApiResponse<EventModel>> call, Response<ApiResponse<EventModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Events response: " + response.body().getCode() + " - " + response.body().getMessage());
                    List<EventModel> events = response.body().getData();
                    if (events != null && !events.isEmpty()) {
                        adapter.setEvents(events);
                        binding.tvEmptyEvents.setVisibility(View.GONE);
                        binding.rvEvents.setVisibility(View.VISIBLE);
                    } else {
                        binding.tvEmptyEvents.setVisibility(View.VISIBLE);
                        binding.rvEvents.setVisibility(View.GONE);
                    }
                } else {
                    Log.e(TAG, "Events request failed: " + response.code());
                    binding.tvEmptyEvents.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<EventModel>> call, Throwable t) {
                Log.e(TAG, "Events request error: " + t.getMessage());
                binding.tvEmptyEvents.setVisibility(View.VISIBLE);
            }
        });

        // Maç detaylarını çek
        repository.getMatchDetail(matchId).enqueue(new Callback<ApiResponse<MatchModel>>() {
            @Override
            public void onResponse(Call<ApiResponse<MatchModel>> call, Response<ApiResponse<MatchModel>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null && !response.body().getData().isEmpty()) {
                    MatchModel detailedMatch = response.body().getData().get(0);
                    Log.d(TAG, "Detailed match info loaded: " + detailedMatch.getLocation());
                    binding.tvVenue.setText(detailedMatch.getLocation() != null ? detailedMatch.getLocation() : "-");
                    //binding.tvWeather.setText(detailedMatch.getWeather() != null ? detailedMatch.getWeather() : "-");
                    
                    // İlk Yarı Skoru
                    binding.tvHalfScore.setText(detailedMatch.getHomeHalfScore() + " - " + detailedMatch.getAwayHalfScore());

                    // Penaltılar
                    if (detailedMatch.getExtraExplain() != null && 
                       (detailedMatch.getExtraExplain().getPenHomeScore() > 0 || detailedMatch.getExtraExplain().getPenAwayScore() > 0)) {
                        binding.llPenalties.setVisibility(View.VISIBLE);
                        binding.tvPenScore.setText(detailedMatch.getExtraExplain().getPenHomeScore() + " - " + detailedMatch.getExtraExplain().getPenAwayScore());
                    }

                    // Uzatma Süresi
                    if (detailedMatch.getInjuryTime() > 0) {
                        binding.llInjuryTime.setVisibility(View.VISIBLE);
                        binding.tvInjuryTimeValue.setText("+" + detailedMatch.getInjuryTime() + " dk");
                    }

                    // VAR Olayı
                    if (detailedMatch.getVar() != null && !detailedMatch.getVar().isEmpty()) {
                        binding.llVar.setVisibility(View.VISIBLE);
                        binding.tvVarValue.setText(detailedMatch.getVar());
                    }
                    
                    if (detailedMatch.getExplain() != null && !detailedMatch.getExplain().isEmpty()) {
                         binding.tvReferee.setText(detailedMatch.getExplain());
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<MatchModel>> call, Throwable t) {
                Log.e(TAG, "Detail request error: " + t.getMessage());
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
