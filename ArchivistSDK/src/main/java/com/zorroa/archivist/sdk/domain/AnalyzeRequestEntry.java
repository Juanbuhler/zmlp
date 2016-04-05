package com.zorroa.archivist.sdk.domain;

import com.google.common.collect.Maps;

import java.net.URI;
import java.util.Map;

/**
 * Created by chambers on 3/9/16.
 */
public class AnalyzeRequestEntry {
    private String uri;
    private Map<String, Object> attrs;

    public AnalyzeRequestEntry() { }

    public AnalyzeRequestEntry(String uri) {
        this.uri = uri;
        this.attrs = Maps.newHashMap();
    }

    public AnalyzeRequestEntry(URI uri) {
        this(uri.toString());
    }

    public AnalyzeRequestEntry setAttr(String key, Object value) {
        if (value == null) {
            return this;
        }
        this.attrs.put(key, value);
        return this;
    }

    public AnalyzeRequestEntry addToAttr(String key, Object value) {
        if (value == null) {
            return this;
        }
        this.attrs.put("@"+ key, value);
        return this;
    }

    public boolean isRemote() {
        if (uri.startsWith("/")) {
            return false;
        }
        return !uri.startsWith("file:");
    }

    public String getUri() {
        return uri;
    }

    public AnalyzeRequestEntry setUri(String uri) {
        this.uri = uri;
        return this;
    }

    public Map<String, Object> getAttrs() {
        return attrs;
    }

    public AnalyzeRequestEntry setAttrs(Map<String, Object> attrs) {
        this.attrs = attrs;
        return this;
    }
}
