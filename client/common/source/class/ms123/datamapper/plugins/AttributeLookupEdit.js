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

qx.Class.define("ms123.datamapper.plugins.AttributeLookupEdit", {
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

			var Item = Clazz.extend({
				construct: function (context) {
					this._context = context;
				},
				id: function () {
					return this._context.id;
				},
				width: function () {
					return this._context.width;
				},
				name: function () {
					return this._context.name;
				},
				config: function () {
					return this._context.config;
				},
				type: function () {
					return this._context.type;
				},
				value: function () {
					return this._context.value;
				},
				optional: function () {
					return this._context.optional;
				}
			})
			var items = [
				new Item({
					"id": "param",
					"name": "Filterparam",
					"type": "label",
					"value": "",
					"width": 50,
					"optional": false
				}),
				new Item({
					"id": "expr",
					"name": "Expression",
					"type": "string",
					"value": "",
					"width": 200,
					"optional": false
				})
]

			var config=  {helperTree:["sw.filter"], kind:"filterboth"};
			this._lookupSelector = new ms123.datamapper.plugins.LookupFilterSelector(config,"Lookup filter",items,"key", this._facade);
			this._lookupSelector.addListener('changeValue', function (e) {
				if (this._model == null || this._internalSetValue === true) return;
				this.executeCommandLookupChange(e.getData(), this._mapping, this._model);
			}, this);
			this.add(this._lookupSelector, {
				edge: "center"
			});
		},
		_selectionChanged: function () {
			this._cleanLookups();
			var selItem = this._tree.getSelection()[0];
			if (selItem == null) {
				this._handleAttributeLookup(null, null);
				return;
			}
			var m = this._facade.mappingEdit.isMapped(selItem.getModel().getId(), ms123.datamapper.Config.OUTPUT);
			if (m.attrMappings && m.attrMappings.length > 0) {
				this._handleAttributeLookup(m, selItem.getModel());
			} else {
				this._handleAttributeLookup(null, null);
			}
		},
		_handleAttributeLookup: function (mapping, model) {
			this._internalSetValue = true;
			this._model = model;
			this._mapping = mapping;
			if (model == null) {
				this._lookupSelector.setValue("");
				this._lookupSelector.setEnabled(false);
			} else {
				this._lookupSelector.setEnabled(true);
				console.log("model:",model);
				console.log("mapping:",mapping);
				var lookup = this._getLookup(model,mapping.sid);
				if( lookup == null){
					lookup = this._getLookup(model,"default");
				}
				if( lookup == null){
					lookup = "";
				}
				this._lookupSelector.setValue(lookup);
			}
			this._internalSetValue = false;
		},
		executeCommandLookupChange: function (lookup, mapping, model) {
			var self = this;
			var CommandClass = Clazz.extend({
				construct: function (lookup, mapping, model) {
					this.lookup = lookup;
					this.model = model;
					this.mapping = mapping;
					console.log("Mapping1:",mapping);
				},
				execute: function () {
					self._internalSetValue = true;
					this.oldLookup = self._getLookup(this.model,this.mapping.sid);
					self._setLookup(this.model,this.mapping.sid,this.lookup);
					self._lookupSelector.setValue(this.lookup);
					self._tree.setModelSelection(this.model);
					self._internalSetValue = false;
					self._cleanLookups();
				},
				rollback: function () {
					self._internalSetValue = true;
					self._setLookup(this.model,this.mapping.sid,this.oldLookup);
					self._lookupSelector.setValue(this.oldLookup);
					self._tree.setModelSelection(this.model);
					self._internalSetValue = false;
					self._cleanLookups();
				},
				toString: function () {
					return "LookupChange";
				}
			})
			var command = new CommandClass(lookup, mapping, model);
			this._facade.executeCommands([command]);
			this._facade.update();
		},
		_getLookup:function(model, sid){
			var lookups =  model.getLookups();
			for( var i=0; i < lookups.getLength(); i++){
				var item = lookups.getItem(i);
				if( item.getId() == sid){
					return item.getLookup();
				}
			}
			return null;
		},
		_setLookup:function(model, sid, lookup){
			var lookups =  model.getLookups();
			for( var i=0; i < lookups.getLength(); i++){
				var item = lookups.getItem(i);
				if( item.getId() == sid){
					item.setLookup(lookup);
					return;
				}
			}
			var m = qx.data.marshal.Json.createModel({id:sid,lookup:lookup}, true);
			lookups.push(m);
		},
		_cleanLookups:function(){
		  this._traverseTree(this._tree.getModel());
		},
		_traverseTree:function(model){
			if( model.getType() == ms123.datamapper.Config.NODETYPE_ATTRIBUTE){
				var lookups =  model.getLookups();
				for( var i=0; i < lookups.getLength(); i++){
					var item = lookups.getItem(i);
					if( !this._isConnected(item.getId())){
						lookups.remove(item);
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
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
