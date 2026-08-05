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
 * {@link FakeTrainingRepository#FULL_LIMIT}개까지 보여줌
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
