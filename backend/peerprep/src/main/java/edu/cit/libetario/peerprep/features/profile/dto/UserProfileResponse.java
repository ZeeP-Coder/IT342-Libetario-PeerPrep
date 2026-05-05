package edu.cit.libetario.peerprep.features.profile.dto;

public class UserProfileResponse {

    private String fullName;
    private String email;
    private String university;
    private String major;
    private boolean googleAuth;

    public UserProfileResponse(String fullName, String email, String university, String major, boolean googleAuth) {
        this.fullName = fullName;
        this.email = email;
        this.university = university;
        this.major = major;
        this.googleAuth = googleAuth;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUniversity() {
        return university;
    }

    public void setUniversity(String university) {
        this.university = university;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public boolean isGoogleAuth() {
        return googleAuth;
    }

    public void setGoogleAuth(boolean googleAuth) {
        this.googleAuth = googleAuth;
    }
}