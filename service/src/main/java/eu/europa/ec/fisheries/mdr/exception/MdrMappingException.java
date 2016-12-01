package eu.europa.ec.fisheries.mdr.exception;

/**
 * Created by kovian on 31/08/2016.
 */
public class MdrMappingException extends Throwable {
    public MdrMappingException(String message, Throwable ex) {
        super(message, ex);
    }
    public MdrMappingException(String message) {
        super(message);
    }
    public MdrMappingException(Throwable ex) {
        super(ex);
    }
}
