package com.zorroa.archivist.sdk.domain;

/**
 * Created by chambers on 9/25/15.
 */
public class AssetSearchBuilder {

    private AssetSearch search;
    private int size;
    private int from;
    private int room;

    public AssetSearchBuilder() { }

    public AssetSearch getSearch() {
        return search;
    }

    public AssetSearchBuilder setSearch(AssetSearch search) {
        this.search = search;
        return this;
    }

    public int getSize() {
        return size;
    }

    public AssetSearchBuilder setSize(int size) {
        this.size = size;
        return this;
    }

    public int getFrom() {
        return from;
    }

    public AssetSearchBuilder setFrom(int from) {
        this.from = from;
        return this;
    }

    public int getRoom() {
        return room;
    }

    public AssetSearchBuilder setRoom(int room) {
        this.room = room;
        return this;
    }
}
