package com.example.deundeun.ui.auth;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.deundeun.R;
import com.example.deundeun.data.repository.AuthRepository;
import com.example.deundeun.databinding.ActivityFindIdBinding;
import com.example.deundeun.databinding.DialogFindIdResultBinding;
import com.example.deundeun.ui.common.InsetsUtil;

public class FindIdActivity extends AppCompatActivity {

    public static final String EXTRA_LOGIN_ID = "loginId";

    private ActivityFindIdBinding binding;
    private final AuthRepository repository = new AuthRepository();

    private AuthCodeTimer codeTimer;
    private String foundLoginId;
    private String foundName;
    private String sentEmail;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityFindIdBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsUtil.applyTopInset(binding.findIdScroll);
        InsetsUtil.applyBottomInset(binding.ctaContainer);

        codeTimer = new AuthCodeTimer(this, binding.inputAuthCode);
        binding.inputEmail.alignWithField(binding.btnSendEmail);
        binding.inputAuthCode.alignWithField(binding.btnVerifyCode);

        binding.header.setOnBackClickListener(v -> finish());
        binding.btnSendEmail.setOnClickListener(v -> sendAuthCode());
        binding.btnVerifyCode.setOnClickListener(v -> verifyAuthCode());
        binding.btnSubmit.setOnClickListener(v -> onSubmit());
    }

    @Override
    protected void onDestroy() {
        codeTimer.cancel();
        super.onDestroy();
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

        binding.btnSendEmail.setEnabled(false);
        repository.sendFindLoginIdCode(name, email, result -> {
            binding.btnSendEmail.setEnabled(true);

            if (result.isSuccess()) {
                foundLoginId = null;
                sentEmail = email;
                binding.btnSendEmail.setText(R.string.action_resend_email);
                binding.inputAuthCode.setText("");
                binding.inputAuthCode.clearState();
                binding.authCodeRow.setVisibility(View.VISIBLE);
                codeTimer.start(result.getData());
                binding.inputAuthCode.getEditView().requestFocus();
                return;
            }

            String message = result.getMessageOr(getString(R.string.error_network));
            if (result.hasCode("NAME")) {
                binding.inputName.showError(message);
            } else {
                binding.inputEmail.showError(message);
            }
        });
    }

    private void verifyAuthCode() {
        String code = binding.inputAuthCode.getText();

        if (binding.authCodeRow.getVisibility() != View.VISIBLE) {
            binding.inputEmail.showError(getString(R.string.error_email_not_verified));
            return;
        }
        if (codeTimer.isExpired()) {
            binding.inputAuthCode.showError(getString(R.string.error_code_expired));
            return;
        }
        if (code.isEmpty()) {
            codeTimer.showError(getString(R.string.error_code_required));
            return;
        }

        String name = binding.inputName.getText();
        String email = sentEmail == null ? binding.inputEmail.getText() : sentEmail;

        binding.btnVerifyCode.setEnabled(false);
        binding.btnSubmit.setEnabled(false);
        repository.verifyFindLoginIdCode(name, email, code, result -> {
            binding.btnVerifyCode.setEnabled(true);
            binding.btnSubmit.setEnabled(true);

            if (result.isSuccess() && result.getData() != null) {
                codeTimer.stop();
                foundLoginId = result.getData();
                foundName = name;
                binding.inputAuthCode.showSuccess(getString(R.string.success_code_verified));
                showResultDialog();
                return;
            }

            String message = result.getMessageOr(getString(R.string.error_network));
            if (result.hasCode("NAME")) {
                binding.inputName.showError(message);
            } else if (result.hasCode("CODE_EXPIRED") || result.hasCode("CODE_ATTEMPTS")
                    || result.hasCode("CODE_NOT_REQUESTED")) {
                codeTimer.stop();
                binding.inputAuthCode.showError(message);
            } else {
                codeTimer.showError(message);
            }
        });
    }

    private void onSubmit() {
        if (foundLoginId != null) {
            showResultDialog();
            return;
        }
        verifyAuthCode();
    }

    private void showResultDialog() {
        DialogFindIdResultBinding dialogBinding =
                DialogFindIdResultBinding.inflate(getLayoutInflater());

        String message = getString(R.string.find_id_result, foundName, foundLoginId);
        SpannableString styled = new SpannableString(message);
        int start = message.lastIndexOf(foundLoginId);
        if (start >= 0) {
            styled.setSpan(new ForegroundColorSpan(
                            ContextCompat.getColor(this, R.color.orange_primary)),
                    start, start + foundLoginId.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        dialogBinding.dialogMessage.setText(styled);

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(dialogBinding.getRoot());

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.78f),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        dialogBinding.dialogConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_LOGIN_ID, foundLoginId));
            finish();
        });
        dialog.show();
    }
}
