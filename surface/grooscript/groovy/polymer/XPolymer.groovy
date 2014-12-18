import org.grooscript.asts.GsNative
import org.grooscript.jquery.GQuery
import org.grooscript.jquery.GQueryImpl

static class XPolymer{
	@GsNative
	def static setup(name, map) {
		/*
		 Polymer(name, map);
		 */
	}
	@GsNative
	def static getThis() {
		/*
		 return this;
		 */
	}
}

