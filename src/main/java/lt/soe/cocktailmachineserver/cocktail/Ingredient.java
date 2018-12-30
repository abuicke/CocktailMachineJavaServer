package lt.soe.cocktailmachineserver.cocktail;

public class Ingredient {

    public String bottleName;
    public int pouringOrder;
    public int millilitresInADrink;

    @Override
    public String toString() {
        return bottleName;
    }
}
