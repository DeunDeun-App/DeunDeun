package com.example.deundeun.network;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiService {
    @GET("api/check")
    Call<HealthCheckResponse> getHealthCheck();

}
