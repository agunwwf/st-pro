package com.example.admin.entity;

import java.time.LocalDateTime;

/**
 * 论坛帖子实体类
 */
public class ForumPost {
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private String section;
    private String cover;
    private Integer votes;
    private Integer stars;
    private Integer comments;
    private LocalDateTime createTime;

    // 额外字段：用于关联查询作者信息
    private String authorName;
    private String authorAvatar;

    /** 仅接口返回：当前用户是否已点赞（不入库） */
    private Boolean votedByMe;
    /** 仅接口返回：当前用户是否已收藏（不入库） */
    private Boolean starredByMe;

    /** 仅列表查询：该用户最近一次浏览本帖的时间（来自 sys_forum_post_view） */
    private LocalDateTime lastViewTime;


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public String getCover() { return cover; }
    public void setCover(String cover) { this.cover = cover; }

    public Integer getVotes() { return votes; }
    public void setVotes(Integer votes) { this.votes = votes; }

    public Integer getStars() { return stars; }
    public void setStars(Integer stars) { this.stars = stars; }

    public Integer getComments() { return comments; }
    public void setComments(Integer comments) { this.comments = comments; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAuthorAvatar() { return authorAvatar; }
    public void setAuthorAvatar(String authorAvatar) { this.authorAvatar = authorAvatar; }

    public Boolean getVotedByMe() { return votedByMe; }
    public void setVotedByMe(Boolean votedByMe) { this.votedByMe = votedByMe; }

    public Boolean getStarredByMe() { return starredByMe; }
    public void setStarredByMe(Boolean starredByMe) { this.starredByMe = starredByMe; }

    public LocalDateTime getLastViewTime() { return lastViewTime; }
    public void setLastViewTime(LocalDateTime lastViewTime) { this.lastViewTime = lastViewTime; }
}
