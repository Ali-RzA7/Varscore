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

    // Tip sabitleri — /events endpoint (docs id=16)
    private static final int TYPE_GOAL = 1;
    private static final int TYPE_RED_CARD = 2;
    private static final int TYPE_YELLOW_CARD = 3;
    private static final int TYPE_PENALTY_GOAL = 7;
    private static final int TYPE_OWN_GOAL = 8;
    private static final int TYPE_SECOND_YELLOW = 9;
    private static final int TYPE_SUBSTITUTION = 11;
    private static final int TYPE_PENALTY_MISS = 13;
    private static final int TYPE_VAR = 14;

    private List<EventModel> events = new ArrayList<>();

    public void setEvents(List<EventModel> events) {
        this.events = events;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_match_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        holder.bind(events.get(position));
    }

    @Override
    public int getItemCount() { return events.size(); }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime;
        TextView tvHomePlayer, tvHomeSubtitle;
        TextView tvAwayPlayer, tvAwaySubtitle;
        ImageView ivHomeEventType, ivAwayEventType;
        View llHomeEvent, llAwayEvent;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvHomePlayer = itemView.findViewById(R.id.tvHomePlayer);
            tvHomeSubtitle = itemView.findViewById(R.id.tvHomeSubtitle);
            tvAwayPlayer = itemView.findViewById(R.id.tvAwayPlayer);
            tvAwaySubtitle = itemView.findViewById(R.id.tvAwaySubtitle);
            ivHomeEventType = itemView.findViewById(R.id.ivHomeEventType);
            ivAwayEventType = itemView.findViewById(R.id.ivAwayEventType);
            llHomeEvent = itemView.findViewById(R.id.llHomeEvent);
            llAwayEvent = itemView.findViewById(R.id.llAwayEvent);
        }

        void bind(EventModel event) {
            tvTime.setText(event.getMinuteDisplay());

            String playerName = safeStr(event.getPlayerName());
            String subtitle = buildSubtitle(event);

            if (event.isHomeEvent()) {
                llHomeEvent.setVisibility(View.VISIBLE);
                llAwayEvent.setVisibility(View.GONE);
                tvHomePlayer.setText(playerName);
                setSubtitle(tvHomeSubtitle, subtitle);
                setEventIcon(ivHomeEventType, event.getType());
            } else {
                llHomeEvent.setVisibility(View.GONE);
                llAwayEvent.setVisibility(View.VISIBLE);
                tvAwayPlayer.setText(playerName);
                setSubtitle(tvAwaySubtitle, subtitle);
                setEventIcon(ivAwayEventType, event.getType());
            }
        }

        private String buildSubtitle(EventModel event) {
            switch (event.getType()) {
                case TYPE_OWN_GOAL:
                    return "Kendi Kalesine";
                case TYPE_PENALTY_GOAL:
                    return "Penaltı";
                case TYPE_PENALTY_MISS:
                    return "Kaçırılan Penaltı";
                case TYPE_VAR:
                    return "VAR İncelemesi";
                case TYPE_SECOND_YELLOW:
                    return "Çift Sarı → Kırmızı";
                case TYPE_SUBSTITUTION:
                    // Giren oyuncu varsa göster
                    String nameIn = safeStr(event.getPlayerNameIn());
                    if (!nameIn.isEmpty()) return "↑ " + nameIn;
                    return "";
                default:
                    return "";
            }
        }

        private void setSubtitle(TextView tv, String text) {
            if (text.isEmpty()) {
                tv.setVisibility(View.GONE);
            } else {
                tv.setText(text);
                tv.setVisibility(View.VISIBLE);
            }
        }

        private void setEventIcon(ImageView iv, int type) {
            switch (type) {
                case TYPE_GOAL:
                case TYPE_PENALTY_GOAL:
                case TYPE_OWN_GOAL:
                    iv.setImageResource(R.drawable.ic_goal);
                    break;
                case TYPE_RED_CARD:
                case TYPE_PENALTY_MISS:
                    iv.setImageResource(R.drawable.ic_red_card);
                    break;
                case TYPE_YELLOW_CARD:
                case TYPE_SECOND_YELLOW:
                    iv.setImageResource(R.drawable.ic_yellow_card);
                    break;
                case TYPE_SUBSTITUTION:
                    iv.setImageResource(R.drawable.ic_substitution);
                    break;
                default:
                    iv.setImageResource(android.R.drawable.ic_menu_info_details);
                    break;
            }
        }

        private static String safeStr(String s) {
            return s != null ? s : "";
        }
    }
}
