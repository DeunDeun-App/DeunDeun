package com.example.deundeun.ui.auth;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.deundeun.R;
import com.example.deundeun.data.repository.AuthRepository;
import com.example.deundeun.data.repository.BaseRepository.Result;
import com.example.deundeun.databinding.ActivityFindIdBinding;
import com.example.deundeun.databinding.DialogCompleteBinding;
import com.example.deundeun.ui.common.DialogHelper;
import com.example.deundeun.ui.common.InsetsUtil;

public class FindIdActivity extends AppCompatActivity {

    public static final String EXTRA_LOGIN_ID = "loginId";

    private ActivityFindIdBinding binding;
    private final AuthRepository repository = new AuthRepository();

    private EmailCodeForm emailForm;
    private String foundLoginId;
    private String foundName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityFindIdBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsUtil.applyTopInset(binding.findIdScroll);
        InsetsUtil.applyBottomInset(binding.ctaContainer);

        emailForm = new EmailCodeForm(this, binding.inputEmail, binding.btnSendEmail,
                binding.authCodeRow, binding.inputAuthCode, binding.btnVerifyCode);

        binding.header.setOnBackClickListener(v -> finish());
        binding.btnSendEmail.setOnClickListener(v -> sendAuthCode());
        binding.btnVerifyCode.setOnClickListener(v -> verifyAuthCode());
        binding.btnSubmit.setOnClickListener(v -> {
            if (foundLoginId != null) {
                showResultDialog();
            } else {
                verifyAuthCode();
            }
        });
    }

    @Override
    protected void onDestroy() {
        emailForm.release();
        super.onDestroy();
    }

    private void sendAuthCode() {
        binding.inputName.clearState();
        String name = binding.inputName.getText();

        if (name.isEmpty()) {
            binding.inputName.showError(getString(R.string.error_name_required));
            return;
        }

        foundLoginId = null;
        emailForm.send((email, callback) -> repository.sendFindLoginIdCode(name, email, callback),
                this::showNameError);
    }

    private void verifyAuthCode() {
        String name = binding.inputName.getText();

        emailForm.<String>verify(
                (email, code, callback) -> repository.verifyFindLoginIdCode(name, email, code, callback),
                loginId -> {
                    foundLoginId = loginId;
                    foundName = name;
                    showResultDialog();
                },
                this::showNameError);
    }

    private boolean showNameError(Result<?> result) {
        if (!result.hasCode("NAME")) {
            return false;
        }
        binding.inputName.showError(result.getMessageOr(getString(R.string.error_network)));
        return true;
    }

    private void showResultDialog() {
        DialogCompleteBinding dialogBinding = DialogCompleteBinding.inflate(getLayoutInflater());

        String message = getString(R.string.find_id_result, foundName, foundLoginId);
        SpannableString styled = new SpannableString(message);
        int start = message.lastIndexOf(foundLoginId);
        if (start >= 0) {
            styled.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.orange_primary)),
                    start, start + foundLoginId.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        dialogBinding.dialogMessage.setText(styled);
        dialogBinding.dialogConfirm.setText(R.string.action_go_login);

        Dialog dialog = DialogHelper.show(this, dialogBinding.getRoot(), 0.78f, true);
        dialogBinding.dialogConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_LOGIN_ID, foundLoginId));
            finish();
        });
    }
}
