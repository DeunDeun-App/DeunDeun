package com.example.deundeun.data.repository;

import android.content.Context;

import com.example.deundeun.data.network.dto.record.RecordResponse;
import com.example.deundeun.data.network.dto.simulation.SimulationRequest;
import com.example.deundeun.data.network.dto.simulation.SimulationResultResponse;

public class RecordRepository extends AuthorizedRepository {

    public RecordRepository(Context context) {
        super(context);
    }

    public void getRecords(Callback<Result<RecordResponse>> callback) {
        enqueue(api.getRecords(authorization()), callback);
    }

    public void saveSimulation(SimulationRequest request,
                               Callback<Result<SimulationResultResponse>> callback) {
        enqueue(api.saveSimulationResult(authorization(), request), callback);
    }
}
