package lt.soe.cocktailmachineserver.systemstate;

public class SystemEvent {

    public String name;

    public SystemEvent() {

    }

    public SystemEvent(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

}
