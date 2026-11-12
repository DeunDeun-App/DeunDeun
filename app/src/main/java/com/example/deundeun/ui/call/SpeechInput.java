package com.example.deundeun.ui.call;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.speech.ModelDownloadListener;
import android.speech.RecognitionListener;
import android.speech.RecognitionSupport;
import android.speech.RecognitionSupportCallback;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;

/**
 * 사용자의 말을 글자로 바꾼다. 안드로이드 기본 {@link SpeechRecognizer} 를 감쌌다.
 *
 * 한 번 시작하면 한 문장을 듣고 조용해지면 알아서 멈춘다. {@link #finish()} 로 먼저 끝낼 수도 있다.
 * 말하는 동안 중간 글자와 소리 크기를 넘겨서 화면에 자막과 파동을 그릴 수 있게 한다.
 * 다른 음성 인식 기술로 바꿀 때는 이 클래스 안만 고치면 된다.
 */
public class SpeechInput {

    public interface Listener {

        /** 마이크가 열려 말할 수 있을 때. */
        default void onReady() {
        }

        /** 한국어 음성 데이터를 내려받는 중. 받으면 바로 듣기 시작한다. */
        default void onPreparing() {
        }

        /** 못 알아들어서 한 번 더 듣는 중. */
        default void onListenAgain() {
        }

        /** 말하는 중에 지금까지 알아들은 글자. */
        default void onPartial(String text) {
        }

        /** 소리 크기. 0(조용) ~ 1(큼). */
        default void onLevel(float level) {
        }

        void onResult(String text);

        /** 아무 말도 안 들렸을 때. */
        void onNoSpeech();

        /** 권한, 네트워크 등으로 인식을 못 했을 때. */
        void onError();
    }

    private static final String TAG = "SpeechInput";

    /** 말을 멈춘 뒤 이만큼 조용하면 끝난 것으로 본다. 기본값은 너무 짧아 말이 잘린다. */
    private static final long SILENCE_MS = 1500L;

    // onRmsChanged 는 대략 -2 ~ 10 dB 범위로 들어온다. 조용한 방의 잡음(2 dB 아래)은 0 으로 본다
    private static final float RMS_MIN = 2f;
    private static final float RMS_MAX = 10f;

    private final Context context;

    @Nullable
    private SpeechRecognizer recognizer;

    @Nullable
    private Listener listener;

    /** 최종 결과가 비어 오면 대신 쓰는 마지막 중간 글자. */
    @Nullable
    private String lastPartial;

    /** 인식기가 바쁘거나 꼬였을 때 새로 만들어 한 번만 다시 시도한다. */
    private boolean retried;

    /** 못 알아들어서 이미 한 번 더 들었는지. */
    private boolean heardAgain;
    /** 사용자가 다 말했다고 눌렀는지. 이때는 다시 듣지 않고 결과를 넘긴다. */
    private boolean finishing;

    /** 인식기에 미리 알려줄 말. */
    private ArrayList<String> hints = new ArrayList<>();

    /** 기기 내장 인식기를 쓰는지. 기본 인식기가 한국어를 못 할 때 쓴다. */
    private boolean onDevice;
    /** 쓸 인식기를 이미 골랐는지. */
    private boolean engineChosen;

    public SpeechInput(Context context) {
        this.context = context;
    }

    public static boolean isAvailable(Context context) {
        return SpeechRecognizer.isRecognitionAvailable(context);
    }

    public void start(Listener listener) {
        this.listener = listener;
        lastPartial = null;
        retried = false;
        heardAgain = false;
        finishing = false;

        if (engineChosen || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            listen();
        } else {
            chooseEngineThenListen();
        }
    }

    /**
     * 한국어를 알아듣는 인식기를 고른다. 처음 한 번만 확인하고 이후로는 고른 것을 쓴다.
     *
     * 기본 인식기가 한국어를 못 하면(기기 안에서만 인식하는데 한국어 데이터가 없는 경우)
     * 기기 내장 인식기에 한국어가 있는지 본다. 둘 다 없으면 한국어 데이터를 내려받는다.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private void chooseEngineThenListen() {
        onDevice = false;
        checkSupport(recognizer(), defaultSupport -> {
            if (listener == null) {
                return;
            }
            if (isReady(defaultSupport)) {
                choose(false);
                return;
            }

            if (!SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                downloadOrListen(defaultSupport);
                return;
            }

            // 기기 내장 인식기를 확인한다
            destroyRecognizer();
            onDevice = true;
            checkSupport(recognizer(), deviceSupport -> {
                if (listener == null) {
                    return;
                }
                if (isReady(deviceSupport)) {
                    choose(true);
                } else if (isDownloadable(deviceSupport)) {
                    downloadKorean(recognizer());
                } else {
                    // 기본 인식기로 돌아간다
                    destroyRecognizer();
                    onDevice = false;
                    downloadOrListen(defaultSupport);
                }
            });
        });
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private void downloadOrListen(@Nullable RecognitionSupport support) {
        if (isDownloadable(support)) {
            downloadKorean(recognizer());
        } else {
            // 확인할 수 없는 인식기는 그냥 들어본다
            choose(false);
        }
    }

    private void choose(boolean useOnDevice) {
        Log.d(TAG, "engine " + (useOnDevice ? "on-device" : "default"));
        if (onDevice != useOnDevice) {
            destroyRecognizer();
            onDevice = useOnDevice;
        }
        engineChosen = true;
        listen();
    }

    private interface SupportResult {
        void onResult(@Nullable RecognitionSupport support);
    }

    /** 한국어 지원 여부를 묻는다. 확인할 수 없으면 null 을 넘긴다. */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private void checkSupport(SpeechRecognizer target, SupportResult result) {
        // 인식기에 따라 결과를 두 번 보내거나 결과와 오류를 둘 다 보낸다. 처음 것만 쓴다
        boolean[] handled = {false};

        target.checkRecognitionSupport(buildIntent(), ContextCompat.getMainExecutor(context),
                new RecognitionSupportCallback() {
                    @Override
                    public void onSupportResult(@NonNull RecognitionSupport support) {
                        if (handled[0]) {
                            return;
                        }
                        handled[0] = true;
                        Log.d(TAG, (onDevice ? "on-device" : "default")
                                + " installed=" + support.getInstalledOnDeviceLanguages()
                                + " pending=" + support.getPendingOnDeviceLanguages()
                                + " online=" + support.getOnlineLanguages());
                        result.onResult(support);
                    }

                    @Override
                    public void onError(int error) {
                        if (handled[0]) {
                            return;
                        }
                        handled[0] = true;
                        Log.d(TAG, (onDevice ? "on-device" : "default") + " support error " + error);
                        result.onResult(null);
                    }
                });
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private static boolean isReady(@Nullable RecognitionSupport support) {
        return support != null && (hasKorean(support.getInstalledOnDeviceLanguages())
                || hasKorean(support.getOnlineLanguages()));
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private static boolean isDownloadable(@Nullable RecognitionSupport support) {
        return support != null && (hasKorean(support.getSupportedOnDeviceLanguages())
                || hasKorean(support.getPendingOnDeviceLanguages()));
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private void downloadKorean(SpeechRecognizer current) {
        Intent intent = buildIntent();
        Executor main = ContextCompat.getMainExecutor(context);
        Listener waiting = listener;
        if (waiting != null) {
            waiting.onPreparing();
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // 받는 중인지 알 수 없다. 받기만 시작하고 나중에 다시 누르게 한다
            current.triggerModelDownload(intent);
            Listener ended = takeListener();
            if (ended != null) {
                ended.onError();
            }
            return;
        }

        current.triggerModelDownload(intent, main, new ModelDownloadListener() {
            @Override
            public void onProgress(int completedPercent) {
                Log.d(TAG, "download " + completedPercent + "%");
            }

            @Override
            public void onSuccess() {
                Log.d(TAG, "download success");
                if (listener != null) {
                    choose(onDevice);
                }
            }

            @Override
            public void onScheduled() {
                // 와이파이 연결 등 조건이 맞으면 받는다. 이번에는 못 듣는다
                Log.d(TAG, "download scheduled");
                Listener ended = takeListener();
                if (ended != null) {
                    ended.onError();
                }
            }

            @Override
            public void onError(int error) {
                Log.w(TAG, "download error " + error);
                Listener ended = takeListener();
                if (ended != null) {
                    ended.onError();
                }
            }
        });
    }

    private static boolean hasKorean(@Nullable List<String> languages) {
        if (languages == null) {
            return false;
        }
        for (String language : languages) {
            if (language != null && language.toLowerCase(Locale.ROOT).startsWith("ko")) {
                return true;
            }
        }
        return false;
    }

    private SpeechRecognizer recognizer() {
        if (recognizer == null) {
            recognizer = onDevice && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    ? SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                    : SpeechRecognizer.createSpeechRecognizer(context);
            recognizer.setRecognitionListener(new Callback());
        }
        return recognizer;
    }

    private void listen() {
        recognizer().startListening(buildIntent());
    }

    private Intent buildIntent() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ko-KR");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, SILENCE_MS);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                SILENCE_MS);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hints.isEmpty()) {
            // 이 말들 쪽으로 알아듣게 한다. 예) "불이" 를 "둘이" 로 잘못 듣는 것을 줄인다
            intent.putStringArrayListExtra(RecognizerIntent.EXTRA_BIASING_STRINGS, hints);
        }
        return intent;
    }

    /** 나올 만한 말을 미리 알려준다. 지원하는 인식기만 쓴다. */
    public void setHints(List<String> words) {
        hints = new ArrayList<>(words);
    }

    /** 다 말했을 때. 지금까지 들은 것으로 결과를 받는다. */
    public void finish() {
        if (recognizer != null && listener != null) {
            finishing = true;
            recognizer.stopListening();
        }
    }

    /** 듣는 중이면 결과 없이 멈춘다. */
    public void stop() {
        listener = null;
        if (recognizer != null) {
            recognizer.cancel();
        }
    }

    public void destroy() {
        stop();
        destroyRecognizer();
    }

    private void destroyRecognizer() {
        if (recognizer != null) {
            recognizer.destroy();
            recognizer = null;
        }
    }

    /** 결과는 한 번만 넘긴다. 넘긴 뒤 들어오는 콜백은 무시한다. */
    @Nullable
    private Listener takeListener() {
        Listener current = listener;
        listener = null;
        return current;
    }

    @Nullable
    private static String firstText(@Nullable Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        ArrayList<String> texts = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (texts == null || texts.isEmpty() || texts.get(0) == null) {
            return null;
        }
        String text = texts.get(0).trim();
        return text.isEmpty() ? null : text;
    }

    /**
     * 못 알아들었을 때 한 번만 조용히 다시 듣는다. 사용자가 다 말했다고 누른 뒤에는 다시 듣지 않는다.
     *
     * @return 다시 듣기 시작했으면 true
     */
    private boolean listenAgain() {
        if (finishing || heardAgain || listener == null) {
            return false;
        }
        heardAgain = true;
        Log.d(TAG, "listen again");
        listener.onListenAgain();
        listen();
        return true;
    }

    private class Callback implements RecognitionListener {

        @Override
        public void onResults(Bundle results) {
            if (listener == null) {
                return;
            }

            String text = firstText(results);
            if (text == null) {
                // 인식기에 따라 최종 결과를 비워 보내고 중간 결과에만 글자를 담는다
                text = lastPartial;
            }
            if (text == null && listenAgain()) {
                return;
            }

            Listener current = takeListener();

            if (text == null) {
                current.onNoSpeech();
            } else {
                current.onResult(text);
            }
        }

        @Override
        public void onError(int error) {
            Log.d(TAG, "recognition error " + error);
            if (listener == null) {
                return;
            }

            boolean stuck = error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY
                    || error == SpeechRecognizer.ERROR_CLIENT
                    || error == SpeechRecognizer.ERROR_SERVER_DISCONNECTED;
            if (stuck && !retried && lastPartial == null) {
                // 이전 인식이 덜 끝났거나 연결이 끊겼다. 새로 만들어 한 번 더 듣는다
                retried = true;
                destroyRecognizer();
                listen();
                return;
            }

            boolean notHeard = error == SpeechRecognizer.ERROR_NO_MATCH
                    || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT;
            if (notHeard && lastPartial == null && listenAgain()) {
                return;
            }

            Listener current = takeListener();
            if (lastPartial != null) {
                // 말하는 도중 끊겼어도 알아들은 만큼은 대답으로 쓴다
                current.onResult(lastPartial);
            } else if (notHeard) {
                current.onNoSpeech();
            } else {
                current.onError();
            }
        }

        @Override
        public void onReadyForSpeech(Bundle params) {
            if (listener != null) {
                listener.onReady();
            }
        }

        @Override
        public void onBeginningOfSpeech() {
        }

        @Override
        public void onRmsChanged(float rmsdB) {
            if (listener != null) {
                float level = (rmsdB - RMS_MIN) / (RMS_MAX - RMS_MIN);
                listener.onLevel(Math.max(0f, Math.min(1f, level)));
            }
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
        }

        @Override
        public void onEndOfSpeech() {
            if (listener != null) {
                listener.onLevel(0f);
            }
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            String text = firstText(partialResults);
            if (text == null || listener == null) {
                return;
            }
            lastPartial = text;
            listener.onPartial(text);
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
        }
    }
}
