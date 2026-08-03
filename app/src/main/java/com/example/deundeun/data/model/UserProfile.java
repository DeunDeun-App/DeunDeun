package com.example.deundeun.data.model;

/** Figma 83:176 / 127:76 — 내 정보. */
public class UserProfile {

    private final String name;
    private final String phone;
    private final String birthDate;
    private final String disabilityType;
    private final String disabilityLevel;
    private final String guardianRelation;
    private final String guardianName;
    private final String guardianPhone;

    public UserProfile(String name, String phone, String birthDate,
                       String disabilityType, String disabilityLevel,
                       String guardianRelation, String guardianName, String guardianPhone) {
        this.name = name;
        this.phone = phone;
        this.birthDate = birthDate;
        this.disabilityType = disabilityType;
        this.disabilityLevel = disabilityLevel;
        this.guardianRelation = guardianRelation;
        this.guardianName = guardianName;
        this.guardianPhone = guardianPhone;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public String getDisabilityType() {
        return disabilityType;
    }

    public String getDisabilityLevel() {
        return disabilityLevel;
    }

    public String getGuardianRelation() {
        return guardianRelation;
    }

    public String getGuardianName() {
        return guardianName;
    }

    public String getGuardianPhone() {
        return guardianPhone;
    }

    /** 내정보 화면의 "엄마 (010-1234-5678)" 형태 한 줄. */
    public String getGuardianSummary() {
        return guardianRelation + " (" + guardianPhone + ")";
    }

    /** 내정보 화면의 "발달장애 중증" 형태 한 줄. */
    public String getDisabilitySummary() {
        return disabilityType + " " + disabilityLevel;
    }
}
