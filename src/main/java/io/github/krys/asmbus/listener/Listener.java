package io.github.krys.asmbus.listener;
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Listener {
    /**
     * Prioridad de ejecución (valores más bajos se ejecutan primero).
     * @return Prioridad del listener.
     */
    byte priority() default 0;
}