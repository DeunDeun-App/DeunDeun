package com.example.deundeun.ui.record;

import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;

import com.example.deundeun.R;
import com.example.deundeun.data.network.dto.record.RecordResponse;
import com.example.deundeun.data.repository.RecordRepository;
import com.example.deundeun.databinding.FragmentRecordBinding;
import com.example.deundeun.databinding.ItemAchievementRateBinding;
import com.example.deundeun.databinding.ViewScoreReportBinding;
import com.example.deundeun.ui.common.InsetsUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 기록탭. 위 탭에서 훈련 종류(재난 시뮬레이션 / 119 신고 연습)를 고르면
 * 그 종류의 숫자 요약(이번 주 횟수, 최근 점수, 지난번 대비), 회차별 점수, 항목별 달성률을 보여준다.
 * 그래프는 점수 하나만 둔다. 주별 꾸준함은 홈의 "이번주 N번 했어요"와 요일별 막대가 맡는다.
 * 하나하나의 훈련 기록은 홈의 "최근 훈련 기록"에서 본다.
 */
public class RecordFragment extends Fragment {

    public static final String TAG = "record";
    private static final String STATE_CALL_TAB = "call_tab";


    /** 지난번 대비의 ↑·↓ 크기. 숫자 대비 비율 */
    private static final float ARROW_SCALE = 0.8f;

    private FragmentRecordBinding binding;
    private RecordRepository repository;

    /** 마지막으로 받은 서버 응답. 탭을 바꿀 때 다시 불러오지 않고 이걸로 그린다 */
    @Nullable
    private RecordResponse data;
    private boolean callTab;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRecordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        InsetsUtil.applyBottomInset(binding.recordScroll);
        repository = new RecordRepository(requireContext());

        if (savedInstanceState != null) {
            callTab = savedInstanceState.getBoolean(STATE_CALL_TAB, false);
        }

        binding.tabSimulation.setOnClickListener(v -> selectTab(false));
        binding.tabCall.setOnClickListener(v -> selectTab(true));

        render();
    }

    /** 훈련을 마치고 돌아오면 새 기록이 보이도록 화면에 나올 때마다 다시 불러온다 */
    @Override
    public void onResume() {
        super.onResume();
        if (!isHidden()) {
            load();
        }
    }

    /** 하단 탭은 hide/show 로 바꿔서 onResume 이 다시 불리지 않는다. 탭이 다시 보일 때도 불러온다 */
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && binding != null) {
            load();
        }
    }

    private void load() {
        if (!repository.isLoggedIn()) {
            return;
        }
        repository.getRecords(result -> {
            if (binding == null) {
                return;
            }
            if (result.isSuccess()) {
                data = result.getData();
                render();
            } else {
                Toast.makeText(requireContext(),
                        result.getMessageOr(getString(R.string.record_load_failed)),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_CALL_TAB, callTab);
    }

    private void selectTab(boolean call) {
        if (callTab == call) {
            return;
        }
        callTab = call;
        render();
    }

    private void render() {
        styleTab(binding.tabSimulation, !callTab);
        styleTab(binding.tabCall, callTab);

        String typeName = getString(callTab ? R.string.record_report_call
                : R.string.record_report_simulation);
        bindReport(binding.report, typeName,
                data == null ? null : (callTab ? data.getCall() : data.getSimulation()));
    }

    /** 주별 횟수의 마지막 칸이 이번 주 */
    private int thisWeekCount() {
        if (data == null) {
            return 0;
        }
        List<Integer> counts = callTab ? data.getCallCounts() : data.getSimulationCounts();
        if (counts.isEmpty()) {
            return 0;
        }
        Integer value = counts.get(counts.size() - 1);
        return value == null ? 0 : value;
    }

    private void styleTab(TextView tab, boolean selected) {
        tab.setBackgroundResource(selected ? R.drawable.bg_segmented_thumb : 0);
        tab.setTextColor(ContextCompat.getColor(requireContext(),
                selected ? R.color.text_primary : R.color.text_secondary));
        tab.setTypeface(ResourcesCompat.getFont(requireContext(),
                selected ? R.font.pretendard_extrabold : R.font.pretendard_semibold));
        tab.setSelected(selected);
    }

    private void bindReport(ViewScoreReportBinding report, String typeName,
                            @Nullable RecordResponse.Report reportData) {
        List<Integer> scores = reportData == null ? new ArrayList<>() : reportData.getScores();

        report.reportMeta.setText(scores.isEmpty()
                ? getString(R.string.record_no_practice)
                : getString(R.string.record_recent_count, scores.size()));
        report.reportChart.setFixedMax(100);
        report.reportChart.setDescription(typeName + " " + getString(R.string.record_tab_score), "점");

        boolean practiced = !scores.isEmpty();
        int[] values = toArray(scores);
        bindSummary(report, values);
        report.reportChart.setVisibility(practiced ? View.VISIBLE : View.GONE);
        report.reportChartEmpty.setVisibility(practiced ? View.GONE : View.VISIBLE);
        if (practiced) {
            // 가장 최근 연습만 진하게 칠해 "지금"이 어디인지 보이게 한다
            boolean[] latestOnly = new boolean[values.length];
            latestOnly[values.length - 1] = true;
            report.reportChart.setData(attemptLabels(values.length), values, latestOnly);
        }

        List<RecordResponse.Rate> rates = reportData == null ? new ArrayList<>() : reportData.getRates();
        bindRates(report.reportRates, rates);
        report.reportRatesEmpty.setVisibility(rates.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /**
     * 숫자 3칸: 이번 주 횟수, 최근 점수, 바로 전 연습 대비 변화(오르면 주황, 내리면 회색).
     * 아직 연습하지 않았으면 점수 칸은 "–"
     */
    private void bindSummary(ViewScoreReportBinding report, int[] scores) {
        int weekCount = thisWeekCount();
        report.reportWeekCount.setText(getString(R.string.record_count_value, weekCount));
        report.summaryWeek.setContentDescription(
                getString(R.string.record_this_week) + " " + weekCount + "회");

        if (scores.length == 0) {
            String none = getString(R.string.record_value_none);
            report.reportLatestScore.setText(none);
            report.reportDelta.setText(none);
            report.reportDelta.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_pill));
            report.summaryLatest.setContentDescription(
                    getString(R.string.record_latest_score) + " " + getString(R.string.record_no_practice));
            report.summaryDelta.setContentDescription(null);
            return;
        }

        int latest = scores[scores.length - 1];
        report.reportLatestScore.setText(getString(R.string.record_score_value, latest));
        report.summaryLatest.setContentDescription(
                getString(R.string.record_latest_score) + " " + latest + "점");

        int colorRes = R.color.text_pill;
        String delta;
        if (scores.length < 2) {
            delta = getString(R.string.record_delta_first);
        } else {
            int diff = latest - scores[scores.length - 2];
            if (diff > 0) {
                delta = getString(R.string.record_delta_up, diff);
                colorRes = R.color.orange_primary;
            } else if (diff < 0) {
                delta = getString(R.string.record_delta_down, -diff);
            } else {
                delta = getString(R.string.record_delta_same);
            }
        }
        report.reportDelta.setText(withSmallArrow(delta));
        report.reportDelta.setTextColor(ContextCompat.getColor(requireContext(), colorRes));
        report.summaryDelta.setContentDescription(getString(R.string.record_delta_title) + " " + delta);
    }

    /** ↑·↓ 는 숫자보다 눈에 덜 띄게 조금 작게 그린다 */
    private static CharSequence withSmallArrow(String text) {
        SpannableString span = new SpannableString(text);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '↑' || c == '↓') {
                span.setSpan(new RelativeSizeSpan(ARROW_SCALE), i, i + 1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return span;
    }

    private void bindRates(ViewGroup container, List<RecordResponse.Rate> rates) {
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(container.getContext());

        // 피그마처럼 높은 항목부터 보여주고, 순위에 따라 진한 주황 → 연한 주황으로 칠한다
        List<RecordResponse.Rate> sorted = new ArrayList<>(rates);
        Collections.sort(sorted, (a, b) -> Integer.compare(b.getPercent(), a.getPercent()));

        for (int rank = 0; rank < sorted.size(); rank++) {
            RecordResponse.Rate rate = sorted.get(rank);
            int color = colorFor(rank);
            ItemAchievementRateBinding row =
                    ItemAchievementRateBinding.inflate(inflater, container, false);

            row.rateLabel.setText(rate.getLabel());
            row.ratePercent.setText(getString(R.string.percent_format, rate.getPercent()));
            row.ratePercent.setTextColor(ContextCompat.getColor(requireContext(), color));

            ConstraintLayout.LayoutParams params =
                    (ConstraintLayout.LayoutParams) row.rateFill.getLayoutParams();
            params.matchConstraintPercentWidth = rate.getPercent() / 100f;
            row.rateFill.setLayoutParams(params);
            // 0% 면 막대 너비가 0 이라 숨긴다
            row.rateFill.setVisibility(rate.getPercent() > 0 ? View.VISIBLE : View.INVISIBLE);
            ViewCompat.setBackgroundTintList(row.rateFill,
                    ContextCompat.getColorStateList(requireContext(), color));

            row.getRoot().setContentDescription(
                    rate.getLabel() + " " + rate.getPercent() + "퍼센트");

            container.addView(row.getRoot());
        }
    }

    /** @param rank 0 이 가장 높은 항목 */
    private static int colorFor(int rank) {
        if (rank == 0) {
            return R.color.orange_primary;
        }
        return rank == 1 ? R.color.orange_mid : R.color.orange_pale;
    }

    /**
     * 점수 그래프 아래 라벨. 같은 날 여러 번 하면 날짜가 겹쳐서 순서로 적는다.
     * 예: 1회 · 2회 · 3회 · 최근
     */
    private String[] attemptLabels(int count) {
        String[] labels = new String[count];
        for (int i = 0; i < count; i++) {
            labels[i] = i == count - 1
                    ? getString(R.string.record_attempt_latest)
                    : getString(R.string.record_attempt, i + 1);
        }
        return labels;
    }

    private static int[] toArray(List<Integer> values) {
        int[] array = new int[values.size()];
        for (int i = 0; i < values.size(); i++) {
            Integer value = values.get(i);
            array[i] = value == null ? 0 : value;
        }
        return array;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
