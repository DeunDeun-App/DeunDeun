package com.example.deundeun.ui.auth;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deundeun.R;
import com.example.deundeun.data.repository.AuthRepository;
import com.example.deundeun.databinding.ActivityFindPasswordBinding;
import com.example.deundeun.ui.common.InsetsUtil;

/**
 * 비밀번호 찾기.
 *
 * 1단계 본인 확인 → 2단계 재설정
 */
public class FindPasswordActivity extends AppCompatActivity {

    private static final String STATE_STEP = "step";
    private static final String STATE_RESET_TOKEN = "resetToken";

    private enum Step {VERIFY, RESET}

    private ActivityFindPasswordBinding binding;
    private final AuthRepository repository = new AuthRepository();

    private Step step = Step.VERIFY;
    private boolean codeVerified;
    private String verifiedEmail;
    private String resetToken;
    private AuthCodeTimer codeTimer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityFindPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsUtil.applyTopInset(binding.findPasswordScroll);
        InsetsUtil.applyBottomInset(binding.ctaContainer);

        codeTimer = new AuthCodeTimer(this, binding.inputAuthCode);
        binding.inputEmail.alignWithField(binding.btnSendEmail);
        binding.inputAuthCode.alignWithField(binding.btnVerifyCode);

        if (savedInstanceState != null) {
            resetToken = savedInstanceState.getString(STATE_RESET_TOKEN);
            step = resetToken == null
                    ? Step.VERIFY
                    : Step.values()[savedInstanceState.getInt(STATE_STEP, 0)];
        }

        binding.header.setOnBackClickListener(v -> onBackPressed());
        binding.btnSendEmail.setOnClickListener(v -> sendAuthCode());
        binding.btnVerifyCode.setOnClickListener(v -> verifyAuthCode());
        binding.btnSubmit.setOnClickListener(v -> onSubmit());

        applyStep();
    }

    @Override
    protected void onDestroy() {
        codeTimer.cancel();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_STEP, step.ordinal());
        outState.putString(STATE_RESET_TOKEN, resetToken);
    }

    @Override
    public void onBackPressed() {
        // 재설정 단계에서 뒤로 가면 화면을 닫지 않고 본인 확인으로 되돌린다.
        if (step == Step.RESET) {
            backToVerifyStep();
            return;
        }
        super.onBackPressed();
    }

    private void applyStep() {
        boolean verifying = step == Step.VERIFY;
        binding.stepVerify.setVisibility(verifying ? View.VISIBLE : View.GONE);
        binding.stepReset.setVisibility(verifying ? View.GONE : View.VISIBLE);
        binding.btnSubmit.setText(verifying ? R.string.action_next : R.string.action_done);
    }

    private void backToVerifyStep() {
        step = Step.VERIFY;
        resetToken = null;
        codeVerified = false;
        verifiedEmail = null;
        codeTimer.stop();
        binding.inputAuthCode.setText("");
        binding.inputAuthCode.clearState();
        applyStep();
    }

    private void sendAuthCode() {
        binding.inputName.clearState();
        binding.inputEmail.clearState();
        String name = binding.inputName.getText();
        String email = binding.inputEmail.getText();

        if (name.isEmpty()) {
            binding.inputName.showError(getString(R.string.error_name_required));
            return;
        }
        if (email.isEmpty()) {
            binding.inputEmail.showError(getString(R.string.error_email_required));
            return;
        }
        if (!repository.isEmailFormatValid(email)) {
            binding.inputEmail.showError(getString(R.string.error_email_format));
            return;
        }

        boolean codeRowShown = binding.authCodeRow.getVisibility() == View.VISIBLE;
        binding.inputAuthCode.setText("");
        binding.authCodeRow.setVisibility(View.VISIBLE);
        codeTimer.showSending();

        binding.btnSendEmail.setEnabled(false);
        repository.sendPasswordResetCode(name, email, result -> {
            binding.btnSendEmail.setEnabled(true);

            if (result.isSuccess()) {
                codeVerified = false;
                verifiedEmail = null;
                binding.btnSendEmail.setText(R.string.action_resend_email);
                codeTimer.start(result.getData());
                binding.inputAuthCode.getEditView().requestFocus();
                return;
            }

            restoreCodeRow(codeRowShown);
            if (result.hasCode("NAME")) {
                binding.inputName.showError(result.getMessageOr(getString(R.string.error_network)));
            } else {
                binding.inputEmail.showError(result.getMessageOr(getString(R.string.error_network)));
            }
        });
    }

    private void restoreCodeRow(boolean shown) {
        codeTimer.stop();
        binding.inputAuthCode.clearState();
        if (!shown) {
            binding.authCodeRow.setVisibility(View.GONE);
        }
    }

    private void verifyAuthCode() {
        String code = binding.inputAuthCode.getText();

        if (codeTimer.isExpired()) {
            binding.inputAuthCode.showError(getString(R.string.error_code_expired));
            return;
        }
        if (code.isEmpty()) {
            codeTimer.showError(getString(R.string.error_code_required));
            return;
        }

        String email = binding.inputEmail.getText();

        binding.btnVerifyCode.setEnabled(false);
        repository.verifyPasswordResetCode(email, code, result -> {
            binding.btnVerifyCode.setEnabled(true);
            codeVerified = result.isSuccess();
            verifiedEmail = codeVerified ? email : null;

            if (codeVerified) {
                codeTimer.stop();
                binding.inputAuthCode.showSuccess(getString(R.string.success_code_verified));
                return;
            }

            String message = result.getMessageOr(getString(R.string.error_network));
            if (result.hasCode("CODE_EXPIRED") || result.hasCode("CODE_ATTEMPTS")) {
                codeTimer.stop();
                binding.inputAuthCode.showError(message);
            } else {
                codeTimer.showError(message);
            }
        });
    }

    private void onSubmit() {
        if (step == Step.VERIFY) {
            goToResetStep();
        } else {
            submitNewPassword();
        }
    }

    private void goToResetStep() {
        binding.inputName.clearState();

        if (binding.inputName.isEmpty()) {
            binding.inputName.showError(getString(R.string.error_name_required));
            return;
        }
        if (!codeVerified || !binding.inputEmail.getText().equals(verifiedEmail)) {
            codeTimer.showError(getString(R.string.error_email_not_verified));
            return;
        }

        binding.btnSubmit.setEnabled(false);
        repository.issueResetToken(binding.inputName.getText(), verifiedEmail, result -> {
            binding.btnSubmit.setEnabled(true);
            if (result.isSuccess() && result.getData() != null) {
                resetToken = result.getData();
                step = Step.RESET;
                applyStep();
                binding.inputNewPassword.getEditView().requestFocus();
                return;
            }

            String message = result.getMessageOr(getString(R.string.error_network));
            if (result.hasCode("EMAIL_NOT_VERIFIED")) {
                codeVerified = false;
                codeTimer.showError(message);
            } else if (result.hasCode("EMAIL")) {
                binding.inputEmail.showError(message);
            } else {
                binding.inputName.showError(message);
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
            binding.inputNewPasswordConfirm.showError(
                    getString(R.string.error_password_mismatch));
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
