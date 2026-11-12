package com.example.deundeun.ui.auth;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deundeun.R;
import com.example.deundeun.data.network.dto.auth.UserResponse;
import com.example.deundeun.data.repository.AuthRepository;
import com.example.deundeun.databinding.ActivitySignUpBinding;
import com.example.deundeun.databinding.DialogCompleteBinding;
import com.example.deundeun.ui.common.AgreementRowView;
import com.example.deundeun.ui.common.DialogHelper;
import com.example.deundeun.ui.common.InsetsUtil;
import com.example.deundeun.ui.common.KeyboardUtil;
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
    private EmailCodeForm emailForm;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsUtil.applyTopInset(binding.signupScroll);
        InsetsUtil.applyBottomInset(binding.ctaContainer);
        KeyboardUtil.fitContentAboveKeyboard(this);

        binding.header.setOnBackClickListener(v -> finish());

        agreements = Arrays.asList(
                binding.agreeAge, binding.agreeTerms,
                binding.agreePrivacy, binding.agreeMarketing);
        setUpAgreements();

        emailForm = new EmailCodeForm(this, binding.inputEmail, binding.btnSendEmail,
                binding.authCodeRow, binding.inputAuthCode, binding.btnVerifyCode);

        binding.btnSendEmail.setOnClickListener(v ->
                emailForm.send(repository::sendSignUpCode, result -> false));
        binding.btnVerifyCode.setOnClickListener(v ->
                emailForm.verify(repository::verifySignUpCode, data -> { }, result -> false));
        binding.btnSubmit.setOnClickListener(v -> attemptSignUp());
    }

    @Override
    protected void onDestroy() {
        emailForm.release();
        super.onDestroy();
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

    // ----- 가입 -----

    private void attemptSignUp() {
        KeyboardUtil.hide(this);
        clearAllStates();

        String name = binding.inputName.getText();
        String loginId = binding.inputLoginId.getText();
        String password = binding.inputPassword.getText();

        if (name.isEmpty()) {
            focusWithError(binding.inputName, R.string.error_name_required);
            return;
        }
        if (loginId.isEmpty()) {
            focusWithError(binding.inputLoginId, R.string.error_login_id_required);
            return;
        }
        if (!repository.isLoginIdFormatValid(loginId)) {
            focusWithError(binding.inputLoginId, R.string.error_login_id_format);
            return;
        }
        if (!emailForm.isVerified()) {
            emailForm.showNotVerified(getString(R.string.error_email_not_verified));
            return;
        }

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
            Toast.makeText(this, R.string.error_agreement_required, Toast.LENGTH_SHORT).show();
            binding.signupScroll.smoothScrollTo(0, binding.agreeAllRow.getTop());
            return;
        }

        binding.btnSubmit.setEnabled(false);
        repository.signUp(loginId, name, emailForm.getVerifiedEmail(), password, result -> {
            binding.btnSubmit.setEnabled(true);

            if (result.isSuccess()) {
                showCompleteDialog(name);
            } else {
                showSignUpError(result);
            }
        });
    }

    private void showSignUpError(AuthRepository.Result<UserResponse> result) {
        String message = result.getMessageOr(getString(R.string.error_network));

        if (result.hasCode("NAME")) {
            binding.inputName.showError(message);
        } else if (result.hasCode("LOGIN_ID")) {
            binding.inputLoginId.showError(message);
            binding.inputLoginId.getEditView().requestFocus();
        } else if (result.hasCode("PASSWORD")) {
            binding.inputPassword.showError(message);
        } else if (result.hasCode("EMAIL_NOT_VERIFIED")) {
            emailForm.showNotVerified(message);
        } else if (result.hasCode("EMAIL")) {
            binding.inputEmail.showError(message);
        } else {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    private void clearAllStates() {
        binding.inputName.clearState();
        binding.inputLoginId.clearState();
        binding.inputPassword.clearState();
        binding.inputPasswordConfirm.clearState();
    }

    private void focusWithError(LabeledInputView input, @StringRes int messageRes) {
        input.showError(getString(messageRes));
        input.getEditView().requestFocus();
    }

    private void showCompleteDialog(String name) {
        DialogCompleteBinding dialogBinding = DialogCompleteBinding.inflate(getLayoutInflater());
        dialogBinding.dialogMessage.setText(getString(R.string.signup_complete_message, name));

        Dialog dialog = DialogHelper.show(this, dialogBinding.getRoot(), 0.78f, false);
        dialogBinding.dialogConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });
    }
}
