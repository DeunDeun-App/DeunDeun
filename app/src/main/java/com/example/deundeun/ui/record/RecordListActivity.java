package com.example.deundeun.ui.record;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deundeun.data.FakeTrainingRepository;
import com.example.deundeun.databinding.ActivityRecordListBinding;
import com.example.deundeun.ui.common.InsetsUtil;
import com.example.deundeun.ui.home.RecentRecordAdapter;

/**
 * 홈의 "전체 보기" 화면. 훈련 기록을 최대
 * {@link FakeTrainingRepository#FULL_LIMIT}개까지 보여준다.
 *
 * <p>카드 모양은 홈·기록탭과 같아야 하므로 {@link RecentRecordAdapter}를 그대로 쓴다.
 */
public class RecordListActivity extends AppCompatActivity {

    private ActivityRecordListBinding binding;
    private final FakeTrainingRepository repository = new FakeTrainingRepository();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityRecordListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsUtil.applyTopInset(binding.btnBack);
        InsetsUtil.applyBottomInset(binding.recordList);

        binding.btnBack.setOnClickListener(v -> finish());

        RecentRecordAdapter adapter = new RecentRecordAdapter();
        binding.recordList.setAdapter(adapter);
        adapter.submit(repository.getAllRecords());
    }
}
