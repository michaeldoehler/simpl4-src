/*
 * This file is part of SIMPL4(http://simpl4.org).
 *
 * 	Copyright [2014] [Manfred Sattler] <manfred@ms123.org>
 *
 * SIMPL4 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SIMPL4 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SIMPL4.  If not, see <http://www.gnu.org/licenses/>.
 */
/**
	* @ignore(CodeMirror.*)
	* @ignore(CodeMirror.getMode)
	* @ignore(CodeMirror.fromTextArea)
	* @ignore(CodeMirror.simpleHint)
	* @ignore(CodeMirror.javascriptHint)
*/

/**
 * Container for the source code editor.
 */
qx.Class.define("ms123.codemirror.CodeMirror", {
	extend: qx.ui.container.Composite,


	construct: function (context) {
		this.base(arguments);
		this.__mode = context.mode;
		if( this.__mode){
			if( this.__mode.name == "htmlembedded"){
				this.__mode.scriptTypes= [{matches: /^<%/, mode: CodeMirror.getMode("text/x-groovy")}];
			}
		}
		this.__init(context);
	},


	events: {},


	members: {
		__textarea: null,
		__codemirror: null,


		getEditor:function(){
			return this.__codemirror;
		},
		/**
		 */
		__init: function (context) {
			this.setLayout(new qx.ui.layout.Dock());

			this.__textarea = new ms123.codemirror.TextArea().set({
//				wrap: false,
//				font: qx.bom.Font.fromString("14px monospace"),
//				decorator: null,
//				autoSize: true,
//				backgroundColor: "white",
//				padding: [0, 0, 0, 0]
			});
			if (context.helper) {
				context.cmWrapper = this;
				this.__helper = this._createHelper(context);
				var split2 = new ms123.codemirror.Split2(this.__textarea, this.__helper);
				this.add(split2, {
					edge: "center"
				});
				this.__textarea.setDroppable(true);
				this.__textarea.addListener("drop", this.__drop, this);
			} else {
				this.add(this.__textarea, {
					edge: "center"
				});
			}
			qx.html.Element.flush();

			this.__textarea.addListenerOnce("appear", function () {
				this.__onEditorAppear();
				this.__toolbar = new ms123.codemirror.CodeMirrorUI(this.__codemirror, {}, {});
				if (context.toolbarAddon) {
					this._addToToolbar(this.__toolbar,context);
				}
				this.add(this.__toolbar, {
					edge: "north"
				});
			}, this);
		},
		_addToToolbar: function (toolbar, context) {
			if( context.toolbarAddon == "WikiMarkdown"){
				new ms123.codemirror.toolbar.WikiMarkdown(toolbar, this.__codemirror,context);
				return;
			}
			ms123.form.Dialog.alert("CodeMirror.toolbarAddon not found:"+context.toolbarAddon);
		},
		_createHelper: function (context) {
			if( context.helper == "ProcessScript"){
				return new ms123.codemirror.helper.ProcessScript(context);
			}
			if( context.helper == "DocumentMarkdown"){
				return new ms123.codemirror.helper.DocumentMarkdown(context);
			}
			ms123.form.Dialog.alert("CodeMirror.helper not found:"+context.helper);
		},

		__onEditorAppear: function () {
      CodeMirror.commands.autocomplete = function(cm) {
        CodeMirror.showHint(cm, CodeMirror.hint.anyword);
      }
			var container = this.__textarea.getTextArea().getDomElement();
			this.__codemirror = CodeMirror.fromTextArea(container, {
				lineNumbers: true,
				//smartIndent: false,
				matchBrackets: true,
        showCursorWhenSelecting: true,
				mode: this.__mode,
        //extraKeys: {"Ctrl-Space": "autocomplete"},
				vimMode:ms123.config.ConfigManager.isVimMode(),
   			showCursorWhenSelecting: true
			});
			if (this.__value) {
				this.__codemirror.setValue(this.__value);
			}

			this.__setCodeMirrorHeight();
			var self = this;
			this.__textarea.addListener("resize", function () {
				window.setTimeout(function () {
					self.__setCodeMirrorHeight();
				}, 0);
			});
		},

		__setCodeMirrorHeight: function (textAreaElem) {
			var textAreaElem = this.__textarea.getContentElement().getDomElement();
			this.__codemirror.getWrapperElement().style.height = textAreaElem.style.height;
			//this.__codemirror.getGutterElement().style.height = textAreaElem.style.height;
			this.__codemirror.refresh();
		},

		__drop: function (e) {
			var cm = this.__codemirror;
			var target = e.getRelatedTarget();

			var model = target.getUserData("model");
			var code = target.getUserData("code");
			var coords = {
				x: e.getDocumentLeft(),
				y: e.getDocumentTop()
			};
			var pos = cm.coordsChar({
				x: coords.x,
				y: coords.y + 1
			});

			var replaceString = "";
			if (code) {
				replaceString = this.__supplant(code, model);
			} else {
				if( model){
					if( model.getId){
						replaceString = model.getId();
					}else if( model.getValue){
						replaceString = model.getValue();
					}
				}
				try{
					replaceString = target.getValue();
				}catch(e){
					console.log("e:"+e+"/"+target);
				}
			}
			if (cm.somethingSelected()) {
				cm.replaceSelection(replaceString);
			} else {
				cm.replaceRange(replaceString, cm.getCursor());
			}
		},
		__supplant: function (s, o) {
			if (!o) return s;
			return s.replace(/[\\$@]{([^{}]*)}/g, function (a, b) {
				var r = o.get(b);
				return typeof r === 'string' || typeof r === 'number' ? r : a;
			});
		},
		/**
		 * Returns the current set code of the editor.
		 * @return {String} The current set text.
		 */
		getValue: function () {
			if (this.__codemirror) {
				return this.__codemirror.getValue();
			} else {
				return this.__value;
			}
		},


		/**
		 * Sets the given code to the editor.
		 * param code {String} The new code.
		 */
		setValue: function (code) {
			this.__value = code;
			if (this.__codemirror) {
				this.__codemirror.setValue(code);
			}
		}
	},



	/**
	 *****************************************************************************
	 DESTRUCTOR
	 *****************************************************************************
	 */

	destruct: function () {
		this._disposeObjects("__textarea");
		this.__codemirror = null;
	}
});
