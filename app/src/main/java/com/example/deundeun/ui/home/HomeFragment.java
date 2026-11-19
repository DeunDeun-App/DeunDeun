package com.example.deundeun.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.deundeun.R;
import com.example.deundeun.data.model.TrainingRecord;
import com.example.deundeun.data.model.WeeklySummary;
import com.example.deundeun.data.network.dto.record.RecordResponse;
import com.example.deundeun.data.repository.RecordRepository;
import com.example.deundeun.databinding.FragmentHomeBinding;
import com.example.deundeun.ui.call.CallIntroActivity;
import com.example.deundeun.ui.common.InsetsUtil;
import com.example.deundeun.ui.notification.NotificationActivity;
import com.example.deundeun.ui.record.RecordListActivity;
import com.example.deundeun.ui.simulation.SimulationIntroActivity;

import java.util.ArrayList;
import java.util.List;

/** 홈화면. 이번 주 훈련 횟수·시간과 최근 훈련 기록 */
public class HomeFragment extends Fragment {

    public static final String TAG = "home";

    /** 홈에 미리 보여줄 최근 훈련 수. 나머지는 "전체 보기" */
    private static final int PREVIEW_LIMIT = 5;

    private FragmentHomeBinding binding;
    private RecordRepository repository;
    private final RecentRecordAdapter adapter = new RecentRecordAdapter();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        InsetsUtil.applyBottomInset(binding.homeScroll);
        repository = new RecordRepository(requireContext());
        binding.recentRecords.setAdapter(adapter);

        binding.btnSeeAll.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), RecordListActivity.class)));
        binding.btnNotification.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), NotificationActivity.class)));
        binding.cardSimulation.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SimulationIntroActivity.class)));
        binding.cardEmergencyCall.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CallIntroActivity.class)));

        bind(null);
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
                bind(result.getData());
            } else {
                Toast.makeText(requireContext(),
                        result.getMessageOr(getString(R.string.record_load_failed)),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** @param data 서버 응답. null 이면 기록이 없는 화면 */
    private void bind(@Nullable RecordResponse data) {
        WeeklySummary summary = WeeklySummary.from(data);

        binding.simulationCount.setText(
                getString(R.string.home_week_count, summary.getSimulationCount()));
        binding.emergencyCallCount.setText(
                getString(R.string.home_week_count, summary.getEmergencyCallCount()));
        binding.weeklyTotal.setText(String.valueOf(summary.getTotalMinutes()));
        WeeklyChartBinder.bind(binding.weeklyChart, summary);

        List<TrainingRecord> records = new ArrayList<>();
        if (data != null) {
            for (RecordResponse.Item item : data.getRecords()) {
                if (records.size() >= PREVIEW_LIMIT) {
                    break;
                }
                records.add(TrainingRecord.from(item));
            }
        }
        adapter.submit(records);
        binding.recentEmpty.setVisibility(records.isEmpty() ? View.VISIBLE : View.GONE);
        binding.btnSeeAll.setVisibility(records.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
