package com.example.deundeun.ui.auth;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deundeun.R;
import com.example.deundeun.data.network.dto.auth.SignUpResponse;
import com.example.deundeun.data.repository.AuthRepository;
import com.example.deundeun.databinding.ActivitySignUpBinding;
import com.example.deundeun.databinding.DialogSignupCompleteBinding;
import com.example.deundeun.ui.common.AgreementRowView;
import com.example.deundeun.ui.common.InsetsUtil;
import com.example.deundeun.ui.common.LabeledInputView;

import java.util.Arrays;
import java.util.List;

/**
 * 회원가입
 */
public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding binding;
    private final AuthRepository repository = new AuthRepository();

    private List<AgreementRowView> agreements;
    private boolean emailVerified;
    private String verifiedEmail;

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

        binding.btnSendEmail.setEnabled(false);
        repository.sendSignUpCode(email, result -> {
            binding.btnSendEmail.setEnabled(true);

            if (result.isSuccess()) {
                emailVerified = false;
                verifiedEmail = null;
                binding.inputEmail.showSuccess(getString(R.string.success_email_sent));
                binding.authCodeRow.setVisibility(View.VISIBLE);
                binding.inputAuthCode.getEditView().requestFocus();
            } else {
                binding.inputEmail.showError(result.getMessageOr(getString(R.string.error_network)));
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

        String email = binding.inputEmail.getText();

        binding.btnVerifyCode.setEnabled(false);
        repository.verifySignUpCode(email, code, result -> {
            binding.btnVerifyCode.setEnabled(true);
            emailVerified = result.isSuccess();
            verifiedEmail = emailVerified ? email : null;

            if (emailVerified) {
                binding.inputAuthCode.showSuccess(getString(R.string.success_code_verified));
            } else {
                binding.inputAuthCode.showError(
                        result.getMessageOr(getString(R.string.error_network)));
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
        if (!emailVerified || !binding.inputEmail.getText().equals(verifiedEmail)) {
            binding.authCodeRow.setVisibility(View.VISIBLE);
            focusWithError(binding.inputAuthCode, R.string.error_email_not_verified);
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

        String name = binding.inputName.getText();

        binding.btnSubmit.setEnabled(false);
        repository.signUp(name, binding.inputEmail.getText(), password, result -> {
            binding.btnSubmit.setEnabled(true);

            if (result.isSuccess()) {
                showCompleteDialog(name);
            } else {
                showSignUpError(result);
            }
        });
    }

    private void showSignUpError(AuthRepository.Result<SignUpResponse> result) {
        String message = result.getMessageOr(getString(R.string.error_network));

        if (result.hasCode("NAME")) {
            binding.inputName.showError(message);
        } else if (result.hasCode("PASSWORD")) {
            binding.inputPassword.showError(message);
        } else if (result.hasCode("EMAIL_NOT_VERIFIED")) {
            emailVerified = false;
            binding.authCodeRow.setVisibility(View.VISIBLE);
            binding.inputAuthCode.showError(message);
        } else if (result.hasCode("EMAIL")) {
            binding.inputEmail.showError(message);
        } else {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
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
