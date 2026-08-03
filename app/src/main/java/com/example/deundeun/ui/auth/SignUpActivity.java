package com.example.deundeun.ui.auth;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deundeun.R;
import com.example.deundeun.databinding.ActivitySignUpBinding;
import com.example.deundeun.databinding.DialogSignupCompleteBinding;
import com.example.deundeun.ui.common.AgreementRowView;
import com.example.deundeun.ui.common.InsetsUtil;
import com.example.deundeun.ui.common.LabeledInputView;

import java.util.Arrays;
import java.util.List;

/**
 * Figma 91:153 — 회원가입.
 *
 * <p>시안의 상태 변형 5종을 한 화면에서 처리한다.
 * 104:93(이메일 발송) · 127:298/128:411/128:497(비밀번호 규칙별 오류) · 104:210(완료).
 */
public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding binding;
    private final FakeAuthRepository repository = new FakeAuthRepository();

    private List<AgreementRowView> agreements;
    private boolean emailVerified;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsUtil.applyTopInset(binding.signupScroll);
        InsetsUtil.applyBottomInset(binding.ctaContainer);

        binding.header.setOnBackClickListener(v -> finish());

        agreements = Arrays.asList(
                binding.agreeAge, binding.agreeTerms,
                binding.agreePrivacy, binding.agreeMarketing);
        setUpAgreements();

        binding.btnSendEmail.setOnClickListener(v -> sendAuthCode());
        binding.btnVerifyCode.setOnClickListener(v -> verifyAuthCode());
        binding.btnSubmit.setOnClickListener(v -> attemptSignUp());
    }

    // ----- 약관 -----

    private void setUpAgreements() {
        binding.agreeAllRow.setOnClickListener(v -> {
            boolean next = !allAgreed();
            for (AgreementRowView row : agreements) {
                row.setCheckedSilently(next);
            }
            syncAgreeAllBox();
        });

        for (AgreementRowView row : agreements) {
            row.setOnCheckedChangeListener((view, checked) -> syncAgreeAllBox());
        }
        syncAgreeAllBox();
    }

    private boolean allAgreed() {
        for (AgreementRowView row : agreements) {
            if (!row.isChecked()) {
                return false;
            }
        }
        return true;
    }

    private boolean requiredAgreed() {
        for (AgreementRowView row : agreements) {
            if (row.isRequiredAgreement() && !row.isChecked()) {
                return false;
            }
        }
        return true;
    }

    private void syncAgreeAllBox() {
        binding.agreeAllBox.setBackgroundResource(
                allAgreed() ? R.drawable.bg_checkbox_checked : R.drawable.bg_checkbox);
    }

    // ----- 이메일 인증 -----

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

        repository.sendAuthCode(email, sent -> {
            if (sent) {
                binding.inputEmail.showSuccess(getString(R.string.success_email_sent));
                binding.authCodeRow.setVisibility(View.VISIBLE);
            } else {
                binding.inputEmail.showError(getString(R.string.error_email_format));
            }
        });
    }

    private void verifyAuthCode() {
        binding.inputAuthCode.clearState();
        String code = binding.inputAuthCode.getText();

        if (code.isEmpty()) {
            binding.inputAuthCode.showError(getString(R.string.error_code_required));
            return;
        }

        repository.verifyAuthCode(code, verified -> {
            emailVerified = verified;
            if (verified) {
                binding.inputAuthCode.showSuccess(getString(R.string.success_code_verified));
            } else {
                binding.inputAuthCode.showError(getString(R.string.error_code_mismatch));
            }
        });
    }

    // ----- 가입 -----

    private void attemptSignUp() {
        clearAllStates();

        if (binding.inputName.isEmpty()) {
            focusWithError(binding.inputName, R.string.error_name_required);
            return;
        }
        if (!emailVerified) {
            focusWithError(binding.inputAuthCode, R.string.error_code_mismatch);
            binding.authCodeRow.setVisibility(View.VISIBLE);
            return;
        }

        String password = binding.inputPassword.getText();
        Integer passwordError = PasswordRule.validate(password);
        if (passwordError != null) {
            focusWithError(binding.inputPassword, passwordError);
            return;
        }
        if (!password.equals(binding.inputPasswordConfirm.getText())) {
            focusWithError(binding.inputPasswordConfirm, R.string.error_password_mismatch);
            return;
        }
        if (!requiredAgreed()) {
            binding.agreeAllRow.announceForAccessibility(
                    getString(R.string.error_agreement_required));
            binding.signupScroll.smoothScrollTo(0, binding.agreeAllRow.getTop());
            return;
        }

        repository.signUp(binding.inputName.getText(), binding.inputEmail.getText(), password,
                success -> showCompleteDialog(binding.inputName.getText()));
    }

    private void clearAllStates() {
        binding.inputName.clearState();
        binding.inputPassword.clearState();
        binding.inputPasswordConfirm.clearState();
    }

    private void focusWithError(LabeledInputView input, int messageRes) {
        input.showError(getString(messageRes));
        input.getEditView().requestFocus();
    }

    private void showCompleteDialog(String name) {
        DialogSignupCompleteBinding dialogBinding =
                DialogSignupCompleteBinding.inflate(getLayoutInflater());
        dialogBinding.dialogMessage.setText(getString(R.string.signup_complete_message, name));

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(dialogBinding.getRoot());
        dialog.setCancelable(false);

        Window window = dialog.getWindow();
        if (window != null) {
            // 기본 다이얼로그 배경을 지워야 bg_dialog 의 둥근 모서리가 살아난다.
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.78f),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        dialogBinding.dialogConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            // 가입 후에는 로그인 화면으로 되돌린다.
            finish();
        });
        dialog.show();
    }
}
