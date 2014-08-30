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
qx.Class.define("ms123.graphicaleditor.PalettePanel", {
	extend: qx.ui.container.Composite,
	include: [qx.locale.MTranslation],

	/**
	 * Constructor
	 */
	construct: function (stencilmanager) {
		this.base(arguments);
		this.__stencilManager = stencilmanager;
		this.setLayout(new qx.ui.layout.Grow());
		this._createPanels();
	},

	/**
	 * ****************************************************************************
	 * MEMBERS
	 * ****************************************************************************
	 */
	members: {
		_createPanels: function () {
			var panels = [];
			var buttons = [];

			var scroll = new qx.ui.container.Scroll();
			this.add(scroll);

			var panelSpace = new qx.ui.container.Composite(new qx.ui.layout.VBox()).set({
				allowShrinkY: false,
				allowGrowX: true
			});

			panelSpace.setPadding(0);
			scroll.add(panelSpace);

			var groupNameList = this.__stencilManager.getGroupNameList();
			for (var i = 0; i < groupNameList.length; i++) {
				var groupName = groupNameList[i];
				var panel = new ms123.widgets.CollapsablePanel(groupName, new qx.ui.layout.VBox());
				var groupList = this.__stencilManager.getGroupListByName(groupName);
				for (var j = 0; j < groupList.length; j++) {
					var stencil = groupList[j];
					var button = this._createButton(stencil.getId(), stencil.getTitle(), stencil.getIcon());

					buttons.push(button);
					panel.add(button);
				}
				panels.push(panel);
				panelSpace.add(panel);
				panel.setValue(false);
			}
			panels[0].setValue(true);
		},
		_createButton: function (stencilid, text, icon) {
			var b = new ms123.graphicaleditor.DraggableButton(text, icon);
			b.setUserData("stencilid", stencilid);
			b.setDraggable(true);
			b.addListener("dragstart", function (e) {
				e.addAction("move");
				console.log("DRAG:dragstart:" + b);

			});
			b.addListener("execute", function (e) {
				console.log("execute:" + b);
			});
			b.setPaddingTop(1);
			b.setPaddingBottom(1);
			return b;
		}
	}
});
