package com.zorroa.archivist.sdk.service;

import java.io.File;

public interface ImageService {

    /**
     * Return the default proxy image format.
     *
     * @return
     */
    String getDefaultProxyFormat();

    /**
     * Generates a file path for the given proxy ID and createa parent
     * directories.
     *
     * @param id
     * @param format
     * @return
     */
    File allocateProxyPath(String id, String format);
}
