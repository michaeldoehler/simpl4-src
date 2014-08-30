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
/** **********************************************************************

   Authors:
     * Manfred Sattler

************************************************************************ */

/**
 * A form widget which allows a multiple selection with traits additions.
 *
 */
qx.Class.define("ms123.form.TraitDoubleSelectBox", {
	extend: ms123.form.DoubleSelectBox,


	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */
	construct: function (context) {
		this._context = context;
		this.base(arguments);
	},


	/**
	 *****************************************************************************
	 PROPERTIES
	 *****************************************************************************
	 */



	/**
	 *****************************************************************************
	 MEMBERS
	 *****************************************************************************
	 */


	members: {
		/**
		 ---------------------------------------------------------------------------
		 PUBLIC SELECTION API
		 ---------------------------------------------------------------------------
		 */
		setModel: function (model) {
			var delegate = {
				configureItem: function (item) {
					item.setPadding(3);
				},
				createItem: function () {
					return new ms123.form.TooltipListItem();
				},
				bindItem: function (controller, item, id) {
					controller.bindProperty("label", "label", null, item, id);
					controller.bindProperty("tooltip", "tooltip", null, item, id);
				}
			};
			var list_ava = this.getChildControl("list_avalaible");
			var list_sel = this.getChildControl("list_selected");
			list_ava.setDelegate(delegate);
			list_ava.setModel(model);

			var _this = this;
			var delegate_sel = {
				configureItem: function (item) {
					item.setPadding(3);
					item.addListener("change", function (data) {
						_this._no_update = true;
						_this.fireDataEvent("changeSelection", _this._modelToValues(list_sel.getModel()), null);
						_this._no_update = false;
					}, this);
				},
				createItem: function () {
					return new ms123.form.TraitListItem(_this._context);
				},
				bindItem: function (controller, item, id) {
					controller.bindProperty("label", "label", null, item, id);
					controller.bindProperty("tooltip", "tooltip", null, item, id);

					var value = list_sel.getModel().getItem(id).getValue();
					var m = _this.__traitData[value];
					if (m === undefined) {
						m = _this.__traitData[value] = {};
						m["traitid"] = value;//@@@MS temp
						m["teamid"] = value;
						_this._no_update = true;
						_this.fireDataEvent("changeSelection", _this._modelToValues(list_sel.getModel()), null);
						_this._no_update = false;
					}
					item.setModel(m);
				}
			};
			list_sel.setDelegate(delegate_sel);

			this._list_all = model.copy();


		},
		_mergeData: function (model, add) {
			var ret = [];
			var len = model.getLength();
			for (var i = 0; i < len; i++) {
				var map = {};
				map["value"] = model.getItem(i).getValue();
				qx.lang.Object.mergeWith(map, add[model.getItem(i).getValue()]);
				ret.push(map);
			}
			return ret;
		},
		_modelToValues: function (model) {
			return this._mergeData(model, this.__traitData);
		},
		_valuesToModel: function (values) {
			if (this.__no_update) return;
			this.__traitData = {};
			var r = qx.util.Serializer.toJson(values);
			console.log("_valuesToModel.values:" + r);
			var selectables = this.getSelectables();
			var model = new qx.data.Array();
			if (values != null && values.length > 0) {
				for (var i = 0; i < selectables.getLength(); i++) {
					var selectable = selectables.getItem(i);
					for (var j = 0; j < values.length; j++) {
						var _value = values[j];
						if( _value.traitid ){//@@@MS temp
							if (selectable.getValue() == _value.traitid) {
								model.push(selectable);
								this.__traitData[_value.traitid] = _value;
							}
						}
						if( _value.teamid ){
							if (selectable.getValue() == _value.teamid) {
								model.push(selectable);
								this.__traitData[_value.teamid] = _value;
							}
						}
					}
				}
			}
			var r = qx.util.Serializer.toJson(model);
			console.log("_valuesToModel.model:" + r);
			return model;
		}
	}
});
