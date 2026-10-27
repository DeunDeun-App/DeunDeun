package com.example.deundeun.ui.auth;

import android.content.Context;
import android.os.CountDownTimer;
import android.text.Editable;

import com.example.deundeun.R;
import com.example.deundeun.ui.common.LabeledInputView;

public class AuthCodeTimer {

    private static final long DEFAULT_SECONDS = 180;

    private final Context context;
    private final LabeledInputView input;

    private CountDownTimer timer;
    private long remainingSeconds;
    private String message;
    private boolean error;
    private boolean running;
    private boolean expired;

    public AuthCodeTimer(Context context, LabeledInputView input) {
        this.context = context;
        this.input = input;
        input.addTextWatcher(new LabeledInputView.SimpleWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (running && input.getState() == LabeledInputView.State.NORMAL) {
                    message = context.getString(R.string.success_email_sent);
                    error = false;
                    render();
                }
            }
        });
    }

    public void start(Long validSeconds) {
        cancel();
        long seconds = validSeconds == null || validSeconds <= 0 ? DEFAULT_SECONDS : validSeconds;
        message = context.getString(R.string.success_email_sent);
        error = false;
        expired = false;
        running = true;
        remainingSeconds = seconds;
        render();

        timer = new CountDownTimer(seconds * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingSeconds = (millisUntilFinished + 999) / 1000;
                render();
            }

            @Override
            public void onFinish() {
                running = false;
                expired = true;
                input.showError(context.getString(R.string.error_code_expired));
            }
        };
        timer.start();
    }

    public void showSending() {
        cancel();
        running = false;
        expired = false;
        input.showSuccess(context.getString(R.string.auth_code_sending));
    }

    public void showError(String errorMessage) {
        if (!running) {
            input.showError(errorMessage);
            return;
        }
        message = errorMessage;
        error = true;
        render();
    }

    public void stop() {
        cancel();
        running = false;
    }

    public boolean isExpired() {
        return expired;
    }

    public void cancel() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void render() {
        String text = context.getString(R.string.auth_code_remaining,
                message, remainingSeconds / 60, remainingSeconds % 60);
        if (error) {
            input.showError(text);
        } else {
            input.showSuccess(text);
        }
    }
}
