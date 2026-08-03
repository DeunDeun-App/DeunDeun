package com.example.deundeun.ui.auth;

import android.util.Patterns;

/**
 * 화면 개발용 임시 인증 소스. 서버 없이 흐름을 끝까지 눌러볼 수 있게 한다.
 *
 * <p>API 스펙이 정해지면 이 클래스를 Retrofit 구현으로 갈아끼운다.
 * 지금은 모두 즉시 반환하지만, 실제로는 콜백이 되므로 화면 쪽은
 * 결과를 콜백에서 받는 형태로 이미 작성해 두었다.
 */
public class FakeAuthRepository {

    /** 개발 중 인증 성공으로 치는 코드. */
    private static final String VALID_CODE = "123456";

    /** 이 이메일만 가입된 것으로 친다. 이메일/비번 찾기의 실패 화면을 보기 위함. */
    private static final String REGISTERED_EMAIL = "abc@email.co.kr";

    public interface Callback<T> {
        void onResult(T result);
    }

    public boolean isEmailFormatValid(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public void sendAuthCode(String email, Callback<Boolean> callback) {
        callback.onResult(isEmailFormatValid(email));
    }

    public void verifyAuthCode(String code, Callback<Boolean> callback) {
        callback.onResult(VALID_CODE.equals(code));
    }

    public void login(String email, String password, Callback<Boolean> callback) {
        callback.onResult(isEmailFormatValid(email) && password.length() >= 8);
    }

    public void signUp(String name, String email, String password, Callback<Boolean> callback) {
        callback.onResult(true);
    }

    /** 이름 + 이메일이 가입 정보와 일치하는지. */
    public void findEmail(String name, String email, Callback<Boolean> callback) {
        callback.onResult(!name.isEmpty() && REGISTERED_EMAIL.equalsIgnoreCase(email));
    }

    public void resetPassword(String email, String newPassword, Callback<Boolean> callback) {
        callback.onResult(true);
    }
}
