package lt.soe.cocktailmachineserver.pumps;

import java.util.ArrayList;
import java.util.List;

public class PumpsConfiguration {

    public List<Pump> pumps = new ArrayList<>();

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder("[");
        for (int i = 0; i < pumps.size() - 1; i++) {
            string.append(pumps.get(i) + " attached to pump " + (i + 1) + ", ");
        }
        string.append(pumps.get(pumps.size() - 1) + " attached to pump " + pumps.size());
        string.append("]");
        return string.toString();
    }
}
