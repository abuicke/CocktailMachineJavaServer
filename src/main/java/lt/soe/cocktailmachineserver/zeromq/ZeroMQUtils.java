package lt.soe.cocktailmachineserver.zeromq;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.nio.ByteBuffer;

public final class ZeroMQUtils {

    private static final byte REQUEST_FAILED = 0;
    private static final byte REQUEST_SUCCEEDED = 1;

    public static void constructCocktail() {
        try (ZContext context = new ZContext()) {
            //  Socket to talk to server
            System.out.println("connecting to sensor server");

            ZMQ.Socket socket = context.createSocket(ZMQ.PAIR);
            String address = "tcp://localhost:5556";
            if (socket.connect(address)) {
                System.out.println("successfully connected to " + address);
            } else {
                throw new IllegalStateException("failed to connect to " + address);
            }

            while (!Thread.currentThread().isInterrupted()) {
                byte[] reply = socket.recv();
                double currentWeightFromSensor = ByteBuffer.wrap(reply).getDouble();
                if (currentWeightFromSensor == -1) {
                    break;
                }
                System.out.println("currentWeightFromSensor = " + currentWeightFromSensor);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    public static void getReadingsFromWeightSensor() {
        sendRequestCode();
    }

    private static void sendRequestCode() {
        try (ZContext context = new ZContext()) {
            //  Socket to talk to server
            System.out.println("connecting to weight sensor");

            ZMQ.Socket socket = context.createSocket(ZMQ.REQ);
            String address = "tcp://localhost:5555";
            if (socket.connect(address)) {
                System.out.println("successfully connected to weight sensor on " + address);
            } else {
                throw new IllegalStateException("failed to connect to weight sensor on " + address);
            }

            if (socket.send(new byte[]{0}, 0)) {
                System.out.println("successfully sent request code to weight sensor");
            } else {
                throw new IllegalStateException("failed to send request code to weight sensor");
            }

            while (!Thread.currentThread().isInterrupted()) {
                byte[] reply = socket.recv(0);
                double currentWeightFromSensor = ByteBuffer.wrap(reply).getDouble();
                System.out.println("currentWeightFromSensor = " + currentWeightFromSensor);
            }
        }
    }

}
