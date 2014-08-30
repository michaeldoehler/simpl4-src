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

qx.Class.define("ms123.datamapper.StructureTree", {
	extend: ms123.datamapper.BaseTree,
	implement: [qx.ui.form.IStringForm, qx.ui.form.IForm, ms123.form.HasOwnHeight],
	include: [qx.ui.form.MForm],
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (config, modelData,label) {
		this._label = label;
		this.base(arguments, modelData);
		this.getTree().addListener("changeSelection", function (ev) {
			var item = this.getTree().getModelSelection().getItem(0);
			item = qx.util.Serializer.toNativeObject(item);
			console.log("changeSelection.Item:" + JSON.stringify(item, null, 2));
			this.fireDataEvent("changeValue", item, this._oldValue);
			this._oldValue = item;
		}, this);
		this.addListener("changeValid", this._changeValid, this);
	},

	/******************************************************************************
	 EVENTS
	 ******************************************************************************/
	events: {
		"changeValue": "qx.event.type.Data"
	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},
	statics: {},

	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		_init: function (modelData) {
			this._borderNormal = new ms123.util.RoundSingleBorder(1, "solid", "#C2C2C2", 5);
			this._borderError = new ms123.util.RoundSingleBorder(2, "solid", "red", 5);
			var layout = new qx.ui.layout.Dock(3,3);
			this.setLayout(layout);

			var tree = this._createTree();

			var label = new qx.ui.basic.Label().set({ rich:true });
			label.setValue("<div style='border-bottom:1px solid gray;padding:3px;'><b>"+this._label + ":"+ modelData.format+"</b></div>");
			this.add( label, {edge:"north"});

			this.add(tree, {
				edge: "center"
			});
			this._customize(modelData);
			this.setModelData(modelData);
		},
		_customize: function (config) {},
		getSide: function () {
			return ms123.datamapper.Config.INPUT;
		},
		_changeValid: function (e) {
			console.log("StructureTree._changeValid:" + this.getValid());
			if (this.getValid()) {
				this.setDecorator(this._borderNormal);
			} else {
				this.setDecorator(this._borderError);
			}
		},
		getModel: function () {
			var sel = this.getTree().getModelSelection();
			var item = sel.getItem(0);
			return item;
		},
		resetValue: function () {},
		getValue: function () {
			var sel = this.getTree().getModelSelection();
			if (!sel) return null;
			var item = sel.getItem(0);
			item = qx.util.Serializer.toNativeObject(item);
			return item;
		},
		setValue: function (value) {
			if (value != null) {}
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
