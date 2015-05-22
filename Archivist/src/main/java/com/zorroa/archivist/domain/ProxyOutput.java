package com.zorroa.archivist.domain;

public class ProxyOutput {

    private int size;
    private int bpp;
    private String format;

    public ProxyOutput() { }

    public ProxyOutput(String format, int size, int bpp) {
        this.format = format;
        this.size = size;
        this.bpp = bpp;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getBpp() {
        return bpp;
    }

    public void setBpp(int bpp) {
        this.bpp = bpp;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String toString() {
        return String.format("<ProxyOutput(\"%s\",%d,%d)>", format, size, bpp);
    }
}
