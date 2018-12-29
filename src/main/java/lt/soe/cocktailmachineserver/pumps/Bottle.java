package lt.soe.cocktailmachineserver.pumps;

public class Bottle {

    public String name;
    public int fullBottleVolumeMillilitres;
    public int currentVolumeMillilitres;

    @Override
    public String toString() {
        return name;
    }
}
