package com.zorroa.archivist.sdk.domain;

import java.util.Set;

public class RoomBuilder {

    private Long sessionId = null;
    private String name;
    private Set<String> inviteList;
    private String password;
    private boolean visible = true;
    private String folderId = null;

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Set<String> getInviteList() {
        return inviteList;
    }
    public void setInviteList(Set<String> inviteList) {
        this.inviteList = inviteList;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public boolean isVisible() {
        return visible;
    }
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    public Long getSessionId() {
        return sessionId;
    }
    public void setSessionId(Long session) {
        this.sessionId = session;
    }
    public String getFolderId() {
        return folderId;
    }
    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }
}
