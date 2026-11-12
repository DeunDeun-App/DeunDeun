package com.example.deundeun.data.network;

import com.example.deundeun.data.network.dto.auth.AuthRequest;
import com.example.deundeun.data.network.dto.auth.LoginResponse;
import com.example.deundeun.data.network.dto.auth.UserResponse;
import com.example.deundeun.data.network.dto.disaster.DisasterMessageResponse;
import com.example.deundeun.data.network.dto.disaster.EasyMessageResponse;
import com.example.deundeun.data.network.dto.sos.GuardianResponse;
import com.example.deundeun.data.network.dto.sos.SosRequest;
import com.example.deundeun.data.network.dto.sos.SosResponse;
import com.example.deundeun.data.network.dto.user.NotificationResponse;
import com.example.deundeun.data.network.dto.user.ProfileResponse;
import com.example.deundeun.data.network.dto.user.ProfileUpdateRequest;
import com.example.deundeun.data.network.dto.user.SettingsRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @POST("auth/login")
    Call<ApiResponse<LoginResponse>> login(@Body AuthRequest request);

    @GET("auth/me")
    Call<ApiResponse<UserResponse>> me(@Header("Authorization") String authorization);

    @POST("auth/logout")
    Call<ApiResponse<Void>> logout(@Header("Authorization") String authorization);

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

    @GET("users/me")
    Call<ApiResponse<ProfileResponse>> getProfile(@Header("Authorization") String authorization);

    @PUT("users/me")
    Call<ApiResponse<ProfileResponse>> updateProfile(@Header("Authorization") String authorization,
                                                     @Body ProfileUpdateRequest request);

    @PATCH("users/me/settings")
    Call<ApiResponse<ProfileResponse>> updateSettings(@Header("Authorization") String authorization,
                                                      @Body SettingsRequest request);

    @GET("users/me/notifications")
    Call<ApiResponse<List<NotificationResponse>>> getNotifications(
            @Header("Authorization") String authorization);

    @DELETE("users/me")
    Call<ApiResponse<Void>> withdraw(@Header("Authorization") String authorization);

    @GET("disaster-messages")
    Call<ApiResponse<List<DisasterMessageResponse>>> getRecentDisasterMessages(
            @Query("refresh") boolean refresh);

    @POST("disaster-messages/{id}/easy")
    Call<ApiResponse<EasyMessageResponse>> toEasyMessage(@Path("id") String id);

    @GET("sos/guardians")
    Call<ApiResponse<List<GuardianResponse>>> getSosGuardians(@Header("Authorization") String authorization);

    @POST("sos")
    Call<ApiResponse<SosResponse>> requestSos(@Header("Authorization") String authorization,
                                              @Body SosRequest request);

    @POST("sos/{sosId}/sent")
    Call<ApiResponse<Void>> markSosSent(@Header("Authorization") String authorization,
                                        @Path("sosId") long sosId);
}
