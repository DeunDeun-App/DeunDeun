package com.example.deundeun.data.repository;

import androidx.annotation.Nullable;

import com.example.deundeun.data.network.dto.disaster.DisasterMessageResponse;
import com.example.deundeun.data.network.dto.disaster.EasyMessageResponse;

import java.util.List;

public class DisasterRepository extends BaseRepository {

    public void getRecentMessages(boolean refresh, @Nullable String region,
                                  Callback<Result<List<DisasterMessageResponse>>> callback) {
        enqueue(api.getRecentDisasterMessages(refresh, region), callback);
    }

    public void toEasyMessage(String id, Callback<Result<EasyMessageResponse>> callback) {
        enqueue(api.toEasyMessage(id), callback);
    }
}
