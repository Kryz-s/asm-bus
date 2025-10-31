package io.github.krys.asmbus.test;

import io.github.krys.asmbus.EventBus;
import io.github.krys.asmbus.event.AbstractCancellableEvent;
import io.github.krys.asmbus.event.Event;
import io.github.krys.asmbus.listener.Listener;

public class EventBusTest {
  public static void main(String[] args) {
    final EventBus eventBus = new EventBus();

    eventBus.register(new EventListener());
    eventBus.register(new OtherEventListener());
    eventBus.register(new NewOtherEventListener());

    eventBus.call(new MyEvent("Mensaje de MyEvent"));
    eventBus.call(new MyOtherEvent("Mensaje de MyOtherEvent"));
  }

  public static final class NewOtherEventListener {
    @Listener(priority = -60)
    public void onEvent(MyEvent event) {
      System.out.println(event.message);
      System.out.println("Evento de prioridad media alta");
//      event.setCancelled(true);
    }
  }

  public static final class EventListener {
    @Listener(priority = -127)
    public void onEvent(MyEvent event) {
      System.out.println(event.message);
      System.out.println("Evento de prioridad alta");
//      event.setCancelled(true);
    }

    @Listener(priority = 0)
    public void onOtherEvent(MyEvent event) {
      System.out.println(event.message);
      System.out.println("Evento de prioridad baja");
    }

    @Listener(priority = 120)
    public void onOtherEvent2(MyEvent event) {
      System.out.println(event.message);
      System.out.println("Evento de prioridad mas baja");
    }
  }

  public static final class OtherEventListener {
    @Listener(priority = -127)
    public void onEvent(MyOtherEvent event) {
      System.out.println(event.message);
      System.out.println("Evento de prioridad alta");
//      event.setCancelled(true);
    }

    @Listener(priority = 0)
    public void onOtherEvent(MyOtherEvent event) {
      System.out.println(event.message);
      System.out.println("Evento de prioridad baja");
    }
  }

  public static final class MyEvent extends AbstractCancellableEvent {
    public final String message;

    public MyEvent(String message) {
      this.message = message;
    }
  }
  public static final class MyOtherEvent implements Event {
    public final String message;

    public MyOtherEvent(String message) {
      this.message = message;
    }
  }
}
