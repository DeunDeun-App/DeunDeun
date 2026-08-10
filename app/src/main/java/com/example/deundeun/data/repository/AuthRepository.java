package com.example.deundeun.data.repository;

import android.util.Patterns;

import androidx.annotation.Nullable;

import com.example.deundeun.data.network.ApiResponse;
import com.example.deundeun.data.network.dto.auth.LoginRequest;
import com.example.deundeun.data.network.dto.auth.LoginResponse;
import com.example.deundeun.data.network.RetrofitClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

public class AuthRepository {
    public interface Callback<T> {
        void onResult(T result);
    }

    /** 로그인 결과. 성공 여부와 함께 화면에 띄울 문구를 담는다. */
    public static class LoginResult {

        private final boolean success;
        private final String message;
        private final LoginResponse user;

        private LoginResult(boolean success, String message, @Nullable LoginResponse user) {
            this.success = success;
            this.message = message;
            this.user = user;
        }

        public boolean isSuccess() { return success; }

        /** 실패 사유. 서버가 준 문구를 그대로 쓸 수 있다. */
        public String getMessage() { return message; }

        @Nullable
        public LoginResponse getUser() { return user; }
    }

    private final Gson gson = new Gson();

    public boolean isEmailFormatValid(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public void login(String email, String password, String fallbackMessage,
                      Callback<LoginResult> callback) {

        RetrofitClient.getApiService()
                .login(new LoginRequest(email, password))
                .enqueue(new retrofit2.Callback<ApiResponse<LoginResponse>>() {

                    @Override
                    public void onResponse(Call<ApiResponse<LoginResponse>> call,
                                           Response<ApiResponse<LoginResponse>> response) {

                        ApiResponse<LoginResponse> body = response.body();

                        if (response.isSuccessful() && body != null && body.isSuccess()) {
                            callback.onResult(new LoginResult(true, body.getMessage(), body.getData()));
                            return;
                        }

                        String message = parseErrorMessage(response);
                        callback.onResult(new LoginResult(
                                false, message != null ? message : fallbackMessage, null));
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<LoginResponse>> call, Throwable t) {
                        callback.onResult(new LoginResult(false, fallbackMessage, null));
                    }
                });
    }

    @Nullable
    private String parseErrorMessage(Response<ApiResponse<LoginResponse>> response) {
        ResponseBody errorBody = response.errorBody();
        if (errorBody == null) {
            return null;
        }

        try {
            Type type = new TypeToken<ApiResponse<LoginResponse>>() {}.getType();
            ApiResponse<LoginResponse> parsed = gson.fromJson(errorBody.charStream(), type);

            return parsed == null ? null : parsed.getMessage();

        } catch (Exception e) {
            return null;
        } finally {
            errorBody.close();
        }
    }


}
