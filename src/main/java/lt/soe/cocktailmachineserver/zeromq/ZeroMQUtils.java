package lt.soe.cocktailmachineserver.zeromq;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.nio.ByteBuffer;
import java.util.Random;

public final class ZeroMQUtils {

    public interface OnNewWeightSensorReadingListener {
        void newWeightSensorReading(double weightSensorReading);
    }

    public static void getReadingsFromWeightSensor(OnNewWeightSensorReadingListener listener) {
        try (ZContext context = new ZContext()) {
            System.out.println("connecting to weight sensor...");
            ZMQ.Socket requestSocket = context.createSocket(ZMQ.REQ);
            System.out.println("opened request socket to communicate with weight sensor");
            requestSocket.connect("tcp://localhost:5555");
            requestSocket.send(new byte[]{0});
            requestSocket.recv();
            System.out.println("received reply over request socket, weight sensor is ready");
            requestSocket.close();
            System.out.println("closed request socket");

            System.out.println("opening subscriber socket to weight sensor to receive incoming weight sensor readings");
            ZMQ.Socket subscribeSocket = context.createSocket(ZMQ.SUB);
            subscribeSocket.connect("tcp://localhost:5555");
            System.out.println("successfully connected to weight sensor over subscriber socket");

            subscribeSocket.subscribe("weight sensor topic".getBytes(ZMQ.CHARSET));
            System.out.println("subscribed to weight sensor socket, waiting to receive readings from weight sensor");

            while (true) {
                subscribeSocket.recvStr();
                byte[] data = subscribeSocket.recv(0);
                double currentWeightFromSensor = ByteBuffer.wrap(data).getDouble();
                if(currentWeightFromSensor != -1) {
                    listener.newWeightSensorReading(currentWeightFromSensor);
                }else {
                    System.out.println("weight sensor has no more readings to return");
                    break;
                }
            }

            System.out.println("closing subscriber socket to weight sensor");
            subscribeSocket.close();
        }
    }

}
