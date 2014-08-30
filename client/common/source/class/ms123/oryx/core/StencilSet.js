/**
 * Copyright (c) 2006
 * Martin Czuchra, Nicolas Peters, Daniel Polak, Willi Tscheschner
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 **/

/**
	* @ignore(Hash)
*/

qx.Class.define("ms123.oryx.core.StencilSet", {
	type: "static",
	/******************************************************************************
	 STATICS
	 ******************************************************************************/
	statics: {
		_stencilSetsByNamespace: new Hash(),

		//storage for stencil sets by url
		_stencilSetsByUrl: new Hash(),

		//storage for stencil set namespaces by editor instances
		_StencilSetNSByEditorInstance : new Hash(),

		//storage for rules by editor instances
		_rulesByEditorInstance: new Hash(),

		/**
		 * 
		 * param {String} editorId
		 * 
		 * @return {Hash} Returns a hash map with all stencil sets that are loaded by
		 * 					the editor with the editorId.
		 */
		stencilSets: function (editorId) {
			var stencilSetNSs = this._StencilSetNSByEditorInstance[editorId];
			var stencilSets = new Hash();
			if (stencilSetNSs) {
				stencilSetNSs.each(function (stencilSetNS) {
					var stencilSet = ms123.oryx.core.StencilSet.stencilSet(stencilSetNS)
					stencilSets[stencilSet.namespace()] = stencilSet;
				});
			}
			return stencilSets;
		},

		/**
		 * 
		 * param {String} namespace
		 * 
		 * @return {this.StencilSet} Returns the stencil set with the specified
		 * 										namespace.
		 * 
		 * The method can handle namespace strings like
		 *  http://www.example.org/stencilset
		 *  http://www.example.org/stencilset#
		 *  http://www.example.org/stencilset#ANode
		 */
		stencilSet: function (namespace) {
			var splitted = namespace.split("#", 1);
			if (splitted.length === 1) {
				return this._stencilSetsByNamespace[splitted[0] + "#"];
			} else {
				return undefined;
			}
		},

		/**
		 * 
		 * param {String} id
		 * 
		 * @return {this.Stencil} Returns the stencil specified by the id.
		 * 
		 * The id must be unique and contains the namespace of the stencil's stencil set.
		 * e.g. http://www.example.org/stencilset#ANode
		 */
		stencil: function (id) {
			var ss = this.stencilSet(id);
			if (ss) {
				return ss.stencil(id);
			} else {

				return undefined;
			}
		},

		/**
		 * 
		 * param {String} editorId
		 * 
		 * @return {this.Rules} Returns the rules object for the editor
		 * 									specified by its editor id.
		 */
		rules: function (editorId) {
			if (!this._rulesByEditorInstance[editorId]) {
				this._rulesByEditorInstance[editorId] = new ms123.oryx.core.stencilset.Rules();;
			}
			return this._rulesByEditorInstance[editorId];
		},

		/**
		 * 
		 * param {String} url
		 * param {String} editorId
		 * 
		 * Loads a stencil set from url, if it is not already loaded.
		 * It also stores which editor instance loads the stencil set and 
		 * initializes the Rules object for the editor instance.
		 */
		clearStencilSet: function (url) {
			this._stencilSetsByUrl[url]=null;
		},
		loadStencilSet: function (url, editorId) {
			var stencilSet = this._stencilSetsByUrl[url];

			if (!stencilSet) {
				//load stencil set
				stencilSet = new ms123.oryx.core.stencilset.StencilSet(url);

				//store stencil set
				this._stencilSetsByNamespace[stencilSet.namespace()] = stencilSet;

				//store stencil set by url
				this._stencilSetsByUrl[url] = stencilSet;
			}

			var namespace = stencilSet.namespace();

			//store which editorInstance loads the stencil set
			if (this._StencilSetNSByEditorInstance[editorId]) {
				this._StencilSetNSByEditorInstance[editorId].push(namespace);
			} else {
				this._StencilSetNSByEditorInstance[editorId] = [namespace];
			}

			//store the rules for the editor instance
			if (this._rulesByEditorInstance[editorId]) {
				this._rulesByEditorInstance[editorId].initializeRules(stencilSet);
			} else {
				var rules = new ms123.oryx.core.stencilset.Rules();
				rules.initializeRules(stencilSet);
				this._rulesByEditorInstance[editorId] = rules;
			}
			return stencilSet;
		},

		/**
		 * Returns the translation of an attribute in jsonObject specified by its name
		 * according to navigator.language
		 */
		getTranslation: function (jsonObject, name) {
			//var lang = ms123.oryx.Translation.Language.toLowerCase();

			var m = qx.locale.Manager.tr("ss.process."+name);
			if( m != "ss.process."+name){
				return m;
			}
			var lang = ms123.config.ConfigManager.getLanguage();
			var result = jsonObject[name + "_" + lang];
			if (result) return result;
			return jsonObject[name];
		}
	}
});
