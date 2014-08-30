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
	* @ignore(Clazz)
	* @ignore(Clazz.extend)
*/

qx.Class.define("ms123.ruleseditor.plugins.Edit", {
	extend: qx.core.Object,
	include: [qx.locale.MTranslation],
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (facade) {
		this.base(arguments);
		this.facade = facade;
		this._init();

		var rule_up_msg = this.tr("ruleseditor.rule_up");
		var rule_down_msg = this.tr("ruleseditor.rule_down");
		var change_column_msg = this.tr("ruleseditor.change_column");
		var delete_column_msg = this.tr("ruleseditor.delete_column");
		var insert_rule_msg = this.tr("ruleseditor.insert_rule");
		var remove_rule_msg = this.tr("ruleseditor.remove_rule");
		var insert_condition_column_msg = this.tr("ruleseditor.insert_condition_column");
		var insert_action_column_msg = this.tr("ruleseditor.insert_action_column");
		var group = "3";
		this.facade.offer({
			name: insert_rule_msg,
			description: insert_rule_msg,
			icon: "icon/16/actions/list-add.png",
			functionality: this.insertRule.bind(this),
			group: group,
			index: 1,
			isEnabled: qx.lang.Function.bind(function () {
				return this.facade.getConditionColumns().length > 0;
			}, this)
		});
		this.facade.offer({
			name: remove_rule_msg,
			description: remove_rule_msg,
			icon: "icon/16/actions/list-remove.png",
			functionality: this.removeRule.bind(this),
			group: group,
			index: 2,
			isEnabled: qx.lang.Function.bind(function () {
				return this.facade.getCountRules() > 0 && facade.getDecisionTable().getTable().getFocusedRow() != null;
			}, this)
		});

		this.facade.offer({
			name: rule_up_msg,
			description: rule_up_msg,
			icon: "icon/16/actions/go-up.png",
			functionality: this.upRule.bind(this),
			group: group,
			index: 3,
			isEnabled: qx.lang.Function.bind(function () {
				var countRules = this.facade.getCountRules();
				if( countRules==0) return false;
				var focusedRow = this.facade.getDecisionTable().getTable().getFocusedRow(); 
				return focusedRow != null && focusedRow > 0;
			}, this)
		});

		this.facade.offer({
			name: rule_down_msg,
			description: rule_down_msg,
			icon: "icon/16/actions/go-down.png",
			functionality: this.downRule.bind(this),
			group: group,
			index: 4,
			isEnabled: qx.lang.Function.bind(function () {
				var countRules = this.facade.getCountRules();
				if( countRules==0) return false;
				var focusedRow = this.facade.getDecisionTable().getTable().getFocusedRow(); 
				return focusedRow != null && focusedRow < (countRules-1);
			}, this)
		});
		this.facade.offer({
			name: insert_condition_column_msg,
			description: insert_condition_column_msg,
			icon: "icon/16/actions/bookmark-new.png",
			functionality: this.insertConditionColumn.bind(this),
			group: group,
			index: 5
		});
		this.facade.offer({
			name: insert_action_column_msg,
			description: insert_action_column_msg,
			icon: "icon/16/actions/document-new.png",
			functionality: this.insertActionColumn.bind(this),
			group: group,
			index: 6,
			isEnabled: qx.lang.Function.bind(function () {
				return this.facade.getConditionColumns().length > 0;
			}, this)
		});


		this.facade.offer({
			name: change_column_msg,
			description: change_column_msg,
			icon: "icon/16/actions/document-new.png",
			functionality: this.changeColumn.bind(this),
			target:ms123.ruleseditor.plugins.ContextMenu,
			group: group,
			index: 1,
			isEnabled: qx.lang.Function.bind(function () {
				var table = this.facade.getDecisionTable().getTable();
				var focusedCol = table.getFocusedColumn();
				return focusedCol < this.facade.getConditionColumns().length;
			}, this)
		});
		this.facade.offer({
			name: delete_column_msg,
			description: delete_column_msg,
			icon: "icon/16/actions/list-remove.png",
			functionality: this.deleteColumn.bind(this),
			target:ms123.ruleseditor.plugins.ContextMenu,
			group: group,
			index: 2
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
		_init:function(){
			this._allops = ['eq', 'ne', 'lt', 'le', 'gt', 'ge', 'bw', 'bn', 'in', 'ni', 'ew', 'en', 'cn', 'nc'];
			var odata = this.tr("meta.lists.odata");
			odata = odata.replace(/'/g, '"');
			try {
				odata = qx.lang.Json.parse(odata);
			} catch (e) {}
			this._opdata = odata;
		},
		insertRule: function (e) {
			var tableModel = this.facade.getDecisionTable().getModel();
			var table = this.facade.getDecisionTable().getTable();
			var focusedRow = table.getFocusedRow() != null ? table.getFocusedRow() + 1 : tableModel.getRowCount();
			this.executeCommandInsertRemoveRule(focusedRow, true);
		},
		removeRule: function (e) {
			var tableModel = this.facade.getDecisionTable().getModel();
			var table = this.facade.getDecisionTable().getTable();
			var focusedRow = table.getFocusedRow();
			console.log("focusedRow:" + focusedRow);
			this.executeCommandInsertRemoveRule(focusedRow, false);
		},
		executeCommandInsertRemoveRule: function (rownum, insert) {
			var facade = this.facade;
			var CommandClass = Clazz.extend({
				construct: function (rownum) {
					this.rownum = rownum;
				},
				execute: function () {
					if (insert) {
						facade.insertRule(this.rownum, null);
					} else {
						facade.removeRule(this.rownum, 1);
					}
				},
				rollback: function () {
					if (insert) {
						facade.removeRule(this.rownum, 1);
					} else {
						facade.insertRule(this.rownum, null);
					}
				}
			})
			var command = new CommandClass(rownum);
			this.facade.executeCommands([command]);
			this.facade.update();
		},
		upRule: function (e) {
			var tableModel = this.facade.getDecisionTable().getModel();
			var table = this.facade.getDecisionTable().getTable();
			var focusedRow = table.getFocusedRow();
			console.log("focusedRow:" + focusedRow);
			this.executeCommandUpDownRule(focusedRow, true);
		},
		downRule: function (e) {
			var tableModel = this.facade.getDecisionTable().getModel();
			var table = this.facade.getDecisionTable().getTable();
			var focusedRow = table.getFocusedRow();
			console.log("focusedRow:" + focusedRow);
			this.executeCommandUpDownRule(focusedRow, false);
		},
		executeCommandUpDownRule: function (rownum, up) {
			var facade = this.facade;
			var CommandClass = Clazz.extend({
				construct: function (rownum) {
					this.rownum = rownum;
				},
				execute: function () {
					facade.getDecisionTable().getTable().stopEditing();
					if (up) {
						var data = facade.removeRule(this.rownum, 1);
						facade.insertRule(this.rownum-1, data);
						facade.getDecisionTable().getTable().setFocusedCell(0,this.rownum-1)
						facade.getDecisionTable().getTable().getSelectionModel().setSelectionInterval(this.rownum-1, this.rownum-1);

					} else {
						var data = facade.removeRule(this.rownum, 1);
						facade.insertRule(this.rownum+1, data);
						facade.getDecisionTable().getTable().setFocusedCell(0,this.rownum+1)
						facade.getDecisionTable().getTable().getSelectionModel().setSelectionInterval(this.rownum+1, this.rownum+1);
					}
				},
				rollback: function () {
					facade.getDecisionTable().getTable().stopEditing();
					if (up) {
						var data = facade.removeRule(this.rownum-1, 1);
						facade.insertRule(this.rownum, data);
						facade.getDecisionTable().getTable().setFocusedCell(0,this.rownum)
						facade.getDecisionTable().getTable().getSelectionModel().setSelectionInterval(this.rownum, this.rownum);
					} else {
						var data = facade.removeRule(this.rownum+1, 1);
						facade.insertRule(this.rownum, data);
						facade.getDecisionTable().getTable().setFocusedCell(0,this.rownum)
						facade.getDecisionTable().getTable().getSelectionModel().setSelectionInterval(this.rownum, this.rownum);
					}
				}
			})
			var command = new CommandClass(rownum);
			this.facade.executeCommands([command]);
			this.facade.update();
		},




		insertActionColumn: function (e) {
			var callback = (function(data){
				var ac = new ms123.ruleseditor.ActionColumn();
				ac.setName("A" + this.facade.getActionColumns().length);
				//ac.setLabel(this.tr("ruleseditor.action") + this.facade.getActionColumns().length);
				ac.setLabel(this.tr("ruleseditor.ergebnis") + this.facade.getActionColumns().length);
				var x = data.variable.split(":");
				ac.setVariableName(x[0]);
				ac.setVariableType(x[1]);
				this.executeCommandInsertCol(ac, "action");
			}).bind(this);
			this._createCreateForm(callback,null,null,"action");
		},

		insertConditionColumn: function (e) {
			var callback = (function(data){
				var cc = new ms123.ruleseditor.ConditionColumn();
				cc.setName("C" + this.facade.getConditionColumns().length);
				//cc.setLabel(this.tr("ruleseditor.condition") + this.facade.getConditionColumns().length);
				cc.setLabel(this.tr("ruleseditor.bedingung") + this.facade.getConditionColumns().length);
				var x = data.variable.split(":");
				cc.setVariableName(x[0]);
				cc.setVariableType(x[1]);
				var op = this._getOperation(data);
				cc.setOperation(op.op);
				cc.setOperationText(op.text);
				this.executeCommandInsertCol(cc, "condition");
			}).bind(this);
			this._createCreateForm(callback,null,null,"condition");
		},
		deleteColumn: function (e) {
			var table = this.facade.getDecisionTable().getTable();
			var focusedCol = table.getFocusedColumn();
			var col = focusedCol;
			if (col >= this.facade.getConditionColumns().length) {
				this.executeCommandDeleteCol( col - this.facade.getConditionColumns().length, this.facade.getActionColumns());
			} else {
				this.executeCommandDeleteCol( col, this.facade.getConditionColumns());
			}
		},

		changeColumn: function (e) {
			var callbackCondition = (function(data,col){
				var new_op = this._getOperation(data);
				this.executeCommandChangeCol(col,new_op);
			}).bind(this);
			var callbackAction = (function(data,col){
				this.executeCommandChangeCol(col);
			}).bind(this);
			var tableModel = this.facade.getDecisionTable().getModel();
			var table = this.facade.getDecisionTable().getTable();
			var focusedCol = table.getFocusedColumn();
			var col = focusedCol;
			if (col >= this.facade.getConditionColumns().length) {
				/*var ac = this.facade.getActionColumns()[col - this.facade.getConditionColumns().length];
				var data = {};
				data["variable"] = ac.getVariableName()+":"+ ac.getVariableType();
				this._createChangeForm(callbackAction,data,"action");*/
			} else {
				var cc = this.facade.getConditionColumns()[col];
				var data = {};
				data["variable"] = cc.getVariableName()+":"+ cc.getVariableType();
				data["operators_"+cc.getVariableType()] = cc.getOperation();
				var f = qx.util.Serializer.toJson(data); console.log("data:"+f);
				this._createChangeForm(callbackCondition,col,data,"condition");
			}
		},
		executeCommandChangeCol: function (col, new_op) {
			var CommandClass = Clazz.extend({
				construct: function (facade, col,new_op) {
					this.facade = facade;
					this.col = col;
					this.new_op = new_op;
				},
				execute: function () {
					var cc = this.facade.getConditionColumns()[this.col];
					this.old_op = {};
					this.old_op.op = cc.getOperation();
					this.old_op.text = cc.getOperationText();
					cc.setOperation(this.new_op.op);
					cc.setOperationText(this.new_op.text);
					this.facade.columnsChanged();
				},
				rollback: function () {
					var cc = this.facade.getConditionColumns()[this.col];
					cc.setOperation(this.old_op.op);
					cc.setOperationText(this.old_op.text);
					this.facade.columnsChanged();
				}
			})
			var command = new CommandClass(this.facade, col,new_op);
			this.facade.executeCommands([command]);
			this.facade.update();
		},
		executeCommandDeleteCol: function (col, columns) {
			var CommandClass = Clazz.extend({
				construct: function (facade, col, columns) {
					this.facade = facade;
					this.col = col;
					this.columns = columns;
					this.oldColData = this.columns[this.col];
				},
				execute: function () {
					this.columns.splice(this.col,1);
					this.facade.columnsChanged();
				},
				rollback: function () {
					this.columns.splice(this.col,0, this.oldColData);
					this.facade.columnsChanged();
				}
			})
			var command = new CommandClass(this.facade, col, columns);
			this.facade.executeCommands([command]);
			this.facade.update();
		},
		executeCommandInsertCol: function (cc, which) {
			var CommandClass = Clazz.extend({
				construct: function (facade, cc, which) {
					this.facade = facade;
					this.which = which;
					this.col = cc;
				},
				execute: function () {
					var columns = this.which == "action" ? this.facade.getActionColumns() : this.facade.getConditionColumns();
					columns.push(this.col);
					this.facade.columnsChanged();
				},
				rollback: function () {
					var columns = this.which == "action" ? this.facade.getActionColumns() : this.facade.getConditionColumns();
					columns.pop();
					this.facade.columnsChanged();
				}
			})
			var command = new CommandClass(this.facade, cc, which);
			this.facade.executeCommands([command]);
			this.facade.update();
		},
		_getOperation: function (data) {
			var op = null;
			if( data.variable.toLowerCase().indexOf(":string") != -1){
				op = data.operators_string;
			}else if( data.variable.toLowerCase().indexOf(":boolean") != -1){
				op = data.operators_boolean;
			}else if( data.variable.toLowerCase().indexOf(":decimal") != -1){
				op = data.operators_decimal;
			}else if( data.variable.toLowerCase().indexOf(":date") != -1){
				op = data.operators_date;
			}else if( data.variable.toLowerCase().indexOf(":integer") != -1){
				op = data.operators_integer;
			}

			var pos = this._allops.indexOf(op);
			return { op:op, text: this._opdata[pos]};
		},
		_createOptionList: function (intList) {
			var retList = [];
			intList.each((function (so, index) {
				var pos = -1;
				if ((pos = this._allops.indexOf(so)) != -1) {
					retList.push({
						value: so,
						label: this._opdata[pos]
					});
				}
			}).bind(this));
			return retList;
		},
		_createChangeForm: function (callback,col,data, what) {
			this._createForm(callback,col,data,what,true);	
		},
		_createCreateForm: function (callback,col,data, what) {
			this._createForm(callback,col,data,what,false);	
		},
		_createForm: function (callback,col,data, what,change) {
			var integerList = ["eq", "ne", "lt", "gt", "ge","le"];
			var decimalList = ["eq", "ne", "lt", "gt", "ge","le"];
			var dateList = ["eq", "ne", "lt", "gt", "ge","le"];
			var stringList = ["eq", "ne", "cn", "bw"];
			var booleanList = ["eq", "ne"];

			var opListInteger = this._createOptionList(integerList);
			var opListString = this._createOptionList(stringList);
			var opListBoolean = this._createOptionList(booleanList);
			var opListDecimal = this._createOptionList(decimalList);
			var opListDate = this._createOptionList(dateList);

			var varList = [];
			var variables = what=="action" ? this.facade.getActionVariables() : this.facade.getConditionVariables();
			for (var i = 0; i < variables.length; i++) {
				var o = {};
				o.label = variables[i].variable+":"+variables[i].vartype;
				o.value = variables[i].variable+":"+variables[i].vartype;
				varList.push(o);
			}
			if( varList.length==0){
				ms123.form.Dialog.alert(this.tr("ruleseditor.no_variables_defined"));
				return;
			}
			var w = this._createConditionFormWindow();
			var formData = {
				"variable": {
					'type': "SelectBox",
					'label': this.tr("ruleseditor.variable"),
					'value': 1,
					'options': varList
				},
				"operators_string": {
					'type': "SelectBox",
					'label': this.tr("ruleseditor.operators"),
					'value': 1,
					'exclude': "variable.toLowerCase().indexOf(':string')==-1",
					'options': opListString
				},
				"operators_integer": {
					'type': "SelectBox",
					'label': this.tr("ruleseditor.operators"),
					'value': 1,
					'exclude': "variable.toLowerCase().indexOf(':integer')==-1",
					'options': opListInteger
				},
				"operators_decimal": {
					'type': "SelectBox",
					'label': this.tr("ruleseditor.operators"),
					'value': 1,
					'exclude': "variable.toLowerCase().indexOf(':decimal')==-1",
					'options': opListDecimal
				},
				"operators_date": {
					'type': "SelectBox",
					'label': this.tr("ruleseditor.operators"),
					'value': 1,
					'exclude': "variable.toLowerCase().indexOf(':date')==-1",
					'options': opListDate
				},
				"operators_boolean": {
					'type': "SelectBox",
					'label': this.tr("ruleseditor.operators"),
					'value': 1,
					'exclude': "variable.toLowerCase().indexOf(':boolean')==-1",
					'options': opListBoolean
				}/*,
				"valuelist" : {
					'type'  : "TextField",
					'label' : this.tr("ruleseditor.valuelist"),
					'validation': {
						validator: "/^([0-9A-Za-z_,]){0,128}$/"
					},
					'value' : ""
				}*/
			}
			if( what == "action"){
				delete formData["operators_boolean"];
				delete formData["operators_string"];
				delete formData["operators_decimal"];
				delete formData["operators_integer"];
				delete formData["operators_date"];
			}
			if( change){
				formData["variable"].readonly=true;
			}

			var buttons = [{
				'label': this.tr("ruleseditor.apply_changes"),
				'icon': "icon/16/actions/dialog-ok.png",
				'callback': function(m){
					var f = qx.util.Serializer.toJson(m); console.log("apply_changes:"+f);
					callback(m,col);
					w.destroy();
				},
				'value': 1
			},
			{
				'label': this.tr("ruleseditor.cancel"),
				'icon': "icon/16/actions/dialog-cancel.png",
				'callback': function(m){
					var f = qx.util.Serializer.toJson(m); console.log("cancelButton:"+f);
					w.destroy();
				},
				'value': 2
			}];

			var context = {};
			context.formData = formData;
			context.buttons = buttons;
			context.formLayout = [{
				id: "tab1", lineheight:-1
			}];
			var form = new ms123.widgets.Form(context);
			if( data ) form.fillForm( data );
					var f = qx.util.Serializer.toJson(form.form.getModel()); console.log("model:"+f);
			w.add(form);
			w.setActive(true);
			w.open();
		},
		_createConditionFormWindow: function () {
			var win = new qx.ui.window.Window(this.tr("ruleseditor.condition_column_config")).set({
				resizable: false,
				useMoveFrame: false,
				useResizeFrame: false
			});
			win.setLayout(new qx.ui.layout.Grow);
			win.setWidth(400);
			win.setHeight(250);
			win.setAllowMaximize(false);
			win.setAllowMinimize(false);
			win.setModal(true);
			win.setActive(false);
			win.minimize();
			win.center();
			var root = qx.core.Init.getApplication().getRoot();

			root.add(win);
			win.addListener("close", function (e) {
console.log("Windowdestroy");
				win.destroy();
			}, this);
			return win;
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
