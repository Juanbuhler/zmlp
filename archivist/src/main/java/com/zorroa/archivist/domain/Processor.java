package com.zorroa.archivist.domain;

import com.google.common.base.MoreObjects;
import com.zorroa.sdk.processor.ProcessorType;

import java.util.*;

/**
 * Created by chambers on 8/16/16.
 */


public class Processor {

    private UUID id;
    private List<String> filters;
    private Set<String> fileTypes;
    private String description;
    private String name;
    private String shortName;
    private String module;
    private ProcessorType type;
    private List<Map<String,Object>> display;

    // Properties from parent Plugin

    private UUID pluginId;
    private String pluginName;
    private String pluginVersion;
    private String pluginLanguage;

    public UUID getId() {
        return id;
    }

    public Processor setId(UUID id) {
        this.id = id;
        return this;
    }

    public List<String> getFilters() {
        return filters;
    }

    public Processor setFilters(List<String> filters) {
        this.filters = filters;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Processor setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getName() {
        return name;
    }

    public Processor setName(String name) {
        this.name = name;
        return this;
    }

    public String getShortName() {
        return shortName;
    }

    public Processor setShortName(String shortName) {
        this.shortName = shortName;
        return this;
    }

    public String getModule() {
        return module;
    }

    public Processor setModule(String module) {
        this.module = module;
        return this;
    }

    public ProcessorType getType() {
        return type;
    }

    public Processor setType(ProcessorType type) {
        this.type = type;
        return this;
    }

    public List<Map<String, Object>> getDisplay() {
        return display;
    }

    public Processor setDisplay(List<Map<String, Object>> display) {
        this.display = display;
        return this;
    }

    public String getPluginName() {
        return pluginName;
    }

    public Processor setPluginName(String pluginName) {
        this.pluginName = pluginName;
        return this;
    }

    public String getPluginVersion() {
        return pluginVersion;
    }

    public Processor setPluginVersion(String pluginVersion) {
        this.pluginVersion = pluginVersion;
        return this;
    }

    public String getPluginLanguage() {
        return pluginLanguage;
    }

    public Processor setPluginLanguage(String pluginLanguage) {
        this.pluginLanguage = pluginLanguage;
        return this;
    }

    public UUID getPluginId() {
        return pluginId;
    }

    public Processor setPluginId(UUID pluginId) {
        this.pluginId = pluginId;
        return this;
    }

    public Set<String> getFileTypes() {
        return fileTypes;
    }

    public Processor setFileTypes(Set<String> fileTypes) {
        this.fileTypes = fileTypes;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Processor processor = (Processor) o;
        return getId() == processor.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("description", description)
                .add("className", name)
                .add("id", id)
                .add("type", type)
                .toString();
    }
}
