package lt.soe.cocktailmachineserver;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lt.soe.cocktailmachineserver.cocktail.Cocktail;
import lt.soe.cocktailmachineserver.cocktail.Ingredient;
import lt.soe.cocktailmachineserver.cocktailorder.CocktailOrder;
import lt.soe.cocktailmachineserver.firebase.Firebase;
import lt.soe.cocktailmachineserver.pumps.Pump;
import lt.soe.cocktailmachineserver.pumps.PumpsConfiguration;
import lt.soe.cocktailmachineserver.systemstate.SystemEvent;
import lt.soe.cocktailmachineserver.systemstate.SystemEventsQueue;
import lt.soe.cocktailmachineserver.zeromq.ZeroMQUtils;
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

    private static PumpsConfiguration pumpsConfiguration;
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

            pumpsConfiguration = new Firebase().getPumpsConfiguration();
            System.out.println("loaded pump configuration from firebase " + pumpsConfiguration);
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
    public List<Cocktail> getCocktails() throws InterruptedException {
        SystemEventsQueue.add(new SystemEvent("/get_cocktails"));
        return cocktails;
    }

    @ResponseBody
    @PostMapping("/construct_cocktail")
    public ServerResponse constructCocktail(@RequestBody Cocktail cocktail) {
        ServerResponse serverResponse = new Firebase().constructCocktail(cocktails.size(), cocktail);
        if (serverResponse.successful) {
            cocktails.add(cocktail);
        }

        return serverResponse;
    }

    @ResponseBody
    @PostMapping("/order_cocktail")
    public ServerResponse orderCocktail(@RequestBody CocktailOrder cocktailOrder) {
        for (Cocktail cocktail : cocktails) {
            if (cocktail.id == cocktailOrder.cocktailId) {
                for (Ingredient ingredient : cocktail.ingredients) {
                    boolean found = false;
                    for (Pump pump : pumpsConfiguration.pumps) {
                        if (pump.bottle.name.equals(ingredient.bottleName)) {
                            found = true;
                            System.out.println("pouring " + ingredient.bottleName);
                            ZeroMQUtils.getReadingsFromWeightSensor(System.out::println);
                            System.out.println("finished pouring " + ingredient.bottleName);
                            pump.bottle.currentVolumeMillilitres =
                                    pump.bottle.currentVolumeMillilitres - ingredient.millilitresInADrink;
                        }
                    }

                    if (!found) {
                        return new ServerResponse(true,
                                "missing ingredient " + ingredient.bottleName);
                    }
                }
                setPumpsConfiguration(pumpsConfiguration);
                return new ServerResponse(true, "ordered " +
                        "cocktail " + cocktail.name + " successfully");
            }
        }
        return new ServerResponse(false, "failed to " +
                "order cocktail with id " + cocktailOrder.cocktailId);
    }

    @GetMapping("/get_pumps_configuration")
    public PumpsConfiguration getPumpsConfiguration() {
        return pumpsConfiguration;
    }

    @ResponseBody
    @PostMapping("/set_pumps_configuration")
    public ServerResponse setPumpsConfiguration(@RequestBody PumpsConfiguration pumpsConfiguration) {
        ServerResponse serverResponse = new Firebase().setPumpsConfiguration(pumpsConfiguration);
        if (serverResponse.successful) {
            CocktailMachineServer.pumpsConfiguration = pumpsConfiguration;
        }

        return serverResponse;
    }

    @GetMapping("/get_last_system_event")
    public SystemEvent getLastSystemEvent() throws InterruptedException {
        return SystemEventsQueue.get();
    }

}
