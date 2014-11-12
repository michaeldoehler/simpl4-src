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
 * @ignore(Hash)
 * @ignore(Clazz)
 * @ignore(jQuery)
 * @ignore(jsPlumb.*)
 * @ignore(Clazz.extend)
 */

qx.Class.define("ms123.datamapper.plugins.AttributeScriptEdit", {
	extend: qx.ui.container.Composite,
	include: [qx.locale.MTranslation],
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (facade, config) {
		this.base(arguments);
		this._facade = facade;
		this._config = config;
		this._tree = this._facade.outputTree;

		this._facade.registerOnEvent(ms123.datamapper.Config.EVENT_MAPPING_CHANGED, this._selectionChanged.bind(this));
		this._tree.getTree().addListener("changeSelection", function (e) {
			this._selectionChanged()
		}, this);
		this._init();

	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},

	/******************************************************************************
	 STATICS
	 ******************************************************************************/
	statics: {},
	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		_init: function () {
			var layout = new qx.ui.layout.Dock();
			this.setLayout(layout);

			this._scriptArea = new qx.ui.form.TextArea();
			this._scriptArea.setMinimalLineHeight(2);
			this._scriptArea.addListener("input", function (e) {
				if (this._model == null || this._internalSetValue === true) return;
				this.executeCommandScriptChange(e.getData(), this._mapping, this._model);
			}, this);
			this.add(this._scriptArea, {
				edge: "center"
			});


			var defArea = new qx.ui.container.Composite(new qx.ui.layout.VBox(0));
			this._defScript = new qx.ui.form.TextArea()
			this._defScript.setReadOnly(true);
			this._defScript.setEnabled(false);
			this._defScript.setSelectable(true);
			this._defScript.setMinimalLineHeight(1);

			this._defButton = new qx.ui.form.Button(this.tr("datamapper.default_script"), "icon/16/actions/insert-text.png");
			defArea.add(this._defButton);
			defArea.add(this._defScript);
			this.add(defArea, {
				edge: "south"
			});

			this._defButton.addListener("execute", function (e) {
				var str = this._constructScriptString();
				if (this._scriptArea.getValue() != str) {
					this.executeCommandScriptChange(str, this._mapping, this._model);
				}
			}, this);
			this._defButton.setEnabled(false);
		},
		_constructScriptString: function () {
			var attrMappings = this._mapping.attrMappings;
			var str = "";
			var delim = "";
			for (var i = 0; i < attrMappings.length; i++) {
				var m = attrMappings[i];
				var imodel = this._facade.mappingEdit.getTreeItemById(m.input.id, ms123.datamapper.Config.INPUT).getModel();
				var omodel = this._facade.mappingEdit.getTreeItemById(m.output.id, ms123.datamapper.Config.OUTPUT).getModel();
				var conv = this._getConverterName(imodel.getFieldType(), omodel.getFieldType());
				var format = omodel.getFieldFormat();
				if (format) {
					format = ",'" + format + "'";
				} else {
					format = "";
				}
				if (conv != null) {
					str += delim + " " + conv + "(" + imodel.getCleanName() + format + ")";
				} else {
					str += delim + imodel.getCleanName();
				}
				delim = "+";
			}
			return str;
		},
		_selectionChanged: function () {
			this._cleanScripts();
			var selItem = this._tree.getSelection()[0];
			if (selItem == null) {
				this._handleAttributeScript(null, null);
				this._defScript.setValue("");
				this._defScript.setEnabled(false);
				return;
			}
			var m = this._facade.mappingEdit.isMapped(selItem.getModel().getId(), ms123.datamapper.Config.OUTPUT);
			if (m.attrMappings && m.attrMappings.length > 0) {
				this._handleAttributeScript(m, selItem.getModel());
				var defScript = this._constructScriptString();
				this._defScript.setValue(defScript);
				this._defScript.setEnabled(true);
			} else {
				this._handleAttributeScript(null, null);
				this._defScript.setEnabled(false);
				this._defScript.setValue("");
			}
		},
		_handleAttributeScript: function (mapping, model) {
			this._internalSetValue = true;
			this._model = model;
			this._mapping = mapping;
			if (model == null) {
				this._defButton.setEnabled(false);
				this._scriptArea.setValue("");
				this._scriptArea.setEnabled(false);
			} else {
				this._scriptArea.setEnabled(true);
				this._defButton.setEnabled(true);
				console.log("model:",model);
				console.log("mapping:",mapping);
				var script = this._getScript(model,mapping.sid);
				if( script == null){
					script = this._getScript(model,"default");
				}
				if( script == null){
					script = "";
				}
				this._scriptArea.setValue(script);
			}
			this._internalSetValue = false;
		},
		executeCommandScriptChange: function (script, mapping, model) {
			var self = this;
			var CommandClass = Clazz.extend({
				construct: function (script, mapping, model) {
					this.script = script;
					this.model = model;
					this.mapping = mapping;
					console.log("Mapping1:",mapping);
				},
				execute: function () {
					self._internalSetValue = true;
					this.oldScript = self._getScript(this.model,this.mapping.sid);
					self._setScript(this.model,this.mapping.sid,this.script);
					self._scriptArea.setValue(this.script);
					self._tree.setModelSelection(this.model);
					self._internalSetValue = false;
					self._cleanScripts();
				},
				rollback: function () {
					self._internalSetValue = true;
					self._setScript(this.model,this.mapping.sid,this.oldScript);
					self._scriptArea.setValue(this.oldScript);
					self._tree.setModelSelection(this.model);
					self._internalSetValue = false;
					self._cleanScripts();
				},
				toString: function () {
					return "ScriptChange";
				}
			})
			var command = new CommandClass(script, mapping, model);
			this._facade.executeCommands([command]);
			this._facade.update();
		},
		_getScript:function(model, sid){
			var scripts =  model.getScripts();
			for( var i=0; i < scripts.getLength(); i++){
				var item = scripts.getItem(i);
				if( item.getId() == sid){
					return item.getScript();
				}
			}
			return null;
		},
		_setScript:function(model, sid, script){
			var scripts =  model.getScripts();
			for( var i=0; i < scripts.getLength(); i++){
				var item = scripts.getItem(i);
				if( item.getId() == sid){
					item.setScript(script);
					return;
				}
			}
			var m = qx.data.marshal.Json.createModel({id:sid,script:script}, true);
			scripts.push(m);
		},
		_cleanScripts:function(){
		  this._traverseTree(this._tree.getModel());
		},
		_traverseTree:function(model){
			if( model.getType() == ms123.datamapper.Config.NODETYPE_ATTRIBUTE){
				var scripts =  model.getScripts();
				for( var i=0; i < scripts.getLength(); i++){
					var item = scripts.getItem(i);
					if( !this._isConnected(item.getId())){
						scripts.remove(item);
					}
				}
			}
			var children = model.getChildren();
			for(var i=0; i < children.getLength(); i++){
				this._traverseTree(children.getItem(i));
			}
		},
		_isConnected:function(sid){
			var mappings = this._facade.mappingEdit.getMappings();
			var keys = Object.keys(mappings);
			var ok = false;
			keys.each(function (k) {
				var m = mappings[k];
				if( m.id == sid){
					ok = true;
					return;
				}
			});
			return ok;
		},
		_getConverterName: function (itype, otype) {
			var numList = ["byte", "integer", "long", "decimal", "double"];
			console.log("_getConverterName:" + itype + "/" + otype);
			if (itype == otype) return null;
			if (itype == "string") return "str2" + otype;
			if (itype == "boolean" && numList.indexOf(otype) != -1) return "bool2num";
			if (numList.indexOf(itype) != -1 && otype == "string") return "num2str";
			if (numList.indexOf(itype) != -1 && otype == "boolean") return "num2bool";
			if (itype == "date" && numList.indexOf(otype) != -1) return "date2num";
			if (itype == "date" && otype == "string") return "date2str";
			if (itype == "date" && otype == "long") return "date2long";
			if (itype == "date" && otype == "calendar") return "date2calendar";
			if (itype == "calendar" && numList.indexOf(otype) != -1) return "calendar2num";
			if (itype == "calendar" && otype == "string") return "calendar2str";
			if (itype == "calendar" && otype == "long") return "calendar2long";
			if (itype == "decimal" && otype == "double") return "decimal2double";
			if (itype == "decimal" && otype == "long") return "decimal2long";
			if (itype == "decimal" && otype == "integer") return "decimal2integer";
			if (itype == "long" && otype == "date") return "long2date";
			if (itype == "long" && otype == "integer") return "long2integer";
			if (itype == "long" && otype == "calendar") return "long2calendar";

			if (itype == "double" && otype == "long") return "double2long";
			if (itype == "double" && otype == "integer") return "double2integer";
			return null;
		},
		_capitalize: function (s) {
			return s[0].toUpperCase() + s.slice(1);
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
