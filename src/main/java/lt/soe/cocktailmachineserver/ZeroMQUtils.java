package lt.soe.cocktailmachineserver;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public final class ZeroMQUtils {

    private static final byte TURN_OFF_WEIGHT_SENSOR = 0;
    private static final byte TURN_ON_WEIGHT_SENSOR = 1;
    private static final byte TURN_OFF_LIQUID_SENSOR = 2;
    private static final byte TURN_ON_LIQUID_SENSOR = 3;

    private static final byte REQUEST_FAILED = 0;
    private static final byte REQUEST_SUCCEEDED = 1;

    public static void turnOffWeightSensor() {
        sendRequestCode(TURN_OFF_WEIGHT_SENSOR);
    }

    public static void turnOnWeightSensor() {
        sendRequestCode(TURN_ON_WEIGHT_SENSOR);
    }

    public static void turnOffLiquidSensor() {
        sendRequestCode(TURN_OFF_LIQUID_SENSOR);
    }

    public static void turnOnLiquidSensor() {
        sendRequestCode(TURN_ON_LIQUID_SENSOR);
    }

    private static void sendRequestCode(byte requestCode) {
        try (ZContext context = new ZContext()) {
            //  Socket to talk to server
            System.out.println("connecting to sensor server");

            ZMQ.Socket socket = context.createSocket(ZMQ.REQ);
            String address = "tcp://localhost:5555";
            if (socket.connect(address)) {
                System.out.println("successfully connected to " + address);
            } else {
                throw new IllegalStateException("failed to connect to " + address);
            }

            if (socket.send(new byte[]{requestCode}, 0)) {
                System.out.println("successfully sent request code " + requestCode);
            } else {
                throw new IllegalStateException("failed to send request code " + requestCode);
            }

            byte[] reply = socket.recv(0);
            byte responseCode = reply[0];
            if (responseCode == REQUEST_SUCCEEDED) {
                System.out.println("request completed successfully");
            } else if (responseCode == REQUEST_FAILED) {
                throw new IllegalStateException("request failed");
            } else {
                throw new IllegalStateException("unrecognised response code " + responseCode);
            }
        }
    }

}
