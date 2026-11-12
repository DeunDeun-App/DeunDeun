package com.example.deundeun.data.network.dto.sos;

import java.util.List;

/** SOS 저장 결과. */
public class SosResponse {

    private long sosId;
    private List<String> phones;

    public long getSosId() { return sosId; }

    /** 문자를 받을 보호자 전화번호 (우선순위 순). */
    public List<String> getPhones() { return phones; }
}
