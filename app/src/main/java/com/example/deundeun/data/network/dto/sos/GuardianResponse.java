package com.example.deundeun.data.network.dto.sos;

/** SOS 를 받을 보호자. */
public class GuardianResponse {

    private Long guardianId;
    private String guardianName;
    private String guardianPhone;
    private String relation;
    private String relationLabel;

    public Long getGuardianId() { return guardianId; }
    public String getGuardianName() { return guardianName; }
    public String getGuardianPhone() { return guardianPhone; }
    public String getRelation() { return relation; }

    /** 화면에 보여줄 관계 이름 (예: 부모님). */
    public String getRelationLabel() { return relationLabel; }
}
