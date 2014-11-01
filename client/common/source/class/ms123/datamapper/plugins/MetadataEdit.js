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
 * @ignore(Clazz.extend)
 */

qx.Class.define("ms123.datamapper.plugins.MetadataEdit", {
	extend: qx.core.Object,
 include : [ qx.locale.MTranslation],
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (facade,context) {
		this.base(arguments);
		this._facade = facade;
		this._side = context.side;
		this._tree = context.tree;

		var ec_msg = this.tr("datamapper.metadata_edit");
		var group = "3";
		this._facade.offer({
			name: ec_msg,
			description: ec_msg,
			icon: "icon/16/actions/document-revert.png",
			functionality: this.edit.bind(this),
			group: group,
			isEnabled: qx.lang.Function.bind(function () {
				return true;
			}, this),
			index: 0
		});


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
		edit:function(){
			this._window = this._createWindow("MetaData");
			this._window.setLayout(new qx.ui.layout.Dock());

			var ts = new ms123.datamapper.create.FormatSelector(this._facade,this._side,true);
			ts.selectFormat(this._tree.getModel().getFormat());
			var container = this._setParentSpec( ts, this._side );
			this._window.add(container, {
				edge: "center"
			});
			var buttons= this._createButtons();
			this._window.add(buttons, {
				edge: "south"
			});
			this._ts = ts;
			ts.addListener("formatChanged", function (ev) {
				this._newFormat = ev.getData();
				if( this._newFormat == ms123.datamapper.Config.FORMAT_CSV || this._newFormat == ms123.datamapper.Config.FORMAT_FW){
					ms123.form.Dialog.alert(this.tr("datamapper.format_not_possible")+":"+this._newFormat);
					return;	
				}
				this.executeCommandChangeFormat(this._newFormat, this._tree.getModel().getFormat());	
				this._buttonOk.setEnabled(true);
				console.log("MetadataEdit.newFormat:"+this._newFormat);
			}, this);
			this._window.open();
		},
		_setParentSpec: function (ts,side) {
			if( side == ms123.datamapper.Config.INPUT){
				return ts;
			}
			var output = this._facade.getConfig().output;
			var relList = this._getRelations(output.cleanName);


			var parentSpecRelationSelect = new qx.ui.form.SelectBox().set({
				width:150
			});
			this._relationSelect = parentSpecRelationSelect;
			var listItem = new qx.ui.form.ListItem("------",null,"-");
			parentSpecRelationSelect.add(listItem);
			//listItem = new qx.ui.form.ListItem(this.tr("datamapper.merge_with_base_objects"),null,"merge");
			//parentSpecRelationSelect.add(listItem);
			for(var i=0; i < relList.length; i++){
				var relMap = relList[i];
				var leftfield = relMap.leftfield || this._baseName(relMap.rightmodule);
				listItem = new qx.ui.form.ListItem(this.tr(relMap.leftmodule)+","+leftfield,null,relMap.leftmodule+","+leftfield);
				parentSpecRelationSelect.add(listItem);
			}

			var container = new qx.ui.container.Composite(new qx.ui.layout.VBox(8));
			container.add(ts);

	    var parentSpecRelationLabel = new qx.ui.basic.Label(this.tr("datamapper.parent_relation")).set({
				rich:true
			});
	    var parentSpecLookupLabel = new qx.ui.basic.Label(this.tr("datamapper.parent_lookup")).set({
				rich:true
			});
			container.add(parentSpecRelationLabel);
			container.add(parentSpecRelationSelect);
			container.add(parentSpecLookupLabel);
		  var tf = new qx.ui.form.TextField(this._parentSpecLookupValue);
			this._parentSpecLookupTextField = tf;
			container.add(tf);

			parentSpecRelationSelect.addListener("changeSelection",function(e){ //@@@MS not ready yet
				var sel = parentSpecRelationSelect.getSelection()[0].getModel();
				console.log("data:",sel);
				if( sel.match(/^-/)){
					parentSpecLookupLabel.exclude();
					tf.exclude();
				}else{
					parentSpecLookupLabel.show();
					tf.show();
				}
			},this)

			var parentSpec = this._facade.getConfig().output.parentSpec;
			if( parentSpec && parentSpec.lookup && parentSpec.relation ){
				var selectables = parentSpecRelationSelect.getSelectables();
				selectables.each( function( s ){
					if( s.getModel()== parentSpec.relation){
						parentSpecRelationSelect.setSelection([s]);
					}
				});
				this._parentSpecLookupTextField.setValue(parentSpec.lookup);
			}
			return container;
		},
		executeCommandChangeFormat: function (newFormat, oldFormat) {
			var self = this;
			var CommandClass = Clazz.extend({
				construct: function (newFormat, oldFormat) {
					this.newFormat = newFormat;
					this.oldFormat = oldFormat;
				},
				execute: function () {
					self._tree.setFormat(this.newFormat);
					self._tree.getModel().setFormat(this.newFormat);
					self._tree.getModel().setIcon(ms123.datamapper.BaseTree.getIconFromFormat(this.newFormat));
				},
				rollback: function () {
					self._tree.setFormat(this.oldFormat);
					self._tree.getModel().setFormat(this.oldFormat);
					self._tree.getModel().setIcon(ms123.datamapper.BaseTree.getIconFromFormat(this.oldFormat));
				}
			})
			var command = new CommandClass(newFormat, oldFormat);
			this._facade.executeCommands([command]);
			this._facade.update();
		},
		executeCommandChangeParentSpec: function (newParentSpec, oldParentSpec) {
			var self = this;
			var CommandClass = Clazz.extend({
				construct: function (newParentSpec, oldParentSpec) {
					this.newParentSpec = newParentSpec;
					this.oldParentSpec = oldParentSpec;
				},
				execute: function () {
					self._tree.setFormat(this.newParentSpec);
					self._tree.getModel().setParentSpec(this.newParentSpec);
				},
				rollback: function () {
					self._tree.setFormat(this.oldParentSpec);
					self._tree.getModel().setParentSpec(this.oldParentSpec);
				}
			})
			var command = new CommandClass(newParentSpec, oldParentSpec);
			this._facade.executeCommands([command]);
			this._facade.update();
		},
		_handleOkButton: function (e) {
			if( this._window) this._window.close();
			var relation = this._relationSelect.getSelection()[0].getModel();
			var lookup = this._parentSpecLookupTextField.getValue();
			var newParentSpec = null;
			if( !relation.match(/^--/) && lookup != ""){
				newParentSpec = {
					relation:relation,
					lookup:lookup
				}
			}
			console.log("parentSpec:"+JSON.stringify(newParentSpec,null,2));
			this.executeCommandChangeParentSpec(newParentSpec, this._tree.getModel().getParentSpec());	
		},
		_createButtons: function () {
			var toolbar = new qx.ui.toolbar.ToolBar();
			toolbar.setSpacing(5);
			toolbar.addSpacer();
			toolbar.addSpacer();

			var buttonOk = new qx.ui.toolbar.Button(this.tr("Ok"), "icon/16/actions/dialog-ok.png");
			buttonOk.addListener("execute", function (e) {
				this._handleOkButton(e);
			}, this);
			toolbar._add(buttonOk)
			this._buttonOk = buttonOk;
			this._buttonOk.setEnabled(true);

			var buttonCancel = new qx.ui.toolbar.Button(this.tr("Cancel"), "icon/16/actions/dialog-close.png");
			buttonCancel.addListener("execute", function () {
				if( this._window)this._window.close();
			}, this);
			//toolbar._add(buttonCancel)

			return toolbar;
		},
		_getRelations: function (thisEntity) {
			var completed = (function (data) {}).bind(this);

			var failed = (function (details) {
				ms123.form.Dialog.alert(this.tr("entitytypes.getRelations_failed") + ":" + details.message);
			}).bind(this);

			try {
				var storeId = this._facade.storeDesc.getStoreId();
				var ret = ms123.util.Remote.rpcSync("entity:getRelations", {
					storeId: this._getStoreId()
				});
				console.log("rel:"+JSON.stringify(ret,null,2));
				var relList = [];
				for( var i = 0; i< ret.length;i++){
					var r = ret[i];
					if( r.rightmodule == "data."+thisEntity){
						relList.push( r );
					}
					
				}
				console.log("rel:"+JSON.stringify(relList,null,2));
				return relList;
			} catch (e) {
				//failed.call(this,e);
				return [];
			}
		},
		_baseName:function(s){
			var dot = s.lastIndexOf(".");
			return dot == -1 ? s : s.substring(dot+1);
		},
		_getStoreId:function(){
			var storeId = this._facade.storeDesc.getStoreId();
			return storeId;
		},
		_createWindow: function (name) {
			var win = new qx.ui.window.Window(name, "").set({
				resizable: true,
				useMoveFrame: true,
				useResizeFrame: true
			});
			win.setLayout(new qx.ui.layout.Dock);
			win.setWidth(450);
			win.setHeight(400);
			win.setAllowMaximize(false);
			win.setAllowMinimize(false);
			win.setModal(true);
			win.setActive(false);
			win.minimize();
			win.center();
			return win;
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
