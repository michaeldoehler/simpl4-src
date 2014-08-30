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
 * Form renderer renderer for {@link qx.ui.form.Form}. 
 */
qx.Class.define("ms123.form.FormTabRenderer", {
	extend: qx.ui.core.Widget,
	implement: qx.ui.form.renderer.IFormRenderer,

	construct: function (form, tabs, useScroll, inWindow, formData) {
		this.base(arguments, form);

		if (tabs == null) {
			tabs = [{
				id: "tab1"
			}];
		}
		this._tabs = tabs;
		this._formData = formData;

		var tabView = new qx.ui.tabview.TabView().set({
			contentPadding: 2
		});
		this._tabView = tabView;
		tabView.setDecorator(null);
		this._internalTabChange = false;
		tabView.addListener("changeSelection", function (e) {
			var tabid = e._target.getSelection()[0].getUserData("tabid");
			if( this._internalTabChange === false){
				this.fireDataEvent("tabChanged", {tabid:tabid}, null);
			}
		}, this);

		this._pages = {};
		this._tabPages = {};
		var curpage;
		for (var i = 0; i < this._tabs.length; i++) {
			var tab = this._tabs[i];
			var scroll = new qx.ui.container.Scroll();
			var page = new qx.ui.tabview.Page(tab.title).set({
				showCloseButton: false
			});
			page.setUserData("tabid", tab.id);
			this._tabPages[tab.id] = page;
			curpage = page;
			page.setDecorator(null);
			page._setLayout(new qx.ui.layout.Grow());

			this._layout[tab.id] = this._createLayout(tab.layout);

			var w = new qx.ui.core.Widget();
			w._setLayout(this._layout[tab.id]);
			if (useScroll) {
				page.add(scroll);
				scroll.add(w);
			} else {
				page.add(w);
			}
			this._pages[tab.id] = w;
			tabView.add(page, {
				edge: 0
			});
		}

		this._setLayout(new qx.ui.layout.Grow());

		if (this._tabs.length == 1) {
			var bar = tabView.getChildControl("bar");
			bar.setVisibility("hidden");
			bar.set({
				height: 1
			});
		}

		this._add(tabView);

		this._visibilityBindingIds = [];

		if (qx.core.Environment.get("qx.dynlocale")) {
			qx.locale.Manager.getInstance().addListener("changeLocale", this._onChangeLocale, this);
			this._names = [];
		}


		// add the groups
		var groups = form.getGroups();
		for (var j = 0; j < groups.length; j++) {
			var group = groups[j];
			for (var i = 0; i < tabs.length; i++) {
				var tab = tabs[i];
				var single = true;
				var full = false;
				if (tab.layout && tab.layout == "double") {
					single = false;
				} else if (tab.layout && tab.layout == "full") {
					full = true;
				}
				this._row = 0;

				var lineheight = 22;
				if (tab.lineheight) {
					lineheight = tab.lineheight;
				}
				this.addItems(group.items, group.labels, tab.id, single, full, lineheight, group.title);
			}
		}

		// add the buttons
		var buttons = form.getButtons();
		for (var i = 0; i < buttons.length; i++) {
			this.addButton(buttons[i]);
		}
	},

	events: { 
		"tabChanged": "qx.event.type.Data"
	},
	members: {
		_tabs: null,
		_pages: null,
		_formData: null,
		_row: 0,
		_buttonRow: null,
		_names: null,
		_layout: [],
		_visibilityBindingIds: null,

		selectTab:function(tabid){
			var page = this._tabPages[tabid];
			this._internalTabChange = true;
			this._tabView.setSelection([page]);
			this._internalTabChange = false;
		},
		setReadonly: function (readonly) {
			for (var i = 0; i < this._tabs.length; i++) {
				var tab = this._tabs[i];
				var page = this._pages[tab.id];
				console.warn("page:" + page + "/tab:" + tab.id + "/enabled:" + !readonly);
				if (page) page.setEnabled(!readonly);
			}
		},
		/**
		 */
		addItems: function (items, names, tabid, single, full, lineheight, title) {
			var colSpan = single ? 2 : 4;

			/**
			 * add the items
			 */
			var j = -1;
			for (var i = 0; i < items.length; i++) {
				var item = items[i];
				/**
				 * current item
				 */
				var key = item.getUserData("key");
				var tab = "tab1";
				if (this._formData[key].tab) {
					tab = this._formData[key].tab;
				}
				if (tabid != tab) continue;


				var position = item.getUserData("position");
				var _row = -1;
				var _col = -1;
				if (position) {
					var x = position.split(",");
					_row = parseInt(x[0]);
					_col = parseInt(x[1]);
					if (!single) _col = _col * 2;
				}
				j++;
				var page = this._pages[tab];

				/**
				 * radio group
				 */
				if (item instanceof qx.ui.form.RadioGroup) {
					/**
					 * create horizontal radio group for a small number of radio buttons 
					 */
					if (item.getUserData("orientation") == "horizontal") {
						var widget = this._createHBoxForRadioGroup(item);
					}
					else {
						var widget = this._createWidgetForRadioGroup(item);
					}
				}

				/**
				 * other form widgets
				 */
				else {
					var widget = item;
				}

				var height = lineheight;
				if (height != -1) {
					if (item instanceof qx.ui.form.TextArea) {
						height = item.getHeight();
					}
					if (item instanceof ms123.form.TextArea) {
						height = item.getHeight();
					}
					if (item instanceof ms123.form.TreeMultiSelectBox) {
						height = item.getHeight();
					}
					if (item instanceof ms123.form.TreeSelector) {
						height = item.getHeight();
					}
					if (item instanceof ms123.form.DoubleSelectBox) {
						height = item.getHeight();
					}
					if (item instanceof ms123.form.TableEdit) {
						height = item.getHeight();
					}
					if (item instanceof ms123.form.UploadField) {
						height = item.getHeight();
					}
					if (item instanceof ms123.form.ComplexEdit) {
						height = item.getHeight();
					}
					if (qx.Class.hasInterface(item.constructor, ms123.form.HasOwnHeight)) {
						height = item.getHeight();
					}
				}
				if( item.getUserData("height")){
					height = item.getUserData("height");
				}

				if (this._formData[key].header) {
					this._row++;
					this._createHeader(this._formData[key].header, tabid, this._row, colSpan)
					this._row++;
					_col = 0;
					j = 0;
				}

				/**
				 * Excluded form elements, used for full-width * labels. 
				 */
				var labelText = names[i];
				if (full) labelText = null;
				if (labelText && item.getUserData("excluded")) {
					var label = new qx.ui.basic.Label(labelText);
					label.setRich(true);
					page._add(label, {
						row: this._row,
						column: 0,
						colSpan: colSpan
					});
				}

				/**
				 * if the label is null, use the full width for the widget
				 */
				else if (!labelText || labelText == '' || labelText == '_ignore_' || labelText == '###') {
					page._add(widget, {
						row: this._row,
						column: 0,
						colSpan: colSpan
					});
					row = this._row;
				}

				/**
				 * normal case: label in col 0, form element in col 2
				 */
				else {
					var label = this._createLabel(labelText, item);
					label.setRich(true);
					var column, row;
					if (_col != -1) {
						column = _col;
					} else {
						column = single ? 0 : ((j * 2) % 4);
					}
					if (_row != -1) {
						row = _row;
					} else {
						row = this._row;
					}
					page._add(label, {
						row: row,
						column: column
					});
					if (_col != -1) {
						column = _col + 1;
					} else {
						column = single ? 1 : (((j * 2) % 4) + 1);
					}
					page._add(widget, {
						row: row,
						column: column
					});
				}

				if (this._formData[key].rowflex != null) {
					this._layout[tab].setRowFlex(row, this._formData[key].rowflex);
				}

				if (height != -1) {
					this._layout[tab].setRowHeight(row, height);
				}
				if (single) {
					this._row++;
				} else {
					if (j % 2 == 1) {
						this._row++;
					}
				}


			}
		},

		/**
		 * Takes the items of the given RadioGroup and adds the to a Composite. 
		 */
		_createWidgetForRadioGroup: function (group) {
			var widget = new qx.ui.container.Composite(new qx.ui.layout.VBox(5));
			var items = group.getItems();
			for (var i = 0; i < items.length; i++) {
				widget.add(items[i]);
			}
			return widget;
		},

		/**
		 */
		_createHBoxForRadioGroup: function (group) {
			var widget = new qx.ui.container.Composite(new qx.ui.layout.HBox(5));
			var items = group.getItems();
			for (var i = 0; i < items.length; i++) {
				widget.add(items[i]);
			}
			return widget;
		},

		/**
		 */
		addButton: function (button) {
			if (this._buttonRow == null) {
				// create button row
				this._buttonRow = new qx.ui.container.Composite();
				this._buttonRow.setMarginTop(5);
				var hbox = new qx.ui.layout.HBox();
				hbox.setAlignX("right");
				hbox.setSpacing(5);
				this._buttonRow.setLayout(hbox);
				// add the button row
				this._add(this._buttonRow, {
					row: this._row,
					column: 0,
					colSpan: 2
				});
				// increase the row
				this._row++;
			}

			// add the button
			this._buttonRow.add(button);
		},


		/**
		 */
		getLayout: function () {
			return this._getLayout();
		},


		/**
		 * Creates a label for the given form item.
		 */
		_createLabel: function (name, item) {
			var label = new qx.ui.basic.Label(this._createLabelText(name, item));
			label.setRich(true);
			item.setUserData("label", label);
			return label;
		},


		/**
		 * Creates a header label for the form groups.
		 *
		 * param title {String} Creates a header label.
		 * @return {qx.ui.basic.Label} The header for the form groups.
		 */
		_createHeader: function (title, tabid, row, colSpan) {
			var header = new qx.ui.basic.Label(title);
			header.setFont("bold");
			if (row != 0) {
				header.setMarginTop(10);
			}
			header.setAlignX("left");
			this._pages[tabid]._add(header, {
				row: row,
				column: 0,
				colSpan: colSpan
			});
			return header;
		},

		/**
		 * Helper to bind the item's visibility to the label's visibility.
		 * param item {qx.ui.core.Widget} The form element.
		 * param label {qx.ui.basic.Label} The label for the form element.
		 */
		_connectVisibility: function (item, label) {
			// map the items visibility to the label
			var id = item.bind("visibility", label, "visibility");
			this._visibilityBindingIds.push({
				id: id,
				item: item
			});
		},


		/**
		 * Locale change event handler
		 */
		_onChangeLocale: qx.core.Environment.select("qx.dynlocale", {
			"true": function (e) {
				for (var i = 0; i < this._names.length; i++) {
					var entry = this._names[i];
					if (entry.name && entry.name.translate) {
						entry.name = entry.name.translate();
					}
					var newText = this._createLabelText(entry.name, entry.item);
					entry.label.setValue(newText);
				};
			},

			"false": null
		}),


		/**
		 * Creates the label text for the given form item.
		 */
		_createLabelText: function (name, item) {
			var required = "";
			if (item.getRequired()) {
				required = " <span style='color:red'>*</span> ";
			}
			var colon = name.length > 0 || item.getRequired() ? " :" : "";
			return name + required + colon;
		},

		_createLayout: function (what) {
			var layout = new qx.ui.layout.Grid();
			layout.setSpacing(3);
			if (what == null || what == '' || what == 'single') {
				layout.setColumnAlign(0, "right", "middle");
				layout.setColumnFlex(0, 0);
				layout.setColumnMaxWidth(0, 50);
				layout.setColumnFlex(1, 1);
			} else if (what == 'double') {
				layout.setColumnAlign(0, "left", "middle");
				layout.setColumnAlign(1, "left", "middle");
				layout.setColumnAlign(2, "left", "middle");
				layout.setColumnAlign(3, "left", "middle");
				layout.setColumnFlex(1, 1);
				layout.setColumnFlex(3, 1);
			} else if (what == 'full') {
				layout.setColumnAlign(0, "right", "top");
				layout.setColumnFlex(0, 0);
				layout.setColumnMaxWidth(0, 50);
				layout.setColumnFlex(1, 1);
				layout.setRowFlex(0, 1);
			}
			return layout;
		}
	},

	/**
	 *****************************************************************************
	 DESTRUCTOR
	 *****************************************************************************
	 */
	destruct: function () {
		if (qx.core.Environment.get("qx.dynlocale")) {
			qx.locale.Manager.getInstance().removeListener("changeLocale", this._onChangeLocale, this);
		}
		this._names = null;

		for (var i = 0; i < this._visibilityBindingIds.length; i++) {
			var entry = this._visibilityBindingIds[i];
			entry.item.removeBinding(entry.id);
		};
		if (this._buttonRow) {
			this._buttonRow.removeAll();
			this._disposeObjects("_buttonRow");
		}
	}

});
