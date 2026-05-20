package com.example.var.ui.adapter;

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
            tvTime           = v.findViewById(R.id.tvTime);
            tvHomePlayer     = v.findViewById(R.id.tvHomePlayer);
            tvHomeSubtitle   = v.findViewById(R.id.tvHomeSubtitle);
            tvAwayPlayer     = v.findViewById(R.id.tvAwayPlayer);
            tvAwaySubtitle   = v.findViewById(R.id.tvAwaySubtitle);
            ivHomeEventType  = v.findViewById(R.id.ivHomeEventType);
            ivAwayEventType  = v.findViewById(R.id.ivAwayEventType);
            llHomeEvent      = v.findViewById(R.id.llHomeEvent);
            llAwayEvent      = v.findViewById(R.id.llAwayEvent);
        }

        void bind(EventModel e) {
            tvTime.setText(e.getMinuteDisplay());

            String player   = safeStr(e.getPlayerName());
            String subtitle = subtitle(e);
            boolean isHome  = e.isHomeEvent();

            if (isHome) {
                llHomeEvent.setVisibility(View.VISIBLE);
                llAwayEvent.setVisibility(View.INVISIBLE); // yer tut, boş bırak
                tvHomePlayer.setText(player);
                applySubtitle(tvHomeSubtitle, subtitle);
                applyIcon(ivHomeEventType, e.getType());
            } else {
                llHomeEvent.setVisibility(View.INVISIBLE);
                llAwayEvent.setVisibility(View.VISIBLE);
                tvAwayPlayer.setText(player);
                applySubtitle(tvAwaySubtitle, subtitle);
                applyIcon(ivAwayEventType, e.getType());
            }
        }

        private String subtitle(EventModel e) {
            switch (e.getType()) {
                case TYPE_OWN_GOAL:      return "Kendi Kalesine";
                case TYPE_PENALTY_GOAL:  return "Penaltı";
                case TYPE_PENALTY_MISS:  return "Kaçırılan Penaltı";
                case TYPE_VAR:           return "VAR İncelemesi";
                case TYPE_SECOND_YELLOW: return "2. Sarı → Kırmızı";
                case TYPE_SUBSTITUTION: {
                    String in = safeStr(e.getPlayerNameIn());
                    return in.isEmpty() ? "" : "↑ " + in;
                }
                default: return "";
            }
        }

        private void applySubtitle(TextView tv, String text) {
            if (text.isEmpty()) {
                tv.setVisibility(View.GONE);
            } else {
                tv.setText(text);
                tv.setVisibility(View.VISIBLE);
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
                case TYPE_SUBSTITUTION:
                    iv.setImageResource(R.drawable.ic_substitution);
                    iv.setVisibility(View.VISIBLE);
                    break;
                case TYPE_VAR:
                    // VAR için sistem ikonu yerine metinle ifade ediliyor
                    iv.setVisibility(View.GONE);
                    break;
                default:
                    iv.setVisibility(View.GONE);
                    break;
            }
        }

        private static String safeStr(String s) { return s != null ? s : ""; }
    }
}
