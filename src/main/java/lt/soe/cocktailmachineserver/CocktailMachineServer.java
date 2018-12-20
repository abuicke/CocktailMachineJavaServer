package lt.soe.cocktailmachineserver;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import static java.lang.System.exit;

@SuppressWarnings("unused")
@RestController
@SpringBootApplication
public class CocktailMachineServer {

    public static void main(String[] args) {
        SpringApplication.run(CocktailMachineServer.class, args);

        try {
            InputStream serviceAccount = CocktailMachineServer.class.getResourceAsStream("/firebase.json");

            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl("https://drinkingmaster-96b6c.firebaseio.com")
                    .build();

            FirebaseApp.initializeApp(options);
        } catch (IOException ioe) {
            System.err.println("couldn't initialise firebase sdk");
            ioe.printStackTrace();
            exit(-1);
        }

        System.out.println("server running...");
    }

    @RequestMapping("/get_cocktails")
    public List<Cocktail> getCocktails() {
        final Semaphore semaphore = new Semaphore(0);
        List<Cocktail> cocktails = new ArrayList<>();

        cocktails.add(new Cocktail("Martini"));

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("cocktails/");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snap : dataSnapshot.getChildren()) {
                    FirebaseCocktailSchema firebaseListing = snap.getValue(FirebaseCocktailSchema.class);
                    System.out.println("cocktail = " + firebaseListing.name);
                    cocktails.add(new Cocktail(firebaseListing.name));
                }
                semaphore.release();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                throw new IllegalStateException(error.toException());
            }
        });

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }

        return cocktails;
    }

    /**
     * TODO: Not returning.
     */
    @RequestMapping("/make_cocktail")
    public Cocktail makeCocktail() {
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

        return new Cocktail("Martini");
    }

}
