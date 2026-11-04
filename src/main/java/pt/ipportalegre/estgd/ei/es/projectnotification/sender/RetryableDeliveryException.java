package pt.ipportalegre.estgd.ei.es.projectnotification.sender;

public class RetryableDeliveryException extends RuntimeException {

    public RetryableDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }

    public RetryableDeliveryException(String message) {
        super(message);
    }
}