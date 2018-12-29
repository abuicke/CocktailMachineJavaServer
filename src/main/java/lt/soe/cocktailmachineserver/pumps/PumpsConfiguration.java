package lt.soe.cocktailmachineserver.pumps;

import java.util.ArrayList;
import java.util.List;

public class PumpsConfiguration {

    public List<Pump> pumps = new ArrayList<>();

    @Override
    public String toString() {
        return pumps.toString();
    }
}
