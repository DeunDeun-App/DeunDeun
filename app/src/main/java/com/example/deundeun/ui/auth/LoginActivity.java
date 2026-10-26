package com.example.deundeun.ui.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deundeun.MainActivity;
import com.example.deundeun.R;
import com.example.deundeun.data.repository.AuthRepository;
import com.example.deundeun.databinding.ActivityLoginBinding;
import com.example.deundeun.ui.common.InsetsUtil;

/** 로그인. 앱의 진입 화면. */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    private final AuthRepository repository = new AuthRepository();

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

        binding.btnLogin.setEnabled(false);

        repository.login(email, password, result -> {
            binding.btnLogin.setEnabled(true);

            if (result.isSuccess()) {
                goToMain();
            } else {
                binding.inputPassword.showError(
                        result.getMessageOr(getString(R.string.error_network)));
            }
        });
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
