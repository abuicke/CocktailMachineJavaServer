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

            byte isFirstIngredient;
            // Check to see if the ingredient is first in the pouring order.
            if(ingredient.pouringOrder == 1) {
                // If the ingredient is first set
                // isFirstIngredient to 1 (true)
                isFirstIngredient = 1;
            }else {
                // If the ingredient is not first
                // set isFirstIngredient to 0 (false)
                isFirstIngredient = 0;
            }

            // Create ByteBuffer with 5 bytes
            ByteBuffer byteBuffer = ByteBuffer.allocate(5)
                    // Insert the first byte to tell the
                    // simulator if this is the first ingredient or not
                    .put(0, isFirstIngredient)
                    // Insert the other 4 bytes which is the millilitres
                    // of the ingredient which needs to be poured
                    .putInt(1, ingredient.millilitresInADrink);

            // Convert ByteBuffer to a standard byte array
            byte[] requestBytes = byteBuffer.array();
            // Send the bytes to the simulator
            requestSocket.send(requestBytes);
            // Wait for the simulator to send back a reply
            // to guarantee the bytes have been received.
            requestSocket.recv();
            // Close this ZeroMQ socket
            requestSocket.close();

            // Open a new ZeroMQ socket to start receiving
            // the weight sensor readings from the simulator
            ZMQ.Socket subscribeSocket = context.createSocket(ZMQ.SUB);
            subscribeSocket.connect("tcp://localhost:5556");
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
