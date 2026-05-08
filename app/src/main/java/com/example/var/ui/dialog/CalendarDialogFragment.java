package com.example.var.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.var.R;
import com.example.var.ui.adapter.CalendarAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * CalendarDialogFragment - Gelişmiş tarih seçici diyaloğu.
 */
public class CalendarDialogFragment extends DialogFragment {

    private Calendar currentMonth;
    private final Calendar selectedDate;
    private final OnDateSelectedListener listener;

    public interface OnDateSelectedListener {
        void onDateSelected(Calendar date);
    }

    public CalendarDialogFragment(Calendar selectedDate, OnDateSelectedListener listener) {
        this.selectedDate = (Calendar) selectedDate.clone();
        this.currentMonth = (Calendar) selectedDate.clone();
        this.currentMonth.set(Calendar.DAY_OF_MONTH, 1);
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_DeviceDefault_Dialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_calendar_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvMonthYear = view.findViewById(R.id.tvMonthYear);
        ImageButton btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
        ImageButton btnNextMonth = view.findViewById(R.id.btnNextMonth);
        RecyclerView rvCalendarDays = view.findViewById(R.id.rvCalendarDays);
        View btnCancel = view.findViewById(R.id.btnCancel);
        View btnToday = view.findViewById(R.id.btnToday);

        rvCalendarDays.setLayoutManager(new GridLayoutManager(getContext(), 7));

        updateCalendar(tvMonthYear, rvCalendarDays);

        btnPrevMonth.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, -1);
            updateCalendar(tvMonthYear, rvCalendarDays);
        });

        btnNextMonth.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, 1);
            updateCalendar(tvMonthYear, rvCalendarDays);
        });

        btnCancel.setOnClickListener(v -> dismiss());

        btnToday.setOnClickListener(v -> {
            listener.onDateSelected(Calendar.getInstance());
            dismiss();
        });
    }

    private void updateCalendar(TextView tvMonthYear, RecyclerView rvCalendarDays) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("tr"));
        tvMonthYear.setText(sdf.format(currentMonth.getTime()));

        List<Calendar> days = generateDaysForMonth(currentMonth);
        CalendarAdapter adapter = new CalendarAdapter(days, selectedDate, date -> {
            listener.onDateSelected(date);
            dismiss();
        });
        rvCalendarDays.setAdapter(adapter);
    }

    private List<Calendar> generateDaysForMonth(Calendar month) {
        List<Calendar> days = new ArrayList<>();
        Calendar cal = (Calendar) month.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);

        // Ayın ilk günü hangi haftanın hangi gününe denk geliyor? (Pazartesi=2, Pazar=1)
        // Türkiye formatında Pazartesi=1 yapmak için:
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK); // 1:Pazar, 2:Pzt...
        int emptyCells = (firstDayOfWeek == Calendar.SUNDAY) ? 6 : firstDayOfWeek - 2;

        // Başlangıçtaki boş hücreler
        for (int i = 0; i < emptyCells; i++) {
            days.add(null);
        }

        // Ayın günleri
        int totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int i = 1; i <= totalDays; i++) {
            Calendar day = (Calendar) cal.clone();
            day.set(Calendar.DAY_OF_MONTH, i);
            days.add(day);
        }

        return days;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            // Giriş animasyonu
            dialog.getWindow().setWindowAnimations(R.style.CalendarDialogAnimation);
        }
    }
}
