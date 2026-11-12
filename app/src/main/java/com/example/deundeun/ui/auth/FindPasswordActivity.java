package com.example.deundeun.ui.auth;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deundeun.R;
import com.example.deundeun.data.repository.AuthRepository;
import com.example.deundeun.data.repository.BaseRepository.Result;
import com.example.deundeun.databinding.ActivityFindPasswordBinding;
import com.example.deundeun.ui.common.InsetsUtil;
import com.example.deundeun.ui.common.KeyboardUtil;

/**
 * 비밀번호 찾기.
 *
 * 1단계 본인 확인 → 2단계 재설정
 */
public class FindPasswordActivity extends AppCompatActivity {

    private static final String STATE_RESET_TOKEN = "resetToken";

    private ActivityFindPasswordBinding binding;
    private final AuthRepository repository = new AuthRepository();

    private EmailCodeForm emailForm;
    private String resetToken;

    private final OnBackPressedCallback backToVerify = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            backToVerifyStep();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityFindPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsUtil.applyTopInset(binding.findPasswordScroll);
        InsetsUtil.applyBottomInset(binding.ctaContainer);
        KeyboardUtil.fitContentAboveKeyboard(this);

        emailForm = new EmailCodeForm(this, binding.inputEmail, binding.btnSendEmail,
                binding.authCodeRow, binding.inputAuthCode, binding.btnVerifyCode);

        if (savedInstanceState != null) {
            resetToken = savedInstanceState.getString(STATE_RESET_TOKEN);
        }

        getOnBackPressedDispatcher().addCallback(this, backToVerify);
        binding.header.setOnBackClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.btnSendEmail.setOnClickListener(v -> sendAuthCode());
        binding.btnVerifyCode.setOnClickListener(v ->
                emailForm.verify(repository::verifyPasswordResetCode, data -> { }, result -> false));
        binding.btnSubmit.setOnClickListener(v -> {
            KeyboardUtil.hide(this);
            if (isResetStep()) {
                submitNewPassword();
            } else {
                goToResetStep();
            }
        });

        applyStep();
    }

    @Override
    protected void onDestroy() {
        emailForm.release();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_RESET_TOKEN, resetToken);
    }

    private boolean isResetStep() {
        return resetToken != null;
    }

    private void applyStep() {
        boolean reset = isResetStep();
        binding.stepVerify.setVisibility(reset ? View.GONE : View.VISIBLE);
        binding.stepReset.setVisibility(reset ? View.VISIBLE : View.GONE);
        binding.btnSubmit.setText(reset ? R.string.action_done : R.string.action_next);
        backToVerify.setEnabled(reset);
    }

    private void backToVerifyStep() {
        resetToken = null;
        emailForm.reset();
        applyStep();
    }

    private void sendAuthCode() {
        binding.inputLoginId.clearState();
        String loginId = binding.inputLoginId.getText();

        if (loginId.isEmpty()) {
            binding.inputLoginId.showError(getString(R.string.error_login_id_required));
            return;
        }
        if (!repository.isLoginIdFormatValid(loginId)) {
            binding.inputLoginId.showError(getString(R.string.error_login_id_format));
            return;
        }

        emailForm.send((email, callback) -> repository.sendPasswordResetCode(loginId, email, callback),
                this::showLoginIdError);
    }

    private boolean showLoginIdError(Result<?> result) {
        if (!result.hasCode("LOGIN_ID")) {
            return false;
        }
        binding.inputLoginId.showError(result.getMessageOr(getString(R.string.error_network)));
        return true;
    }

    private void goToResetStep() {
        binding.inputLoginId.clearState();

        if (binding.inputLoginId.isEmpty()) {
            binding.inputLoginId.showError(getString(R.string.error_login_id_required));
            return;
        }
        if (!emailForm.isVerified()) {
            emailForm.showNotVerified(getString(R.string.error_email_not_verified));
            return;
        }

        binding.btnSubmit.setEnabled(false);
        repository.issueResetToken(binding.inputLoginId.getText(), emailForm.getVerifiedEmail(), result -> {
            binding.btnSubmit.setEnabled(true);
            if (result.isSuccess() && result.getData() != null) {
                resetToken = result.getData();
                applyStep();
                binding.inputNewPassword.getEditView().requestFocus();
                return;
            }

            String message = result.getMessageOr(getString(R.string.error_network));
            if (result.hasCode("EMAIL_NOT_VERIFIED")) {
                emailForm.showNotVerified(message);
            } else if (result.hasCode("EMAIL")) {
                binding.inputEmail.showError(message);
            } else if (!showLoginIdError(result)) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void submitNewPassword() {
        binding.inputNewPassword.clearState();
        binding.inputNewPasswordConfirm.clearState();

        String password = binding.inputNewPassword.getText();
        Integer passwordError = PasswordRule.validate(password);
        if (passwordError != null) {
            binding.inputNewPassword.showError(getString(passwordError));
            return;
        }
        if (!password.equals(binding.inputNewPasswordConfirm.getText())) {
            binding.inputNewPasswordConfirm.showError(getString(R.string.error_password_mismatch));
            return;
        }

        binding.btnSubmit.setEnabled(false);
        repository.resetPassword(resetToken, password, result -> {
            binding.btnSubmit.setEnabled(true);
            String message = result.getMessageOr(getString(R.string.error_network));

            if (result.isSuccess()) {
                Toast.makeText(this, R.string.reset_password_done, Toast.LENGTH_SHORT).show();
                finish();
            } else if (result.hasCode("PASSWORD")) {
                binding.inputNewPassword.showError(message);
            } else if (result.hasCode("RESET_TOKEN")) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                backToVerifyStep();
            } else {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
