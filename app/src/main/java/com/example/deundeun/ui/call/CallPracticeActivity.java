package com.example.deundeun.ui.call;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.example.deundeun.R;
import com.example.deundeun.data.network.dto.call.CallAnswerResponse;
import com.example.deundeun.data.network.dto.call.CallItemResponse;
import com.example.deundeun.data.network.dto.call.CallItemResultResponse;
import com.example.deundeun.data.network.dto.call.CallResultResponse;
import com.example.deundeun.data.network.dto.call.CallSessionResponse;
import com.example.deundeun.data.repository.CallRepository;
import com.example.deundeun.databinding.ActivityCallPracticeBinding;
import com.example.deundeun.ui.common.InsetsUtil;
import com.example.deundeun.ui.common.SessionNavigator;
import com.example.deundeun.ui.common.TrainingCompleteActivity;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 가상 119 통화
 *
 * 상담원 질문 → 사용자 대답(음성 인식) → 서버 판정 → 다음 질문 순서로 주고받는다.
 * 판정에 실패하면 도움말에 힌트를 보여주고 같은 질문을 한 번 더 묻는다.
 * 기회를 다 쓰면 실패로 기록하고 다음 질문으로 넘어간다.
 *
 * 개발 빌드에서는 마이크를 길게 누르면 글자로 대답할 수 있다 (에뮬레이터 테스트용).
 */
public class CallPracticeActivity extends AppCompatActivity {

    /** 화면이 지금 무엇을 하고 있는지. 상태마다 마이크와 안내 문구가 달라진다. */
    private enum State {
        CONNECTING, SPEAKING, READY, LISTENING, JUDGING, FINISHED
    }

    private ActivityCallPracticeBinding binding;
    private CallMessageAdapter adapter;

    private CallRepository repository;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private SpeechInput speechInput;
    private OperatorVoice operatorVoice;

    private final ActivityResultLauncher<String> micPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startListening();
                } else {
                    showHint(getString(R.string.call_mic_denied));
                }
            });

    private State state;
    /** 못 알아들어서 한 번 더 듣는 중인지. 안내 문구를 지우지 않으려고 쓴다. */
    private boolean listeningAgain;
    private long startedAt;
    private boolean debuggable;

    private long sessionId;
    private List<CallItemResponse> items = new ArrayList<>();
    private int index;

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
        // 새 말풍선은 어댑터에서 올라오는 효과를 직접 준다
        binding.messageList.setItemAnimator(null);
        binding.hintCard.setVisibility(View.GONE);

        debuggable = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        speechInput = new SpeechInput(this);
        speechInput.setHints(Arrays.asList(getResources().getStringArray(R.array.call_speech_hints)));
        operatorVoice = new OperatorVoice(this);

        binding.btnMic.setOnClickListener(v -> onMicPressed());
        binding.btnMic.setOnLongClickListener(v -> {
            if (!debuggable || state != State.READY) {
                return false;
            }
            showTypeAnswerDialog();
            return true;
        });
        binding.btnEndCall.setOnClickListener(v -> endCall());

        startedAt = SystemClock.elapsedRealtime();
        updateTimer();
        setState(State.CONNECTING);

        repository = new CallRepository(this);
        if (!repository.isLoggedIn()) {
            Toast.makeText(this, R.string.call_login_required, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        startSession();
    }

    @Override
    protected void onStart() {
        super.onStart();
        handler.post(timerTick);

        // 마무리 멘트 중에 화면을 벗어났다가 돌아오면 바로 종료한다
        if (state == State.READY && isAllAsked()) {
            endCall();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacks(timerTick);

        // 화면을 벗어나면 듣기와 읽기를 멈추고, 돌아오면 사용자가 다시 마이크를 누르게 한다
        if (state == State.SPEAKING || state == State.LISTENING) {
            speechInput.stop();
            operatorVoice.stop();
            setState(State.READY);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        speechInput.destroy();
        operatorVoice.shutdown();
    }

    private void startSession() {
        repository.startSession(result -> {
            CallSessionResponse session = result.getData();

            if ("SESSION_EXPIRED".equals(result.getCode())) {
                SessionNavigator.toLogin(this, R.string.error_session_expired);
                return;
            }
            if (!result.isSuccess() || session == null || session.getItems().isEmpty()) {
                Toast.makeText(this, result.getMessageOr(getString(R.string.error_network)),
                        Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            sessionId = session.getSessionId();
            items = session.getItems();
            showSituation(session.getSituationText());
            index = 0;
            askCurrentQuestion();
        });
    }

    // ① 상담원 질문
    private void askCurrentQuestion() {
        // 앞 질문의 도움말은 새 질문과 맞지 않으므로 지운다
        binding.hintCard.setVisibility(View.GONE);
        String question = items.get(index).getQuestion();
        operatorSays(question, () -> setState(State.READY));
    }

    private void operatorSays(String text, Runnable onDone) {
        addMessage(CallMessage.fromOperator(text));
        setState(State.SPEAKING);
        operatorVoice.speak(text, onDone);
    }

    // ② 마이크. 듣는 중에 한 번 더 누르면 다 말한 것으로 본다
    private void onMicPressed() {
        if (state == State.LISTENING) {
            speechInput.finish();
            return;
        }
        if (state != State.READY || isAllAsked()) {
            return;
        }
        if (!SpeechInput.isAvailable(this)) {
            if (debuggable) {
                showTypeAnswerDialog();
            } else {
                showHint(getString(R.string.call_speech_unavailable));
            }
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            micPermission.launch(Manifest.permission.RECORD_AUDIO);
            return;
        }
        startListening();
    }

    private void startListening() {
        setState(State.LISTENING);
        listeningAgain = false;
        showLiveText(null);
        speechInput.start(new SpeechInput.Listener() {
            @Override
            public void onPreparing() {
                binding.liveText.setText(R.string.call_speech_preparing);
            }

            @Override
            public void onReady() {
                if (!listeningAgain) {
                    showLiveText(null);
                }
            }

            @Override
            public void onListenAgain() {
                listeningAgain = true;
                binding.liveText.setText(R.string.call_listen_again);
                binding.liveText.setTextColor(ContextCompat.getColor(CallPracticeActivity.this,
                        R.color.text_secondary));
            }

            @Override
            public void onPartial(String text) {
                showLiveText(text);
            }

            @Override
            public void onLevel(float level) {
                binding.waveform.setLevel(level);
            }

            @Override
            public void onResult(String text) {
                onAnswer(text);
            }

            @Override
            public void onNoSpeech() {
                // 아무 말도 안 들렸을 때는 기회를 쓰지 않고 다시 듣는다
                showHint(getString(R.string.call_no_speech));
                setState(State.READY);
            }

            @Override
            public void onError() {
                showHint(getString(R.string.call_speech_error));
                setState(State.READY);
            }
        });
    }

    // ③ ~ ④ 사용자 대답 → 서버 판정
    private void onAnswer(String text) {
        addMessage(CallMessage.fromMe(text));
        setState(State.JUDGING);

        long itemId = items.get(index).getItemId();
        repository.answer(sessionId, itemId, text, result -> {
            if (state != State.JUDGING) {
                return;
            }

            CallAnswerResponse answer = result.getData();
            if (!result.isSuccess() || answer == null) {
                showHint(result.getMessageOr(getString(R.string.error_network)));
                setState(State.READY);
                return;
            }

            if (answer.isPassed()) {
                binding.hintCard.setVisibility(View.GONE);
                nextQuestion();
            } else if (!answer.isRetryable()) {
                // 기회를 다 썼다. 말없이 넘어가면 맞은 줄 알기 쉬우니, 어떻게 말하면 되는지 보여주고 넘어간다
                if (answer.getFeedback() != null) {
                    showHint(answer.getFeedback());
                }
                // 말하는 도중 화면을 벗어나도 끝난 질문에 다시 답하지 않게 순서를 먼저 넘긴다
                index++;
                operatorSays(getString(R.string.call_moving_on), this::askNextOrClose);
            } else if (answer.getFollowUp() != null) {
                // 상황을 일부만 말했다. 실제 상담원처럼 빠진 부분을 되묻는다
                binding.hintCard.setVisibility(View.GONE);
                operatorSays(answer.getFollowUp(), () -> setState(State.READY));
            } else {
                if (answer.getFeedback() != null) {
                    showHint(answer.getFeedback());
                }
                setState(State.READY);
            }
        });
    }

    private boolean isAllAsked() {
        return !items.isEmpty() && index >= items.size();
    }

    private void nextQuestion() {
        index++;
        askNextOrClose();
    }

    /** 지금 순서의 질문을 묻는다. 다 물었으면 마무리 멘트 후 종료한다. */
    private void askNextOrClose() {
        if (index < items.size()) {
            askCurrentQuestion();
        } else {
            operatorSays(getString(R.string.call_closing), this::endCall);
        }
    }

    // ⑤ 종료. 답하지 않은 질문은 서버에서 실패로 처리한다
    private void endCall() {
        if (state == State.FINISHED) {
            return;
        }
        if (state == State.CONNECTING) {
            finish();
            return;
        }

        speechInput.stop();
        operatorVoice.stop();
        setState(State.FINISHED);

        repository.complete(sessionId, result -> {
            CallResultResponse response = result.getData();

            if (!result.isSuccess() || response == null) {
                Toast.makeText(this, result.getMessageOr(getString(R.string.error_network)),
                        Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            showComplete(response);
        });
    }

    private void showComplete(CallResultResponse response) {
        List<CallItemResultResponse> results = response.getResults();
        String[] labels = new String[results.size()];
        boolean[] passed = new boolean[results.size()];
        for (int i = 0; i < results.size(); i++) {
            labels[i] = results.get(i).getLabel();
            passed[i] = results.get(i).isPassed();
        }

        Intent intent = new Intent(this, TrainingCompleteActivity.class);
        intent.putExtra(TrainingCompleteActivity.EXTRA_TITLE_RES, R.string.complete_practice);
        intent.putExtra(TrainingCompleteActivity.EXTRA_CHECK_LABEL_TEXTS, labels);
        intent.putExtra(TrainingCompleteActivity.EXTRA_CHECK_RESULTS, passed);
        intent.putExtra(TrainingCompleteActivity.EXTRA_RETRY_ACTIVITY, CallPracticeActivity.class);
        startActivity(intent);
        finish();
    }

    /** 개발용. 에뮬레이터처럼 말하기 어려운 환경에서 글자로 대답한다. */
    private void showTypeAnswerDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        new AlertDialog.Builder(this)
                .setTitle(R.string.call_type_answer_title)
                .setView(input)
                .setPositiveButton(R.string.call_type_answer_ok, (dialog, which) -> {
                    String text = input.getText().toString().trim();
                    if (!text.isEmpty() && state == State.READY && !isAllAsked()) {
                        onAnswer(text);
                    }
                })
                .setNegativeButton(R.string.call_type_answer_cancel, null)
                .show();
    }

    private void setState(State state) {
        this.state = state;

        boolean listening = state == State.LISTENING;
        binding.btnMic.setEnabled(state == State.READY || listening);
        if (listening) {
            binding.livePanel.setVisibility(View.VISIBLE);
            binding.waveform.start();
        } else {
            binding.livePanel.setVisibility(View.GONE);
            binding.waveform.stop();
        }
        binding.btnMic.setContentDescription(getString(listening ? R.string.cd_mic_done : R.string.cd_mic));
        // 안 눌렀을 때: 흰 원 + 주황 테두리 + 주황 마이크 / 말하는 중: 빨간 원 + 흰 마이크
        binding.btnMic.setBackgroundResource(listening
                ? R.drawable.bg_mic_listening : R.drawable.bg_mic_button);
        binding.btnMic.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this,
                listening ? R.color.white : R.color.orange_primary)));

        @StringRes int status;
        switch (state) {
            case CONNECTING:
                status = R.string.call_connecting;
                break;
            case SPEAKING:
                status = R.string.call_operator_speaking;
                break;
            case LISTENING:
                status = R.string.call_listening;
                break;
            case JUDGING:
            case FINISHED:
                status = R.string.call_judging;
                break;
            case READY:
            default:
                status = R.string.call_tap_mic;
                break;
        }
        binding.callStatus.setText(status);
    }

    /**
     * 이번에 신고할 상황을 단서 칩으로 보여준다. 통화 내내 위에 둔다.
     * 상황 이름(예: 홍수 신고)은 그대로 읽으면 통과하므로 보여주지 않는다.
     */
    private void showSituation(@Nullable String cues) {
        binding.situationCues.removeAllViews();
        if (cues == null || cues.trim().isEmpty()) {
            binding.situationCard.setVisibility(View.GONE);
            return;
        }

        List<String> words = new ArrayList<>();
        for (String cue : cues.split("·")) {
            String word = cue.trim();
            if (!word.isEmpty()) {
                words.add(word);
                binding.situationCues.addView(createCue(word));
            }
        }
        binding.situationCard.setContentDescription(
                getString(R.string.cd_call_situation, TextUtils.join(", ", words)));
        binding.situationCard.setVisibility(View.VISIBLE);
    }

    private Chip createCue(String word) {
        Chip chip = new Chip(this);
        chip.setText(word);
        chip.setClickable(false);
        chip.setFocusable(false);
        chip.setCheckable(false);
        chip.setEnsureMinTouchTargetSize(false);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        chip.setTypeface(ResourcesCompat.getFont(this, R.font.pretendard_semibold));
        chip.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        chip.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white)));
        chip.setChipStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.orange_pale)));
        chip.setChipStrokeWidth(getResources().getDisplayMetrics().density);
        chip.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return chip;
    }

    /** 말하는 동안 알아들은 글자. 아직 없으면 안내 문구를 흐리게 보여준다. */
    private void showLiveText(@Nullable String text) {
        boolean empty = text == null || text.isEmpty();
        binding.liveText.setText(empty ? getString(R.string.call_live_placeholder) : text);
        binding.liveText.setTextColor(ContextCompat.getColor(this,
                empty ? R.color.text_secondary : R.color.text_primary));
    }

    private void showHint(String message) {
        binding.hintMessage.setText(message);
        binding.hintCard.setVisibility(View.VISIBLE);
    }

    private void addMessage(CallMessage message) {
        adapter.add(message);
        binding.messageList.smoothScrollToPosition(adapter.getLastPosition());
    }

    private void updateTimer() {
        long seconds = (SystemClock.elapsedRealtime() - startedAt) / 1000L;
        String elapsed = String.format(Locale.KOREA, "%02d:%02d", seconds / 60, seconds % 60);
        binding.callTimer.setText(getString(R.string.call_ongoing, elapsed));
    }
}
