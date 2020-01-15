package com.zorroa.zmlp.client.domain.asset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains Filter Categories that can be used to search Assets Documents
 */
public class AssetFilesFilter {

    /**
     * Filter Document by Name
     */
    private List<String> name;

    /**
     * Filter Document by Category
     */
    private List<String> category;
    /**
     * Filter Document by MimeType
     */
    private List<String> mimetype;
    /**
     * Filter Document by Extension
     */
    private List<String> extension;
    /**
     * Filter Document that contains several Attributes
     */
    private Map<String, Object> attrs;
    /**
     * Filter Document by Attributes Keys
     */
    private List attrKeys;

    public AssetFilesFilter() {
        this.name = new ArrayList();
        this.category = new ArrayList();
        this.mimetype = new ArrayList();
        this.extension = new ArrayList();
        this.attrKeys = new ArrayList();
        this.attrs = new HashMap();
    }

    public AssetFilesFilter addName(String value) {
        this.name.add(value);
        return this;
    }

    public AssetFilesFilter addCategory(String value) {
        this.category.add(value);
        return this;
    }

    public AssetFilesFilter addMimetype(String value) {
        this.mimetype.add(value);
        return this;
    }

    public AssetFilesFilter addExtension(String value) {
        this.extension.add(value);
        return this;
    }

    public AssetFilesFilter addAttr(String key, Object value) {
        this.attrs.put(key, value);
        return this;
    }

    public AssetFilesFilter addAttrKey(String key) {
        this.attrKeys.add(key);
        return this;
    }

    public List<String> getName() {
        return name;
    }

    public AssetFilesFilter setName(List<String> name) {
        this.name = name;
        return this;
    }

    public List<String> getCategory() {
        return category;
    }

    public AssetFilesFilter setCategory(List<String> category) {
        this.category = category;
        return this;
    }

    public List<String> getMimetype() {
        return mimetype;
    }

    public AssetFilesFilter setMimetype(List<String> mimetype) {
        this.mimetype = mimetype;
        return this;
    }

    public List<String> getExtension() {
        return extension;
    }

    public AssetFilesFilter setExtension(List<String> extension) {
        this.extension = extension;
        return this;
    }

    public Map<String, Object> getAttrs() {
        return attrs;
    }

    public AssetFilesFilter setAttrs(Map<String, Object> attrs) {
        this.attrs = attrs;
        return this;
    }

    public List getAttrKeys() {
        return attrKeys;
    }

    public AssetFilesFilter setAttrKeys(List attrKeys) {
        this.attrKeys = attrKeys;
        return this;
    }
}
