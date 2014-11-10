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
			var container = this._setPersistenceSpecification( ts, this._side );
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
		_setPersistenceSpecification: function (ts,side) {
			if( side == ms123.datamapper.Config.INPUT){
				return ts;
			}
			var output = this._facade.getConfig().output;
			var relList = this._getRelations(output.cleanName);


			var persistenceSpecificationRelationSelect = new qx.ui.form.SelectBox().set({
				width:150
			});
			this._relationSelect = persistenceSpecificationRelationSelect;
			var listItem = new qx.ui.form.ListItem("------",null,"-");
			persistenceSpecificationRelationSelect.add(listItem);
			for(var i=0; i < relList.length; i++){
				var relMap = relList[i];
				var leftfield = relMap.leftfield || this._baseName(relMap.rightmodule);
				listItem = new qx.ui.form.ListItem(this.tr(relMap.leftmodule)+","+leftfield,null,relMap.leftmodule+","+leftfield);
				persistenceSpecificationRelationSelect.add(listItem);
			}

			var container = new qx.ui.container.Composite(new qx.ui.layout.VBox(8));
			container.add(ts);

	    var persistenceSpecificationRelationLabel = new qx.ui.basic.Label(this.tr("datamapper.parent_relation")).set({
				rich:true
			});
	    var persistenceSpecificationLookupLabel = new qx.ui.basic.Label(this.tr("datamapper.parent_lookup")).set({
				rich:true
			});
	    var delim = new qx.ui.basic.Label("<hr style='width:500px;'/>").set({
				rich:true
			});
	    var persistenceSpecificationUpdateLabel = new qx.ui.basic.Label(this.tr("datamapper.parent_update")).set({
				rich:true
			});
	    var persistenceSpecificationUpdateExprLabel = new qx.ui.basic.Label(this.tr("datamapper.parent_update_expr")).set({
				rich:true
			});
			container.add(persistenceSpecificationRelationLabel);
			container.add(persistenceSpecificationRelationSelect);
			container.add(persistenceSpecificationLookupLabel);
		  var tf = new qx.ui.form.TextField();
			this._persistenceSpecificationLookupTextField = tf;
			container.add(tf);
			container.add(delim);

			var persistenceSpecification = this._facade.getConfig().output.persistenceSpecification;
			this._persistenceSpecificationUpdateCheckBox = new qx.ui.form.CheckBox();
			this._persistenceSpecificationUpdateCheckBox.setValue(persistenceSpecification.lookupUpdateObjectExpr != null);
			container.add(persistenceSpecificationUpdateLabel);
			container.add(this._persistenceSpecificationUpdateCheckBox);
		  this._persistenceSpecificationUpdateTextField = new qx.ui.form.TextField();
			if( persistenceSpecification.lookupUpdateObjectExpr != null){
				this._persistenceSpecificationUpdateTextField.setValue( persistenceSpecification.lookupUpdateObjectExpr);
			}else{
				this._persistenceSpecificationUpdateTextField.exclude();
				persistenceSpecificationUpdateExprLabel.exclude();
			}
			container.add(persistenceSpecificationUpdateExprLabel);
			container.add(this._persistenceSpecificationUpdateTextField);

			persistenceSpecificationRelationSelect.addListener("changeSelection",function(e){ 
				var sel = persistenceSpecificationRelationSelect.getSelection()[0].getModel();
				console.log("data:",sel);
				if( sel.match(/^-/)){
					persistenceSpecificationLookupLabel.exclude();
					tf.exclude();
				}else{
					persistenceSpecificationLookupLabel.show();
					tf.show();
				}
			},this)

			this._persistenceSpecificationUpdateCheckBox.addListener("changeValue",function(e){ 
				var update = this._persistenceSpecificationUpdateCheckBox.getValue();
				if( update ){
					this._persistenceSpecificationUpdateTextField.show();
					persistenceSpecificationUpdateExprLabel.show();
				}else{
					this._persistenceSpecificationUpdateTextField.exclude();
					persistenceSpecificationUpdateExprLabel.exclude();
				}
			},this)

			if( persistenceSpecification && persistenceSpecification.lookupRelationObjectExpr && persistenceSpecification.relation ){
				var selectables = persistenceSpecificationRelationSelect.getSelectables();
				selectables.each( function( s ){
					if( s.getModel()== persistenceSpecification.relation){
						persistenceSpecificationRelationSelect.setSelection([s]);
					}
				});
				this._persistenceSpecificationLookupTextField.setValue(persistenceSpecification.lookupRelationObjectExpr);
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
		executeCommandChangePersistenceSpecification: function (newPersistenceSpecification, oldPersistenceSpecification) {
			var self = this;
			var CommandClass = Clazz.extend({
				construct: function (newPersistenceSpecification, oldPersistenceSpecification) {
					this.newPersistenceSpecification = newPersistenceSpecification;
					this.oldPersistenceSpecification = oldPersistenceSpecification;
				},
				execute: function () {
					self._tree.setFormat(this.newPersistenceSpecification);
					self._tree.getModel().setPersistenceSpecification(this.newPersistenceSpecification);
				},
				rollback: function () {
					self._tree.setFormat(this.oldPersistenceSpecification);
					self._tree.getModel().setPersistenceSpecification(this.oldPersistenceSpecification);
				}
			})
			var command = new CommandClass(newPersistenceSpecification, oldPersistenceSpecification);
			this._facade.executeCommands([command]);
			this._facade.update();
		},
		_handleOkButton: function (e) {
			if( this._window) this._window.close();
			var update = this._persistenceSpecificationUpdateCheckBox.getValue();
			var updateValue=null;
			if( update ){
				updateValue = this._persistenceSpecificationUpdateTextField.getValue();
				if( updateValue && updateValue.trim()==="") updateValue=null;
			}
			var relation = this._relationSelect.getSelection()[0].getModel();
			var lookup = this._persistenceSpecificationLookupTextField.getValue();
			var newPersistenceSpecification = null;
			if( !relation.match(/^--/) && lookup != ""){
				newPersistenceSpecification = {
					relation:relation,
					lookupUpdateObjectExpr:updateValue,
					lookupRelationObjectExpr:lookup
				}
			}
			console.log("persistenceSpecification:"+JSON.stringify(newPersistenceSpecification,null,2));
			this.executeCommandChangePersistenceSpecification(newPersistenceSpecification, this._tree.getModel().getPersistenceSpecification());	
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
			win.setWidth(500);
			win.setHeight(500);
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
