package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.DebugImportSpec;
import com.zorroa.archivist.domain.ImportSpec;
import com.zorroa.archivist.domain.Job;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
/**
 * Created by chambers on 7/11/16.
 */
public interface ImportService {

    PagedList<Job> getAll(Pager page);

    Job create(DebugImportSpec spec);

    /**
     * Create a import job with the given import spec.
     *
     * @param spec
     * @return
     */
    Job create(ImportSpec spec);
}
