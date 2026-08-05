package com.example.deundeun.ui.auth;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deundeun.R;
import com.example.deundeun.databinding.ActivityFindPasswordBinding;
import com.example.deundeun.ui.common.InsetsUtil;

/**
 * 비밀번호 찾기.
 *
 * 1단계 본인 확인 → 2단계 재설정
 */
public class FindPasswordActivity extends AppCompatActivity {

    private static final String STATE_STEP = "step";

    private enum Step {VERIFY, RESET}

    private ActivityFindPasswordBinding binding;
    private final FakeAuthRepository repository = new FakeAuthRepository();

    private Step step = Step.VERIFY;
    private boolean codeVerified;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityFindPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsUtil.applyTopInset(binding.findPasswordScroll);
        InsetsUtil.applyBottomInset(binding.ctaContainer);

        if (savedInstanceState != null) {
            step = Step.values()[savedInstanceState.getInt(STATE_STEP, 0)];
        }

        binding.header.setOnBackClickListener(v -> onBackPressed());
        binding.btnSendEmail.setOnClickListener(v -> sendAuthCode());
        binding.btnVerifyCode.setOnClickListener(v -> verifyAuthCode());
        binding.btnSubmit.setOnClickListener(v -> onSubmit());

        applyStep();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_STEP, step.ordinal());
    }

    @Override
    public void onBackPressed() {
        // 재설정 단계에서 뒤로 가면 화면을 닫지 않고 본인 확인으로 되돌린다.
        if (step == Step.RESET) {
            step = Step.VERIFY;
            applyStep();
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

    private void sendAuthCode() {
        binding.inputEmail.clearState();
        String email = binding.inputEmail.getText();

        if (email.isEmpty()) {
            binding.inputEmail.showError(getString(R.string.error_email_required));
            return;
        }
        if (!repository.isEmailFormatValid(email)) {
            binding.inputEmail.showError(getString(R.string.error_email_format));
            return;
        }

        repository.sendAuthCode(email, sent ->
                binding.inputEmail.showSuccess(getString(R.string.success_email_sent)));
    }

    private void verifyAuthCode() {
        binding.inputAuthCode.clearState();
        String code = binding.inputAuthCode.getText();

        if (code.isEmpty()) {
            binding.inputAuthCode.showError(getString(R.string.error_code_required));
            return;
        }

        repository.verifyAuthCode(code, verified -> {
            codeVerified = verified;
            if (verified) {
                binding.inputAuthCode.showSuccess(getString(R.string.success_code_verified));
            } else {
                binding.inputAuthCode.showError(getString(R.string.error_code_mismatch));
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
        binding.inputAuthCode.clearState();

        if (binding.inputName.isEmpty()) {
            binding.inputName.showError(getString(R.string.error_name_required));
            return;
        }
        if (!codeVerified) {
            binding.inputAuthCode.showError(getString(R.string.error_code_mismatch));
            return;
        }

        step = Step.RESET;
        applyStep();
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

        repository.resetPassword(binding.inputEmail.getText(), password, success -> {
            Toast.makeText(this, R.string.reset_password_done, Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
