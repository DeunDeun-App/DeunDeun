package com.example.deundeun.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.deundeun.MainActivity;
import com.example.deundeun.R;
import com.example.deundeun.databinding.ActivityLoginBinding;
import com.example.deundeun.network.HealthCheckResponse;
import com.example.deundeun.network.RetrofitClient;
import com.example.deundeun.ui.common.InsetsUtil;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** 로그인. 앱의 진입 화면. */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private final FakeAuthRepository repository = new FakeAuthRepository();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // 서버 연결 체크 시작
        RetrofitClient.getApiService().getHealthCheck().enqueue(new Callback<HealthCheckResponse>() {
            @Override
            public void onResponse(Call<HealthCheckResponse> call, Response<HealthCheckResponse> response) {
                if(response.isSuccessful()) {
                    Log.d("SERVER_LOG", "연결 성공: " + response.body().getMessage());
                }
            }

            @Override
            public void onFailure(Call<HealthCheckResponse> call, Throwable t) {
                Log.e("SERVER_LOG", "연결 실패: " + t.getMessage());
            }
        });

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
                binding.inputPassword.showError(getString(R.string.error_login_failed));
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
