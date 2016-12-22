package fr.inria.spirals.jtravis.entities;

import java.util.Date;

/**
 * Created by urli on 21/12/2016.
 */
public class Commit {
    private String hash;
    private String branch;
    private String message;
    private Date committedAt;
    private String authorName;
    private String authorEmail;
    private String committerName;
    private String committerEmail;
    private String compareUrl;

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
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

        Commit commit = (Commit) o;

        if (hash != null ? !hash.equals(commit.hash) : commit.hash != null) return false;
        if (branch != null ? !branch.equals(commit.branch) : commit.branch != null) return false;
        if (message != null ? !message.equals(commit.message) : commit.message != null) return false;
        if (committedAt != null ? !committedAt.equals(commit.committedAt) : commit.committedAt != null) return false;
        if (authorName != null ? !authorName.equals(commit.authorName) : commit.authorName != null) return false;
        if (authorEmail != null ? !authorEmail.equals(commit.authorEmail) : commit.authorEmail != null) return false;
        if (committerName != null ? !committerName.equals(commit.committerName) : commit.committerName != null)
            return false;
        if (committerEmail != null ? !committerEmail.equals(commit.committerEmail) : commit.committerEmail != null)
            return false;
        return compareUrl != null ? compareUrl.equals(commit.compareUrl) : commit.compareUrl == null;
    }

    @Override
    public int hashCode() {
        return hash != null ? hash.hashCode() : 0;
    }
}
