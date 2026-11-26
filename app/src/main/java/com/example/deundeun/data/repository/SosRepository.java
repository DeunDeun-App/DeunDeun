package com.example.deundeun.data.repository;

import android.content.Context;

import com.example.deundeun.data.network.dto.sos.GuardianResponse;
import com.example.deundeun.data.network.dto.sos.SosRequest;
import com.example.deundeun.data.network.dto.sos.SosResponse;

import java.util.List;

public class SosRepository extends AuthorizedRepository {

    public SosRepository(Context context) {
        super(context);
    }

    public void getGuardians(Callback<Result<List<GuardianResponse>>> callback) {
        enqueue(api.getSosGuardians(authorization()), callback);
    }

    public void request(SosRequest request, Callback<Result<SosResponse>> callback) {
        enqueue(api.requestSos(authorization(), request), callback);
    }

    public void markSent(long sosId, Callback<Result<Void>> callback) {
        enqueue(api.markSosSent(authorization(), sosId), callback);
    }
}
