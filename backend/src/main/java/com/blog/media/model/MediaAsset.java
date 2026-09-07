package com.blog.media.model;

import java.time.Instant;

/** 数据库媒体资产实体；业务引用和物理文件通过该实体建立可追溯关系。 */
public class MediaAsset {

    private Long id;
    private MediaType mediaType;
    private String storageName;
    private String urlFileName;
    private String originalName;
    private String contentType;
    private Long sizeBytes;
    private String sha256;
    private Integer uploadedBy;
    private MediaStatus status;
    private Instant deleteAfter;
    private String claimToken;
    private Instant claimedAt;
    private int deleteAttempts;
    private String lastError;
    private Instant createTime;
    private Instant updateTime;

    public static MediaAsset from(StoredMedia stored) {
        MediaAsset asset = new MediaAsset();
        asset.setMediaType(stored.getMediaType());
        asset.setStorageName(stored.getStorageName());
        asset.setUrlFileName(stored.getUrlFileName());
        asset.setOriginalName(stored.getOriginalName());
        asset.setContentType(stored.getContentType());
        asset.setSizeBytes(stored.getSizeBytes());
        asset.setSha256(stored.getSha256());
        asset.setStatus(MediaStatus.TEMP);
        return asset;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    public String getStorageName() {
        return storageName;
    }

    public void setStorageName(String storageName) {
        this.storageName = storageName;
    }

    public String getUrlFileName() {
        return urlFileName;
    }

    public void setUrlFileName(String urlFileName) {
        this.urlFileName = urlFileName;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public Integer getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(Integer uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public MediaStatus getStatus() {
        return status;
    }

    public void setStatus(MediaStatus status) {
        this.status = status;
    }

    public Instant getDeleteAfter() {
        return deleteAfter;
    }

    public void setDeleteAfter(Instant deleteAfter) {
        this.deleteAfter = deleteAfter;
    }

    public String getClaimToken() {
        return claimToken;
    }

    public void setClaimToken(String claimToken) {
        this.claimToken = claimToken;
    }

    public Instant getClaimedAt() {
        return claimedAt;
    }

    public void setClaimedAt(Instant claimedAt) {
        this.claimedAt = claimedAt;
    }

    public int getDeleteAttempts() {
        return deleteAttempts;
    }

    public void setDeleteAttempts(int deleteAttempts) {
        this.deleteAttempts = deleteAttempts;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public Instant getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Instant createTime) {
        this.createTime = createTime;
    }

    public Instant getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Instant updateTime) {
        this.updateTime = updateTime;
    }
}
