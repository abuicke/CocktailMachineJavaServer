package lt.soe.cocktailmachineserver;

public class Ingredient {

    public String bottleName;
    public int pumpNumber;
    public int pouringOrder;
    public int millilitresInADrink;
    public double millilitresInAFullBottle;

    public Ingredient(
            String bottleName,
            int pumpNumber,
            int pouringOrder,
            int millilitresInADrink,
            double millilitresInAFullBottle
    ) {
        this.bottleName = bottleName;
        this.pumpNumber = pumpNumber;
        this.pouringOrder = pouringOrder;
        this.millilitresInADrink = millilitresInADrink;
        this.millilitresInAFullBottle = millilitresInAFullBottle;
    }
}
