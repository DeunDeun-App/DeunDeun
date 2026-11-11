package com.example.deundeun.data.repository;

import com.example.deundeun.data.network.dto.auth.AuthRequest;
import com.example.deundeun.data.network.dto.auth.LoginResponse;
import com.example.deundeun.data.network.dto.auth.UserResponse;

import java.util.regex.Pattern;

public class AuthRepository extends BaseRepository {

    private static final Pattern LOGIN_ID_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9]{3,19}$");

    public boolean isLoginIdFormatValid(String loginId) {
        return loginId != null && LOGIN_ID_PATTERN.matcher(loginId).matches();
    }

    public void login(String loginId, String password, Callback<Result<LoginResponse>> callback) {
        enqueue(api.login(new AuthRequest(loginId, null, null, password, null, null)), callback);
    }

    public void me(String authorization, Callback<Result<UserResponse>> callback) {
        enqueue(api.me(authorization), callback);
    }

    public void sendSignUpCode(String email, Callback<Result<Long>> callback) {
        enqueue(api.sendSignUpCode(new AuthRequest(null, null, email, null, null, null)), callback);
    }

    public void verifySignUpCode(String email, String code, Callback<Result<Void>> callback) {
        enqueue(api.verifySignUpCode(new AuthRequest(null, null, email, null, code, null)),
                callback);
    }

    public void signUp(String loginId, String name, String email, String password,
                       Callback<Result<UserResponse>> callback) {
        enqueue(api.signUp(new AuthRequest(loginId, name, email, password, null, null)), callback);
    }

    public void sendFindLoginIdCode(String name, String email, Callback<Result<Long>> callback) {
        enqueue(api.sendFindLoginIdCode(new AuthRequest(null, name, email, null, null, null)),
                callback);
    }

    public void verifyFindLoginIdCode(String name, String email, String code,
                                      Callback<Result<String>> callback) {
        enqueue(api.verifyFindLoginIdCode(new AuthRequest(null, name, email, null, code, null)),
                callback);
    }

    public void sendPasswordResetCode(String loginId, String email,
                                      Callback<Result<Long>> callback) {
        enqueue(api.sendPasswordResetCode(new AuthRequest(loginId, null, email, null, null, null)),
                callback);
    }

    public void verifyPasswordResetCode(String email, String code, Callback<Result<Void>> callback) {
        enqueue(api.verifyPasswordResetCode(new AuthRequest(null, null, email, null, code, null)),
                callback);
    }

    public void issueResetToken(String loginId, String email, Callback<Result<String>> callback) {
        enqueue(api.issueResetToken(new AuthRequest(loginId, null, email, null, null, null)),
                callback);
    }

    public void resetPassword(String resetToken, String newPassword,
                              Callback<Result<Void>> callback) {
        enqueue(api.resetPassword(new AuthRequest(null, null, null, newPassword, null, resetToken)),
                callback);
    }
}
