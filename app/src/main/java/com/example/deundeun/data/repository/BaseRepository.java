package com.example.deundeun.data.repository;

import androidx.annotation.Nullable;

import com.example.deundeun.data.network.ApiResponse;
import com.example.deundeun.data.network.ApiService;
import com.example.deundeun.data.network.RetrofitClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

public abstract class BaseRepository {

    public interface Callback<T> {
        void onResult(T result);
    }

    public static class Result<T> {

        private final boolean success;
        private final String code;
        private final String message;
        private final T data;

        Result(boolean success, @Nullable String code, @Nullable String message,
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

        public boolean hasCode(String prefix) {
            return code != null && code.startsWith(prefix);
        }
    }

    private static final Type ERROR_TYPE = new TypeToken<ApiResponse<Object>>() {}.getType();

    private final Gson gson = new Gson();
    protected final ApiService api = RetrofitClient.getApiService();

    protected <T> void enqueue(Call<ApiResponse<T>> call, Callback<Result<T>> callback) {
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
