package ws.siri.jscore.js;

import org.graalvm.polyglot.Context;

import ws.siri.jscore.runtime.LangDef;
import ws.siri.jscore.runtime.LangSpecificModule;
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

	@Override
	public void prepare(Context ctx, LangSpecificModule module) {
        ctx.getBindings(this.id()).putMember("module", module);
	}
}
