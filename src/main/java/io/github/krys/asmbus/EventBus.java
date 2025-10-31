package io.github.krys.asmbus;

import io.github.krys.asmbus.dispatcher.EventDispatcher;
import io.github.krys.asmbus.event.Event;
import io.github.krys.asmbus.generator.ASMEventDispatcherGenerator;
import io.github.krys.asmbus.listener.Listener;
import io.github.krys.asmbus.reflection.ListenerMethodInfo;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

public class EventBus implements EventBusCaller {

  private final Map<Class<? extends Event>, List<ListenerMethodInfo>> listenerMap = new ConcurrentHashMap<>();

  private final Map<Class<? extends Event>, EventDispatcher<? extends Event>> dispatcherCache = new ConcurrentHashMap<>();

  private final Executor executor;

  public EventBus(Executor executor) {
    this.executor = executor;
  }

  public EventBus() {
    this(null);
  }

  public void register(Object listenerInstance) {
    Map<Class<? extends Event>, List<ListenerMethodInfo>> newListeners = scan(listenerInstance);

    newListeners.forEach((eventType, infoList) -> {
      listenerMap.computeIfAbsent(eventType, k -> new ArrayList<>()).addAll(infoList);
    });

    newListeners.keySet().forEach(this::rebuildDispatcher);
  }

  private void rebuildDispatcher(Class<? extends Event> eventType) {
    List<ListenerMethodInfo> allListeners = listenerMap.get(eventType);

    if (allListeners == null || allListeners.isEmpty()) {
      dispatcherCache.remove(eventType);
      return;
    }

    List<ListenerMethodInfo> sortedListeners = allListeners.stream()
      .sorted(Comparator.comparingInt(info -> info.method.getAnnotation(Listener.class).priority()))
      .collect(Collectors.toList());

    try {
      EventDispatcher<? extends Event> dispatcher = ASMEventDispatcherGenerator.generate(eventType, sortedListeners);

      dispatcherCache.put(eventType, dispatcher);
    } catch (Exception e) {
      System.err.println("Error al generar el EventDispatcher para " + eventType.getName());
      e.printStackTrace();
    }
  }

  @SuppressWarnings("unchecked")
  public void call(Event event) {
    EventDispatcher<Event> dispatcher = (EventDispatcher<Event>) dispatcherCache.get(event.getClass());

    if (dispatcher != null) {
      dispatcher.dispatch(event);
    }
  }

  @SuppressWarnings("unchecked")
  public void callAsync(Event event) {
    EventDispatcher<Event> dispatcher = (EventDispatcher<Event>) dispatcherCache.get(event.getClass());

    if (dispatcher != null) {
      if (executor == null)
        CompletableFuture.runAsync(() -> dispatcher.dispatch(event));
      else
        CompletableFuture.runAsync(() -> dispatcher.dispatch(event), executor);
    }
  }

  private Map<Class<? extends Event>, List<ListenerMethodInfo>> scan(Object listenerInstance) {
    Map<Class<? extends Event>, List<ListenerMethodInfo>> found = new HashMap<>();

    for (Method method : listenerInstance.getClass().getMethods()) {
      if (method.isAnnotationPresent(Listener.class) && method.getParameterCount() == 1) {
        Class<?> paramType = method.getParameterTypes()[0];

        if (Event.class.isAssignableFrom(paramType)) {
          @SuppressWarnings("unchecked")
          Class<? extends Event> eventType = (Class<? extends Event>) paramType;

          ListenerMethodInfo info = new ListenerMethodInfo(listenerInstance, method);

          found.computeIfAbsent(eventType, k -> new ArrayList<>()).add(info);
        }
      }
    }
    return found;
  }
}