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

/** 홈 "이번 주 훈련 기록" 카드의 요일 막대 7개를 그린다. */
final class WeeklyChartBinder {

    /**
     * 가장 큰 막대가 트랙 꼭대기에 닿지 않도록 두는 여유분.
     * 시안(29:59)의 최고 막대가 트랙의 약 93% 였다.
     */
    private static final float HEADROOM = 1.08f;

    /**
     * 요일별 막대 색. 시안(월=#F97316, 화=#FFC677, 수=#FF9246)을 그대로 옮긴 것으로,
     * 값에 따른 규칙이 아니라 장식이다. 규칙이 필요하면 디자이너 확인 후 바꾼다.
     */
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

            // 막대는 시각 정보뿐이라 스크린리더용으로 값을 말로 붙여준다.
            bar.getRoot().setContentDescription(
                    context.getString(R.string.cd_chart_bar, labels[day], minutes[day]));
        }
    }
}
