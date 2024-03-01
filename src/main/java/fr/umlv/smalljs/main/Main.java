package fr.umlv.smalljs.main;

import fr.umlv.smalljs.ast.Script;
import fr.umlv.smalljs.astinterp.ASTInterpreter;
import fr.umlv.smalljs.jvminterp.JVMInterpreter;
import fr.umlv.smalljs.stackinterp.StackInterpreter;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.function.BiConsumer;

import static fr.umlv.smalljs.ast.ASTBuilder.createScript;
import static java.lang.System.in;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.newBufferedReader;

// run with /path/to/jdk-22/bin/java --class-path lib/tatoo-runtime.jar:target/smalljs-1.0.jar fr.umlv.smalljs.main.Main ast samples/hello.js
public final class Main {
  private static BiConsumer<Script, PrintStream> interpreter(String name) {
    return switch (name) {
      case "ast" -> ASTInterpreter::interpret;
      case "stack" -> StackInterpreter::interpret;
      case "jvm" -> JVMInterpreter::interpret;
      default -> throw new IllegalArgumentException("unkonwn interpreter " + name);
    };
  }

  private static void printHelp() {
    System.err.println("""
            Help:
              fr.umlv.smalljs.main.Main interpreter [input-file.js]
                available interpreters: ast, stack and jvm
            """);
    System.exit(1);
  }

  public static void main(String[] args) throws IOException {
    if (args.length == 0 || args.length > 2) {
      printHelp();
      return;
    }
    try {
      var interpreter = interpreter(args[0]);
      try (var reader = (args.length == 2) ? newBufferedReader(Path.of(args[1])) : new InputStreamReader(in, UTF_8)) {
        var script = createScript(reader);
        interpreter.accept(script, System.out);
      }
    } catch(RuntimeException e) {
      e.printStackTrace();
      printHelp();
    }
  }
}
