package ws.siri.jscore.js;

import net.fabricmc.api.ModInitializer;
import ws.siri.jscore.runtime.Runtime;

/**
 * Registers GraalJS with the jscore runtime.
 *
 * jscore's own initializer runs first (Fabric orders dependencies before dependents), but
 * it only registers a lifecycle callback -- Runtime.initialise() does not fire until
 * SERVER_STARTING/CLIENT_STARTED, so the language registration window is still open here.
 */
public class JsLangInit implements ModInitializer {
    public static final String MOD_ID = "jscore-js-runtime";

    @Override
    public void onInitialize() {
        Runtime.registerSupportedLanguage(new JsLangDef());
    }
}
