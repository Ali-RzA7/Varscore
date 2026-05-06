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

import com.example.var.R;
import com.example.var.data.model.MatchModel;
import com.example.var.util.DateUtils;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    /** Context referansı */
    private final Context context;

    /**
     * Maç tıklama callback arayüzü.
     */
    public interface OnMatchClickListener {
        /**
         * Bir maç satırına tıklandığında çağrılır.
         * @param match Tıklanan maç modeli
         */
        void onMatchClick(MatchModel match);
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
     * Maç listesini lig bazında gruplar ve adapter'a yükler.
     * Aynı lig altındaki maçlar bir başlık altında toplanır.
     *
     * Gruplama algoritması:
     * 1. Maçlar leagueName'e göre LinkedHashMap'e gruplandırılır
     * 2. Her grup için önce başlık, sonra maçlar eklenir
     *
     * @param matches API'den gelen maç listesi
     */
    public void setMatches(List<MatchModel> matches) {
        items.clear();

        if (matches == null || matches.isEmpty()) {
            notifyDataSetChanged();
            return;
        }

        // Maçları lig adına göre grupla (ekleme sırası korunur)
        LinkedHashMap<String, List<MatchModel>> grouped = new LinkedHashMap<>();
        for (MatchModel match : matches) {
            String leagueName = match.getLeagueName() != null
                    ? match.getLeagueName() : "Other";
            grouped.computeIfAbsent(leagueName, k -> new ArrayList<>()).add(match);
        }

        // Gruplanmış verileri listeye ekle (başlık + maçlar)
        for (Map.Entry<String, List<MatchModel>> entry : grouped.entrySet()) {
            // Lig başlığı ekle
            items.add(new LeagueHeader(
                    entry.getKey(),
                    entry.getValue().get(0).getLeagueColor(),
                    entry.getValue().get(0).getLeagueType()
            ));
            // Lig altındaki maçları ekle
            items.addAll(entry.getValue());
        }

        notifyDataSetChanged();
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

    /**
     * LeagueHeader - Adapter'da lig başlığı olarak gösterilecek veri modeli.
     * Bu sınıf sadece adapter içinde kullanılır.
     */
    static class LeagueHeader {
        final String leagueName;
        final String leagueColor;
        final int leagueType;

        LeagueHeader(String leagueName, String leagueColor, int leagueType) {
            this.leagueName = leagueName;
            this.leagueColor = leagueColor;
            this.leagueType = leagueType;
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

        LeagueHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            viewLeagueColor = itemView.findViewById(R.id.viewLeagueColor);
            tvLeagueName = itemView.findViewById(R.id.tvLeagueName);
            tvLeagueType = itemView.findViewById(R.id.tvLeagueType);
        }

        /**
         * Lig başlık verilerini görünüme bağlar.
         * @param header Lig başlık modeli
         */
        void bind(LeagueHeader header) {
            tvLeagueName.setText(header.leagueName);

            // Lig renk çizgisini ayarla
            try {
                if (header.leagueColor != null && !header.leagueColor.isEmpty()) {
                    viewLeagueColor.setBackgroundColor(Color.parseColor(header.leagueColor));
                }
            } catch (IllegalArgumentException e) {
                // Geçersiz renk kodu - varsayılan rengi kullan
                viewLeagueColor.setBackgroundColor(context.getColor(R.color.primary));
            }

            // Lig tipi göstergesi (Lig veya Kupa)
            tvLeagueType.setText(header.leagueType == 2 ? "🏆" : "⚽");
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
        private final TextView tvHomeName;
        private final TextView tvAwayName;
        private final TextView tvHomeScore;
        private final TextView tvAwayScore;

        MatchViewHolder(@NonNull View itemView) {
            super(itemView);
            cardMatch = itemView.findViewById(R.id.cardMatch);
            viewLiveIndicator = itemView.findViewById(R.id.viewLiveIndicator);
            tvMatchTime = itemView.findViewById(R.id.tvMatchTime);
            tvMatchStatus = itemView.findViewById(R.id.tvMatchStatus);
            viewHomeRedCard = itemView.findViewById(R.id.viewHomeRedCard);
            viewAwayRedCard = itemView.findViewById(R.id.viewAwayRedCard);
            tvHomeName = itemView.findViewById(R.id.tvHomeName);
            tvAwayName = itemView.findViewById(R.id.tvAwayName);
            tvHomeScore = itemView.findViewById(R.id.tvHomeScore);
            tvAwayScore = itemView.findViewById(R.id.tvAwayScore);
        }

        /**
         * Maç verilerini görünüme bağlar.
         * Maç durumuna göre (canlı, bitmemiş, bitmiş) farklı görünüm uygulanır.
         *
         * @param match Maç modeli
         */
        void bind(MatchModel match) {
            // Takım adlarını ayarla
            tvHomeName.setText(match.getHomeName());
            tvAwayName.setText(match.getAwayName());

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

            // Maç kartına tıklama
            cardMatch.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMatchClick(match);
                }
            });
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
