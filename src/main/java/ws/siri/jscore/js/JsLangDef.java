package ws.siri.jscore.js;

import ws.siri.jscore.runtime.ClassMarkers.LangDef;
import ws.siri.jscore.runtime.ClassMarkers.LangSpecificModule;
import ws.siri.jscore.runtime.Module;

public class JsLangDef implements LangDef {
    @Override
    public String id() {
        return "js";
    }

    @Override
    public String[] exts() {
        return new String[] { "js" };
    }

    @Override
    public LangSpecificModule wrapModule(Module module) {
        return new JsModule(module);
    }
}
