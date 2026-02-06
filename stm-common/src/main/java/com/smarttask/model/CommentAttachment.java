package com.smarttask.model;

import java.util.Date;
import java.util.UUID;

/**
 * Modèle pour les pièces jointes aux commentaires
 */
public class CommentAttachment {

    private String id;
    private String commentId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String filePath;
    private Date uploadedAt;

    // Constructeurs
    public CommentAttachment() {
        this.id = UUID.randomUUID().toString();
        this.uploadedAt = new Date();
    }

    public CommentAttachment(String commentId, String fileName, String fileType, Long fileSize, String filePath) {
        this();
        this.commentId = commentId;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.filePath = filePath;
    }

    // Getters et Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCommentId() { return commentId; }
    public void setCommentId(String commentId) { this.commentId = commentId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public Date getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Date uploadedAt) { this.uploadedAt = uploadedAt; }

    @Override
    public String toString() {
        return "CommentAttachment{" +
                "id='" + id + '\'' +
                ", fileName='" + fileName + '\'' +
                ", fileType='" + fileType + '\'' +
                ", fileSize=" + fileSize +
                ", uploadedAt=" + uploadedAt +
                '}';
    }
}
