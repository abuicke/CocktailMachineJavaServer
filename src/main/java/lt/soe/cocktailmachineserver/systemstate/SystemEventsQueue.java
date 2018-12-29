package lt.soe.cocktailmachineserver.systemstate;

import java.util.LinkedList;
import java.util.Queue;

public class SystemEventsQueue {

    private static final Queue<SystemEvent> eventsQueue = new LinkedList<>();

    public static void add(SystemEvent event) throws InterruptedException {
        eventsQueue.add(event);
    }

    public static SystemEvent get() throws InterruptedException {
        if(eventsQueue.peek() != null) {
            return eventsQueue.poll();
        }else {
            return new SystemEvent();
        }
    }

}
