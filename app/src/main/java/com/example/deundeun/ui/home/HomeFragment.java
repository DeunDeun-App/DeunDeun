package com.example.deundeun.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.example.deundeun.R;
import com.example.deundeun.data.model.TrainingRecord;
import com.example.deundeun.data.model.WeeklySummary;
import com.example.deundeun.data.network.dto.record.RecordResponse;
import com.example.deundeun.data.repository.RecordRepository;
import com.example.deundeun.data.session.SessionManager;
import com.example.deundeun.databinding.FragmentHomeBinding;
import com.example.deundeun.ui.call.CallIntroActivity;
import com.example.deundeun.ui.common.InsetsUtil;
import com.example.deundeun.ui.notification.NotificationActivity;
import com.example.deundeun.ui.record.RecordListActivity;
import com.example.deundeun.ui.simulation.SimulationIntroActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** 홈화면. 인사말, 재난 행동요령, 이번 주 훈련 횟수·시간과 최근 훈련 기록 */
public class HomeFragment extends Fragment {

    public static final String TAG = "home";
    private static final String PREFS_NAME = "home";
    private static final String KEY_BANNER_INDEX = "safety_banner_index";

    /**
     * 행동요령 배너 그림과 낭독 문장. 앱에 새로 들어올 때마다 다음 것으로 넘어간다.
     * 그림을 더 만들면 두 배열에 같은 순서로 하나씩 더하면 된다.
     */
    @DrawableRes
    private static final int[] BANNER_IMAGES = {
            R.drawable.banner_safety_fire,
            R.drawable.banner_safety_quake,
    };
    @StringRes
    private static final int[] BANNER_DESCRIPTIONS = {
            R.string.cd_safety_banner_fire,
            R.string.cd_safety_banner_quake,
    };

    /** 홈에 미리 보여줄 최근 훈련 수. 나머지는 "전체 보기" */
    private static final int PREVIEW_LIMIT = 5;

    private FragmentHomeBinding binding;
    private RecordRepository repository;
    private SessionManager session;
    private final RecentRecordAdapter adapter = new RecentRecordAdapter();
    /** 지금 보여주는 배너 번호. 화면을 돌려도 같은 그림이 유지되게 저장한다 */
    private int bannerIndex;

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
        session = new SessionManager(requireContext());
        binding.recentRecords.setAdapter(adapter);

        bannerIndex = savedInstanceState == null
                ? nextBannerIndex()
                : savedInstanceState.getInt(KEY_BANNER_INDEX, 0);
        bindSafetyBanner();
        bindGreeting();

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

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_BANNER_INDEX, bannerIndex);
    }

    private void load() {
        // 홈에 다시 올 때마다 시간대와 이름(정보 수정 후)을 반영한다
        bindGreeting();
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
        bindWeeklyChart(summary);

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

    /** 요일별 훈련 시간(분). 훈련한 요일은 막대와 요일 글씨를 주황색으로 강조한다 */
    private void bindWeeklyChart(WeeklySummary summary) {
        String[] days = getResources().getStringArray(R.array.weekday_labels);
        int[] minutes = summary.getMinutesByDay();
        boolean[] trained = new boolean[minutes.length];
        for (int i = 0; i < minutes.length; i++) {
            trained[i] = minutes[i] > 0;
        }
        binding.weeklyChart.setValueSuffix(getString(R.string.home_unit_minute));
        binding.weeklyChart.setDescription(getString(R.string.home_weekly_title),
                getString(R.string.home_unit_minute));
        binding.weeklyChart.setData(days, minutes, trained);
    }

    /** "이현서 님, 좋은 아침이에요". 이름을 모르면 인사만 */
    private void bindGreeting() {
        String hello = getString(greetingFor(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)));
        String name = session.getUserName();
        binding.homeGreeting.setText(TextUtils.isEmpty(name)
                ? hello
                : getString(R.string.home_greeting, name, hello));
    }

    /** 05~11시 아침, 11~17시 오후, 17~22시 저녁, 그 밖은 밤 */
    @StringRes
    private static int greetingFor(int hour) {
        if (hour >= 5 && hour < 11) {
            return R.string.home_greeting_morning;
        }
        if (hour >= 11 && hour < 17) {
            return R.string.home_greeting_afternoon;
        }
        if (hour >= 17 && hour < 22) {
            return R.string.home_greeting_evening;
        }
        return R.string.home_greeting_night;
    }

    /** 앱에 새로 들어올 때 부른다. 지난번 다음 배너를 골라 저장한다 */
    private int nextBannerIndex() {
        SharedPreferences prefs = requireContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int next = (prefs.getInt(KEY_BANNER_INDEX, -1) + 1) % BANNER_IMAGES.length;
        prefs.edit().putInt(KEY_BANNER_INDEX, next).apply();
        return next;
    }

    private void bindSafetyBanner() {
        int index = bannerIndex % BANNER_IMAGES.length;
        binding.safetyBanner.setImageResource(BANNER_IMAGES[index]);
        binding.safetyBanner.setContentDescription(getString(BANNER_DESCRIPTIONS[index]));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
