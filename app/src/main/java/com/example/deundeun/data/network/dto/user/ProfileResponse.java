package com.example.deundeun.data.network.dto.user;

import androidx.annotation.Nullable;

public class ProfileResponse {

    private String loginId;
    private String email;
    private String name;
    private String phone;
    private String birthDate;
    private String disabilityType;
    private String severity;
    private boolean shareLocation;
    private boolean shareResult;
    private boolean sosAlert;
    private GuardianResponse guardian;

    public String getLoginId() { return loginId; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    @Nullable public String getPhone() { return phone; }
    @Nullable public String getBirthDate() { return birthDate; }
    @Nullable public String getDisabilityType() { return disabilityType; }
    @Nullable public String getSeverity() { return severity; }
    public boolean isShareLocation() { return shareLocation; }
    public boolean isShareResult() { return shareResult; }
    public boolean isSosAlert() { return sosAlert; }
    @Nullable public GuardianResponse getGuardian() { return guardian; }
}
