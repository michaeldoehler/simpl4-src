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
/*
*/
qx.Class.define("ms123.searchfilter.MenuButton", {
	extend: qx.ui.form.MenuButton,


	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (params) {
		this.params = params;
		this.base(arguments);
		this.createMenu();
	},

	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {
		labelx: {
			check: "String",
			init: "",
			event: "changeName"
		}
	},
	events: {
		"change": "qx.event.type.Data"
	},

	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		setDetails: function (controller, item, id) {
			this.controller = controller;
			this.item = item;
			this.id = id;
			//console.log("controller:" + controller + ",item:" + item + ",id:" + id);
			if( this.id.getLabel() == "1" || this.id.getLabel() == "2" ){
				this.changeButton.setEnabled( false );
				this.insertParentButton.setEnabled( false );
				this.delButton.setEnabled( false );
				this.changeLabelButton.setEnabled( false );
			}
			if (this.id.getConnector()) {
				this.changeButton.setEnabled( false );
				this.insertParentButton.setEnabled( false );
				this.changeLabelButton.setEnabled( false );
			}
			if( this.id.getLabel() == " " ){
				this.changeButton.setEnabled( false );
				this.addButton.setEnabled( false );
				this.delButton.setEnabled( false );
				this.insertParentButton.setEnabled( false );
				this.changeLabelButton.setEnabled( false );
			}
		},
		setModel: function (model) {
			this.model = model;
		},
		createMenu: function () {
			// create main menu and buttons
			var menu = new qx.ui.menu.Menu();

			this.addButton = new qx.ui.menu.Button(this.tr("filter.text.new_condition"), "icon/16/actions/list-add.png");
			this.addExistsSubSelectButton = new qx.ui.menu.Button(this.tr("filter.text.new_exists_subselect"), "icon/16/actions/list-add.png");
			this.changeButton = new qx.ui.menu.Button(this.tr("filter.text.change_to_compound"), "icon/16/actions/insert-link.png");
			this.insertParentButton = new qx.ui.menu.Button(this.tr("filter.text.insert_parent"), "icon/16/actions/edit-undo.png");
			this.delButton = new qx.ui.menu.Button(this.tr("filter.text.del_condition"), "icon/16/actions/edit-cut.png");
			this.changeLabelButton = new qx.ui.menu.Button(this.tr("filter.text.change_label"), "icon/16/apps/utilities-text-editor.png");



			// add execute listeners
			this.addButton.addListener("execute", function () {
				var newNode = new ms123.searchfilter.Node();
				if (this.id.getConnector()) {
					var children = this.id.getChildren();
					children.push(newNode);
					this.item.setOpen(true);
				} else {
					var p = this.getParentModel(this.id);
					var children = p.getChildren();
					
					p.getChildren().push(newNode);
				}
				this.renameModelLabel( this.model, "1" );
				this.fireDataEvent("change", this.model, null);
			}, this);

			this.addExistsSubSelectButton.addListener("execute", function () {
				var newNode = new ms123.searchfilter.Node();
				newNode.setField("_exists_subselect");	
				if (this.id.getConnector()) {
					var children = this.id.getChildren();
					children.push(newNode);
					this.item.setOpen(true);
				} else {
					var p = this.getParentModel(this.id);
					var children = p.getChildren();
					
					p.getChildren().push(newNode);
				}
				this.renameModelLabel( this.model, "1" );
				this.fireDataEvent("change", this.model, null);
			}, this);

			this.changeButton.addListener("execute", function () {
				var p = this.getParentModel(this.id);
				var children = p.getChildren();
				for( var i=0; i < children.getLength(); i++){
					var id = children.getItem(i);
					if( id == this.id ){
							children.removeAt( i);
							var newNode = new ms123.searchfilter.Node();
							newNode.setConnector("and");	
							children.insertAt(i, newNode );
							newNode.getChildren().push( id );
							break;
					}
				}
				this.renameModelLabel( this.model, "1" );
				this.fireDataEvent("change", this.model, null);
			}, this);

			this.insertParentButton.addListener("execute", function () {
				var p = this.getParentModel(this.id);
				var children = p.getChildren();
				var newNode = new ms123.searchfilter.Node();
				newNode.setConnector("and");	
				for( var i=0; i < children.getLength(); i++){
					var id = children.getItem(i);
					newNode.getChildren().push( id );
				}
				children.removeAll();
				children.push(newNode );
				this.renameModelLabel( this.model, "1" );
				this.fireDataEvent("change", this.model, null);
			}, this);

			this.delButton.addListener("execute", function () {
				var p = this.getParentModel(this.id);
				var children = p.getChildren();
				for( var i=0; i < children.getLength(); i++){
					var id = children.getItem(i);
					if( id == this.id ){
							children.removeAt( i);
							break;
					}
				}
				this.renameModelLabel( this.model, "1" );
				this.fireDataEvent("change", this.model, null);
			}, this);
			this.changeLabelButton.addListener("execute", function () {

				console.log("Label:"+this.id.getLabel());
				//this.id.setLabel( "X"+this.id.getLabel());
				this.__changeLabelName( this.id );
				this.fireDataEvent("change", this.model, null);
			}, this);

			// add buttons to menu
			menu.add(this.addButton);
			menu.add(this.changeButton);
			menu.add(this.insertParentButton);
			menu.add(this.delButton);
			menu.add(this.addExistsSubSelectButton);
			menu.add(this.changeLabelButton);

			this.setMenu(menu);
		},
		renameModelLabel: function (model, label) {
			if( model.getConnector() == 'union' || model.getConnector() == 'except' || model.getConnector() == 'intersect' ){
				var children = model.getChildren();
				for( var i=0; i < children.getLength(); i++ ){
					this._renameModelLabel( children.getItem(i), (i+1)+"" );
				}
			}else{
				this._renameModelLabel( model, "1" );
			}
		},
		_renameModelLabel: function (model, label) {
			var oLabel = model.getLabel();
			if( oLabel && oLabel.toLowerCase().match("^[a-z]")){
				console.log("Label starts with letter");
			}else{
				model.setLabel(label);
			}
			model.setNodeName(label);
			var children = model.getChildren();
			if (children.getLength() > 0) {
				for (var i = 0; i < children.getLength(); i++) {
					this._renameModelLabel(children.getItem(i), label+"."+(i+1));
				}
			}
			return null;
		},
		findModel: function (model, label) {
			if (model.getLabel() == label) return model;
			var children = model.getChildren();
			if (children.getLength() > 0) {
				for (var i = 0; i < children.getLength(); i++) {
					var m = this.findModel(children.getItem(i), label);
					if (m != null) return m;
				}
			}
			return null;
		},
		getParentModel: function (id) {
			var index = id.getNodeName().lastIndexOf(".");
			var parentLabel = id.getNodeName().substring(0, index);
			var p = this.findModel(this.model, parentLabel);
			return p;
		},
		__changeLabelName: function () {
			var formData = {
				"name": {
					'type': "TextField",
					'label': this.tr("filter.text.new_label_name"),
					'validation': {
						required: true,
						validator: "/^[A-Za-z]([0-9A-Za-z_]){2,32}$/"
					},
					'value': ""
				}
			};

			var _this = this;
			var form = new ms123.form.Form({
				"formData": formData,
				"allowCancel": true,
				"inWindow": true,
				"callback": function (m) {
					if (m !== undefined) {
						var val = m.get("name");
						_this.id.setLabel( val );
					}
				},
				"context": _this
			});
			form.show();
		}
	}
});
