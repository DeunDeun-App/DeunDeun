package com.example.deundeun.data.repository;

import android.content.Context;

import com.example.deundeun.data.network.dto.record.RecordResponse;
import com.example.deundeun.data.network.dto.simulation.SimulationRequest;
import com.example.deundeun.data.network.dto.simulation.SimulationResultResponse;
import com.example.deundeun.data.session.SessionManager;

/** 기록 화면 조회와 재난 시뮬레이션 결과 저장. */
public class RecordRepository extends BaseRepository {

    private final SessionManager sessionManager;

    public RecordRepository(Context context) {
        sessionManager = new SessionManager(context);
    }

    public boolean isLoggedIn() {
        return sessionManager.isLoggedIn();
    }

    public void getRecords(Callback<Result<RecordResponse>> callback) {
        enqueue(api.getRecords(sessionManager.getAuthorization()), callback);
    }

    public void saveSimulation(SimulationRequest request,
                               Callback<Result<SimulationResultResponse>> callback) {
        enqueue(api.saveSimulationResult(sessionManager.getAuthorization(), request), callback);
    }
}
