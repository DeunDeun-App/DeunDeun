package com.example.deundeun.data.network;

import com.example.deundeun.data.network.dto.auth.AuthRequest;
import com.example.deundeun.data.network.dto.auth.UserResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {

    @POST("api/auth/login")
    Call<ApiResponse<UserResponse>> login(@Body AuthRequest request);

    @POST("api/auth/signup/email-code")
    Call<ApiResponse<Void>> sendSignUpCode(@Body AuthRequest request);

    @POST("api/auth/signup/email-verify")
    Call<ApiResponse<Void>> verifySignUpCode(@Body AuthRequest request);

    @POST("api/auth/signup")
    Call<ApiResponse<UserResponse>> signUp(@Body AuthRequest request);

    @POST("api/auth/find-email")
    Call<ApiResponse<Void>> findEmail(@Body AuthRequest request);

    @POST("api/auth/password/email-code")
    Call<ApiResponse<Void>> sendPasswordResetCode(@Body AuthRequest request);

    @POST("api/auth/password/email-verify")
    Call<ApiResponse<Void>> verifyPasswordResetCode(@Body AuthRequest request);

    @POST("api/auth/password/reset-token")
    Call<ApiResponse<String>> issueResetToken(@Body AuthRequest request);

    @POST("api/auth/password/reset")
    Call<ApiResponse<Void>> resetPassword(@Body AuthRequest request);
}
