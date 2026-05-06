package com.example.var.ui.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.var.R;
import com.example.var.util.DateUtils;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * DatePickerAdapter - Yatay tarih seçici RecyclerView adapter'ı.
 * 30 günlük bir tarih aralığı (15 gün önce - 14 gün sonra) gösterir.
 * Seçili gün altın sarısı arka planla vurgulanır.
 * Bugünün tarihi özel olarak işaretlenir.
 *
 * Kullanıcı bir güne tıkladığında OnDateSelectedListener callback'i tetiklenir.
 */
public class DatePickerAdapter extends RecyclerView.Adapter<DatePickerAdapter.DateViewHolder> {

    /** Tarih listesi */
    private final List<Calendar> dates;

    /** Seçili tarihin index'i */
    private int selectedPosition;

    /** Tarih seçim callback arayüzü */
    private final OnDateSelectedListener listener;

    /** Context referansı */
    private final Context context;

    /**
     * Tarih seçildiğinde tetiklenen callback arayüzü.
     */
    public interface OnDateSelectedListener {
        /**
         * Kullanıcı bir tarih seçtiğinde çağrılır.
         * @param calendar Seçilen tarih
         */
        void onDateSelected(Calendar calendar);
    }

    /**
     * Constructor - Tarih listesini oluşturur ve bugünü seçili yapar.
     *
     * @param context  Activity/Fragment context'i
     * @param listener Tarih seçim callback'i
     */
    public DatePickerAdapter(Context context, OnDateSelectedListener listener) {
        this.context = context;
        this.listener = listener;
        this.dates = new ArrayList<>();

        // 15 gün önce ile 14 gün sonrası arası tarih listesi oluştur
        Calendar startDate = Calendar.getInstance();
        startDate.add(Calendar.DAY_OF_YEAR, -15);

        for (int i = 0; i < 30; i++) {
            Calendar date = (Calendar) startDate.clone();
            date.add(Calendar.DAY_OF_YEAR, i);
            dates.add(date);
        }

        // Bugünün pozisyonunu bul ve seçili yap (15. index)
        selectedPosition = 15;
    }

    @NonNull
    @Override
    public DateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_date, parent, false);
        return new DateViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DateViewHolder holder, int position) {
        Calendar date = dates.get(position);
        holder.bind(date, position == selectedPosition);
    }

    @Override
    public int getItemCount() {
        return dates.size();
    }

    /**
     * Bugünün index'ini döndürür (RecyclerView'ı bu pozisyona kaydırmak için).
     * @return Bugünün pozisyonu (15)
     */
    public int getTodayPosition() {
        return 15;
    }

    /**
     * DateViewHolder - Her bir tarih öğesinin görünüm tutucu sınıfı.
     * Gün adı, gün numarası ve ay adı gösterilir.
     * Seçili durumda arka plan rengi değişir.
     */
    class DateViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView cardDate;
        private final TextView tvDayName;
        private final TextView tvDayNumber;
        private final TextView tvMonthName;

        DateViewHolder(@NonNull View itemView) {
            super(itemView);
            cardDate = itemView.findViewById(R.id.cardDate);
            tvDayName = itemView.findViewById(R.id.tvDayName);
            tvDayNumber = itemView.findViewById(R.id.tvDayNumber);
            tvMonthName = itemView.findViewById(R.id.tvMonthName);
        }

        /**
         * Tarih verisini görünüme bağlar.
         * @param calendar   Gösterilecek tarih
         * @param isSelected Bu tarih seçili mi?
         */
        void bind(Calendar calendar, boolean isSelected) {
            // Gün adı, numarası ve ay bilgisini ayarla
            tvDayName.setText(DateUtils.getDayShortName(context, calendar));
            tvDayNumber.setText(DateUtils.getDayNumber(calendar));
            tvMonthName.setText(DateUtils.getMonthShortName(context, calendar));

            // Seçili duruma göre arka plan ve metin rengi ayarla
            if (isSelected) {
                // Seçili: Altın sarısı arka plan, koyu metin
                cardDate.setCardBackgroundColor(
                        context.getColor(R.color.secondary));
                tvDayName.setTextColor(context.getColor(R.color.on_secondary));
                tvDayNumber.setTextColor(context.getColor(R.color.on_secondary));
                tvMonthName.setTextColor(context.getColor(R.color.on_secondary));
                tvDayName.setAlpha(1.0f);
                tvMonthName.setAlpha(1.0f);
            } else {
                // Seçili değil: Şeffaf arka plan, beyaz metin
                cardDate.setCardBackgroundColor(Color.TRANSPARENT);
                tvDayName.setTextColor(context.getColor(R.color.white));
                tvDayNumber.setTextColor(context.getColor(R.color.white));
                tvMonthName.setTextColor(context.getColor(R.color.white));
                tvDayName.setAlpha(0.7f);
                tvMonthName.setAlpha(0.7f);
            }

            // Bugün ise özel border ekle
            if (DateUtils.isToday(calendar) && !isSelected) {
                cardDate.setStrokeColor(context.getColor(R.color.date_today_border));
                cardDate.setStrokeWidth(2);
            } else {
                cardDate.setStrokeWidth(0);
            }

            // Tıklama olayı - tarih seçimi
            itemView.setOnClickListener(v -> {
                int adapterPosition = getAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION) return;

                int oldPosition = selectedPosition;
                selectedPosition = adapterPosition;
                notifyItemChanged(oldPosition);   // Eski seçimi güncelle
                notifyItemChanged(adapterPosition); // Yeni seçimi güncelle

                // Callback'i tetikle
                if (listener != null) {
                    listener.onDateSelected(dates.get(adapterPosition));
                }
            });
        }
    }
}
