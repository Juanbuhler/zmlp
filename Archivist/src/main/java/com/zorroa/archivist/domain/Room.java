package com.zorroa.archivist.domain;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Room {

    private long id;
    private String session;
    private String name;
    private Set<String> inviteList;
    private boolean visible;

    public Room() { }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

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

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
