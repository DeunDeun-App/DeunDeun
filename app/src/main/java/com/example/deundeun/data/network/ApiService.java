package com.example.deundeun.data.network;

import com.example.deundeun.data.network.dto.auth.LoginRequest;
import com.example.deundeun.data.network.dto.auth.LoginResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {

    @POST("api/auth/login")
    Call<ApiResponse<LoginResponse>> login(@Body LoginRequest request);
}