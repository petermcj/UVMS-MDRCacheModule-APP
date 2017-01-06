package eu.europa.ec.fisheries.mdr.exception;

/**
 * Created by kovian on 30/08/2016.
 */
public class MdrStatusTableException extends Exception  {
    public MdrStatusTableException() {
        super();
    }

    public MdrStatusTableException(String message) {
        super(message);
    }

    public MdrStatusTableException(String message, Throwable cause) {
        super(message, cause);
    }
}
