package com.example.deundeun.ui.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.example.deundeun.R;
import com.example.deundeun.data.model.WeeklySummary;
import com.example.deundeun.databinding.ItemChartBarBinding;

final class WeeklyChartBinder {

    private static final float HEADROOM = 1.08f;

    private static final int[] BAR_COLORS = {
            R.color.orange_primary,
            R.color.orange_pale,
            R.color.orange_mid,
            R.color.orange_primary,
            R.color.orange_pale,
            R.color.orange_mid,
            R.color.orange_primary,
    };

    private WeeklyChartBinder() {
    }

    static void bind(LinearLayout chart, WeeklySummary summary) {
        Context context = chart.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        String[] labels = context.getResources().getStringArray(R.array.weekday_labels);
        int[] minutes = summary.getMinutesByDay();
        int trackHeight = context.getResources().getDimensionPixelSize(R.dimen.chart_track_height);
        float scale = summary.getMaxMinutes() * HEADROOM;

        chart.removeAllViews();
        for (int day = 0; day < labels.length; day++) {
            ItemChartBarBinding bar = ItemChartBarBinding.inflate(inflater, chart, false);

            bar.barLabel.setText(labels[day]);

            View fill = bar.barFill;
            fill.getLayoutParams().height = Math.round(trackHeight * (minutes[day] / scale));
            ViewCompat.setBackgroundTintList(fill,
                    ContextCompat.getColorStateList(context, BAR_COLORS[day]));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            chart.addView(bar.getRoot(), params);

            bar.getRoot().setContentDescription(
                    context.getString(R.string.cd_chart_bar, labels[day], minutes[day]));
        }
    }
}
