package com.example.deundeun.ui.call;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * 상담원 질문을 소리로 읽어준다. 안드로이드 기본 {@link TextToSpeech} 를 감쌌다.
 *
 * 기기에 한국어 음성이 없으면 소리 없이 바로 다 읽은 것으로 처리한다. 말풍선은 그대로 보이므로 연습은 이어진다.
 */
public class OperatorVoice {

    private static final String UTTERANCE_ID = "operator";

    private final Handler handler = new Handler(Looper.getMainLooper());
    // 초기화 콜백 안에서 자기 자신을 써야 해서 final 로 둘 수 없다
    private TextToSpeech tts;

    private boolean initialized;
    private boolean available;

    @Nullable
    private String pendingText;

    @Nullable
    private Runnable onDone;

    public OperatorVoice(Context context) {
        tts = new TextToSpeech(context.getApplicationContext(), status -> {
            initialized = true;
            available = status == TextToSpeech.SUCCESS
                    && tts.setLanguage(Locale.KOREAN) >= TextToSpeech.LANG_AVAILABLE;

            if (pendingText != null) {
                String text = pendingText;
                pendingText = null;
                speakNow(text);
            }
        });
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
            }

            @Override
            public void onDone(String utteranceId) {
                handler.post(OperatorVoice.this::finish);
            }

            @Override
            public void onError(String utteranceId) {
                handler.post(OperatorVoice.this::finish);
            }
        });
    }

    /**
     * 읽기 시작. 다 읽으면 {@code onDone} 을 메인 스레드에서 한 번 부른다.
     */
    public void speak(String text, Runnable onDone) {
        this.onDone = onDone;

        if (!initialized) {
            pendingText = text;
            return;
        }
        speakNow(text);
    }

    /** 읽는 중이면 멈춘다. 이때 {@code onDone} 은 불리지 않는다. */
    public void stop() {
        pendingText = null;
        onDone = null;
        tts.stop();
    }

    public void shutdown() {
        stop();
        tts.shutdown();
    }

    private void speakNow(String text) {
        if (!available) {
            finish();
            return;
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID);
    }

    private void finish() {
        Runnable done = onDone;
        onDone = null;
        if (done != null) {
            done.run();
        }
    }
}
