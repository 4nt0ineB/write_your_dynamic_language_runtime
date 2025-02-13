package fr.umlv.smalljs.astinterp;

import fr.umlv.smalljs.ast.Expr;
import fr.umlv.smalljs.ast.Expr.Block;
import fr.umlv.smalljs.ast.Expr.FieldAccess;
import fr.umlv.smalljs.ast.Expr.FieldAssignment;
import fr.umlv.smalljs.ast.Expr.Fun;
import fr.umlv.smalljs.ast.Expr.FunCall;
import fr.umlv.smalljs.ast.Expr.If;
import fr.umlv.smalljs.ast.Expr.Literal;
import fr.umlv.smalljs.ast.Expr.LocalVarAccess;
import fr.umlv.smalljs.ast.Expr.LocalVarAssignment;
import fr.umlv.smalljs.ast.Expr.MethodCall;
import fr.umlv.smalljs.ast.Expr.New;
import fr.umlv.smalljs.ast.Expr.Return;
import fr.umlv.smalljs.ast.Script;
import fr.umlv.smalljs.rt.Failure;
import fr.umlv.smalljs.rt.JSObject;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static fr.umlv.smalljs.rt.JSObject.UNDEFINED;
import static java.util.stream.Collectors.joining;

public final class ASTInterpreter {
  private static JSObject asJSObject(Object value, int lineNumber) {
    if (!(value instanceof JSObject jsObject)) {
      throw new Failure("at line " + lineNumber + ", type error " + value + " is not a JSObject");
    }
    return jsObject;
  }

  static Object visit(Expr expression, JSObject env) {
    return switch (expression) {
      case Block(List<Expr> instrs, int lineNumber) -> {
				//throw new UnsupportedOperationException("TODO Block");
        // TODO loop over all instructions
        for (var expr: instrs) {
          visit(expr, env);
        }
        yield UNDEFINED;
      }
      case Literal<?>(Object value, int lineNumber) -> {
        yield value;
      }
      case FunCall(Expr qualifier, List<Expr> args, int lineNumber) -> {
        var value = visit(qualifier, env);
        if(!(value instanceof JSObject jsObject)) {
          throw new Failure("Not a function at line " + lineNumber);
        }
        var values = args.stream().map(expr -> visit(expr, env)).toArray();
        yield jsObject.invoke(UNDEFINED,values);
      }
      case LocalVarAccess(String name, int lineNumber) -> {
        yield env.lookup(name);
      }
      case LocalVarAssignment(String name, Expr expr, boolean declaration, int lineNumber) -> {
        if(declaration && env.lookup(name) != UNDEFINED) {
          throw new Failure("variable \"" + name + "\" is already defined at line " + lineNumber);
        }
        var value = visit(expr, env);
        env.register(name, value);
        yield value;
      }
      case Fun(Optional<String> optName, List<String> parameters, Block body, int lineNumber) -> {
				//throw new UnsupportedOperationException("TODO Fun");
        var functionName = optName.orElse("lambda");
        var invoker = new JSObject.Invoker() {
          @Override
          public Object invoke(Object receiver, Object... args) {
            if(args.length != parameters.size()) {
              throw new Failure("wrong number of arguments");
            }
            var localEnv = JSObject.newEnv(env);
            localEnv.register("this", receiver);
            for(var i = 0; i < parameters.size(); i++) {
              localEnv.register(parameters.get(i), args[i]);
            }
            
            try {
              return visit(body, localEnv);
            } catch (ReturnError re) {
              return re.getValue();
            }
          }
        };
        var function = JSObject.newFunction(functionName, invoker);
        optName.ifPresent(name -> env.register(name, function));
        yield function;
      }
      case Return(Expr expr, int lineNumber) -> {
        throw new ReturnError(visit(expr, env));
      }
      case If(Expr condition, Block trueBlock, Block falseBlock, int lineNumber) -> {
        var value = visit(condition, env);
        if(value instanceof Integer v && v == 1) {
          visit(trueBlock, env);
        } else {
          visit(falseBlock, env);
        }
        yield UNDEFINED;
      }
      case New(Map<String, Expr> initMap, int lineNumber) -> {
        var object = JSObject.newObject(null);
        initMap.forEach(object::register);
        for(var entry: initMap.entrySet()) {
          var value = visit(entry.getValue(), env);
          object.register(entry.getKey(), value);
        }
        yield object;
      }
      case FieldAccess(Expr receiver, String name, int lineNumber) -> {
        var value = visit(receiver, env);
        if(!(value instanceof JSObject jsObject)) {
          throw new Failure("Not an field at line " + lineNumber);
        }
        yield jsObject.lookup(name);
      }
      case FieldAssignment(Expr receiver, String name, Expr expr, int lineNumber) -> {
        var value = visit(expr, env);
        var field = visit(receiver, env);
        if(!(field instanceof JSObject jsObject)) {
          throw new Failure("Not an field at line " + lineNumber);
        }
        jsObject.register(name, value);
        yield value;
      }
      case MethodCall(Expr receiver, String name, List<Expr> args, int lineNumber) -> {
        var object = visit(receiver, env);
        if (!(object instanceof JSObject jsObject)) {
          throw new Failure("Not an object at line " + lineNumber);
        }
        var method = jsObject.lookup(name);
        if (!(method instanceof JSObject func)) {
          throw new Failure("Method " + name + " is not a function at line " + lineNumber);
        }
        var reifiedArgs = args.stream()
                             .map(arg -> visit(arg, env))
                             .toArray();
        yield func.invoke(jsObject, reifiedArgs);
      }
    };
  }

  @SuppressWarnings("unchecked")
  private static JSObject createGlobalEnv(PrintStream outStream) {
    JSObject globalEnv = JSObject.newEnv(null);
    globalEnv.register("global", globalEnv);
    globalEnv.register("print", JSObject.newFunction("print", (_, args) -> {
      System.err.println("print called with " + Arrays.toString(args));
      outStream.println(Arrays.stream(args).map(Object::toString).collect(Collectors.joining(" ")));
      return UNDEFINED;
    }));
    globalEnv.register("+", JSObject.newFunction("+", (_, args) -> (Integer) args[0] + (Integer) args[1]));
    globalEnv.register("-", JSObject.newFunction("-", (_, args) -> (Integer) args[0] - (Integer) args[1]));
    globalEnv.register("/", JSObject.newFunction("/", (_, args) -> (Integer) args[0] / (Integer) args[1]));
    globalEnv.register("*", JSObject.newFunction("*", (_, args) -> (Integer) args[0] * (Integer) args[1]));
    globalEnv.register("%", JSObject.newFunction("%", (_, args) -> (Integer) args[0] % (Integer) args[1]));
    globalEnv.register("==", JSObject.newFunction("==", (_, args) -> args[0].equals(args[1]) ? 1 : 0));
    globalEnv.register("!=", JSObject.newFunction("!=", (_, args) -> !args[0].equals(args[1]) ? 1 : 0));
    globalEnv.register("<", JSObject.newFunction("<", (_, args) -> (((Comparable<Object>) args[0]).compareTo(args[1]) < 0) ? 1 : 0));
    globalEnv.register("<=", JSObject.newFunction("<=", (_, args) -> (((Comparable<Object>) args[0]).compareTo(args[1]) <= 0) ? 1 : 0));
    globalEnv.register(">", JSObject.newFunction(">", (_, args) -> (((Comparable<Object>) args[0]).compareTo(args[1]) > 0) ? 1 : 0));
    globalEnv.register(">=", JSObject.newFunction(">=", (_, args) -> (((Comparable<Object>) args[0]).compareTo(args[1]) >= 0) ? 1 : 0));
    return globalEnv;
  }

  public static void interpret(Script script, PrintStream outStream) {
    JSObject globalEnv = createGlobalEnv(outStream);
    Block body = script.body();
    visit(body, globalEnv);
  }
}