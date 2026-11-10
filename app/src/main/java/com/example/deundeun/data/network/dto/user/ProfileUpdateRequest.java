package com.example.deundeun.data.network.dto.user;

public class ProfileUpdateRequest {

    private final String name;
    private final String phone;
    private final String birthDate;
    private final String disabilityType;
    private final String severity;
    private final String guardianRelation;
    private final String guardianName;
    private final String guardianPhone;

    public ProfileUpdateRequest(String name, String phone, String birthDate, String disabilityType,
                                String severity, String guardianRelation, String guardianName,
                                String guardianPhone) {
        this.name = name;
        this.phone = phone;
        this.birthDate = birthDate;
        this.disabilityType = disabilityType;
        this.severity = severity;
        this.guardianRelation = guardianRelation;
        this.guardianName = guardianName;
        this.guardianPhone = guardianPhone;
    }
}
