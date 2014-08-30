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
qx.Class.define("ms123.searchfilter.Condition", {
	extend: qx.ui.core.Widget,

	construct: function (fieldsModel, selectableFields, params) {
		this.base(arguments);

		this._fieldsModel = fieldsModel;
		this._selectableFields = selectableFields;

		if (this._selectableFields.length > 0) {
			this._selectedField = this._selectableFields[0];
		}

		this._params = params;
		var hboxLayout = new qx.ui.layout.HBox().set({
			spacing: 4,
			alignX: "right"
		});
		this._setLayout(hboxLayout);
		this._controls = {};

		this._add(this._getChildControl("field_select"), {
		});
		this._add(this._getChildControl("op_select", this._selectedField), {
		});
		this._add(this._getChildControl("data_text", this._selectedField), {
		});
		this._changeOpsControl(null);
		this._changeDataControl(null);
	},

	properties: {
		field: {
			check: "String",
			init: "",
			event: "changeField"
		},
		op: {
			check: "String",
			init: "",
			event: "changeOp"
		},
		data: {
			check: "String",
			init: "",
			event: "changeData"
		}
	},
	events: {
		"change": "qx.event.type.Data"
	},

	members: {
		_getChildControl: function (id, field) {
			var name = (field == null) ? "" : field.module + "_" + field.itemval;
			var key = id + "__" + name;
			if (this._controls[key]) {
				return this._controls[key];
			}
			var control;


			switch (id) {
			case "field_select":
				control = new ms123.form.TreeSelectBox("fs");
				control.setWidth(170);
				var model = qx.data.marshal.Json.createModel(this._fieldsModel);
				control.setModel(model);
				control.addListener("changeSelection", function (e) {
					var model = e.getData()[0];
					var id = model.getId();
					var mod = model.getModule();
					console.log("Condition.changeSelection:"+model+",id:"+id+",mod:"+mod);

					//var model = e.getData();
					this._selectableFields.forEach(function (f) {
						if (f.itemval == model.getId() && f.module == model.getModule()) {
							this._selectedField = f;
						}
					}, this);
					this._changeOpsControl(id);
					this._changeDataControl(id);
					if (e.getData().length > 0) {
						this.fireDataEvent("changeField", model, null);
					}
				}, this);
				break;
			case "op_select":
				control = new qx.ui.form.SelectBox();
				for (var j = 0; j < field.ops.length; j++) {
					var op = field.ops[j];
					var li = new qx.ui.form.ListItem(op.text, null, op.op);
					if( op.tooltip) li.setToolTipText(op.tooltip);
					control.add(li);
				}
				control.addListener("changeSelection", function (e) {
					if (e.getData().length > 0) {
						this.fireDataEvent("changeOp", e.getData()[0].getModel(), null);
					}
				}, this);
				break
			case "data_treeselect":
				control = new ms123.form.TreeSelectBox("data");
				control.setWidth(200);
				var model = qx.data.marshal.Json.createModel(field.dataValues);
				control.setModel(model);
				control.addListener("changeSelection", function (e) {
					if (e.getData().length > 0) {
						var model = e.getData()[0];
						console.log("Condition.data_treeselect.changeSelection:"+model+",id:"+this._getModelValue(model)+"/"+model.getTitle());
						this.fireDataEvent("changeData", this._getModelValue(model), null);
					}
				}, this);
				break
			case "data_select":
				control = new qx.ui.form.SelectBox();
				control.addListener("changeSelection", function (e) {
					if (e.getData().length > 0) {
						this.fireDataEvent("changeData", e.getData()[0].getModel(), null);
					}
				}, this);
				for (var j = 0; field.dataValues && j < field.dataValues.length; j++) {
					var dv = field.dataValues[j];
					var li = new qx.ui.form.ListItem(dv.label, null, dv.value);
					if( dv.tooltip) li.setToolTipText(dv.tooltip);
					control.add(li);
				}
				break
			case "data_number":
			case "data_decimal":
				if( id == "data_number"){
					control = new ms123.form.NumberField();
					control.setFilter(/[0-9]/);
					control.setValue(0);
				}else if( id == "data_decimal"){
					control = new ms123.form.DecimalField();
					control.setFilter(/[0-9.,]/);
					control.setValue(0.0);
				}
				control.setWidth(150);
				control.setMarginTop(1);
				control.addListener("keypress", function(e) {
          if (e.getKeyIdentifier() === "Left" || e.getKeyIdentifier() === "Right") {
            e.stopPropagation();
          }
        });
				this.__inFireDataChange = false;
				control.addListener("changeValue", function (e) {
					var value = this._getNumber(e.getData());
					console.error("changeValue:" + e.getData()+"/"+value);
					if( this.__InFireDataChange) return;
					this.__InFireDataChange = true;
					this.fireDataEvent("changeData", value, null);
					this.__InFireDataChange = false;
				}, this);
				break
			case "data_text":
				control = new qx.ui.form.TextField();
				control.setValue("");
				control.setWidth(150);
				control.setMarginTop(1);
				control.addListener("keypress", function(e) {
          if (e.getKeyIdentifier() === "Left" || e.getKeyIdentifier() === "Right") {
            e.stopPropagation();
          }
        });
				control.addListener("changeValue", function (e) {
					console.log("changeValue:" + e.getData());
					if (e.getData().length > 0) {
						this.fireDataEvent("changeData", e.getData(), null);
					} else {
						this.fireDataEvent("changeData", "", null);
					}
				}, this);
				break
			case "data_boolean":
				control = new qx.ui.form.CheckBox();
				control.addListener("changeValue", function (e) {
					if (typeof e.getData() == "boolean") {
						this.fireDataEvent("changeData", e.getData(), null);
					}
				}, this);
				break

			case "data_date":
			case "data_datetime":
				var m = qx.locale.Manager.getInstance();
				var lang = m.getLanguage();

				if( id == "data_date" ){
					control = new qx.ui.form.DateField();
				}else{
					control = new ms123.form.DateTimeField();
					if( lang == "de" ){
						control.setTimeFormat("24");
					}else{
						control.setTimeFormat("12ampm");
					}
				}
				var format = new qx.util.format.DateFormat("MM-dd-yyyy"); 
				if( lang == "de" ){
					format = new qx.util.format.DateFormat("dd.MM.yyyy"); 
				}
				control.setDateFormat(format);
				control.addListener("changeValue", function (e) {
					var utc = ms123.util.UTC.toUTC(e.getData());
					console.log("date:"+e.getData().getTime()+"/"+utc);
					this.fireDataEvent("changeData", utc, null);
				}, this);
				break
			}

			this._controls[key] = control;
			return control;
		},
		_changeDataControl: function (value) {
			for (var i = 0; i < this._selectableFields.length; i++) {
				var f = this._selectableFields[i];
				if (f.itemval == value || value == null) {
					if (this._getChildren().length > 2) {
						this._removeAt(2);
					}
					if (f.dataValues && f.dataValues.length != undefined) { //dataValues is a Array
						this.data_controlType = "select";
						var data_select = this._getChildControl("data_select", this._selectedField);
						this._addAt(data_select, 2, {
						});
						this.fireDataEvent("changeData", data_select.getModelSelection().getItem(0), null);
					} else if (f.dataValues && f.dataValues.length == undefined) {//dataValues is a Object
						this.data_controlType = "treeselect";
						var data_select = this._getChildControl("data_treeselect", this._selectedField);
						this._addAt(data_select, 2, {
						});
						var id = this._getModelValue(data_select.getModelSelection().getItem(0));
						this.fireDataEvent("changeData", id, null);
					} else if (f.type == "date" || f.type == "datetime") {
						this.data_controlType = f.type;
						var data_date = this._getChildControl("data_"+f.type, this._selectedField);
						this._addAt(data_date, 2, {
						});
						if (data_date.getValue()) {
							this.fireDataEvent("changeData", data_date.getValue().getTime(), null);
						}
					} else if (f.type == "decimal") {
						this.data_controlType = "decimal";
						var data_number = this._getChildControl("data_decimal", this._selectedField);
						this._addAt(data_number, 2, { });
						this.fireDataEvent("changeData", this._getNumber(data_number.getValue()), null);
					} else if (f.type == "number") {
						this.data_controlType = "number";
						var data_number = this._getChildControl("data_number", this._selectedField);
						this._addAt(data_number, 2, { });
						this.fireDataEvent("changeData", this._getNumber(data_number.getValue()), null);
					} else if (f.type == "checkbox") {
						this.data_controlType = "boolean";
						var data_boolean = this._getChildControl("data_boolean", this._selectedField);
						this._addAt(data_boolean, 2, {
						});
						this.fireDataEvent("changeData", data_boolean.getValue(), null);
					} else {
						this.data_controlType = "string";
						var data_text = this._getChildControl("data_text", this._selectedField);
						this._addAt(data_text, 2, {
						});
						//console.log("changeData:" + data_text.getValue());
						this.fireDataEvent("changeData", data_text.getValue(), null);
					}
					break;
				}
			}
		},
		_changeOpsControl: function (value) {
			if (this._getChildren().length > 1) {
				this._removeAt(1);
			}
			var op_select = this._getChildControl("op_select", this._selectedField);
			this._addAt(op_select, 1);
			this.fireDataEvent("changeOp", op_select.getModelSelection().getItem(0), null);
		},
		getField: function () {
			var x = this._getChildControl("field_select").getModelSelection();
			return x.getItem(0);
		},
		setField: function (value) {
		  this.fireDataEvent("change", value, null);
			var id = null;
			var mod = null;
			try {
				id = value.getId();
				mod = value.getModule();
			} catch (e) {}
			if (id) {
				var selectables = this._getChildControl("field_select").getSelectables(true);
				for (var i = 0; i < selectables.length; i++) {
					var s = selectables[i];
					var m = s;//.getModel();
					if (m.getId() == id && ((m.getModule && m.getModule() == mod) || (!m.getModule && mod == null))) {
						console.log("Treffer:" + id + "/" + mod);
						value = m;
					}
				}
			}
try{
			this._getChildControl("field_select").setModelSelection([value]);
}catch(e){
	console.warn("e:"+e);
	console.warn("value:"+value.getId());
}
		},
		getOp: function () {
			var x = this._getChildControl("op_select", this._selectedField).getModelSelection();
			return x.getItem(0);
		},
		setOp: function (value) {
		  this.fireDataEvent("change", value, null);
			this._getChildControl("op_select", this._selectedField).setModelSelection([value]);
		},
		getData: function () {
			if (this.data_controlType == "select") {
				var x = this._getChildControl("data_select", this._selectedField).getModelSelection();
				return x.getItem(0);
			} else if (this.data_controlType == "treeselect") {
				var x = this._getChildControl("data_treeselect", this._selectedField).getModelSelection();
				return this._getModelValue(x.getItem(0));
			} else if (this.data_controlType == "date") {
				return this._getChildControl("data_date", this._selectedField).getValue().getTime();
			} else if (this.data_controlType == "datetime") {
				return this._getChildControl("data_datetime", this._selectedField).getValue().getTime();
			} else if (this.data_controlType == "boolean") {
				return this._getChildControl("data_boolean", this._selectedField).getValue();
			} else if (this.data_controlType == "number") {
				return this._getNumber(this._getChildControl("data_number", this._selectedField).getValue());
			} else if (this.data_controlType == "decimal") {
				return this._getNumber(this._getChildControl("data_decimal", this._selectedField).getValue());
			} else {
				//console.log("getData:" + this._getChildControl("data_text", this._selectedField).getValue());
				return this._getChildControl("data_text", this._selectedField).getValue();
			}
		},
		setData: function (value) {
			 this.fireDataEvent("change", value, null);
			if (this.data_controlType == "select") {
				this._getChildControl("data_select", this._selectedField).setModelSelection([value]);
			} else if (this.data_controlType == "treeselect") {
				var control = this._getChildControl("data_treeselect", this._selectedField);
				var item = this._getTreeItem( control, value );
				if( item ){
					value = item;
				}
				this._getChildControl("data_treeselect", this._selectedField).setModelSelection([value]);
			} else if (this.data_controlType == "decimal") {
				this._getChildControl("data_decimal", this._selectedField).setValue(value);
			} else if (this.data_controlType == "number") {
				this._getChildControl("data_number", this._selectedField).setValue(value);
			} else if (this.data_controlType == "date") {
				this._getChildControl("data_date", this._selectedField).setValue(this._getDate(value));
			} else if (this.data_controlType == "datetime") {
				this._getChildControl("data_datetime", this._selectedField).setValue(this._getDate(value));
			} else if (this.data_controlType == "boolean") {
				var value = this._getBoolean(value);
				this._getChildControl("data_boolean", this._selectedField).setValue(value);
			} else {
				//console.log("setData:" + value);
				this._getChildControl("data_text", this._selectedField).setValue(value);
			}
		},
		_getTreeItem: function (control,id) {
			var selectables = control.getSelectables(true);
			for (var i = 0; i < selectables.length; i++) {
				var s = selectables[i];
				var m = s;//.getModel();
				if (this._getModelValue(m) == id ) {
					return s;
				}
			}
			return null;
		},
		_getNumber:function(value){
			if( value == null || value == "" || isNaN(value)) value = 0;
			return value;
		},
		_getBoolean: function (value) {
			if (typeof value == "boolean") return value;
			if (value === null || value === "") return false;
			if (value == "true" || value == "ok" || value == "yes") return true;
			return false;
		},
		_getDate: function (value) {
			var d = new Date();
			try {
				var t = parseInt(value);
				if (!isNaN(t)) {
					d.setTime(t);
					return d; //TODO @@@MS
				} else {
					return d;
				}
			} catch (e) {
				return d;
			}
		},
		_getModelValue:function(model){
			if( model == null ) return null;
			if( model.getValue != null){
				return model.getValue();
			}
			if( model.getId != null){
				return model.getId();
			}
		}
	}
});
