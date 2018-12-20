package lt.soe.cocktailmachineserver;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Cocktail {

    private final String _name;

    public Cocktail(String name) {
        _name = name;
    }

    @JsonProperty("name")
    public String getCocktailName() {
        return _name;
    }

}
