package com.example.deundeun.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.deundeun.R;
import com.example.deundeun.data.repository.FakeTrainingRepository;
import com.example.deundeun.data.model.WeeklySummary;
import com.example.deundeun.databinding.FragmentHomeBinding;
import com.example.deundeun.ui.call.CallIntroActivity;
import com.example.deundeun.ui.common.InsetsUtil;
import com.example.deundeun.ui.notification.NotificationActivity;
import com.example.deundeun.ui.record.RecordListActivity;
import com.example.deundeun.ui.simulation.SimulationIntroActivity;

/** 홈화면 */
public class HomeFragment extends Fragment {

    public static final String TAG = "home";

    private FragmentHomeBinding binding;

    // TODO: API 스펙 확정 후 Retrofit 기반 Repository 로 교체
    private final FakeTrainingRepository repository = new FakeTrainingRepository();

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

        WeeklySummary summary = repository.getWeeklySummary();

        binding.simulationCount.setText(
                getString(R.string.home_week_count, summary.getSimulationCount()));
        binding.emergencyCallCount.setText(
                getString(R.string.home_week_count, summary.getEmergencyCallCount()));
        binding.weeklyTotal.setText(String.valueOf(summary.getTotalMinutes()));

        WeeklyChartBinder.bind(binding.weeklyChart, summary);

        RecentRecordAdapter adapter = new RecentRecordAdapter();
        binding.recentRecords.setAdapter(adapter);
        adapter.submit(repository.getRecentRecords());

        binding.btnSeeAll.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), RecordListActivity.class)));
        binding.btnNotification.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), NotificationActivity.class)));
        binding.cardSimulation.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SimulationIntroActivity.class)));
        binding.cardEmergencyCall.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CallIntroActivity.class)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
