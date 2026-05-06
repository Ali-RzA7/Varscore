package com.example.var.ui.adapter;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.var.R;
import com.example.var.util.DateUtils;
import com.google.android.material.card.MaterialCardView;

import java.util.Calendar;
import java.util.List;

/**
 * CalendarAdapter - Takvimdeki günleri listeleyen adapter.
 */
public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.DayViewHolder> {

    private final List<Calendar> days;
    private final Calendar selectedDate;
    private final OnDayClickListener listener;

    public interface OnDayClickListener {
        void onDayClick(Calendar date);
    }

    public CalendarAdapter(List<Calendar> days, Calendar selectedDate, OnDayClickListener listener) {
        this.days = days;
        this.selectedDate = selectedDate;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        Calendar day = days.get(position);
        if (day == null) {
            holder.tvDayNumber.setText("");
            holder.cardDay.setCardBackgroundColor(Color.TRANSPARENT);
            holder.cardDay.setClickable(false);
            holder.viewTodayIndicator.setVisibility(View.GONE);
        } else {
            holder.tvDayNumber.setText(String.valueOf(day.get(Calendar.DAY_OF_MONTH)));
            holder.cardDay.setClickable(true);
            holder.cardDay.setOnClickListener(v -> listener.onDayClick(day));

            boolean isSelected = isSameDay(day, selectedDate);
            boolean isToday = DateUtils.isToday(day);

            if (isSelected) {
                holder.cardDay.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.secondary));
                holder.tvDayNumber.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.on_secondary));
                holder.tvDayNumber.setTypeface(null, Typeface.BOLD);
            } else {
                holder.cardDay.setCardBackgroundColor(Color.TRANSPARENT);
                holder.tvDayNumber.setTextColor(Color.WHITE);
                holder.tvDayNumber.setTypeface(null, Typeface.NORMAL);
            }

            holder.viewTodayIndicator.setVisibility(isToday ? View.VISIBLE : View.GONE);
            
            // Farklı aydaki günlerin rengini soluklaştır (Eğer listede varsa)
            // Şimdilik sadece mevcut ayın günlerini ve boşlukları gönderiyoruz.
        }
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardDay;
        TextView tvDayNumber;
        View viewTodayIndicator;

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            cardDay = itemView.findViewById(R.id.cardDay);
            tvDayNumber = itemView.findViewById(R.id.tvDayNumber);
            viewTodayIndicator = itemView.findViewById(R.id.viewTodayIndicator);
        }
    }
}
