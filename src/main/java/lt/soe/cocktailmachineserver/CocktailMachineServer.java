package lt.soe.cocktailmachineserver;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import lt.soe.cocktailmachineserver.cocktail.Cocktail;
import lt.soe.cocktailmachineserver.cocktailorder.CocktailOrder;
import lt.soe.cocktailmachineserver.firebase.Firebase;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.Semaphore;

import static java.lang.System.exit;

@RestController
@SpringBootApplication
public class CocktailMachineServer {

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

    @ResponseBody
    @PostMapping("/construct_cocktail")
    public ServerResponse constructCocktail(@RequestBody Cocktail cocktail) {
        System.out.println("received cocktail " + cocktail.name);
        cocktails.add(cocktail);
        Semaphore semaphore = new Semaphore(0);
        ServerResponse serverResponse = new ServerResponse();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("cocktails/" + (cocktails.size() - 1));
        ref.setValue(cocktail, (databaseError, databaseReference) -> {
            if (databaseError == null) {
                serverResponse.successful = true;
                serverResponse.message = "added cocktail " + cocktail.name + " to firebase successfully";
                System.out.println(serverResponse.message);
                semaphore.release();

            } else {
                serverResponse.successful = true;
                serverResponse.message = databaseError.getMessage();
                databaseError.toException().printStackTrace();
                semaphore.release();
            }
        });

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return serverResponse;
    }

    @ResponseBody
    @PostMapping("/order_cocktail")
    public ServerResponse orderCocktail(@RequestBody CocktailOrder cocktailOrder) {
        System.out.println("/order_cocktail = " + cocktailOrder.cocktailId);
        for(Cocktail cocktail: cocktails) {
            if(cocktail.id == cocktailOrder.cocktailId) {
                return new ServerResponse(true, "ordered cocktail " + cocktail.name + " successfully");
            }
        }

        return new ServerResponse(false, "failed to order cocktail with id " + cocktailOrder.cocktailId);
    }

}
