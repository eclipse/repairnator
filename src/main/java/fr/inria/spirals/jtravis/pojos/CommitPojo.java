package fr.inria.spirals.jtravis.pojos;

import java.util.Date;

/**
 * Created by urli on 22/12/2016.
 */
public class CommitPojo {
    private String sha;
    private String branch;
    private String message;
    private Date committedAt;
    private String authorName;
    private String authorEmail;
    private String committerName;
    private String committerEmail;
    private String compareUrl;

    public String getSha() {
        return sha;
    }

    public void setSha(String sha) {
        this.sha = sha;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getCommittedAt() {
        return committedAt;
    }

    public void setCommittedAt(Date committedAt) {
        this.committedAt = committedAt;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
    }

    public String getCommitterName() {
        return committerName;
    }

    public void setCommitterName(String committerName) {
        this.committerName = committerName;
    }

    public String getCommitterEmail() {
        return committerEmail;
    }

    public void setCommitterEmail(String committerEmail) {
        this.committerEmail = committerEmail;
    }

    public String getCompareUrl() {
        return compareUrl;
    }

    public void setCompareUrl(String compareUrl) {
        this.compareUrl = compareUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CommitPojo that = (CommitPojo) o;

        if (sha != null ? !sha.equals(that.sha) : that.sha != null) return false;
        if (branch != null ? !branch.equals(that.branch) : that.branch != null) return false;
        if (message != null ? !message.equals(that.message) : that.message != null) return false;
        if (committedAt != null ? !committedAt.equals(that.committedAt) : that.committedAt != null) return false;
        if (authorName != null ? !authorName.equals(that.authorName) : that.authorName != null) return false;
        if (authorEmail != null ? !authorEmail.equals(that.authorEmail) : that.authorEmail != null) return false;
        if (committerName != null ? !committerName.equals(that.committerName) : that.committerName != null)
            return false;
        if (committerEmail != null ? !committerEmail.equals(that.committerEmail) : that.committerEmail != null)
            return false;
        return compareUrl != null ? compareUrl.equals(that.compareUrl) : that.compareUrl == null;
    }

    @Override
    public int hashCode() {
        int result = sha != null ? sha.hashCode() : 0;
        result = 31 * result + (branch != null ? branch.hashCode() : 0);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (committedAt != null ? committedAt.hashCode() : 0);
        result = 31 * result + (authorName != null ? authorName.hashCode() : 0);
        result = 31 * result + (authorEmail != null ? authorEmail.hashCode() : 0);
        result = 31 * result + (committerName != null ? committerName.hashCode() : 0);
        result = 31 * result + (committerEmail != null ? committerEmail.hashCode() : 0);
        result = 31 * result + (compareUrl != null ? compareUrl.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "CommitPojo{" +
                "sha='" + sha + '\'' +
                ", branch='" + branch + '\'' +
                ", message='" + message + '\'' +
                ", committedAt=" + committedAt +
                ", authorName='" + authorName + '\'' +
                ", authorEmail='" + authorEmail + '\'' +
                ", committerName='" + committerName + '\'' +
                ", committerEmail='" + committerEmail + '\'' +
                ", compareUrl='" + compareUrl + '\'' +
                '}';
    }
}
