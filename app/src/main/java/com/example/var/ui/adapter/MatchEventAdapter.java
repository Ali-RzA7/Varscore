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

/**
 * MatchEventAdapter - Maç olaylarını (timeline) listeleyen adapter.
 */
public class MatchEventAdapter extends RecyclerView.Adapter<MatchEventAdapter.EventViewHolder> {

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
        EventModel event = events.get(position);
        holder.bind(event);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvHomePlayer, tvAwayPlayer;
        ImageView ivHomeEventType, ivAwayEventType;
        View llHomeEvent, llAwayEvent;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvHomePlayer = itemView.findViewById(R.id.tvHomePlayer);
            tvAwayPlayer = itemView.findViewById(R.id.tvAwayPlayer);
            ivHomeEventType = itemView.findViewById(R.id.ivHomeEventType);
            ivAwayEventType = itemView.findViewById(R.id.ivAwayEventType);
            llHomeEvent = itemView.findViewById(R.id.llHomeEvent);
            llAwayEvent = itemView.findViewById(R.id.llAwayEvent);
        }

        void bind(EventModel event) {
            tvTime.setText(event.getTime() + "'");
            
            if (event.isHomeEvent()) {
                llHomeEvent.setVisibility(View.VISIBLE);
                llAwayEvent.setVisibility(View.GONE);
                tvHomePlayer.setText(event.getPlayerName());
                setEventIcon(ivHomeEventType, event.getType());
            } else {
                llHomeEvent.setVisibility(View.GONE);
                llAwayEvent.setVisibility(View.VISIBLE);
                tvAwayPlayer.setText(event.getPlayerName());
                setEventIcon(ivAwayEventType, event.getType());
            }
        }

        private void setEventIcon(ImageView imageView, int type) {
            switch (type) {
                case 1: imageView.setImageResource(R.drawable.ic_goal); break;
                case 2: imageView.setImageResource(R.drawable.ic_red_card); break;
                case 3: imageView.setImageResource(R.drawable.ic_yellow_card); break;
                case 9: imageView.setImageResource(R.drawable.ic_substitution); break;
                default: imageView.setImageResource(android.R.drawable.ic_menu_info_details);
            }
        }
    }
}
