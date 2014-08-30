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
 * @ignore(jQuery.ajax) 
 * @ignore(jQuery.each)
 * @ignore(jQuery.inArray)
 */
qx.Class.define("ms123.bomviewer.BOMViewer", {
	extend: qx.ui.core.Widget,


	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (context) {
		this.base(arguments);
		this._init(context);
	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},


	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		__valueModelMap: null,
		_createViewer: function () {
			var url = '/sw/resource/SD_2100097.pdf';
			var mapUrl = '/sw/resource/png_2100097/map.xml';
			var v = new ms123.bomviewer.ImageViewer({
				url: '2100097',
				scale: "page-height",
				hotspots: this._getHotspots(mapUrl)
			});
			return v;
		},
		_openViewer: function (part) {
			var url = '/sw/resource/SD_' + part + '.pdf';
			var mapUrl = '/sw/resource/png_' + part + '/map.xml';
			this._viewer.open(part, this._getHotspots(mapUrl), "page-width");
		},

		_init: function (context) {
			this._setLayout(new qx.ui.layout.Grow());
			this.setBackgroundColor("white");
			context.window.add(this, {
				edge: 0
			});

			this.__storeDesc = context.storeDesc;

			var rightSplit = this._rightSplit();
			var mainSplit = this._mainSplit(rightSplit);
			this._add(mainSplit, {
				edge: 0
			});
			var childs = mainSplit.getChildren();
			this._treeWidget = childs[0];
			var childsRight = rightSplit.getChildren();
			var imageWidget = childsRight[0];
			var tableWidget = childsRight[1];

			this._viewer = this._createViewer();
			this._viewer.addListener("hotspot", function (e) {
				console.log("BOMViewer:" + JSON.stringify(e.getData(), null, 2));
				this._selectTableRow(e.getData().href);
			}, this);
			imageWidget._add(this._viewer);
			var table = this._createTable([]);
			tableWidget._add(table);

			this.__createTree();
			if (false) {
				this._createChildControl("searchField");
			}
		},
		__createTree: function () {
			var tree = this._createChildControl("tree");
			var t = null;
			try {
				t = ms123.util.Remote.rpcSync("bhs:getBOMTree", {
					namespace: this.__storeDesc.getNamespace(),
					machine: "2100097"
				});
			} catch (e) {
				ms123.form.Dialog.alert("Viewer._init:" + e);
				return;
			}
			t.title = this.tr("meta.teams.root.title");
			this.__valueModelMap = {};
			this.__completeTree(t);
			var model = qx.data.marshal.Json.createModel(t);
			this.__modelList = [];
			this.__createValueToModelMap(model, this.__valueModelMap);
			tree.setModel(model);
		},
		__completeTree: function (model) {
			if (!model.children) {
				model.children = [];
			}
			for (var i = 0; model.children && i < model.children.length; i++) {
				var c = model.children[i];
				this.__completeTree(c);
			}
		},
		_mainSplit: function (rightSplit) {
			var splitpane = new qx.ui.splitpane.Pane("horizontal");
			splitpane.setDecorator("main");


			var leftWidget = new qx.ui.core.Widget();
			leftWidget._setLayout(new qx.ui.layout.Dock());
			leftWidget.setDecorator(null);
			leftWidget.setMinWidth(50);
			splitpane.add(leftWidget, 3);

			var rightWidget = rightSplit;
			rightWidget.setMinWidth(150);
			splitpane.add(rightWidget, 8);

			return splitpane;
		},
		_rightSplit: function () {
			var splitpane = new qx.ui.splitpane.Pane("vertical");
			splitpane.setDecorator("main");

			var upperWidget = new qx.ui.core.Widget();
			upperWidget._setLayout(new qx.ui.layout.Dock());
			upperWidget.setDecorator(null);
			upperWidget.setMinHeight(150);
			splitpane.add(upperWidget, 7);

			var lowerWidget = new qx.ui.core.Widget();
			lowerWidget._setLayout(new qx.ui.layout.Dock());
			lowerWidget.setDecorator(null);
			lowerWidget.setMinHeight(100);
			splitpane.add(lowerWidget, 3);
			return splitpane;
		},
		__getTree: function () {
			return this.getChildControl("tree");
		},
		__createValueToModelMap: function (model, map) {
			if (model.getPath) {
				map[model.getPath()] = model;
				//this.__modelList.push(model);
			}
			var children = model.getChildren();
			for (var i = 0; i < children.getLength(); i++) {
				var c = children.getItem(i);
				this.__createValueToModelMap(c, map);
			}
		},
		_getTreeContextMenu: function (name, level) {
			var menu = new qx.ui.menu.Menu();

			var label = this.tr("team.new_team");
			var newButton = new qx.ui.menu.Button(label, "icon/16/actions/edit-undo.png");
			newButton.addListener("execute", function () {}, this);
			menu.add(newButton);

			if (level > 0) {
				label = this.tr("team.count_assignments");
				var newButton = new qx.ui.menu.Button(label, "icon/16/status/dialog-information.png");
				newButton.addListener("execute", function () {
					this.__createEntityDialog();
				}, this);
				menu.add(newButton);
			}


			return menu;
		},
		_onTreeContextMenu: function (e) {
			var tree = this.__getTree()
			var item = tree.getSelection().getItem(0);
			var filter = qx.util.Serializer.toJson(item);
			console.log("item:" + filter);
			var lookup = tree.getLookupTable();
			var index = lookup.indexOf(item);
			var level = tree.getLevel(index);
			var name = item.getTeamid();
			var teamid = item.getTeamid();
			var model = this.__valueModelMap[teamid];
			console.log("_onTreeContextMenu:" + teamid + ",name:" + name + ",index:" + index + ",level:" + level);
			var menu = this._getTreeContextMenu(name, level);
			menu.setOpener(this);
			menu.openAtMouse(e);
		},
		_onTreeClick: function (e) {
			var tree = this.__getTree()
			var item = tree.getSelection().getItem(0);
			var lookup = tree.getLookupTable();
			var index = lookup.indexOf(item);
			var level = tree.getLevel(index);
			var name = item.getName();
			var path = item.getPath();
			var part = item.getPart();
			var model = this.__valueModelMap[path];

			this._getAssembly(path);

			if (this._viewer) {
				this._viewer.close();
			}
			this._openViewer(part);

		},
		_onOpen: function (e) {
			var item = e.getData();
			var tree = this.__getTree()
			var lookup = tree.getLookupTable();
			var index = lookup.indexOf(item);
			if (index == -1) {
				tree.openNodeAndParents(item);
				index = lookup.indexOf(item);
			}
			var sel = tree.getSelection();
			sel.splice(0, 1, item);
		},

		_getHotspots: function (url) {
			var xmlResult;
			jQuery.ajax({
				url: url,
				async: false,
				dataType: "text",
				success: function (e) {
					xmlResult = e;
				}
			});

			var xml = new ms123.util.Xml2Json();
			var jsonData = xml.convert(xmlResult);
			var hotspots = jsonData.area;
			return hotspots;
		},
		_getAssembly: function (path) {
			try {
				var d = ms123.util.Remote.rpcSync("bhs:getAssembly", {
					namespace: this.__storeDesc.getNamespace(),
					path: path + ".*{1}"
				});
				this._tableModel.setDataAsMapArray(d);
			} catch (e) {
				ms123.form.Dialog.alert("BOMViewer._getAssemby:" + e);
				return;
			}
		},
		_createTable: function (data) {
			var colIds = new Array();
			var colHds = new Array();
			var colWidth = new Array();
			colIds.push("part");
			colHds.push(this.tr("bomviewer.part"));
			colWidth.push("23%");

			colIds.push("name");
			colHds.push(this.tr("bomviewer.name"));
			colWidth.push("41%");

			colIds.push("qty");
			colHds.push(this.tr("bomviewer.qty"));
			colWidth.push("11%");

			colIds.push("group");
			colHds.push(this.tr("bomviewer.group"));
			colWidth.push("11%");

			colIds.push("price");
			colHds.push(this.tr("bomviewer.price"));
			colWidth.push("11%");

			var tableModel = new qx.ui.table.model.Simple();
			tableModel.setColumns(colHds, colIds);
			var customMap = {
				tableColumnModel: function (obj) {
					return new qx.ui.table.columnmodel.Resize(obj);
				}
			};
			var table = new qx.ui.table.Table(tableModel, customMap);
			var tcm = table.getTableColumnModel();
			colWidth.each((function (w, index) {
				var resizeBehavior = tcm.getBehavior();
				resizeBehavior.setWidth(index, w);
			}).bind(this));
			table.setStatusBarVisible(false);
			tableModel.setDataAsMapArray(data, true);
			this._tableModel = tableModel;
			this._table = table;
			this._createTableListener();
			return table;
		},
		_selectTableRow: function (key) {
			var i = this._getRecordPosByNamedField("part", key);
			var selModel = this._table.getSelectionModel();
			console.log("i=" + i);
			if( i == null){
				console.log("part("+key+") not found");
				return;
			}
			selModel.resetSelection();
			this._table.clearFocusedRowHighlight(null);
			selModel.setSelectionInterval(i, i);
			this._table.scrollCellVisible(0, i);
		},
		_createTableListener: function () {
			var table = this._table;
			var tableModel = table.getTableModel();
			var selModel = table.getSelectionModel();
			selModel.setSelectionMode(qx.ui.table.selection.Model.SINGLE_SELECTION);
			selModel.addListener("changeSelection", function (e) {
				var index = selModel.getLeadSelectionIndex();
				var map = this._tableModel.getRowDataAsMap(index);
				var count = selModel.getSelectedCount();
				if (count == 0) {
					return;
				}
				console.log("table.click:" + map["part"]);
				this._viewer.selectHotspot(map["part"]);
			}, this);
		},
		_getRecordPosByNamedField: function (fieldname, value) {
			var rc = this._tableModel.getRowCount();
			for (var i = 0; i < rc; i++) {
				var rd = this._tableModel.getRowDataAsMap(i);
				if (rd[fieldname] == value) {
					return i;
				}
			}
			return null;
		},
		/**---------------------------------------------------------------------------
		 WIDGET API
		 ---------------------------------------------------------------------------*/
		// overridden
		_createChildControlImpl: function (id) {
			var control;

			switch (id) {
			case "tree":
				control = new qx.ui.tree.VirtualTree(null, "name", "children").set({
					focusable: false,
					hideRoot: false,
					keepFocus: true,
					openMode: "none",
					height: null,
					itemHeight: 20,
					width: null,
					maxWidth: this.getWidth(),
					maxHeight: this.getHeight(),
					selectionMode: "one",
					contentPaddingLeft: 0,
					showTopLevelOpenCloseIcons: true,
					quickSelection: false
				});
				control.setIconPath("path");
				control.setIconOptions({
					converter: function (value, model) {
						if (model.getChildren != null && model.getChildren().getLength() > 0) {
							return "qx/decoration/Classic/shadow/shadow-small-r.png";
						} else {
							return "qx/decoration/Classic/shadow/shadow-small-tl.png";
						}
					}
				});
				control.addListener("contextmenu", this._onTreeContextMenu, this);
				control.addListener("open", this._onOpen, this);
				control.addListener("click", this._onTreeClick, this);
				this._treeWidget._add(control, {
					edge: "center"
				});
				break;

			case "searchField":
				var searchField = new qx.ui.form.TextField().set({
					padding: 2,
					margin: 2
				});
				searchField.setLiveUpdate(true);
				searchField.setFocusable(true);
				searchField.setEnabled(true);

				var bSearch = new qx.ui.form.Button(null, "icon/16/actions/system-search.png").set({
					padding: 2,
					margin: 2
				});
				bSearch.setEnabled(false);

				searchField.addListener('keyup', (function (e) {
					var value = searchField.getValue();
					if (value.length > 2) {
						bSearch.setEnabled(true);
					} else {
						bSearch.setEnabled(false);
					}
				}).bind(this));

				bSearch.setFocusable(false);
				bSearch.addListener("execute", function () {
					var text = searchField.getValue();
					var len = this.__modelList.length;
					var start = this._lastIndex != undefined ? this._lastIndex + 1 : 0;
					if (this._lastSearch != text) {
						start = 0;
					}
					if (start >= len) {
						start = 0;
					}
					for (var i = start; i < len; i++) {
						var model = this.__modelList[i];
						var desc = model.getDescription();
						var name = model.getName();
						var teamid = model.getTeamid();
						if ((desc && desc.toLowerCase().indexOf(text) != -1) || (name && name.toLowerCase().indexOf(text) != -1)) {
							var tree = this.__getTree()
							var sel = tree.getSelection();
							sel.removeAll();
							sel.push(model);
							tree.openNodeAndParents(model);
							tree.setSelection(sel);
							this._onTreeClick();
							this._lastIndex = i;
							this._lastSearch = text;
							break;
						}
					}
				}, this);

				var c = new qx.ui.container.Composite();
				c.setLayout(new qx.ui.layout.Dock());
				this._treeWidget._add(c, {
					edge: "north"
				});
				control = searchField;
				c.add(searchField, {
					edge: "center"
				});
				c.add(bSearch, {
					edge: "east"
				});
				break;
			}

			return control || this.base(arguments, id);
		}
	},
	destruct: function () {
		console.error("BOMViewer.close");
		this._viewer.destroy();
	}
});
