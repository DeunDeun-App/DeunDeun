package com.example.deundeun.data.network.dto.sos;

/** SOS 요청. 회원은 로그인 토큰으로 확인하므로 담지 않는다. */
public class SosRequest {

    private final Double lat;
    private final Double lng;
    private final String situation;
    private final Boolean trapped;
    private final Boolean hurt;
    private final String message;

    public SosRequest(Double lat, Double lng, String situation,
                      Boolean trapped, Boolean hurt, String message) {
        this.lat = lat;
        this.lng = lng;
        this.situation = situation;
        this.trapped = trapped;
        this.hurt = hurt;
        this.message = message;
    }
}
