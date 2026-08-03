package com.example.deundeun.ui.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deundeun.MainActivity;
import com.example.deundeun.R;
import com.example.deundeun.databinding.ActivityLoginBinding;
import com.example.deundeun.ui.common.InsetsUtil;

/** Figma 88:54 — 로그인. 앱의 진입 화면. */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private final FakeAuthRepository repository = new FakeAuthRepository();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsUtil.applyVerticalInsets(binding.loginRoot);

        binding.btnLogin.setOnClickListener(v -> attemptLogin());
        binding.btnSignup.setOnClickListener(v ->
                startActivity(new Intent(this, SignUpActivity.class)));
        binding.btnFindEmail.setOnClickListener(v ->
                startActivity(new Intent(this, FindEmailActivity.class)));
        binding.btnFindPassword.setOnClickListener(v ->
                startActivity(new Intent(this, FindPasswordActivity.class)));
    }

    private void attemptLogin() {
        binding.inputEmail.clearState();
        binding.inputPassword.clearState();

        String email = binding.inputEmail.getText();
        String password = binding.inputPassword.getText();

        if (email.isEmpty()) {
            binding.inputEmail.showError(getString(R.string.error_email_required));
            return;
        }
        if (!repository.isEmailFormatValid(email)) {
            binding.inputEmail.showError(getString(R.string.error_email_format));
            return;
        }
        if (password.isEmpty()) {
            binding.inputPassword.showError(getString(R.string.error_password_length));
            return;
        }

        repository.login(email, password, success -> {
            if (success) {
                goToMain();
            } else {
                // 어느 쪽이 틀렸는지 알려주지 않는다. 계정 존재 여부가 새 나가면 안 된다.
                binding.inputPassword.showError(getString(R.string.error_login_failed));
            }
        });
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        // 뒤로 눌러 로그인 화면으로 되돌아오지 못하게 스택을 비운다.
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
