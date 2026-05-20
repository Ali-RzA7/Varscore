package com.example.var.ui.adapter;

import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.var.R;
import com.example.var.data.model.EventModel;

import java.util.ArrayList;
import java.util.List;

public class MatchEventAdapter extends RecyclerView.Adapter<MatchEventAdapter.EventViewHolder> {

    private static final int TYPE_GOAL          = 1;
    private static final int TYPE_RED_CARD      = 2;
    private static final int TYPE_YELLOW_CARD   = 3;
    private static final int TYPE_PENALTY_GOAL  = 7;
    private static final int TYPE_OWN_GOAL      = 8;
    private static final int TYPE_SECOND_YELLOW = 9;
    private static final int TYPE_SUBSTITUTION  = 11;
    private static final int TYPE_PENALTY_MISS  = 13;
    private static final int TYPE_VAR           = 14;

    private static final int COLOR_GREEN = Color.parseColor("#2E7D32");
    private static final int COLOR_RED   = Color.parseColor("#C62828");
    private static final int COLOR_AMBER = Color.parseColor("#F57F17");
    private static final int COLOR_GREY  = Color.parseColor("#757575");

    private List<EventModel> events = new ArrayList<>();

    public void setEvents(List<EventModel> events) {
        this.events = events;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_match_event, parent, false);
        return new EventViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder h, int position) {
        h.bind(events.get(position));
    }

    @Override
    public int getItemCount() { return events.size(); }

    // ----------------------------------------------------------------

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime;
        TextView tvHomePlayer, tvHomeSubtitle;
        TextView tvAwayPlayer, tvAwaySubtitle;
        ImageView ivHomeEventType, ivAwayEventType;
        View llHomeEvent, llAwayEvent;

        EventViewHolder(@NonNull View v) {
            super(v);
            tvTime          = v.findViewById(R.id.tvTime);
            tvHomePlayer    = v.findViewById(R.id.tvHomePlayer);
            tvHomeSubtitle  = v.findViewById(R.id.tvHomeSubtitle);
            tvAwayPlayer    = v.findViewById(R.id.tvAwayPlayer);
            tvAwaySubtitle  = v.findViewById(R.id.tvAwaySubtitle);
            ivHomeEventType = v.findViewById(R.id.ivHomeEventType);
            ivAwayEventType = v.findViewById(R.id.ivAwayEventType);
            llHomeEvent     = v.findViewById(R.id.llHomeEvent);
            llAwayEvent     = v.findViewById(R.id.llAwayEvent);
        }

        void bind(EventModel e) {
            tvTime.setText(e.getMinuteDisplay());

            boolean isHome = e.isHomeEvent();
            TextView tvPlayer   = isHome ? tvHomePlayer   : tvAwayPlayer;
            TextView tvSubtitle = isHome ? tvHomeSubtitle : tvAwaySubtitle;
            ImageView ivIcon    = isHome ? ivHomeEventType : ivAwayEventType;

            if (isHome) {
                llHomeEvent.setVisibility(View.VISIBLE);
                llAwayEvent.setVisibility(View.INVISIBLE);
            } else {
                llHomeEvent.setVisibility(View.INVISIBLE);
                llAwayEvent.setVisibility(View.VISIBLE);
            }

            if (e.getType() == TYPE_SUBSTITUTION) {
                bindSubstitution(e, tvPlayer, tvSubtitle, ivIcon);
            } else {
                bindRegular(e, tvPlayer, tvSubtitle, ivIcon);
            }
        }

        /** Değişiklik olayı: giren ve çıkan oyuncuyu ayrı satırlarda göster. */
        private void bindSubstitution(EventModel e,
                TextView tvPlayer, TextView tvSubtitle, ImageView ivIcon) {

            SubPair pair = parseSubstitution(e);

            // Çıkan oyuncu — kırmızı ↓ önek
            tvPlayer.setText(colored("↓ " + pair.out, COLOR_RED));

            // Giren oyuncu — yeşil ↑ önek
            if (!pair.in.isEmpty()) {
                tvSubtitle.setText(colored("↑ " + pair.in, COLOR_GREEN));
                tvSubtitle.setVisibility(View.VISIBLE);
            } else {
                tvSubtitle.setVisibility(View.GONE);
            }

            ivIcon.setImageResource(R.drawable.ic_substitution);
            ivIcon.setVisibility(View.VISIBLE);
        }

        /** Değişiklik dışı tüm olaylar (gol, kart, VAR vb.) */
        private void bindRegular(EventModel e,
                TextView tvPlayer, TextView tvSubtitle, ImageView ivIcon) {

            tvPlayer.setTextColor(Color.parseColor("#212121"));

            boolean isGoal = (e.getType() == TYPE_GOAL
                    || e.getType() == TYPE_PENALTY_GOAL
                    || e.getType() == TYPE_OWN_GOAL);

            if (isGoal) {
                GoalPair pair = parseGoal(e);
                tvPlayer.setText(pair.scorer);
                if (!pair.assist.isEmpty()) {
                    tvSubtitle.setText("Asist: " + pair.assist);
                    tvSubtitle.setTextColor(COLOR_GREEN);
                    tvSubtitle.setVisibility(View.VISIBLE);
                } else {
                    String extra = regularSubtitle(e);
                    applySubtitleOrHide(tvSubtitle, extra, subtitleColor(e.getType()));
                }
            } else {
                tvPlayer.setText(safeStr(e.getPlayerName()));
                String sub = regularSubtitle(e);
                applySubtitleOrHide(tvSubtitle, sub, subtitleColor(e.getType()));
            }

            applyIcon(ivIcon, e.getType());
        }

        private void applySubtitleOrHide(TextView tv, String text, int color) {
            if (!text.isEmpty()) {
                tv.setText(text);
                tv.setTextColor(color);
                tv.setVisibility(View.VISIBLE);
            } else {
                tv.setVisibility(View.GONE);
            }
        }

        private String regularSubtitle(EventModel e) {
            switch (e.getType()) {
                case TYPE_OWN_GOAL:      return "Kendi Kalesine";
                case TYPE_PENALTY_GOAL:  return "Penaltı";
                case TYPE_PENALTY_MISS:  return "Kaçırılan Penaltı";
                case TYPE_VAR:           return "VAR İncelemesi";
                case TYPE_SECOND_YELLOW: return "2. Sarı → Kırmızı";
                default:                 return "";
            }
        }

        /**
         * API gol olaylarında oyuncu adını iki şekilde gönderir:
         *  1. playerName = "Ali Sowe (Assist:Modibo Sagnan)"  — birleşik string
         *  2. playerName = "Ali Sowe", assistPlayerId ayrı alan (isim yok)
         */
        private static GoalPair parseGoal(EventModel e) {
            String raw = safeStr(e.getPlayerName());

            int idx = raw.indexOf("(Assist:");
            if (idx != -1) {
                String scorer = raw.substring(0, idx).trim();
                String assist = raw.substring(idx + "(Assist:".length()).trim();
                if (assist.endsWith(")")) assist = assist.substring(0, assist.length() - 1).trim();
                return new GoalPair(scorer, assist);
            }

            // Birleşik format yoksa yalnızca gol atan bilinir
            return new GoalPair(raw, "");
        }

        private int subtitleColor(int type) {
            switch (type) {
                case TYPE_OWN_GOAL:
                case TYPE_PENALTY_MISS:
                case TYPE_RED_CARD:
                case TYPE_SECOND_YELLOW: return COLOR_RED;
                case TYPE_YELLOW_CARD:   return COLOR_AMBER;
                case TYPE_VAR:           return COLOR_GREY;
                default:                 return COLOR_GREY;
            }
        }

        private void applyIcon(ImageView iv, int type) {
            switch (type) {
                case TYPE_GOAL:
                case TYPE_PENALTY_GOAL:
                case TYPE_OWN_GOAL:
                    iv.setImageResource(R.drawable.ic_goal);
                    iv.setVisibility(View.VISIBLE);
                    break;
                case TYPE_RED_CARD:
                case TYPE_PENALTY_MISS:
                    iv.setImageResource(R.drawable.ic_red_card);
                    iv.setVisibility(View.VISIBLE);
                    break;
                case TYPE_YELLOW_CARD:
                case TYPE_SECOND_YELLOW:
                    iv.setImageResource(R.drawable.ic_yellow_card);
                    iv.setVisibility(View.VISIBLE);
                    break;
                default:
                    iv.setVisibility(View.GONE);
                    break;
            }
        }

        // ---- Yardımcı: Değişiklik oyuncu adlarını çöz ----

        /**
         * API değişiklik olaylarında oyuncu adını iki şekilde gönderebilir:
         *  1. playerName = "Çıkan Oyuncu↑Giren Oyuncu"  (birleşik string)
         *  2. playerName = çıkan, playerNameIn = giren   (ayrı alanlar)
         *  3. playerName = çıkan, playerNameOut = çıkan  (eski format)
         */
        private static SubPair parseSubstitution(EventModel e) {
            String rawName  = safeStr(e.getPlayerName());
            String nameIn   = safeStr(e.getPlayerNameIn());
            String nameOut  = safeStr(e.getPlayerNameOut());

            // Öncelik: ayrı alanlar doluysa onları kullan
            if (!nameIn.isEmpty() || !nameOut.isEmpty()) {
                String out = nameOut.isEmpty() ? rawName : nameOut;
                return new SubPair(out, nameIn);
            }

            // API birleşik string göndermişse "↑" ya da "/" ile ayır
            if (rawName.contains("↑")) {
                String[] parts = rawName.split("↑", 2);
                return new SubPair(parts[0].trim(), parts[1].trim());
            }
            if (rawName.contains("↓")) {
                String[] parts = rawName.split("↓", 2);
                return new SubPair(parts[0].trim(), parts.length > 1 ? parts[1].trim() : "");
            }

            // Yalnızca çıkan oyuncu biliniyorsa
            return new SubPair(rawName, "");
        }

        private static SpannableString colored(String text, int color) {
            SpannableString s = new SpannableString(text);
            s.setSpan(new ForegroundColorSpan(color), 0, text.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return s;
        }

        private static String safeStr(String s) { return s != null ? s : ""; }

        private static class SubPair {
            final String out, in;
            SubPair(String out, String in) { this.out = out; this.in = in; }
        }

        private static class GoalPair {
            final String scorer, assist;
            GoalPair(String scorer, String assist) { this.scorer = scorer; this.assist = assist; }
        }
    }
}
