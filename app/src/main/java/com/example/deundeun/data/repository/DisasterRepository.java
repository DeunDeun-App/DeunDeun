package com.example.deundeun.data.repository;

import com.example.deundeun.data.network.dto.disaster.DisasterMessageResponse;
import com.example.deundeun.data.network.dto.disaster.EasyMessageResponse;

import java.util.List;

public class DisasterRepository extends BaseRepository {

    public void getRecentMessages(boolean refresh,
                                  Callback<Result<List<DisasterMessageResponse>>> callback) {
        enqueue(api.getRecentDisasterMessages(refresh), callback);
    }

    public void toEasyMessage(String id, Callback<Result<EasyMessageResponse>> callback) {
        enqueue(api.toEasyMessage(id), callback);
    }
}
