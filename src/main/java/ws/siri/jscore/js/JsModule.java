package ws.siri.jscore.js;

import ws.siri.jscore.runtime.Module;
import ws.siri.jscore.runtime.ClassMarkers.LangSpecificModule;
import ws.siri.jscore.runtime.ClassMarkers.Prelude;
import ws.siri.jscore.runtime.Errors;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * The module object accessible in that file.
 */
public class JsModule implements LangSpecificModule {
    private Module internal;

    public JsModule(Module internal) {
        this.internal = internal;
    }

    @FunctionalInterface
    public interface ThroableBiFunction<T, U, R, E extends Exception> {
        R apply(T t, U u) throws E;
    }

    @Override
    public Object getMember(String key) {
        switch (key) {
            case "exports":
                return internal.getExportsInternal().map(v -> (Object) v).orElse(Undefined.instance);
            case "onunload":
                return internal.getOnUnloadInternal().map(v -> (Object) v).orElse(Undefined.instance);
            case "import":
                // note: Value is String[]
                return (ThroableBiFunction<String, List<Prelude>, Object, IOException>) (path, preludes) -> {
                    Optional<Value> res = internal.importRelative(path, preludes);
                    return JsUtils.unwrapOrUndefined(res);
                };
            case "unimport":
                return (Consumer<String>) (path) -> this.internal.unimportRelative(path);
            case "createPrelude":
                return new ProxyExecutable() {
                    @Override
                    public Object execute(Value... arguments) {
                        if (arguments.length != 1)
                            throw new IllegalArgumentException(
                                    String.format("expected %d argument, got %d", 1, arguments.length));

                        return internal.createPrelude(arguments[0]::executeVoid);
                    }
                };
            default:
                return Undefined.instance;
        }
    }

    @Override
    public Object getMemberKeys() {
        return new String[] { "exports", "onload", "import", "unimport", "createPrelude" };
    }

    @Override
    public boolean hasMember(String key) {
        switch (key) {
            case "exports":
            case "onload":
            case "import":
            case "unimport":
            case "createPrelude":
                return true;
            default:
                return false;
        }
    }

    @Override
    public void putMember(String key, Value value) {
        switch (key) {
            case "exports":
                if (JsUtils.isUndefined(value))
                    this.internal.setExportsInternal(Optional.empty());
                else
                    this.internal.setExportsInternal(Optional.of(value));
                break;
            case "onunload":
                if (JsUtils.isUndefined(value))
                    this.internal.setOnUnloadInternal(Optional.empty());
                else if (!value.canExecute())
                    throw new Errors.TypeMismatchException("function", value.getMetaObject().getMetaSimpleName());
                else
                    this.internal.setOnUnloadInternal(Optional.of(value.as(Runnable.class)));
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public boolean removeMember(String key) {
        switch (key) {
            case "exports":
                internal.setExportsInternal(Optional.empty());
                return true;
            case "onunload":
                internal.setOnUnloadInternal(Optional.empty());
                return true;
            default:
                return false;
        }
    }
}
