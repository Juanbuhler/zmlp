package com.zorroa.archivist.web.api;

import com.zorroa.archivist.HttpUtils;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.service.BlobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class BlobController {

    @Autowired
    BlobService blobService;

    @RequestMapping(value="/api/v1/blobs/{app}/{feature}/{name}", method=RequestMethod.POST)
    public Blob set(@RequestBody Object data, @PathVariable String app, @PathVariable String feature, @PathVariable String name) {
        return blobService.set(app, feature, name, data);
    }

    @RequestMapping(value="/api/v1/blobs/{app}/{feature}/{name}", method=RequestMethod.GET)
    public Blob get(@PathVariable String app, @PathVariable String feature, @PathVariable String name) {
        return blobService.get(app, feature, name);
    }

    @RequestMapping(value="/api/v1/blobs/{app}/{feature}/{name}", method=RequestMethod.DELETE)
    public Object delete(@PathVariable String app, @PathVariable String feature, @PathVariable String name) {
        BlobId blob = blobService.getId(app, feature, name, Access.Write);
        return HttpUtils.deleted("blob", blob.getBlobId(), blobService.delete(blob));
    }

    /**
     * Gets just the data structure without any of the other properties.
     *
     * @param app
     * @param feature
     * @param name
     * @return
     */
    @RequestMapping(value="/api/v1/blobs/{app}/{feature}/{name}/_raw", method=RequestMethod.GET)
    public Object getRaw(@PathVariable String app, @PathVariable String feature, @PathVariable String name) {
        return blobService.get(app, feature, name).getData();
    }

    @RequestMapping(value="/api/v1/blobs/{app}/{feature}", method=RequestMethod.GET)
    public List<Blob> getAll(@PathVariable String app, @PathVariable String feature) {
        return blobService.getAll(app, feature);
    }

    @RequestMapping(value="/api/v1/blobs/{app}/{feature}/{name}/_permissions", method=RequestMethod.PUT)
    public Acl setPermissions(@RequestBody SetPermissions req, @PathVariable String app, @PathVariable String feature, @PathVariable String name) {
        BlobId blob = blobService.getId(app, feature, name, Access.Write);
        return blobService.setPermissions(blob, req);
    }

    @RequestMapping(value="/api/v1/blobs/{app}/{feature}/{name}/_permissions", method=RequestMethod.GET)
    public Acl getPermisions(@PathVariable String app, @PathVariable String feature, @PathVariable String name) {
        BlobId blob = blobService.getId(app, feature, name, Access.Read);
        return blobService.getPermissions(blob);
    }
}
