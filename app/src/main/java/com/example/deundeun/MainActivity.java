package com.example.deundeun;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.deundeun.databinding.ActivityMainBinding;
import com.example.deundeun.ui.common.InsetsUtil;
import com.example.deundeun.ui.common.NavItemView;
import com.example.deundeun.ui.common.NotchedBarDrawable;
import com.example.deundeun.ui.home.HomeFragment;
import com.example.deundeun.ui.profile.ProfileFragment;
import com.example.deundeun.ui.record.RecordFragment;
import com.example.deundeun.ui.sos.SosActivity;
import com.example.deundeun.ui.training.TrainingFragment;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 4개 탭(홈·훈련·기록·내정보)을 담는 단일 Activity.
 *
 * <p>탭 전환은 show/hide 로 처리해 각 탭의 스크롤 위치와 상태를 유지한다.
 * (replace 로 갈아끼우면 탭을 옮길 때마다 화면이 처음부터 다시 그려진다.)
 */
public class MainActivity extends AppCompatActivity {

    private static final String STATE_SELECTED_TAB = "selected_tab";

    /**
     * 열고 싶은 탭의 태그. 다른 화면에서 이 Activity 로 되돌아올 때 어느 탭을 볼지 정한다.
     * 예) SOS 를 마치고 "괜찮아요" 를 누르면 홈으로 온다.
     */
    public static final String EXTRA_TAB = "tab";

    private ActivityMainBinding binding;

    /** 탭 뷰 → 해당 탭의 프래그먼트 태그. 순서가 곧 화면 순서다. */
    private final Map<Integer, String> tabs = new LinkedHashMap<>();

    private String selectedTag;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsUtil.applyTopInset(binding.fragmentContainer);
        InsetsUtil.applyBottomBarInset(binding.bottomBar);
        applyBottomBarBackground();

        tabs.put(R.id.nav_home, HomeFragment.TAG);
        tabs.put(R.id.nav_training, TrainingFragment.TAG);
        tabs.put(R.id.nav_record, RecordFragment.TAG);
        tabs.put(R.id.nav_profile, ProfileFragment.TAG);

        for (Integer viewId : tabs.keySet()) {
            findViewById(viewId).setOnClickListener(this::onTabClicked);
        }

        binding.btnSos.setOnClickListener(v ->
                startActivity(new Intent(this, SosActivity.class)));

        String initialTag = savedInstanceState != null
                ? savedInstanceState.getString(STATE_SELECTED_TAB, HomeFragment.TAG)
                : HomeFragment.TAG;
        showTab(requestedTab(getIntent(), initialTag));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // 이미 떠 있는 상태로 되돌아온 경우. onCreate 는 다시 불리지 않는다.
        showTab(requestedTab(intent, selectedTag));
    }

    private String requestedTab(@Nullable Intent intent, String fallback) {
        String tab = intent == null ? null : intent.getStringExtra(EXTRA_TAB);
        return tab == null ? fallback : tab;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_SELECTED_TAB, selectedTag);
    }

    /** SOS 버튼 지름에서 노치 반지름을 계산해 배경을 만든다. 시안 기준 여유는 4dp. */
    private void applyBottomBarBackground() {
        float sosRadius = getResources().getDimensionPixelSize(R.dimen.sos_size) / 2f;
        float gap = getResources().getDisplayMetrics().density * 4f;
        binding.bottomBar.setBackground(new NotchedBarDrawable(
                ContextCompat.getColor(this, R.color.white), sosRadius + gap));
    }

    private void onTabClicked(View view) {
        String tag = tabs.get(view.getId());
        if (tag != null) {
            showTab(tag);
        }
    }

    private void showTab(String tag) {
        if (tag.equals(selectedTag)) {
            return;
        }
        selectedTag = tag;

        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        for (String candidate : tabs.values()) {
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(candidate);
            if (candidate.equals(tag)) {
                if (fragment == null) {
                    tx.add(R.id.fragment_container, createFragment(candidate), candidate);
                } else {
                    tx.show(fragment);
                }
            } else if (fragment != null) {
                tx.hide(fragment);
            }
        }
        tx.commit();

        syncTabSelection(tag);
    }

    private void syncTabSelection(String tag) {
        for (Map.Entry<Integer, String> entry : tabs.entrySet()) {
            NavItemView item = findViewById(entry.getKey());
            item.setSelected(entry.getValue().equals(tag));
        }
    }

    private Fragment createFragment(String tag) {
        switch (tag) {
            case TrainingFragment.TAG:
                return new TrainingFragment();
            case RecordFragment.TAG:
                return new RecordFragment();
            case ProfileFragment.TAG:
                return new ProfileFragment();
            case HomeFragment.TAG:
            default:
                return new HomeFragment();
        }
    }
}
