package io.github.krys.asmbus.dispatcher;

import io.github.krys.asmbus.event.Event;

public interface EventDispatcher<E extends Event> {
    void dispatch(E event);
}