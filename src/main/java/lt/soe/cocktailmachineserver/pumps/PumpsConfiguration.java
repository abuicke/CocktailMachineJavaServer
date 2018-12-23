package lt.soe.cocktailmachineserver.pumps;

import java.util.List;

public class PumpsConfiguration {

    public class Bottle {
        public String name;
        public int fullBottleVolumeMillilitres;
        public int currentVolumeMillilitres;
    }

    public static class Pump {
        public int pumpNumber;
        public int millilitresPoured;
        public Bottle bottle;
    }

    public List<Pump> pumps;

}
