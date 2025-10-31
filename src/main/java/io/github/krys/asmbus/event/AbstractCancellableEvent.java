package io.github.krys.asmbus.event;

public class AbstractCancellableEvent implements Cancellable, Event {

  private boolean cancelled = false;

  @Override
  public boolean isCancelled() {
    return cancelled;
  }

  @Override
  public void setCancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }
}
