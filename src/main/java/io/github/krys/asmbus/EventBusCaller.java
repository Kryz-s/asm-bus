package io.github.krys.asmbus;

import io.github.krys.asmbus.event.Event;

public interface EventBusCaller {
  void call(Event event);
  void callAsync(Event event);
}
