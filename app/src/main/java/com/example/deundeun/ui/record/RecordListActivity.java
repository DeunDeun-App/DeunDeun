package com.example.deundeun.ui.record;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deundeun.R;
import com.example.deundeun.data.model.TrainingRecord;
import com.example.deundeun.data.network.dto.record.RecordResponse;
import com.example.deundeun.data.repository.RecordRepository;
import com.example.deundeun.databinding.ActivityRecordListBinding;
import com.example.deundeun.ui.common.InsetsUtil;
import com.example.deundeun.ui.home.RecentRecordAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * 홈의 "전체 보기" 화면. 서버가 주는 최근 훈련 기록(최대 15개)을 보여줌
 */
public class RecordListActivity extends AppCompatActivity {

    private ActivityRecordListBinding binding;

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

        RecordRepository repository = new RecordRepository(this);
        if (!repository.isLoggedIn()) {
            return;
        }
        repository.getRecords(result -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (!result.isSuccess() || result.getData() == null) {
                Toast.makeText(this, result.getMessageOr(getString(R.string.record_load_failed)),
                        Toast.LENGTH_SHORT).show();
                return;
            }
            List<TrainingRecord> records = new ArrayList<>();
            for (RecordResponse.Item item : result.getData().getRecords()) {
                records.add(TrainingRecord.from(item));
            }
            adapter.submit(records);
        });
    }
}
