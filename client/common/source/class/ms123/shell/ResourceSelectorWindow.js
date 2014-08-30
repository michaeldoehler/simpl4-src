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
	@asset(qx/icon/${qx.icontheme}/16/apps/*)
	@asset(qx/icon/${qx.icontheme}/48/actions/*)
	@asset(qx/icon/${qx.icontheme}/48/apps/*)
	@ignore($)
*/
qx.Class.define("ms123.shell.ResourceSelectorWindow", {
	extend: qx.core.Object,
	include: [qx.locale.MTranslation],


	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */

	construct: function (context, curPath,destName) {
		this.base(arguments);

console.log("curPath:"+curPath);
		this._user = context.user;

		this.__storeDesc = context.storeDesc;
		this._ok_callback = context.ok_callback;
		var title = context.title;
		var app = qx.core.Init.getApplication();

		var resdata = this._getResources();
		var win = this._createWindow(title);
		var centerContent = new qx.ui.container.Composite();
		centerContent.setLayout(new qx.ui.layout.Dock());

		var buttons = this._createButtons(win);
		this._tree = this._createTree();
		centerContent.add(this._tree, {
			edge: "center"
		});
		var textField = this._createFilenameField(destName);
		centerContent.add(textField, {
			edge: "south"
		});

		win.add(centerContent, {
			edge: "center"
		});

		this._setup(resdata);

		win.add(buttons, {
			edge: "south"
		});
		this._selectNode(curPath);
		app.getRoot().add(win);
		win.open();
	},

	/**
	 *****************************************************************************
	 PROPERTIES
	 *****************************************************************************
	 */
	properties: {},

	/**
	 *****************************************************************************
	 EVENTS
	 *****************************************************************************
	 */

	events: {
		/** Whenever the value is changed this event is fired
		 *
		 *  Event data: The new text value of the field.
		 */
		"changeValue": "qx.event.type.Data"
	},


	/**
	 *****************************************************************************
	 MEMBERS
	 *****************************************************************************
	 */

	members: {
		_getSelectables: function () {
			var selectables = [];
			if (!this.__model) return selectables;
			this.__getItemFromModel(this.__model, selectables);
			return selectables;
		},
		__getItemFromModel: function (model, selectables) {
			selectables.push(model);
			var children = model.getChildren();
			for (var i = 0; i < children.getLength(); i++) {
				var c = children.getItem(i);
				this.__getItemFromModel(c, selectables);
			}
		},
		_selectNode: function (path) {
			var selectables = this._getSelectables();
			for (var i = 0; i < selectables.length; i++) {
				var p = selectables[i].getPath();
				console.log("p:" + p + "|" + path);
				if (p == path) {
					this._tree.openNodeAndParents(selectables[i]);
					var sel = this._tree.getSelection();
					sel.splice(0, 1, selectables[i]);
				}
			}
		},
		_createButtons: function (win, table) {
			var toolbar = new qx.ui.toolbar.ToolBar();
			toolbar.setSpacing(5);
			toolbar.addSpacer();
			toolbar.addSpacer();

			this._buttonSelect = new qx.ui.toolbar.Button(this.tr("Ok"), "icon/16/actions/dialog-ok.png");
			this._buttonSelect.addListener("execute", function () {
				var sel = this._tree.getSelection().getItem(0);
				var data = {};
				data.path = sel.getPath();
				data.name = this._filenameTextfield.getValue();
				if (this._ok_callback) {
					this._ok_callback(data);
				}
				win.close();
			}, this);
			toolbar._add(this._buttonSelect)
			this._buttonSelect.setEnabled(false);

			var buttonCancel = new qx.ui.toolbar.Button(this.tr("Cancel"), "icon/16/actions/dialog-close.png");
			buttonCancel.addListener("execute", function () {
				win.close();
			}, this);
			toolbar._add(buttonCancel)
			return toolbar;
		},

		_onTreeChangeSelection: function (e) {
			var tree = this._tree;
			if (!tree) return;
			var selection = tree.getSelection();
			if (!selection || selection.getLength() == 0) return;
			var item = tree.getSelection().getItem(0);
			var type = item.getType();
		},
		_createFilenameField: function (destName) {
			var container = new qx.ui.container.Composite();
			var layout = new qx.ui.layout.HBox();
			container.setLayout(layout);

			var l1 = new qx.ui.basic.Label().set({
				value: this.tr("shell.dest_filename")
			});
			container.add(l1, {
				flex: 1
			});

			var tf1 = new qx.ui.form.TextField();
			if( destName ){
				tf1.setValue( destName );
			}
			tf1.setFilter(/[A-Za-z0-9._]/);
			tf1.setFocusable(true);
			tf1.setReadOnly(false);
			tf1.setEnabled(true);
			tf1.setValid(false);
			tf1.setRequired(true);

			var manager = new qx.ui.form.validation.Manager();
			var validator = qx.util.Validate.regExp(new RegExp("^[A-Za-z]([0-9A-Za-z._]){1,32}$"));
			manager.add(tf1, validator);
			manager.validate();

			manager.bind("valid", this._buttonSelect, "enabled");
			this._buttonSelect.setEnabled(destName ? true : false);

			tf1.addListener("input", function (e) {
				console.log("input:" + e.getData());
				manager.validate();
			}, this);

			tf1.addListener("changeValid", function (e) {
				console.log("changeValid:" + e.getData()+"/"+tf1.getValue());
			}, this);

			container.add(tf1, {
				flex: 1
			});
			this._filenameTextfield = tf1;
			return container;
		},
		_getResources: function () {
			var t = ms123.util.Remote.rpcSync("git:getWorkingTree", {
				name: this.__storeDesc.getNamespace(),
				includeTypeList: ["sw.directory"],
				excludePathList: ["data_description", "enumerations", "messages"],
				mapping: {
					path: "path",
					value: "name",
					title: "name",
					type: "type"
				}
			});
			return t;
		},

		_createTree: function () {
			var control = new qx.ui.tree.VirtualTree(null, "title", "children").set({
				focusable: false,
				hideRoot: false,
				keepFocus: true,
				openMode: "none",
				height: null,
				itemHeight: 20,
				selectionMode: "one",
				contentPaddingLeft: 0,
				showTopLevelOpenCloseIcons: true,
				quickSelection: false
			});
			control.setIconPath("path");
			control.setIconOptions({
				converter: (function (value, model, source, target) {
					return ms123.shell.FileType.getIcon(target, model, value);
				}).bind(this)
			});
			return control;
		},
		_setup: function (treeData) {
			var delegate = {
				configureItem: function (item) {
					item.setIndent(13);
				},
				createItem: function () {
					return new ms123.shell.TreeItem();
				},
				bindItem: (function (controller, item, id) {
					controller.bindProperty("", "model", null, item, id);
					controller.bindProperty("title", "label", null, item, id);
					controller.bindProperty(controller.getIconPath(), "icon", controller.getIconOptions(), item, id);
				}).bind(this)
			};

			this._tree.setDelegate(delegate);
			var model = qx.data.marshal.Json.createModel(treeData, true);
			this._tree.setModel(model);
			this._tree.getSelection().addListener("change", this._onTreeChangeSelection, this);
			this.__model = model;
		},

		_createWindow: function (name) {
			var win = new qx.ui.window.Window(name, "").set({
				resizable: true,
				useMoveFrame: true,
				useResizeFrame: true
			});
			win.setLayout(new qx.ui.layout.Dock);
			win.setWidth(500);
			win.setHeight(400);
			win.setAllowMaximize(false);
			win.setAllowMinimize(false);
			win.setModal(true);
			win.setActive(false);
			win.minimize();
			win.center();
			win.addListener("close", function () {
				win.destroy();
			}, this);
			return win;
		}
	}
});
