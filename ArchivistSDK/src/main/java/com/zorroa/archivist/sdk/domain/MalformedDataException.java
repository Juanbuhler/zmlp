package com.zorroa.archivist.sdk.domain;

/**
 * Created by chambers on 10/30/15.
 */
public class MalformedDataException extends ArchivistException {

    public MalformedDataException() {
        super();
    }

    public MalformedDataException(String message) {
        super(message);
    }

    public MalformedDataException(String message, Throwable cause) {
        super(message, cause);
    }

    public MalformedDataException(Throwable cause) {
        super(cause);
    }
}
