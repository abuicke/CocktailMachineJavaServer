package lt.soe.cocktailmachineserver;

import java.util.ArrayList;
import java.util.List;

public class Cocktail {

    static Cocktail TEST_COCKTAIL(String name) {
        return new Cocktail(
                name,
                "The jimmy whiskey cocktail",
                new ArrayList<Ingredient>() {{
                    add(new Ingredient(
                            "Jack Daniels",
                            1,
                            1,
                            35,
                            700
                    ));
                }});
    }

    public String name;
    public String description;
    public List<Ingredient> ingredients;

    public Cocktail(String name, String description, List<Ingredient> ingredients) {
        this.name = name;
        this.description = description;
        this.ingredients = ingredients;
    }

    @Override
    public String toString() {
        return name;
    }

}
