package io.github.krys.asmbus.event;

public interface Cancellable {
  boolean isCancelled();
  void setCancelled(boolean cancelled);
}
