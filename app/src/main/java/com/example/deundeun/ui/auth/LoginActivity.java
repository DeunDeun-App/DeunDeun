package com.example.deundeun.ui.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deundeun.MainActivity;
import com.example.deundeun.R;
import com.example.deundeun.data.network.dto.auth.LoginResponse;
import com.example.deundeun.data.repository.AuthRepository;
import com.example.deundeun.data.session.SessionManager;
import com.example.deundeun.databinding.ActivityLoginBinding;
import com.example.deundeun.ui.common.InsetsUtil;
import com.example.deundeun.ui.common.KeyboardUtil;

/** 로그인. 앱의 진입 화면. */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    private final AuthRepository repository = new AuthRepository();

    private SessionManager sessionManager;

    private final ActivityResultLauncher<Intent> findIdLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                Intent data = result.getData();
                String loginId = data == null ? null
                        : data.getStringExtra(FindIdActivity.EXTRA_LOGIN_ID);
                if (result.getResultCode() == RESULT_OK && loginId != null) {
                    binding.inputLoginId.setText(loginId);
                    binding.inputPassword.getEditView().requestFocus();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            goToMain();
            return;
        }

        EdgeToEdge.enable(this);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsUtil.applyVerticalInsets(binding.loginRoot);
        KeyboardUtil.fitContentAboveKeyboard(this);

        binding.btnLogin.setOnClickListener(v -> attemptLogin());
        binding.btnSignup.setOnClickListener(v ->
                startActivity(new Intent(this, SignUpActivity.class)));
        binding.btnFindId.setOnClickListener(v ->
                findIdLauncher.launch(new Intent(this, FindIdActivity.class)));
        binding.btnFindPassword.setOnClickListener(v ->
                startActivity(new Intent(this, FindPasswordActivity.class)));
    }

    private void attemptLogin() {
        KeyboardUtil.hide(this);
        binding.inputLoginId.clearState();
        binding.inputPassword.clearState();

        String loginId = binding.inputLoginId.getText();
        String password = binding.inputPassword.getText();

        if (loginId.isEmpty()) {
            binding.inputLoginId.showError(getString(R.string.error_login_id_required));
            return;
        }
        if (password.isEmpty()) {
            binding.inputPassword.showError(getString(R.string.error_password_required));
            return;
        }

        binding.btnLogin.setEnabled(false);

        repository.login(loginId, password, result -> {
            binding.btnLogin.setEnabled(true);

            LoginResponse data = result.getData();
            if (result.isSuccess() && data != null && data.getToken() != null) {
                String userName = data.getUser() == null ? null : data.getUser().getUserName();
                String savedLoginId = data.getUser() == null ? loginId : data.getUser().getLoginId();
                sessionManager.save(data.getToken(), savedLoginId, userName);
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
