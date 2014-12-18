import org.grooscript.jquery.GQuery
import org.grooscript.jquery.GQueryImpl
import org.grooscript.asts.GsNative


class Simpl4Input{
  public Simpl4Input(){
		def definition = [
			publish: [
				label: 'XXX',
				floatingLabel: true,
				disabled: [value: false, reflect: true],
				value: '',
				committedValue: ''
			],
			valueChanged: {
				def t = XPolymer.getThis();
				println("valueChanged1:"+t.value);
				t.$.decorator.updateLabelVisibility(t.value);
			},
			changeAction: {
				def t = GPolymer.getThis();
				println("changeAction:"+t.committedValue);
				t.fire('change', null, t);
			}
		]
		XPolymer.setup("simpl4-input", definition);
	}
}

//new GQueryImpl().onReady { new Simpl4Input(); }
class GPolymer{
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

