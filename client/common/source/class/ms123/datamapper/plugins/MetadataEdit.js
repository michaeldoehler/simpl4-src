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
			this._window.add(ts, {
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
		_handleOkButton: function (e) {
			if( this._window) this._window.close();
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
