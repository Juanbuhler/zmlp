package com.zorroa.archivist.sdk.service;

import com.zorroa.archivist.sdk.domain.Export;
import com.zorroa.archivist.sdk.domain.ExportBuilder;

/**
 * Created by chambers on 11/1/15.
 */
public interface ExportService {

    Export create(ExportBuilder builder);
}
