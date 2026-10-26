package com.example.deundeun.ui.auth;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deundeun.R;
import com.example.deundeun.data.repository.AuthRepository;
import com.example.deundeun.databinding.ActivityFindEmailBinding;
import com.example.deundeun.ui.common.InsetsUtil;

/**
 * 이메일 찾기.
 * 확인 결과에 따라 (실패) / (성공) 상태로 갈린다.
 */
public class FindEmailActivity extends AppCompatActivity {

    private ActivityFindEmailBinding binding;
    private final AuthRepository repository = new AuthRepository();

    private boolean confirmed;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityFindEmailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsUtil.applyTopInset(binding.findEmailScroll);
        InsetsUtil.applyBottomInset(binding.ctaContainer);

        binding.header.setOnBackClickListener(v -> finish());
        binding.btnSubmit.setOnClickListener(v -> {
            if (confirmed) {
                // 성공 상태에서는 CTA 가 "로그인하기" 로 바뀐다 (시안 107:374)
                finish();
            } else {
                attemptFind();
            }
        });
    }

    private void attemptFind() {
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

        binding.btnSubmit.setEnabled(false);
        repository.findEmail(name, email, result -> {
            binding.btnSubmit.setEnabled(true);

            if (result.isSuccess()) {
                confirmed = true;
                binding.inputEmail.showSuccess(getString(R.string.find_email_success));
                binding.btnSubmit.setText(R.string.action_go_login);
            } else if (result.hasCode("NAME")) {
                binding.inputName.showError(result.getMessageOr(getString(R.string.error_network)));
            } else {
                binding.inputEmail.showError(result.getMessageOr(getString(R.string.error_network)));
            }
        });
    }
}
