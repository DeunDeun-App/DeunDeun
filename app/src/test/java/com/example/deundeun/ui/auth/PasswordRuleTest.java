package com.example.deundeun.ui.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.example.deundeun.R;

import org.junit.Test;

/**
 * 시안(127:298 / 128:411 / 128:497)이 위반 종류별로 다른 문구를 보여주므로,
 * "무엇을 어겼는지"까지 맞는지 확인한다.
 */
public class PasswordRuleTest {

    @Test
    public void 영문_숫자_특수문자를_모두_갖추면_통과한다() {
        assertNull(PasswordRule.validate("deundeun1234!"));
    }

    @Test
    public void 특수문자가_없으면_특수문자_문구를_돌려준다() {
        assertEquals(Integer.valueOf(R.string.error_password_no_special),
                PasswordRule.validate("deundeun1234"));
    }

    @Test
    public void 숫자가_없으면_숫자_문구를_돌려준다() {
        assertEquals(Integer.valueOf(R.string.error_password_no_digit),
                PasswordRule.validate("deundeun!!"));
    }

    @Test
    public void 영문이_없으면_영문_문구를_돌려준다() {
        assertEquals(Integer.valueOf(R.string.error_password_no_letter),
                PasswordRule.validate("12345678!"));
    }

    @Test
    public void 여덟자_미만이면_길이_문구가_다른_규칙보다_먼저다() {
        assertEquals(Integer.valueOf(R.string.error_password_length),
                PasswordRule.validate("de1!"));
    }

    @Test
    public void 열여섯자를_넘으면_길이_문구를_돌려준다() {
        assertEquals(Integer.valueOf(R.string.error_password_length),
                PasswordRule.validate("deundeun12345678!"));
    }

    @Test
    public void 경계값인_여덟자와_열여섯자는_통과한다() {
        assertNull(PasswordRule.validate("deun123!"));
        assertNull(PasswordRule.validate("deundeun1234567!"));
    }

    @Test
    public void null_은_길이_위반으로_본다() {
        assertEquals(Integer.valueOf(R.string.error_password_length),
                PasswordRule.validate(null));
    }
}
