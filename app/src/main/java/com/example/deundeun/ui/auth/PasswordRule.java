package com.example.deundeun.ui.auth;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.example.deundeun.R;

/**
 * 회원가입 비밀번호 규칙
 */
public final class PasswordRule {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 16;
    private static final String SPECIAL_CHARS = "!@#$%^&*()_+-=[]{}|;:'\",.<>/?`~\\";

    private PasswordRule() {
    }

    /**
     * @return 위반한 규칙의 안내 문구 리소스. 통과하면 {@code null}.
     */
    @Nullable
    @StringRes
    public static Integer validate(String password) {
        if (password == null || password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            return R.string.error_password_length;
        }
        if (!containsLetter(password)) {
            return R.string.error_password_no_letter;
        }
        if (!containsDigit(password)) {
            return R.string.error_password_no_digit;
        }
        if (!containsSpecial(password)) {
            return R.string.error_password_no_special;
        }
        return null;
    }

    private static boolean containsLetter(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsDigit(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isDigit(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsSpecial(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (SPECIAL_CHARS.indexOf(value.charAt(i)) >= 0) {
                return true;
            }
        }
        return false;
    }
}
