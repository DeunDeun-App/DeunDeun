package com.example.deundeun.ui.auth;

import android.content.Context;
import android.util.Patterns;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.deundeun.R;
import com.example.deundeun.data.repository.BaseRepository.Callback;
import com.example.deundeun.data.repository.BaseRepository.Result;
import com.example.deundeun.ui.common.LabeledInputView;
import com.google.android.material.button.MaterialButton;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class EmailCodeForm {

    public interface SendRequest {
        void send(String email, Callback<Result<Long>> callback);
    }

    public interface VerifyRequest<T> {
        void verify(String email, String code, Callback<Result<T>> callback);
    }

    private final Context context;
    private final LabeledInputView emailInput;
    private final MaterialButton sendButton;
    private final View codeRow;
    private final LabeledInputView codeInput;
    private final MaterialButton verifyButton;
    private final AuthCodeTimer timer;

    private String sentEmail;
    private String verifiedEmail;

    public EmailCodeForm(Context context, LabeledInputView emailInput, MaterialButton sendButton,
                         View codeRow, LabeledInputView codeInput, MaterialButton verifyButton) {
        this.context = context;
        this.emailInput = emailInput;
        this.sendButton = sendButton;
        this.codeRow = codeRow;
        this.codeInput = codeInput;
        this.verifyButton = verifyButton;
        this.timer = new AuthCodeTimer(context, codeInput);

        emailInput.alignWithField(sendButton);
        codeInput.alignWithField(verifyButton);
    }

    public String getEmail() {
        return emailInput.getText();
    }

    @Nullable
    public String getVerifiedEmail() {
        return verifiedEmail != null && verifiedEmail.equals(getEmail()) ? verifiedEmail : null;
    }

    public boolean isVerified() {
        return getVerifiedEmail() != null;
    }

    public void send(SendRequest request, Predicate<Result<?>> failureHandler) {
        emailInput.clearState();
        String email = getEmail();

        if (email.isEmpty()) {
            emailInput.showError(context.getString(R.string.error_email_required));
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.showError(context.getString(R.string.error_email_format));
            return;
        }

        sendButton.setEnabled(false);
        request.send(email, result -> {
            sendButton.setEnabled(true);

            if (result.isSuccess()) {
                sentEmail = email;
                verifiedEmail = null;
                sendButton.setText(R.string.action_resend_email);
                codeInput.setText("");
                codeInput.clearState();
                codeRow.setVisibility(View.VISIBLE);
                timer.start(result.getData());
                codeInput.getEditView().requestFocus();
            } else if (!failureHandler.test(result)) {
                emailInput.showError(result.getMessageOr(context.getString(R.string.error_network)));
            }
        });
    }

    public <T> void verify(VerifyRequest<T> request, Consumer<T> onVerified,
                           Predicate<Result<?>> failureHandler) {
        if (sentEmail == null) {
            showNotVerified(context.getString(R.string.error_email_not_verified));
            return;
        }
        if (timer.isExpired()) {
            codeInput.showError(context.getString(R.string.error_code_expired));
            return;
        }

        String code = codeInput.getText();
        if (code.isEmpty()) {
            timer.showError(context.getString(R.string.error_code_required));
            return;
        }

        String email = sentEmail;
        verifyButton.setEnabled(false);
        request.verify(email, code, result -> {
            verifyButton.setEnabled(true);

            if (result.isSuccess()) {
                verifiedEmail = email;
                timer.stop();
                codeInput.showSuccess(context.getString(R.string.success_code_verified));
                onVerified.accept(result.getData());
                return;
            }

            verifiedEmail = null;
            if (failureHandler.test(result)) {
                return;
            }

            String message = result.getMessageOr(context.getString(R.string.error_network));
            if (result.hasCode("CODE_EXPIRED") || result.hasCode("CODE_ATTEMPTS")
                    || result.hasCode("CODE_NOT_REQUESTED")) {
                timer.stop();
                codeInput.showError(message);
            } else {
                timer.showError(message);
            }
        });
    }

    public void showNotVerified(String message) {
        if (codeRow.getVisibility() == View.VISIBLE && sentEmail != null) {
            timer.showError(message);
            codeInput.getEditView().requestFocus();
        } else {
            emailInput.showError(message);
            emailInput.getEditView().requestFocus();
        }
    }

    public void reset() {
        verifiedEmail = null;
        timer.stop();
        codeInput.setText("");
        codeInput.clearState();
    }

    public void release() {
        timer.cancel();
    }
}
