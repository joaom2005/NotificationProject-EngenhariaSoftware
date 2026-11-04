package pt.ipportalegre.estgd.ei.es.projectnotification.sender;

/** Thrown by {@link FailureSimulatingSender} to imitate a temporary delivery problem. */
public class SimulatedDeliveryException extends RuntimeException {

    public SimulatedDeliveryException(String message) {
        super(message);
    }
}
