package lt.soe.cocktailmachineserver;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static java.lang.System.exit;

@RestController
@SpringBootApplication
public class CocktailMachineServer {

    @Autowired private Gson gson;

    private static List<Cocktail> cocktails;

    public static void main(String[] args) {
        SpringApplication.run(CocktailMachineServer.class, args);

        try {
            InputStream serviceAccount = CocktailMachineServer.class.getResourceAsStream("/firebase.json");

            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl("https://drinkingmaster-96b6c.firebaseio.com")
                    .build();

            FirebaseApp.initializeApp(options);

            cocktails = new Firebase().getCocktails();
            System.out.println("loaded cocktails from firebase " + cocktails);
        } catch (IOException ioe) {
            System.err.println("couldn't initialise firebase sdk");
            ioe.printStackTrace();
            exit(-1);
        }

        System.out.println("server running...");
    }

    @GetMapping("/get_cocktails")
    public List<Cocktail> getCocktails() {
        System.out.println("/get_cocktails = " + cocktails);
        return cocktails;
    }

    @PostMapping("/construct_cocktail")
    public void constructCocktail(@RequestBody Cocktail cocktail) {
        System.out.println("received cocktail " + cocktail.name);
        cocktails.add(cocktail);
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("cocktails/" + (cocktails.size() - 1));
        ref.setValue(cocktail, (databaseError, databaseReference) -> {
            if (databaseError == null) {
                System.out.println("added cocktail " +
                        cocktail.name + " to firebase successfully");
            } else {
                databaseError.toException().printStackTrace();
            }
        });
    }

//    /**
//     * TODO: Not returning.
//     */
//    @PostMapping("/order_cocktail")
//    public void orderCocktail() {
//        try (ZContext context = new ZContext()) {
//            //  Socket to talk to server
//            System.out.println("connecting to sensor server");
//
//            ZMQ.Socket socket = context.createSocket(ZMQ.PAIR);
//            String address = "tcp://localhost:5556";
//            if (socket.connect(address)) {
//                System.out.println("successfully connected to " + address);
//            } else {
//                throw new IllegalStateException("failed to connect to " + address);
//            }
//
//            while (!Thread.currentThread().isInterrupted()) {
//                byte[] reply = socket.recv();
//                double currentWeightFromSensor = ByteBuffer.wrap(reply).getDouble();
//                if (currentWeightFromSensor == -1) {
//                    break;
//                }
//                System.out.println("currentWeightFromSensor = " + currentWeightFromSensor);
//                try {
//                    Thread.sleep(100);
//                } catch (InterruptedException e) {
//                    throw new IllegalStateException(e);
//                }
//            }
//        }
//    }

}
