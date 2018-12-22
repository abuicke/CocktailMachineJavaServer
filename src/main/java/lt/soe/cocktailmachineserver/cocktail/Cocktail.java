package lt.soe.cocktailmachineserver.cocktail;

import java.util.List;

public class Cocktail {

    public long id;
    public String name;
    public String description;
    public List<Ingredient> ingredients;

    @Override
    public String toString() {
        return name;
    }

}
