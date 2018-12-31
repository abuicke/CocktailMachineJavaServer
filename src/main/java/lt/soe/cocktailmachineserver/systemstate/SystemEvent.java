package lt.soe.cocktailmachineserver.systemstate;

public class SystemEvent {

    public String name;
    public boolean isError;

    public SystemEvent() {

    }

    public SystemEvent(String name) {
        this.name = name;
    }

    public SystemEvent(String name, boolean isError) {
        this.name = name;
        this.isError = isError;
    }

    @Override
    public String toString() {
        return name;
    }

}
