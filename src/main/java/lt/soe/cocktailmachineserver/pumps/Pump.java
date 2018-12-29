package lt.soe.cocktailmachineserver.pumps;

public class Pump {

    public int pumpNumber;
    public int millilitresPoured;
    public Bottle bottle;

    @Override
    public String toString() {
        return bottle.toString();
    }
}
