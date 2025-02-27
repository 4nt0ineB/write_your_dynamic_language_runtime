package fr.umlv.smalljs;

import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.List;

// https://docs.oracle.com/javase/8/docs/api/java/lang/invoke/MethodHandle.html

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Complete the following class
// If you use a recent version of IntelliJ, IntelliJ is smart enough
// to do completion and emit helpful warnings
public class MethodHandleInAngerTest {
  private static MethodHandle TODO() {
    throw new UnsupportedOperationException("TODO");
  }

  private static int foo(int value) {
    return value * 2;
  }

  // Q1. create a method handle from the method "foo" above,
  //     using MethodHandles.lookup() and one of the method "find*"
  //     then call it using invokeExact with 42 as argument
  @Test
  public void callFoo() throws Throwable {
    var mh = MethodHandles.lookup().findStatic(MethodHandleInAngerTest.class, "foo", MethodType.methodType(int.class, int.class));
    var result = (int) mh.invokeExact(42);
    assertEquals(84, result);
  }

  private String hello(String message, String name) {
    return message + " " + name + "!";
  }

  // Q2. create a method handle on the method "hello" above,
  //     from it, create a new method handle that has "Hello"
  //     as first argument (WARNING, do not forget 'this') (using MethodHandles.insertArguments())
  //     then call  using invokeExact with "Bob" as argument
  @Test
  public void createMHWithHelloAsFirstArgument() throws Throwable {
    var mh = MethodHandles.lookup().findVirtual(MethodHandleInAngerTest.class, "hello", MethodType.methodType(String.class, String.class, String.class));
    var mh2 = MethodHandles.insertArguments(mh, 1, "Hello");  // use MHs.insertArguments
    var result = (String) mh2.invokeExact(this, "Bob");
    assertEquals("Hello Bob!", result);
  }

  private static void drop(int value) {
    assertEquals(42, value);
  }

  // Q3. create a method handle on the method "drop" above,
  //     from it, create a new method handle that drops the first argument
  //     (using MethodHandles.dropArguments())
  //     then call using invokeExact with 42 as argument
  @Test
  public void createMHThatDropsTheFirstArgument() throws Throwable {
    var mh = MethodHandles.lookup()
        .findStatic(MethodHandleInAngerTest.class, "drop", MethodType.methodType(void.class, int.class));
    var mh2 = MethodHandles.dropArguments(mh, 0, String.class);  // use MHs.dropArguments
    mh2.invokeExact("Boom", 42);
  }

  private static void bar(String s) {
    assertEquals("whizz", s);
  }

  // Q4. create a method handle on the method "bar" above,
  //     from it, create a new method handle that takes an Object
  //     as first parameter (using MethodHandle.asType())
  //     then call it with "whizz" and 12
  @Test
  public void createMHThatDoesARuntimeCheck() throws Throwable {
    var mh = MethodHandles.lookup()
        .findStatic(MethodHandleInAngerTest.class, "bar", MethodType.methodType(void.class, String.class));
    var mh2 = mh.asType(MethodType.methodType(void.class, Object.class));
    assertAll(
        () -> { mh2.invokeExact((Object) "whizz"); },
        () -> assertThrows(ClassCastException.class, () -> { mh2.invokeExact((Object) 12); }));
  }

  // Q5. create a method handle on the method Arrays.asList(Object...),
  //     call it with an array of objects,
  //     test if it's a varargs collector with (MethodHandle.isVarargsCollector()),
  //     use asType() to see it an a method that takes two objects ("foo" and "bar")
  @Test
  public void createAVarargsCollector() throws Throwable {
    var mh = MethodHandles.publicLookup().findStatic(Arrays.class, "asList", MethodType.methodType(List.class, Object[].class));

    assertEquals(List.of("foo", "bar"), (List<?>) mh.invokeExact(new Object[] { "foo", "bar" }));
    assertTrue(mh.isVarargsCollector());

    var mh2 = mh.asType(MethodType.methodType(List.class, Object.class, Object.class));
    assertEquals(List.of("foo", "bar"), (List<?>) mh2.invokeExact((Object) "foo", (Object) "bar"));
  }

  // Q6. create a method handle on the method Arrays.asList(Object...),
  //     test if it's a varargs collector with (MethodHandle.isVarargsCollector()),
  //     then create a new method handle that takes a String array as first parameter (using asType()),
  //     Is it a varargs collector ? How to make it a varargs collector using (MethodHandle.withVarargs()) ?
  //     How to create a new method handle able to be called with "foo" and "bar" (as a varargs)
  @Test
  public void resurrectAVarargsCollector() throws Throwable {
    var mh = MethodHandles.lookup()
        .findStatic(Arrays.class, "asList", MethodType.methodType(List.class, Object[].class));

    assertTrue(mh.isVarargsCollector());

    var mh2 = mh.asType(MethodType.methodType(List.class, String[].class));
    assertFalse(mh2.isVarargsCollector());

    var mh3 = mh.asType(MethodType.methodType(List.class, String[].class)).withVarargs(true);
    assertTrue(mh3.isVarargsCollector());

    var mh4 = mh3.asType(MethodType.methodType(List.class, String.class, String.class));
    assertEquals(List.of("foo", "bar"), (List<?>) mh4.invokeExact("foo", "bar"));
  }

  private static MethodHandle baz(String s) {
    if (s.equals("magic")) {
      return MethodHandles.dropArguments(
          MethodHandles.constant(String.class, "!!MAGIC!!"),
          0, String.class);
    }
    return MethodHandles.identity(String.class);
  }

  // Q7. using MethodHandles.exactInvoker(), create a method handle that can be called
  //     with a method handle and an integer
  @Test
  public void createAnInvoker() throws Throwable {
    var invoker = MethodHandles.exactInvoker(MethodType.methodType(String.class, String.class));

    var result = (String) invoker.invokeExact(baz("one"), "hello");
    assertEquals("hello", result);
  }

  // Q8. create a method handle on the method baz()
  //     create an invoker on a method type that takes a String and returns a String
  //     use MethodHandles.foldArguments so the method handle returned by baz is invoked by the invoker
  @Test
  public void createAMHFold() throws Throwable {
    var combiner = MethodHandles.lookup()
        .findStatic(MethodHandleInAngerTest.class, "baz", MethodType.methodType(MethodHandle.class, String.class));
    var invoker = MethodHandles.exactInvoker(MethodType.methodType(String.class, String.class));
    var fold = MethodHandles.foldArguments(invoker, combiner);

    assertEquals("hello", (String) fold.invokeExact("hello"));
    assertEquals("!!MAGIC!!", (String) fold.invokeExact("magic"));
  }
}
