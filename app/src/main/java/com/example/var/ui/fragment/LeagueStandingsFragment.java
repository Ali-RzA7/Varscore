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
import com.example.var.R;
import com.example.var.data.model.StandingLeagueResponse;
import com.example.var.data.model.StandingModel;
import com.example.var.data.repository.MatchRepository;
import com.example.var.databinding.FragmentLeagueStandingsBinding;
import com.example.var.ui.adapter.StandingsTableAdapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.var.util.FirebaseManager;
import com.google.android.material.snackbar.Snackbar;



import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * LeagueStandingsFragment - Belirli bir ligin puan tablosu ekranı.
 *
 * StandingsFragment'tan veya SearchDialogFragment'tan lig seçilince açılır.
 * Seçilen ligin puan tablosunu iSportsAPI /league/table endpoint'inden çeker.
 *
 * Gösterilen Sütunlar: Sıra | Takım | O | G | B | M | A | P
 * (O=Oynanan, G=Galibiyet, B=Beraberlik, M=Mağlubiyet, A=Averaj, P=Puan)
 *
 * Navigasyon:
 * - Toolbar geri butonu → StandingsFragment'a döner (back stack)
 * - Takıma tıklanınca → TeamMatchesFragment.newInstance(teamId, teamName)
 *
 * Parametreler (Bundle üzerinden):
 * - ARG_LEAGUE_ID: Puan tablosu çekilecek ligin ID'si
 * - ARG_LEAGUE_NAME: Toolbar'da gösterilecek lig adı
 */
public class LeagueStandingsFragment extends Fragment
        implements StandingsTableAdapter.OnTeamClickListener {

    /** Bundle argüman anahtarları */
    private static final String ARG_LEAGUE_ID = "league_id";
    private static final String ARG_LEAGUE_NAME = "league_name";

    /** ViewBinding referansı */
    private FragmentLeagueStandingsBinding binding;

    /** Puan tablosu adaptörü */
    private StandingsTableAdapter standingsAdapter;

    /** Veri erişim katmanı */
    private MatchRepository repository;

    /** Gösterilecek ligin ID'si */
    private String leagueId;

    /** Toolbar'da gösterilecek lig adı */
    private String leagueName;

    /** Lig favori durumu */
    private boolean isFavorite = false;

    /** Standings'ten elde edilen teamId→logoUrl haritası; TeamMatchesFragment'a aktarılır */
    private final HashMap<String, String> teamLogoMap = new HashMap<>();

    /**
     * Fragment oluşturma fabrika metodu.
     * newInstance kullanımı, Fragment oluşturmada tavsiye edilen yaklaşımdır.
     *
     * @param leagueId   Puan tablosu çekilecek ligin ID'si
     * @param leagueName Toolbar'da gösterilecek lig adı
     * @return Argümanları yüklenmiş LeagueStandingsFragment örneği
     */
    public static LeagueStandingsFragment newInstance(String leagueId, String leagueName) {
        LeagueStandingsFragment fragment = new LeagueStandingsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_LEAGUE_ID, leagueId);
        args.putString(ARG_LEAGUE_NAME, leagueName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Argümanları al
        if (getArguments() != null) {
            leagueId = getArguments().getString(ARG_LEAGUE_ID, "");
            leagueName = getArguments().getString(ARG_LEAGUE_NAME, "");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentLeagueStandingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new MatchRepository(BuildConfig.API_KEY);

        setupToolbar();
        setupRecyclerView();
        checkFavoriteStatus();
        loadStandings();
    }

    /**
     * Toolbar'ı yapılandırır.
     * Başlık olarak lig adını, sol tarafta geri okunu gösterir.
     */
    private void setupToolbar() {
        // Lig adını toolbar başlığı olarak ayarla
        binding.toolbar.setTitle(leagueName);

        // Geri butonuna tıklanınca bir önceki sayfaya dön
        binding.toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        // Favori butonuna tıklandığında işlemi yap
        binding.btnFavorite.setOnClickListener(v -> toggleFavorite());
    }

    /**
     * RecyclerView ve StandingsTableAdapter'ı başlatır.
     */
    private void setupRecyclerView() {
        standingsAdapter = new StandingsTableAdapter(this);
        binding.rvStandings.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvStandings.setAdapter(standingsAdapter);
    }

    /**
     * iSportsAPI /league/table endpoint'inden puan tablosunu çeker.
     * Yükleme, başarı ve hata durumlarını yönetir.
     */
    private void loadStandings() {
        showLoading();

        repository.getLeagueTable(leagueId).enqueue(new Callback<StandingLeagueResponse>() {
            @Override
            public void onResponse(@NonNull Call<StandingLeagueResponse> call,
                    @NonNull Response<StandingLeagueResponse> response) {
                if (!isAdded() || binding == null) return;

                Log.d("STANDINGS", "HTTP " + response.code() + " leagueId=" + leagueId);

                if (response.isSuccessful() && response.body() != null) {
                    Log.d("STANDINGS", "code=" + response.body().getCode()
                            + " success=" + response.body().isSuccess());
                }

                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()
                        && response.body().getData() != null) {
                    StandingLeagueResponse.StandingData data = response.body().getData();
                    List<StandingModel> standings = data.getTotalStandings();

                    Log.d("STANDINGS", "standings count=" + (standings != null ? standings.size() : "null"));

                    if (standings != null && !standings.isEmpty()) {
                        Log.d("STANDINGS", "sample teamId=" + standings.get(0).getTeamId());

                        // teamInfos is at data level (not inside leagueInfo)
                        List<StandingLeagueResponse.TeamInfo> teamInfos = data.getTeamInfos();
                        Log.d("STANDINGS", "teamInfos=" + (teamInfos == null ? "NULL" : teamInfos.size() + " entries"));

                        if (teamInfos != null && !teamInfos.isEmpty()) {
                            Log.d("STANDINGS", "sample teamInfo: id=" + teamInfos.get(0).getTeamId()
                                    + " name=" + teamInfos.get(0).getName()
                                    + " logo=" + teamInfos.get(0).getLogo());

                            Map<String, StandingLeagueResponse.TeamInfo> teamInfoMap = new HashMap<>();
                            for (StandingLeagueResponse.TeamInfo ti : teamInfos) {
                                String id = ti.getTeamId();
                                if (id != null) teamInfoMap.put(id, ti);
                            }
                            teamLogoMap.clear();
                            for (StandingModel s : standings) {
                                StandingLeagueResponse.TeamInfo ti = teamInfoMap.get(s.getTeamId());
                                if (ti != null) {
                                    s.setTeamName(ti.getName());
                                    s.setLogoUrl(ti.getLogo());
                                    if (ti.getLogo() != null && !ti.getLogo().isEmpty()) {
                                        teamLogoMap.put(s.getTeamId(), ti.getLogo());
                                    }
                                }
                            }
                            Log.d("STANDINGS", "merge done, first name=" + standings.get(0).getTeamName()
                                    + " logoMap size=" + teamLogoMap.size());
                        }
                        standingsAdapter.setStandings(standings);
                        showContent();
                    } else {
                        showEmpty(getString(R.string.no_standings));
                    }
                } else {
                    Log.d("STANDINGS", "response failed or data null, body=" + response.body());
                    showEmpty(getString(R.string.error_loading));
                }
            }

            @Override
            public void onFailure(@NonNull Call<StandingLeagueResponse> call,
                    @NonNull Throwable t) {
                if (!isAdded() || binding == null) return;
                Log.e("STANDINGS", "onFailure: " + t.getMessage());
                showEmpty(getString(R.string.error_loading));
            }
        });
    }

    /**
     * Puan tablosundaki bir takıma tıklandığında TeamMatchesFragment'ı açar.
     * Animasyonlu geçiş ve back stack kullanılır.
     *
     * @param standing Tıklanan takımın puan tablosu verisi
     */
    @Override
    public void onTeamClick(StandingModel standing) {
        Fragment target = TeamMatchesFragment.newInstance(
                standing.getTeamId(),
                standing.getTeamName(),
                leagueId,
                teamLogoMap
        );
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                                     R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.fragmentContainer, target)
                .addToBackStack(null)
                .commit();
    }

    /**
     * Firebase Firestore'dan ligin favori durumunu kontrol eder.
     * Favori ise yıldız dolu, değilse boş gösterilir.
     */
    private void checkFavoriteStatus() {
        if (!FirebaseManager.isLoggedIn()) return;

        // isTeamOrLeagueFavorite method can check league alone if teamId is null.
        FirebaseManager.isTeamOrLeagueFavorite(null, leagueId, isFav -> {
            if (!isAdded() || binding == null) return;
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
     * Ligi favoriye ekler veya çıkarır.
     * Giriş yapılmamışsa uyarı gösterir.
     */
    private void toggleFavorite() {
        if (!FirebaseManager.isLoggedIn()) {
            showSnackbar(getString(R.string.login_required));
            return;
        }

        if (isFavorite) {
            // Favoriden çıkar
            FirebaseManager.removeFavoriteLeague(leagueId,
                    unused -> {
                        if (!isAdded() || binding == null) return;
                        isFavorite = false;
                        updateFavoriteIcon();
                        showSnackbar(getString(R.string.removed_from_favorites));
                    },
                    e -> showSnackbar(getString(R.string.error_loading))
            );
        } else {
            // Favoriye ekle
            FirebaseManager.addFavoriteLeague(leagueId, leagueName,
                    unused -> {
                        if (!isAdded() || binding == null) return;
                        isFavorite = true;
                        updateFavoriteIcon();
                        showSnackbar(getString(R.string.added_to_favorites));
                    },
                    e -> showSnackbar(getString(R.string.error_loading))
            );
        }
    }

    private void showSnackbar(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT).show();
        }
    }

    // ===== UI Durum Metodları =====

    private void showLoading() {
        binding.loadingContainer.setVisibility(View.VISIBLE);
        binding.emptyContainer.setVisibility(View.GONE);
        binding.rvStandings.setVisibility(View.GONE);
    }

    private void showContent() {
        binding.loadingContainer.setVisibility(View.GONE);
        binding.emptyContainer.setVisibility(View.GONE);
        binding.rvStandings.setVisibility(View.VISIBLE);
    }

    private void showEmpty(String message) {
        binding.loadingContainer.setVisibility(View.GONE);
        binding.emptyContainer.setVisibility(View.VISIBLE);
        binding.rvStandings.setVisibility(View.GONE);
        binding.tvEmptyMessage.setText(message);
        binding.btnRetry.setOnClickListener(v -> loadStandings());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
