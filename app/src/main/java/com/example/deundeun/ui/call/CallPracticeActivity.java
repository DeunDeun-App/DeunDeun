package com.example.deundeun.ui.call;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deundeun.R;
import com.example.deundeun.databinding.ActivityCallPracticeBinding;
import com.example.deundeun.ui.common.InsetsUtil;
import com.example.deundeun.ui.common.TrainingCompleteActivity;

import java.util.Locale;

/**
 * 가상 119 통화
 *
 * 음성 인식은 아직 붙지 않음. 마이크 버튼을 누르면 정해진 대본이 한 줄씩 진행되어
 * 화면 흐름을 확인할 수 있다. STT 가 붙으면 {@link #onMicPressed()} 안에서
 * 인식 결과를 {@link CallMessage#fromMe} 로 넣고, 판정 결과를 완료 화면에 넘기면 된다.
 */
public class CallPracticeActivity extends AppCompatActivity {

    private ActivityCallPracticeBinding binding;
    private CallMessageAdapter adapter;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private long startedAt;
    private int scriptStep;

    /** 1초마다 통화 시간을 갱신한다. */
    private final Runnable timerTick = new Runnable() {
        @Override
        public void run() {
            updateTimer();
            handler.postDelayed(this, 1000L);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityCallPracticeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsUtil.applyTopInset(binding.callRoot);
        InsetsUtil.applyBottomInset(binding.ctaContainer);

        adapter = new CallMessageAdapter();
        binding.messageList.setAdapter(adapter);

        binding.hintMessage.setText(R.string.call_hint_1);
        binding.btnMic.setOnClickListener(v -> onMicPressed());
        binding.btnEndCall.setOnClickListener(v -> endCall());

        adapter.add(CallMessage.fromOperator(getString(R.string.call_line_1)));

        startedAt = SystemClock.elapsedRealtime();
        updateTimer();
    }

    @Override
    protected void onStart() {
        super.onStart();
        handler.post(timerTick);
    }

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacks(timerTick);
    }

    private void updateTimer() {
        long seconds = (SystemClock.elapsedRealtime() - startedAt) / 1000L;
        String elapsed = String.format(Locale.KOREA, "%02d:%02d", seconds / 60, seconds % 60);
        binding.callTimer.setText(getString(R.string.call_ongoing, elapsed));
    }

    /** TODO: 음성 인식 연동. 지금은 대본을 한 단계씩 진행한다. */
    private void onMicPressed() {
        if (scriptStep == 0) {
            adapter.add(CallMessage.fromMe(getString(R.string.call_line_2)));
            adapter.add(CallMessage.fromOperator(getString(R.string.call_line_3)));
            scriptStep++;
        } else {
            endCall();
            return;
        }
        binding.messageList.smoothScrollToPosition(adapter.getLastPosition());
    }

    private void endCall() {
        Intent intent = new Intent(this, TrainingCompleteActivity.class);
        intent.putExtra(TrainingCompleteActivity.EXTRA_TITLE_RES, R.string.complete_practice);
        // 시안(125:402)의 판정 결과. STT 가 붙으면 실제 판정으로 대체한다.
        intent.putExtra(TrainingCompleteActivity.EXTRA_CHECK_LABELS, new int[]{
                R.string.check_situation, R.string.check_address, R.string.check_condition});
        intent.putExtra(TrainingCompleteActivity.EXTRA_CHECK_RESULTS,
                new boolean[]{true, true, false});
        startActivity(intent);
        finish();
    }
}
