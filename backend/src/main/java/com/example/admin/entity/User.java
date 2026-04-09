package com.example.admin.entity;

import java.time.LocalDateTime;
import java.time.LocalDate;

public class User {
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String signature;
    private String role; // ADMIN or STUDENT
    private Long teacherId;
    private String gender;
    private LocalDate birthday;
    private String region;
    // 仅用于展示：由后端按请求 IP 推断
    private String ipLocation;
    private Integer progress;
    private Integer checkInCount;
    private Integer isModel;
    private LocalDateTime createTime;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public LocalDate getBirthday() { return birthday; }
    public void setBirthday(LocalDate birthday) { this.birthday = birthday; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getIpLocation() { return ipLocation; }
    public void setIpLocation(String ipLocation) { this.ipLocation = ipLocation; }

    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }

    public Integer getCheckInCount() { return checkInCount; }
    public void setCheckInCount(Integer checkInCount) { this.checkInCount = checkInCount; }

    public Integer getIsModel() { return isModel; }
    public void setIsModel(Integer isModel) { this.isModel = isModel; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}