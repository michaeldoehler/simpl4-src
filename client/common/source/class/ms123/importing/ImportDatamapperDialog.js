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
	@asset(qx/icon/${qx.icontheme}/16/actions/*)
	@asset(qx/icon/${qx.icontheme}/16/places/*)
	@asset(ms123/icons/*)
	@asset(ms123/*)
*/

qx.Class.define("ms123.importing.ImportDatamapperDialog", {
	extend: qx.core.Object,
	include: qx.locale.MTranslation,

	statics: {
	},

	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (context) {
		this.base(arguments);

		this.__storeDesc = context.storeDesc;
		this.__id = context.id;
		this.__prefix = context.prefix;
		this.__mainEntity = context.mainEntity;
		this.__fileType = context.fileType;
		this.__configManager = new ms123.config.ConfigManager();

		var win = context.parentWidget;
		if (win.hasChildren()) {
			win.removeAll();
		}
		this.__user = ms123.config.ConfigManager.getUserProperties();
		win.add(this._createDatamapper(), {
			edge: "center"
		});
	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},


	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		_createDatamapper:function(){
			var context = {};
			context.storeDesc = this.__storeDesc;
			context.use=ms123.datamapper.Config.USE_IMPORT;
			context.importingid= this.__prefix + "/" + this.__id;
			var dm = new ms123.datamapper.Datamapper(context);
			dm.addListener("save2", function(e){
				var settings = e.getData();
				this._saveSettings(settings);
			}, this);
			var setting = this._loadSettings();
			dm.init(setting.input ? setting : null );
			return dm;
		},
		_saveSettings: function (settings) {
			var ret = null;
			try {
				ret = ms123.util.Remote.rpcSync("importing:updateImporting", {
					namespace: this.__storeDesc.getNamespace(),
					settings: settings,
					importingid: this.__prefix + "/" + this.__id
				});
				ms123.form.Dialog.alert(this.tr("import.import_saved"));
			} catch (e) {
				ms123.form.Dialog.alert("ImportDatamapperDialog._saveSettings:" + e);
				return null;
			}
			return ret;
		},
		_loadSettings: function () {
			var settings = null;
			try {
				settings = ms123.util.Remote.rpcSync("importing:getSettings", {
					namespace: this.__storeDesc.getNamespace(),
					importingid: this.__prefix + "/" + this.__id
				});
			} catch (e) {
				ms123.form.Dialog.alert("ImportDatamapperDialog._loadSettings:" + e);
				return null;
			}
			return settings;
		}
	}
});
