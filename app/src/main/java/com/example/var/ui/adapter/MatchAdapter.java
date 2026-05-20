package com.example.var.ui.adapter;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.var.R;
import com.example.var.data.model.MatchModel;
import com.example.var.util.DateUtils;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * MatchAdapter - Maçları lig bazında gruplanmış olarak listeleyen adapter.
 * İki farklı ViewType kullanır:
 *   - TYPE_LEAGUE_HEADER (0): Lig başlık satırı
 *   - TYPE_MATCH (1): Maç bilgi satırı
 *
 * Maçlar API'den gelen leagueName alanına göre otomatik gruplandırılır.
 * Canlı maçlarda kırmızı nabız animasyonu gösterilir.
 */
public class MatchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    /** Lig başlık ViewType sabiti */
    private static final int TYPE_LEAGUE_HEADER = 0;

    /** Maç satır ViewType sabiti */
    private static final int TYPE_MATCH = 1;

    /** Listeye eklenen öğeler (başlık + maç karışık) */
    private final List<Object> items;

    /** Maç tıklama callback'i */
    private final OnMatchClickListener listener;

    /** Lig başlığı tıklama callback'i */
    private OnLeagueHeaderClickListener leagueHeaderListener;

    /** Context referansı */
    private final Context context;

    /** teamId → logo URL haritası (standings'ten elde edilir, null olabilir) */
    private Map<String, String> teamLogoMap = new HashMap<>();

    /** Favori takım ID seti */
    private Set<String> favoriteTeamIds = new HashSet<>();

    /** Favori lig ID seti */
    private Set<String> favoriteLeagueIds = new HashSet<>();

    /**
     * Maç tıklama callback arayüzü.
     */
    public interface OnMatchClickListener {
        void onMatchClick(MatchModel match);
    }

    /**
     * Lig başlığı tıklama callback arayüzü.
     */
    public interface OnLeagueHeaderClickListener {
        void onLeagueHeaderClick(String leagueId, String leagueName);
    }

    /**
     * Constructor.
     * @param context  Activity/Fragment context
     * @param listener Maç tıklama callback'i
     */
    public MatchAdapter(Context context, OnMatchClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.items = new ArrayList<>();
    }

    /**
     * Takım logo haritasını set eder.
     * @param logoMap teamId → logoUrl haritası
     */
    public void setTeamLogoMap(Map<String, String> logoMap) {
        this.teamLogoMap = logoMap != null ? logoMap : new HashMap<>();
        notifyDataSetChanged();
    }

    /**
     * Lig başlığı tıklama listener'ını set eder.
     */
    public void setOnLeagueHeaderClickListener(OnLeagueHeaderClickListener l) {
        this.leagueHeaderListener = l;
    }

    /**
     * Favori takım ve lig ID setlerini günceller.
     * Yeni setMatches() çağrısında favoriler yansıtılır.
     */
    public void setFavorites(Set<String> teamIds, Set<String> leagueIds) {
        this.favoriteTeamIds = teamIds != null ? teamIds : new HashSet<>();
        this.favoriteLeagueIds = leagueIds != null ? leagueIds : new HashSet<>();
    }

    /**
     * Maç listesini lig bazında gruplar ve adapter'a yükler.
     * Aynı lig altındaki maçlar bir başlık altında toplanır.
     *
     * @param matches API'den gelen maç listesi
     */
    public void setMatches(List<MatchModel> matches) {
        setMatchesWithSections(new ArrayList<>(), new ArrayList<>(), matches);
    }

    /**
     * Maçları üç gruba ayırarak adapter'a yükler:
     *   1. Favoriler (yıldız işaretli, en üstte, bölüm başlığı yok)
     *   2. Popüler ligler ("POPÜLER LİGLER" bölüm başlığıyla)
     *   3. Diğer ligler (bölüm başlığı yok)
     *
     * Her grup içinde maçlar leagueName'e göre kendi içinde gruplandırılır.
     *
     * @param favorites  Favori lig/takım içeren maçlar
     * @param popular    Popüler lig maçları
     * @param others     Diğer liglerin maçları
     */
    public void setMatchesWithSections(List<MatchModel> favorites,
                                       List<MatchModel> popular,
                                       List<MatchModel> others) {
        items.clear();
        addLeagueGroupToItems(favorites, false);
        addLeagueGroupToItems(popular, true);
        addLeagueGroupToItems(others, false);
        notifyDataSetChanged();
    }

    /** Maç listesini lig başlığı + maç satırları olarak items'a ekler. */
    private void addLeagueGroupToItems(List<MatchModel> matches, boolean isPopular) {
        if (matches == null || matches.isEmpty()) return;

        LinkedHashMap<String, List<MatchModel>> grouped = new LinkedHashMap<>();
        for (MatchModel match : matches) {
            String leagueName = match.getLeagueName() != null
                    ? match.getLeagueName() : context.getString(R.string.other);
            grouped.computeIfAbsent(leagueName, k -> new ArrayList<>()).add(match);
        }

        for (Map.Entry<String, List<MatchModel>> entry : grouped.entrySet()) {
            MatchModel firstMatch = entry.getValue().get(0);
            String leagueId = firstMatch.getLeagueId();
            boolean isLeagueFav = leagueId != null && favoriteLeagueIds.contains(leagueId);
            items.add(new LeagueHeader(
                    leagueId,
                    entry.getKey(),
                    firstMatch.getLeagueColor(),
                    firstMatch.getLeagueType(),
                    isLeagueFav,
                    isPopular
            ));
            items.addAll(entry.getValue());
        }
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof LeagueHeader ? TYPE_LEAGUE_HEADER : TYPE_MATCH;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_LEAGUE_HEADER) {
            View view = inflater.inflate(R.layout.item_league_header, parent, false);
            return new LeagueHeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_match, parent, false);
            return new MatchViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof LeagueHeaderViewHolder) {
            ((LeagueHeaderViewHolder) holder).bind((LeagueHeader) items.get(position));
        } else if (holder instanceof MatchViewHolder) {
            ((MatchViewHolder) holder).bind((MatchModel) items.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ================================================================
    // İç Sınıf: LeagueHeader - Lig başlık veri modeli
    // ================================================================

    static class LeagueHeader {
        final String leagueId;
        final String leagueName;
        final String leagueColor;
        final int leagueType;
        final boolean isFavorite;
        final boolean isPopular;

        LeagueHeader(String leagueId, String leagueName, String leagueColor,
                     int leagueType, boolean isFavorite, boolean isPopular) {
            this.leagueId = leagueId;
            this.leagueName = leagueName;
            this.leagueColor = leagueColor;
            this.leagueType = leagueType;
            this.isFavorite = isFavorite;
            this.isPopular = isPopular;
        }
    }

    // ================================================================
    // ViewHolder: LeagueHeaderViewHolder
    // ================================================================

    /**
     * LeagueHeaderViewHolder - Lig başlık satırının görünüm tutucu sınıfı.
     */
    class LeagueHeaderViewHolder extends RecyclerView.ViewHolder {

        private final View viewLeagueColor;
        private final TextView tvLeagueName;
        private final TextView tvLeagueType;
        private final TextView tvFavoriteStar;
        private final TextView tvPopularBadge;

        LeagueHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            viewLeagueColor = itemView.findViewById(R.id.viewLeagueColor);
            tvLeagueName = itemView.findViewById(R.id.tvLeagueName);
            tvLeagueType = itemView.findViewById(R.id.tvLeagueType);
            tvFavoriteStar = itemView.findViewById(R.id.tvFavoriteStar);
            tvPopularBadge = itemView.findViewById(R.id.tvPopularBadge);
        }

        void bind(LeagueHeader header) {
            tvLeagueName.setText(header.leagueName);

            // Lig renk çizgisini ayarla
            try {
                if (header.leagueColor != null && !header.leagueColor.isEmpty()) {
                    viewLeagueColor.setBackgroundColor(Color.parseColor(header.leagueColor));
                }
            } catch (IllegalArgumentException e) {
                viewLeagueColor.setBackgroundColor(context.getColor(R.color.primary));
            }

            // Lig tipi göstergesi (Lig veya Kupa)
            tvLeagueType.setText(header.leagueType == 2 ? "🏆" : "⚽");

            // Favori lig yıldızı
            tvFavoriteStar.setVisibility(header.isFavorite ? View.VISIBLE : View.GONE);

            // Popüler lig etiketi
            tvPopularBadge.setVisibility(header.isPopular ? View.VISIBLE : View.GONE);

            // Lig başlığına tıklama → puan durumu aç
            itemView.setOnClickListener(v -> {
                if (leagueHeaderListener != null && header.leagueId != null && !header.leagueId.isEmpty()) {
                    leagueHeaderListener.onLeagueHeaderClick(header.leagueId, header.leagueName);
                }
            });
        }
    }

    // ================================================================
    // ViewHolder: MatchViewHolder
    // ================================================================

    /**
     * MatchViewHolder - Maç satırının görünüm tutucu sınıfı.
     * Maç saati, takım adları, skorlar ve canlı göstergesi burada yönetilir.
     */
    class MatchViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView cardMatch;
        private final View viewLiveIndicator;
        private final TextView tvMatchTime;
        private final TextView tvMatchStatus;
        private final View viewHomeRedCard;
        private final View viewAwayRedCard;
        private final ImageView ivHomeLogo;
        private final ImageView ivAwayLogo;
        private final TextView tvHomeName;
        private final TextView tvAwayName;
        private final TextView tvHomeScore;
        private final TextView tvAwayScore;
        private final TextView tvTeamFavoriteStar;

        MatchViewHolder(@NonNull View itemView) {
            super(itemView);
            cardMatch = itemView.findViewById(R.id.cardMatch);
            viewLiveIndicator = itemView.findViewById(R.id.viewLiveIndicator);
            tvMatchTime = itemView.findViewById(R.id.tvMatchTime);
            tvMatchStatus = itemView.findViewById(R.id.tvMatchStatus);
            viewHomeRedCard = itemView.findViewById(R.id.viewHomeRedCard);
            viewAwayRedCard = itemView.findViewById(R.id.viewAwayRedCard);
            ivHomeLogo = itemView.findViewById(R.id.ivHomeLogo);
            ivAwayLogo = itemView.findViewById(R.id.ivAwayLogo);
            tvHomeName = itemView.findViewById(R.id.tvHomeName);
            tvAwayName = itemView.findViewById(R.id.tvAwayName);
            tvHomeScore = itemView.findViewById(R.id.tvHomeScore);
            tvAwayScore = itemView.findViewById(R.id.tvAwayScore);
            tvTeamFavoriteStar = itemView.findViewById(R.id.tvTeamFavoriteStar);
        }

        void bind(MatchModel match) {
            // Takım adlarını ayarla
            tvHomeName.setText(match.getHomeName());
            tvAwayName.setText(match.getAwayName());

            // Takım logolarını logo haritasından yükle (varsa)
            loadTeamLogo(ivHomeLogo, match.getHomeId());
            loadTeamLogo(ivAwayLogo, match.getAwayId());

            // === CANLI MAÇ DURUMU ===
            if (match.isLive()) {
                // Canlı göstergesi (kırmızı nokta) göster
                viewLiveIndicator.setVisibility(View.VISIBLE);
                startPulseAnimation(viewLiveIndicator);

                // Maç durumunu göster (İlk Yarı, İkinci Yarı, vs.)
                tvMatchStatus.setVisibility(View.VISIBLE);
                tvMatchStatus.setText(DateUtils.getStatusText(context, match.getStatus()));

                // Dakika bilgisi göster
                tvMatchTime.setText(match.getExtraExplain() != null
                        ? match.getExtraExplain().getMinute() + "'"
                        : DateUtils.getStatusText(context, match.getStatus()));

                // Skorları göster (canlı - kalın ve yeşil)
                tvHomeScore.setText(String.valueOf(match.getHomeScore()));
                tvAwayScore.setText(String.valueOf(match.getAwayScore()));
                tvHomeScore.setTextColor(context.getColor(R.color.score_green));
                tvAwayScore.setTextColor(context.getColor(R.color.score_green));

            }
            // === BİTMİŞ MAÇ DURUMU ===
            else if (match.isFinished()) {
                viewLiveIndicator.setVisibility(View.GONE);
                tvMatchStatus.setVisibility(View.VISIBLE);
                tvMatchStatus.setText(DateUtils.getStatusText(context, match.getStatus()));
                tvMatchStatus.setTextColor(context.getColor(R.color.match_finished));

                tvMatchTime.setText(DateUtils.formatMatchTime(match.getMatchTime()));

                // Skorları göster (bitmiş - normal renk)
                tvHomeScore.setText(String.valueOf(match.getHomeScore()));
                tvAwayScore.setText(String.valueOf(match.getAwayScore()));
                tvHomeScore.setTextColor(context.getColor(R.color.match_finished));
                tvAwayScore.setTextColor(context.getColor(R.color.match_finished));

            }
            // === BAŞLAMAMIŞ MAÇ DURUMU ===
            else if (match.isNotStarted()) {
                viewLiveIndicator.setVisibility(View.GONE);
                tvMatchStatus.setVisibility(View.GONE);

                tvMatchTime.setText(DateUtils.formatMatchTime(match.getMatchTime()));

                // Skor yerine "-" göster
                tvHomeScore.setText("-");
                tvAwayScore.setText("-");
                tvHomeScore.setTextColor(context.getColor(R.color.text_secondary_light));
                tvAwayScore.setTextColor(context.getColor(R.color.text_secondary_light));

            }
            // === İPTAL/ERTELEME DURUMU ===
            else {
                viewLiveIndicator.setVisibility(View.GONE);
                tvMatchStatus.setVisibility(View.VISIBLE);
                tvMatchStatus.setText(DateUtils.getStatusText(context, match.getStatus()));
                tvMatchStatus.setTextColor(context.getColor(R.color.match_finished));

                tvMatchTime.setText(DateUtils.formatMatchTime(match.getMatchTime()));

                tvHomeScore.setText("-");
                tvAwayScore.setText("-");
            }

            // Kırmızı kart göstergesi
            viewHomeRedCard.setVisibility(match.getHomeRed() > 0 ? View.VISIBLE : View.GONE);
            viewAwayRedCard.setVisibility(match.getAwayRed() > 0 ? View.VISIBLE : View.GONE);

            // Favori takım yıldızı (ev sahibi veya deplasman favori takımdaysa göster)
            boolean isTeamFav = (match.getHomeId() != null && favoriteTeamIds.contains(match.getHomeId()))
                    || (match.getAwayId() != null && favoriteTeamIds.contains(match.getAwayId()));
            tvTeamFavoriteStar.setVisibility(isTeamFav ? View.VISIBLE : View.GONE);

            // Maç kartına tıklama
            cardMatch.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMatchClick(match);
                }
            });
        }

        private void loadTeamLogo(ImageView imageView, String teamId) {
            String logoUrl = teamLogoMap.get(teamId);
            if (logoUrl != null && !logoUrl.isEmpty()) {
                imageView.setVisibility(View.VISIBLE);
                Glide.with(imageView.getContext())
                        .load(logoUrl)
                        .override(48, 48)
                        .into(imageView);
            } else {
                imageView.setVisibility(View.GONE);
            }
        }

        /**
         * Canlı maç göstergesi için nabız (pulse) animasyonu başlatır.
         * Kırmızı nokta alfa değeri 0.3-1.0 arasında sürekli değişir.
         *
         * @param view Animasyon uygulanacak View
         */
        private void startPulseAnimation(View view) {
            ValueAnimator animator = ValueAnimator.ofFloat(0.3f, 1.0f);
            animator.setDuration(1000);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setRepeatMode(ValueAnimator.REVERSE);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.addUpdateListener(animation ->
                    view.setAlpha((float) animation.getAnimatedValue()));
            animator.start();
        }
    }
}
