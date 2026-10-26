package com.example.deundeun.data.network;

import com.example.deundeun.data.network.dto.auth.EmailCodeRequest;
import com.example.deundeun.data.network.dto.auth.EmailVerifyRequest;
import com.example.deundeun.data.network.dto.auth.FindEmailRequest;
import com.example.deundeun.data.network.dto.auth.LoginRequest;
import com.example.deundeun.data.network.dto.auth.LoginResponse;
import com.example.deundeun.data.network.dto.auth.PasswordResetRequest;
import com.example.deundeun.data.network.dto.auth.ResetTokenRequest;
import com.example.deundeun.data.network.dto.auth.ResetTokenResponse;
import com.example.deundeun.data.network.dto.auth.SignUpRequest;
import com.example.deundeun.data.network.dto.auth.SignUpResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {

    @POST("api/auth/login")
    Call<ApiResponse<LoginResponse>> login(@Body LoginRequest request);

    @POST("api/auth/signup/email-code")
    Call<ApiResponse<Void>> sendSignUpCode(@Body EmailCodeRequest request);

    @POST("api/auth/signup/email-verify")
    Call<ApiResponse<Void>> verifySignUpCode(@Body EmailVerifyRequest request);

    @POST("api/auth/signup")
    Call<ApiResponse<SignUpResponse>> signUp(@Body SignUpRequest request);

    @POST("api/auth/find-email")
    Call<ApiResponse<Void>> findEmail(@Body FindEmailRequest request);

    @POST("api/auth/password/email-code")
    Call<ApiResponse<Void>> sendPasswordResetCode(@Body EmailCodeRequest request);

    @POST("api/auth/password/email-verify")
    Call<ApiResponse<Void>> verifyPasswordResetCode(@Body EmailVerifyRequest request);

    @POST("api/auth/password/reset-token")
    Call<ApiResponse<ResetTokenResponse>> issueResetToken(@Body ResetTokenRequest request);

    @POST("api/auth/password/reset")
    Call<ApiResponse<Void>> resetPassword(@Body PasswordResetRequest request);
}
