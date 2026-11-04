package pt.ipportalegre.estgd.ei.es.projectnotification.sender;

/** Thrown by FailureSimulatingSender to imitate a temporary delivery problem. */
public class SimulatedDeliveryException extends RetryableDeliveryException {

    public SimulatedDeliveryException(String message) {
        super(message);
    }
}