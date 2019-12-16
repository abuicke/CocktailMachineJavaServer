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
        SystemEventsQueue.add(new SystemEvent("SpringBoot RESTful server initialised"));

        try {
            InputStream serviceAccount = CocktailMachineServer.class.getResourceAsStream("/firebase.json");

            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl("https://drinkbase-d4ca3.firebaseio.com/")
                    .build();

            FirebaseApp.initializeApp(options);
            SystemEventsQueue.add(new SystemEvent(
                    "server connected to Firebase database " +
                            "https://drinkbase-d4ca3.firebaseio.com/"));

            pumpsConfiguration = new Firebase().getPumpsConfiguration();
            System.out.println("loaded pumps configuration from firebase database " + pumpsConfiguration);
            SystemEventsQueue.add(new SystemEvent(
                    "loaded pumps configuration from firebase database " + pumpsConfiguration));

            cocktails = new Firebase().getCocktails();
            System.out.println("loaded cocktails from firebase " + cocktails);
            SystemEventsQueue.add(new SystemEvent(
                    "loaded cocktails from firebase database " + cocktails));

        } catch (IOException ioe) {
            ioe.printStackTrace();
            SystemEventsQueue.add(new SystemEvent(
                    "failed to connect to firebase database," +
                            " server shutting down...", true));
            exit(-1);
        }

        SystemEventsQueue.add(new SystemEvent("server running..."));
        System.out.println("server running...");
    }

    @GetMapping("/get_cocktails")
    public List<Cocktail> getCocktails() {
        SystemEventsQueue.add(new SystemEvent(
                "request made to server for list of available " +
                        "cocktails, sending cocktails " + cocktails));
        return cocktails;
    }

    @PostMapping("/construct_cocktail")
    public void constructCocktail(@RequestBody Cocktail cocktail) {
        SystemEventsQueue.add(new SystemEvent(
                "request made to construct new cocktail " + cocktail));
        boolean successful = new Firebase().constructCocktail(cocktails.size(), cocktail);
        if (successful) {
            cocktails.add(cocktail);
            SystemEventsQueue.add(new SystemEvent(
                    "constructed new cocktail " + cocktail + " successfully"));
        } else {
            SystemEventsQueue.add(new SystemEvent(
                    "failed to construct new cocktail " + cocktail, true));
        }
    }

    @PostMapping("/order_cocktail")
    public void orderCocktail(@RequestBody CocktailOrder cocktailOrder) {
        SystemEventsQueue.add(new SystemEvent("request made to order cocktail with ID " +
                cocktailOrder.cocktailId + " looking up cocktail by ID..."));
        for (Cocktail cocktail : cocktails) {
            if (cocktail.id == cocktailOrder.cocktailId) {
                SystemEventsQueue.add(new SystemEvent("found cocktail with ID " +
                        cocktailOrder.cocktailId + " making cocktail " + cocktail + "..."));
                System.out.println("making " + cocktail);
                Ingredient[] ingredients = rearrangeIngredientsByPouringOrder(cocktail.ingredients);
                for (Ingredient ingredient : ingredients) {
                    boolean found = false;
                    for (Pump pump : pumpsConfiguration.pumps) {
                        if (pump.bottle.name.equals(ingredient.bottleName)) {
                            found = true;
                            SystemEventsQueue.add(new SystemEvent("pouring " + pump));
                            System.out.println("pouring " + ingredient.bottleName);

                            if (pump.bottle.currentVolumeMillilitres < ingredient.millilitresInADrink) {
                                System.err.println("not enough " + ingredient + ". needs " +
                                        ingredient.millilitresInADrink + "mls, but only " +
                                        pump.bottle.currentVolumeMillilitres + "mls is available");
                                SystemEventsQueue.add(new SystemEvent("not enough " + ingredient +
                                        ". needs " + ingredient.millilitresInADrink + "mls, but only " +
                                        pump.bottle.currentVolumeMillilitres + "mls is available"));
                                return;
                            }

                            ZeroMQUtils.simulatePouring(ingredient, weightSensorReading -> {
                                SystemEventsQueue.add(new SystemEvent(
                                        "weight sensor reading: " + weightSensorReading + "g"));
                            });
                            SystemEventsQueue.add(new SystemEvent(
                                    "finished pouring " + ingredient.bottleName));
                            System.out.println("finished pouring " + ingredient.bottleName);
                            pump.bottle.currentVolumeMillilitres =
                                    pump.bottle.currentVolumeMillilitres - ingredient.millilitresInADrink;
                        }
                    }

                    if (!found) {
                        System.err.println(ingredient + " is not attached to pumps");
                        SystemEventsQueue.add(new SystemEvent("missing ingredient " +
                                ingredient.bottleName + ", couldn't make cocktail " + cocktail));
                        return;
                    }
                }

                SystemEventsQueue.add(new SystemEvent("finished making " + cocktail));
                System.out.println("finished making " + cocktail);
                setPumpsConfiguration(pumpsConfiguration);
                return;
            }
        }

        SystemEventsQueue.add(new SystemEvent("couldn't find cocktail with ID " +
                cocktailOrder.cocktailId + ", failed to make cocktail", true));
    }

    @GetMapping("/get_pumps_configuration")
    public PumpsConfiguration getPumpsConfiguration() {
        SystemEventsQueue.add(new SystemEvent("request made to " +
                "get_pumps_configuration, returning " + pumpsConfiguration));
        return pumpsConfiguration;
    }

    @PostMapping("/set_pumps_configuration")
    public void setPumpsConfiguration(@RequestBody PumpsConfiguration pumpsConfiguration) {
        boolean successful = new Firebase().setPumpsConfiguration(pumpsConfiguration);
        if (successful) {
            CocktailMachineServer.pumpsConfiguration = pumpsConfiguration;
        }
    }

    @GetMapping("/get_last_system_event")
    public SystemEvent getLastSystemEvent() {
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
