package lt.soe.cocktailmachineserver.pumps;

public class Pump {

    public int pumpNumber;
    public Bottle bottle;

    @Override
    public String toString() {
        return bottle + " attached to pump " + pumpNumber;
    }
}
