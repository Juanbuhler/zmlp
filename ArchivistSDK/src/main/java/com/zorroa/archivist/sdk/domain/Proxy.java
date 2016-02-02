package com.zorroa.archivist.sdk.domain;

import com.google.common.base.MoreObjects;

public class Proxy {
    private String name;
    private String path;
    private int width;
    private int height;
    private String format;

    public Proxy() { }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getName() {
        return name;
    }

    public Proxy setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Proxy)) return false;

        Proxy proxy = (Proxy) o;

        if (getWidth() != proxy.getWidth()) return false;
        if (getHeight() != proxy.getHeight()) return false;
        return !(getPath() != null ? !getPath().equals(proxy.getPath()) : proxy.getPath() != null);

    }

    @Override
    public int hashCode() {
        int result = getPath() != null ? getPath().hashCode() : 0;
        result = 31 * result + getWidth();
        result = 31 * result + getHeight();
        return result;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("path", path)
                .add("name", name)
                .add("width", width)
                .add("height", height)
                .add("format", format)
                .toString();
    }
}
