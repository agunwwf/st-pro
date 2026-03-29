package com.example.admin.entity;

import java.time.LocalDateTime;

/**
 * 对应表 sys_learning_completion。
 * 写入时通常只设 userId、moduleId、kind；查询时 MyBatis 会填 id、createTime。
 */
public class LearningCompletion {
    private Long id;
    private Long userId;
    private String moduleId;
    private String kind;
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getModuleId() {
        return moduleId;
    }

    public void setModuleId(String moduleId) {
        this.moduleId = moduleId;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
