package com.example.deundeun.data.network;

import com.example.deundeun.data.network.dto.auth.AuthRequest;
import com.example.deundeun.data.network.dto.auth.LoginResponse;
import com.example.deundeun.data.network.dto.auth.UserResponse;
import com.example.deundeun.data.network.dto.call.CallAnswerResponse;
import com.example.deundeun.data.network.dto.call.CallRequest;
import com.example.deundeun.data.network.dto.call.CallResultResponse;
import com.example.deundeun.data.network.dto.call.CallSessionResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {

    @POST("auth/login")
    Call<ApiResponse<LoginResponse>> login(@Body AuthRequest request);

    @GET("auth/me")
    Call<ApiResponse<UserResponse>> me(@Header("Authorization") String authorization);

    @POST("auth/signup/email-code")
    Call<ApiResponse<Long>> sendSignUpCode(@Body AuthRequest request);

    @POST("auth/signup/email-verify")
    Call<ApiResponse<Void>> verifySignUpCode(@Body AuthRequest request);

    @POST("auth/signup")
    Call<ApiResponse<UserResponse>> signUp(@Body AuthRequest request);

    @POST("auth/find-id/email-code")
    Call<ApiResponse<Long>> sendFindLoginIdCode(@Body AuthRequest request);

    @POST("auth/find-id/email-verify")
    Call<ApiResponse<String>> verifyFindLoginIdCode(@Body AuthRequest request);

    @POST("auth/password/email-code")
    Call<ApiResponse<Long>> sendPasswordResetCode(@Body AuthRequest request);

    @POST("auth/password/email-verify")
    Call<ApiResponse<Void>> verifyPasswordResetCode(@Body AuthRequest request);

    @POST("auth/password/reset-token")
    Call<ApiResponse<String>> issueResetToken(@Body AuthRequest request);

    @POST("auth/password/reset")
    Call<ApiResponse<Void>> resetPassword(@Body AuthRequest request);

    @POST("call/sessions")
    Call<ApiResponse<CallSessionResponse>> startCallSession(@Header("Authorization") String authorization);

    @POST("call/sessions/{sessionId}/answers")
    Call<ApiResponse<CallAnswerResponse>> answerCall(@Header("Authorization") String authorization,
                                                     @Path("sessionId") long sessionId,
                                                     @Body CallRequest request);

    @POST("call/sessions/{sessionId}/complete")
    Call<ApiResponse<CallResultResponse>> completeCall(@Header("Authorization") String authorization,
                                                       @Path("sessionId") long sessionId);
}
