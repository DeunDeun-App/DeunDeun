package com.example.deundeun.data.repository;

import android.content.Context;

import androidx.annotation.Nullable;

import com.example.deundeun.data.network.ApiResponse;
import com.example.deundeun.data.network.ApiService;
import com.example.deundeun.data.network.RetrofitClient;
import com.example.deundeun.data.network.dto.call.CallAnswerResponse;
import com.example.deundeun.data.network.dto.call.CallRequest;
import com.example.deundeun.data.network.dto.call.CallResultResponse;
import com.example.deundeun.data.network.dto.call.CallSessionResponse;
import com.example.deundeun.data.session.SessionManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * 119 신고 연습 서버 연동. 로그인 토큰은 {@link SessionManager} 에서 꺼내 붙인다
 *
 * 요청/응답 처리는 {@link AuthRepository} 와 같다. 공통 코드로 합치는 건 팀 합의 후 진행한다.
 */
public class CallRepository {

    public interface Callback<T> {
        void onResult(T result);
    }

    public static class Result<T> {

        private final boolean success;
        private final String code;
        private final String message;
        private final T data;

        private Result(boolean success, @Nullable String code, @Nullable String message,
                       @Nullable T data) {
            this.success = success;
            this.code = code;
            this.message = message;
            this.data = data;
        }

        public boolean isSuccess() { return success; }

        @Nullable
        public String getCode() { return code; }

        public String getMessageOr(String fallback) {
            return message == null || message.isEmpty() ? fallback : message;
        }

        @Nullable
        public T getData() { return data; }
    }

    private static final Type ERROR_TYPE = new TypeToken<ApiResponse<Object>>() {}.getType();

    private final Gson gson = new Gson();
    private final ApiService api = RetrofitClient.getApiService();
    private final SessionManager sessionManager;

    public CallRepository(Context context) {
        this.sessionManager = new SessionManager(context);
    }

    public boolean isLoggedIn() {
        return sessionManager.isLoggedIn();
    }

    public void startSession(Callback<Result<CallSessionResponse>> callback) {
        enqueue(api.startCallSession(sessionManager.getAuthorization()), callback);
    }

    public void answer(long sessionId, long itemId, String answer,
                       Callback<Result<CallAnswerResponse>> callback) {
        enqueue(api.answerCall(sessionManager.getAuthorization(), sessionId,
                new CallRequest(itemId, answer)), callback);
    }

    public void complete(long sessionId, Callback<Result<CallResultResponse>> callback) {
        enqueue(api.completeCall(sessionManager.getAuthorization(), sessionId), callback);
    }

    private <T> void enqueue(Call<ApiResponse<T>> call, Callback<Result<T>> callback) {
        call.enqueue(new retrofit2.Callback<ApiResponse<T>>() {

            @Override
            public void onResponse(Call<ApiResponse<T>> call, Response<ApiResponse<T>> response) {
                ApiResponse<T> body = response.body();

                if (response.isSuccessful() && body != null && body.isSuccess()) {
                    callback.onResult(new Result<>(true, body.getCode(), body.getMessage(),
                            body.getData()));
                    return;
                }

                ApiResponse<Object> error = parseError(response.errorBody());
                callback.onResult(new Result<>(false,
                        error == null ? null : error.getCode(),
                        error == null ? null : error.getMessage(),
                        null));
            }

            @Override
            public void onFailure(Call<ApiResponse<T>> call, Throwable t) {
                callback.onResult(new Result<>(false, null, null, null));
            }
        });
    }

    @Nullable
    private ApiResponse<Object> parseError(@Nullable ResponseBody errorBody) {
        if (errorBody == null) {
            return null;
        }

        try {
            return gson.fromJson(errorBody.charStream(), ERROR_TYPE);

        } catch (Exception e) {
            return null;
        } finally {
            errorBody.close();
        }
    }
}
