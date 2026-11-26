package com.example.deundeun.data.repository;

import android.content.Context;

import com.example.deundeun.data.network.dto.call.CallAnswerResponse;
import com.example.deundeun.data.network.dto.call.CallRequest;
import com.example.deundeun.data.network.dto.call.CallResultResponse;
import com.example.deundeun.data.network.dto.call.CallSessionResponse;

public class CallRepository extends AuthorizedRepository {

    public CallRepository(Context context) {
        super(context);
    }

    public void startSession(Callback<Result<CallSessionResponse>> callback) {
        enqueue(api.startCallSession(authorization()), callback);
    }

    public void answer(long sessionId, long itemId, String answer,
                       Callback<Result<CallAnswerResponse>> callback) {
        enqueue(api.answerCall(authorization(), sessionId, new CallRequest(itemId, answer)), callback);
    }

    public void complete(long sessionId, Callback<Result<CallResultResponse>> callback) {
        enqueue(api.completeCall(authorization(), sessionId), callback);
    }
}
