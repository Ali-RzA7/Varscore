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

import com.example.var.BuildConfig;
import com.example.var.ui.adapter.MatchEventAdapter;
import com.example.var.data.model.ApiResponse;
import com.example.var.data.model.EventModel;
import com.example.var.data.model.EventsResponse;
import com.example.var.data.model.MatchModel;
import com.example.var.data.repository.MatchRepository;
import com.example.var.databinding.FragmentMatchSummaryBinding;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MatchSummaryFragment extends Fragment {

    private static final String TAG = "MatchSummary";
    private static final String ARG_MATCH = "match_data";

    private FragmentMatchSummaryBinding binding;
    private MatchRepository repository;
    private MatchModel match;
    private MatchEventAdapter adapter;

    public static MatchSummaryFragment newInstance(MatchModel match) {
        MatchSummaryFragment fragment = new MatchSummaryFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_MATCH, match);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            match = getArguments().getSerializable(ARG_MATCH, MatchModel.class);
        }
        repository = new MatchRepository(BuildConfig.API_KEY);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentMatchSummaryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecyclerView();
        loadEvents();
        loadMatchDetail();
    }

    private void setupRecyclerView() {
        adapter = new MatchEventAdapter();
        binding.rvEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvEvents.setAdapter(adapter);
    }

    private void loadEvents() {
        if (match == null || match.getMatchId() == null) return;

        String dateStr = matchDateString(match.getMatchTime());
        Log.d(TAG, "Loading events for matchId=" + match.getMatchId() + " date=" + dateStr);

        repository.getEvents(dateStr).enqueue(new Callback<ApiResponse<EventsResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<EventsResponse>> call,
                    @NonNull Response<ApiResponse<EventsResponse>> response) {
                if (!isAdded() || binding == null) return;

                List<EventModel> matchEvents = new ArrayList<>();
                if (response.isSuccessful() && response.body() != null
                        && response.body().getData() != null) {
                    for (EventsResponse er : response.body().getData()) {
                        if (match.getMatchId().equals(er.getMatchId()) && er.getEvents() != null) {
                            matchEvents.addAll(er.getEvents());
                            break;
                        }
                    }
                }

                if (!matchEvents.isEmpty()) {
                    adapter.setEvents(matchEvents);
                    binding.tvEmptyEvents.setVisibility(View.GONE);
                    binding.rvEvents.setVisibility(View.VISIBLE);
                } else {
                    binding.tvEmptyEvents.setVisibility(View.VISIBLE);
                    binding.rvEvents.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<EventsResponse>> call,
                    @NonNull Throwable t) {
                if (!isAdded() || binding == null) return;
                Log.e(TAG, "Events request error: " + t.getMessage());
                binding.tvEmptyEvents.setVisibility(View.VISIBLE);
            }
        });
    }

    private void loadMatchDetail() {
        if (match == null || match.getMatchId() == null) return;

        repository.getMatchDetail(match.getMatchId()).enqueue(new Callback<ApiResponse<MatchModel>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<MatchModel>> call,
                    @NonNull Response<ApiResponse<MatchModel>> response) {
                if (!isAdded() || binding == null) return;
                if (response.isSuccessful() && response.body() != null
                        && response.body().getData() != null
                        && !response.body().getData().isEmpty()) {
                    MatchModel detail = response.body().getData().get(0);

                    binding.tvVenue.setText(detail.getLocation() != null ? detail.getLocation() : "-");
                    binding.tvHalfScore.setText(detail.getHomeHalfScore() + " - " + detail.getAwayHalfScore());

                    if (detail.getExtraExplain() != null
                            && (detail.getExtraExplain().getPenHomeScore() > 0
                                    || detail.getExtraExplain().getPenAwayScore() > 0)) {
                        binding.llPenalties.setVisibility(View.VISIBLE);
                        binding.tvPenScore.setText(detail.getExtraExplain().getPenHomeScore()
                                + " - " + detail.getExtraExplain().getPenAwayScore());
                    }

                    if (detail.getInjuryTime() > 0) {
                        binding.llInjuryTime.setVisibility(View.VISIBLE);
                        binding.tvInjuryTimeValue.setText("+" + detail.getInjuryTime() + " dk");
                    }

                    if (detail.getVar() != null && !detail.getVar().isEmpty()) {
                        binding.llVar.setVisibility(View.VISIBLE);
                        binding.tvVarValue.setText(detail.getVar());
                    }

                    if (detail.getExplain() != null && !detail.getExplain().isEmpty()) {
                        binding.tvReferee.setText(detail.getExplain());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<MatchModel>> call, @NonNull Throwable t) {
                Log.e(TAG, "Detail request error: " + t.getMessage());
            }
        });
    }

    private static String matchDateString(long matchTimeUnix) {
        if (matchTimeUnix <= 0) {
            Calendar today = Calendar.getInstance();
            return formatDate(today);
        }
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(matchTimeUnix * 1000L);
        return formatDate(cal);
    }

    private static String formatDate(Calendar cal) {
        return String.format("%d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
