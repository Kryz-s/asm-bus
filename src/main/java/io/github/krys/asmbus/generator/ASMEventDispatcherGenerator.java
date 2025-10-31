package io.github.krys.asmbus.generator;

import io.github.krys.asmbus.dispatcher.EventDispatcher;
import io.github.krys.asmbus.event.AbstractCancellableEvent;
import io.github.krys.asmbus.event.Event;
import io.github.krys.asmbus.reflection.ListenerMethodInfo;
import org.objectweb.asm.*;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ASMEventDispatcherGenerator {

  private static final AtomicInteger CLASS_COUNTER = new AtomicInteger(0);

  @SuppressWarnings("unchecked")
  public static <E extends Event> EventDispatcher<E> generate(
    Class<E> eventType,
    List<ListenerMethodInfo> listeners) throws Exception {

    String dispatcherName = "GeneratedEventDispatcher" + CLASS_COUNTER.incrementAndGet();
    String internalName = "io/github/krys/asmbus/generated/" + dispatcherName;
    String superName = "java/lang/Object";
    String[] interfaces = new String[]{Type.getInternalName(EventDispatcher.class)};

    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

    cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, internalName, null, superName, interfaces);

    for (int i = 0; i < listeners.size(); i++) {
      cw.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL,
          "listener" + i,
          Type.getDescriptor(listeners.get(i).instance.getClass()),
          null, null)
        .visitEnd();
    }

    // Constructor
    MethodVisitor ctor = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", getConstructorDescriptor(listeners), null, null);
    ctor.visitCode();
    ctor.visitVarInsn(Opcodes.ALOAD, 0);
    ctor.visitMethodInsn(Opcodes.INVOKESPECIAL, superName, "<init>", "()V", false); // Llamar a super()

    int paramIndex = 1;
    for (int i = 0; i < listeners.size(); i++) {
      Class<?> listenerClass = listeners.get(i).instance.getClass();
      String fieldName = "listener" + i;
      String fieldDesc = Type.getDescriptor(listenerClass);

      ctor.visitVarInsn(Opcodes.ALOAD, 0);
      ctor.visitVarInsn(Opcodes.ALOAD, paramIndex);
      ctor.visitFieldInsn(Opcodes.PUTFIELD, internalName, fieldName, fieldDesc);
      paramIndex++;
    }

    ctor.visitInsn(Opcodes.RETURN);
    ctor.visitMaxs(0, 0);
    ctor.visitEnd();

    // dispatch()
    String dispatchDesc = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Event.class));
    MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "dispatch", dispatchDesc, null, null);
    mv.visitCode();

    Label endLabel = new Label();

    int eventLocalIndex = 1;

    for (int i = 0; i < listeners.size(); i++) {
      ListenerMethodInfo info = listeners.get(i);
      String fieldName = "listener" + i;
      String fieldDesc = Type.getDescriptor(info.instance.getClass());
      Method method = info.method;
      String methodDesc = Type.getMethodDescriptor(method);
      String listenerInternalName = Type.getInternalName(info.instance.getClass());
      Class<?> eventParamType = method.getParameterTypes()[0];
      String eventParamInternalName = Type.getInternalName(eventParamType);

      mv.visitVarInsn(Opcodes.ALOAD, 0); // Cargar 'this'
      mv.visitFieldInsn(Opcodes.GETFIELD, internalName, fieldName, fieldDesc);

      mv.visitVarInsn(Opcodes.ALOAD, eventLocalIndex);

      mv.visitTypeInsn(Opcodes.CHECKCAST, eventParamInternalName);

      mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, listenerInternalName, method.getName(), methodDesc, false);

      if (AbstractCancellableEvent.class.isAssignableFrom(eventType)) {
        Label nextListener = new Label();

        mv.visitVarInsn(Opcodes.ALOAD, eventLocalIndex);

        String cancelableName = Type.getInternalName(AbstractCancellableEvent.class);
        mv.visitTypeInsn(Opcodes.CHECKCAST, cancelableName);

        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, cancelableName, "isCancelled", "()Z", false);

        mv.visitJumpInsn(Opcodes.IFEQ, nextListener);

        mv.visitJumpInsn(Opcodes.GOTO, endLabel);

        mv.visitLabel(nextListener);
      }
    }

    mv.visitLabel(endLabel);
    mv.visitInsn(Opcodes.RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();

    // 6. Generar el método puente (bridge method) requerido por Java 8+ para genericidad
    // Este método simplemente llama al método dispatch(Event)
    String bridgeDesc = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Object.class));
    MethodVisitor bridgeMv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_BRIDGE | Opcodes.ACC_SYNTHETIC,
      "dispatch", bridgeDesc, null, null);
    bridgeMv.visitCode();
    bridgeMv.visitVarInsn(Opcodes.ALOAD, 0);
    bridgeMv.visitVarInsn(Opcodes.ALOAD, 1);
    bridgeMv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(eventType));
    bridgeMv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, internalName, "dispatch", dispatchDesc, false);
    bridgeMv.visitInsn(Opcodes.RETURN);
    bridgeMv.visitMaxs(0, 0);
    bridgeMv.visitEnd();

    cw.visitEnd();

    byte[] bytecode = cw.toByteArray();
//    try {
//      String outputPath = "C:/Users/tcikm/IdeaProjects/commit/asm-bus/build/temp/" + dispatcherName + ".class";
//
//      java.nio.file.Files.write(
//        java.nio.file.Paths.get(outputPath),
//        bytecode
//      );
//      System.out.println("✅ Bytecode generado y guardado en: " + outputPath);
//    } catch (Exception e) {
//      System.err.println("Error al guardar el bytecode: " + e.getMessage());
//      e.printStackTrace();
//    }
    Class<?> dispatcherClass = new EventBusClassLoader().defineClass(internalName.replace('/', '.'), bytecode);

    Class<?>[] constructorTypes = listeners.stream()
      .map(info -> info.instance.getClass())
      .toArray(Class<?>[]::new);

    Object[] constructorArgs = listeners.stream()
      .map(info -> info.instance)
      .toArray(Object[]::new);

    return (EventDispatcher<E>) dispatcherClass.getConstructor(constructorTypes).newInstance(constructorArgs);
  }

  private static String getConstructorDescriptor(List<ListenerMethodInfo> listeners) {
    StringBuilder sb = new StringBuilder("(");
    for (ListenerMethodInfo info : listeners) {
      sb.append(Type.getDescriptor(info.instance.getClass()));
    }
    sb.append(")V");
    return sb.toString();
  }

  private static class EventBusClassLoader extends ClassLoader {
    public Class<?> defineClass(String name, byte[] bytecode) {
      return super.defineClass(name, bytecode, 0, bytecode.length);
    }
  }
}