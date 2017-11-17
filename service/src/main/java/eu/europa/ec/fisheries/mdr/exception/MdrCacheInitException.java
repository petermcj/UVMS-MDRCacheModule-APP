package eu.europa.ec.fisheries.mdr.exception;

/**
 * Created by kovian on 30/08/2016.
 */
public class MdrCacheInitException extends Exception {

    public MdrCacheInitException(Throwable cause) {
        super(cause);
    }

    public MdrCacheInitException(String message, Throwable cause) {
        super(message, cause);
    }
}
