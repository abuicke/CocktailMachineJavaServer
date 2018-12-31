package lt.soe.cocktailmachineserver.zeromq;

import lt.soe.cocktailmachineserver.cocktail.Ingredient;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.nio.ByteBuffer;
import java.util.Random;

public final class ZeroMQUtils {

    public interface OnNewWeightSensorReadingListener {
        void newWeightSensorReading(double weightSensorReading);
    }

    public static void simulatePouring(Ingredient ingredient, OnNewWeightSensorReadingListener listener) {
        try (ZContext context = new ZContext()) {
            ZMQ.Socket requestSocket = context.createSocket(ZMQ.REQ);
            requestSocket.connect("tcp://localhost:5555");
            byte[] requestBytes = ByteBuffer.allocate(5)
                    .put(0, (byte) (ingredient.pouringOrder == 1 ? 0 : 1))
                    .putInt(1, ingredient.millilitresInADrink)
                    .array();
            requestSocket.send(requestBytes);
            requestSocket.recv();
            requestSocket.close();

            //  Wait for weight sensor simulator to open publisher socket
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                throw new IllegalStateException(ie);
            }

            ZMQ.Socket subscribeSocket = context.createSocket(ZMQ.SUB);
            subscribeSocket.connect("tcp://localhost:5555");

            subscribeSocket.subscribe("weight sensor topic".getBytes(ZMQ.CHARSET));

            while (true) {
                subscribeSocket.recvStr();
                byte[] data = subscribeSocket.recv(0);
                double currentWeightFromSensor = ByteBuffer.wrap(data).getDouble();
                if (currentWeightFromSensor != -1) {
                    listener.newWeightSensorReading(currentWeightFromSensor);
                } else {
                    break;
                }
            }

            subscribeSocket.close();
        }
    }

}
