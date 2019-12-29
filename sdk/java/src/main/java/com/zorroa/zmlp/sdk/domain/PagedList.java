package com.zorroa.zmlp.sdk.domain;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

public class PagedList<T> implements Iterable<T> {

    private Page page;
    private List<T> list;

    public PagedList() { }

    public PagedList(Page page, List<T> list) {
        this.page = page;
        this.list = list;
    }

    @Override
    public Iterator<T> iterator() {
        return list.iterator();
    }

    @Override
    public void forEach(Consumer action) {
        list.forEach(action);
    }

    @Override
    public Spliterator<T> spliterator() {
        return list.spliterator();
    }
}
