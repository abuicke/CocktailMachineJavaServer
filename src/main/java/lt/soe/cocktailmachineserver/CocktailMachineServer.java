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

    public static void main(String[] args) throws InterruptedException {
        SpringApplication.run(CocktailMachineServer.class, args);
        SystemEventsQueue.add(new SystemEvent("SpringBoot RESTful server initialised"));

        try {
            InputStream serviceAccount = CocktailMachineServer.class.getResourceAsStream("/firebase.json");

            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl("https://drinkingmaster-96b6c.firebaseio.com")
                    .build();

            FirebaseApp.initializeApp(options);
            SystemEventsQueue.add(new SystemEvent(
                    "server connected to Firebase database " +
                            "https://drinkingmaster-96b6c.firebaseio.com"
            ));

            pumpsConfiguration = new Firebase().getPumpsConfiguration();
            System.out.println("loaded pump configurations from firebase database " + pumpsConfiguration);
            SystemEventsQueue.add(new SystemEvent(
                    "loaded pump configurations from firebase database " + pumpsConfiguration
            ));
            cocktails = new Firebase().getCocktails();
            System.out.println("loaded cocktails from firebase " + cocktails);
            SystemEventsQueue.add(new SystemEvent(
                    "loaded cocktails from firebase database " + cocktails
            ));
        } catch (IOException ioe) {
            System.err.println("couldn't initialise firebase sdk");
            SystemEventsQueue.add(new SystemEvent(
                    "failed to connect to firebase database," +
                            " server shutting down...", true
            ));
            ioe.printStackTrace();
            exit(-1);
        }

        System.out.println("server running...");
    }

    @GetMapping("/get_cocktails")
    public List<Cocktail> getCocktails() throws InterruptedException {
        SystemEventsQueue.add(new SystemEvent(
                "request made to server for list of available " +
                        "cocktails, sending cocktails " + cocktails
        ));
        return cocktails;
    }

    @ResponseBody
    @PostMapping("/construct_cocktail")
    public ServerResponse constructCocktail(@RequestBody Cocktail cocktail) throws InterruptedException {
        SystemEventsQueue.add(new SystemEvent(
                "request made to construct new cocktail " + cocktail
        ));
        ServerResponse serverResponse = new Firebase().constructCocktail(cocktails.size(), cocktail);
        if (serverResponse.successful) {
            cocktails.add(cocktail);
            SystemEventsQueue.add(new SystemEvent(
                    "constructed new cocktail " + cocktail + " successfully"
            ));
        } else {
            SystemEventsQueue.add(new SystemEvent(
                    "failed to construct new cocktail " + cocktail, true
            ));
        }

        return serverResponse;
    }

    @ResponseBody
    @PostMapping("/order_cocktail")
    public ServerResponse orderCocktail(@RequestBody CocktailOrder cocktailOrder) throws InterruptedException {
        SystemEventsQueue.add(new SystemEvent(
                "request made to order cocktail with ID " +
                        cocktailOrder.cocktailId + " looking up cocktail by ID..."
        ));
        for (Cocktail cocktail : cocktails) {
            if (cocktail.id == cocktailOrder.cocktailId) {
                SystemEventsQueue.add(new SystemEvent(
                        "found cocktail with ID " +
                                cocktailOrder.cocktailId +
                                " making cocktail " + cocktail + "..."
                ));
                System.out.println("making " + cocktail);
                Ingredient[] ingredients = rearrangeIngredientsByPouringOrder(cocktail.ingredients);
                for (Ingredient ingredient : ingredients) {
                    boolean found = false;
                    for (Pump pump : pumpsConfiguration.pumps) {
                        if (pump.bottle.name.equals(ingredient.bottleName)) {
                            found = true;
                            SystemEventsQueue.add(new SystemEvent(
                                    "pouring " + pump
                            ));
                            System.out.println("pouring " + ingredient.bottleName);
                            ZeroMQUtils.simulatePouring(ingredient, weightSensorReading -> {
                                try {
                                    SystemEventsQueue.add(new SystemEvent(
                                            "weight sensor reading: " + weightSensorReading + "g"
                                    ));
                                } catch (InterruptedException ie) {
                                    ie.printStackTrace();
                                }
                            });
                            SystemEventsQueue.add(new SystemEvent(
                                    "finished pouring " + ingredient.bottleName
                            ));
                            System.out.println("finished pouring " + ingredient.bottleName);
                            pump.bottle.currentVolumeMillilitres =
                                    pump.bottle.currentVolumeMillilitres - ingredient.millilitresInADrink;
                        }
                    }

                    if (!found) {
                        System.err.println(ingredient + " is not attached to pumps");
                        return new ServerResponse(true,
                                "missing ingredient " + ingredient.bottleName);
                    }
                }

                SystemEventsQueue.add(new SystemEvent(
                        "finished making " + cocktail
                ));
                System.out.println("finished making " + cocktail);
                setPumpsConfiguration(pumpsConfiguration);
                return new ServerResponse(true, "ordered " +
                        "cocktail " + cocktail + " successfully");
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

    private Ingredient[] rearrangeIngredientsByPouringOrder(List<Ingredient> ingredientsList) {
        Ingredient[] ingredients = new Ingredient[ingredientsList.size()];
        int cnt = 0;
        while (cnt < ingredients.length) {
            for (Ingredient ingredient : ingredientsList) {
                if (ingredient.pouringOrder == (cnt + 1)) {
                    ingredients[cnt] = ingredient;
                    cnt++;
                    break;
                }
            }
        }

        return ingredients;
    }

}
