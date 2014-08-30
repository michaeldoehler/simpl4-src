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
qx.Class.define('ms123.ruleseditor.RulesEditor', {
	extend: qx.ui.container.Composite,
	include: [qx.locale.MTranslation],

	construct: function (context) {
		this.base(arguments);
		this.context = context;

		this._pluginsData = [];
		this._eventsQueue = [];
		this._eventListeners = new Hash();
		this._cellMetaData = new Hash();

		this.setActionColumns(new Array());
		this.setConditionColumns(new Array());
		this.setCurrentRule(0);
		this._countRules = 0;
		this._facade = this.getPluginFacade();
		this._facade.storeDesc = context.storeDesc;
		this._facade.name = context.name;

		new ms123.ruleseditor.plugins.Save(this._facade, this);
		new ms123.baseeditor.Undo(this._facade);
		new ms123.ruleseditor.plugins.Edit(this._facade);
		new ms123.ruleseditor.plugins.Test(this._facade,this);
		new ms123.ruleseditor.plugins.ContextMenu(this._facade);
		var toolbar = new ms123.ruleseditor.plugins.Toolbar(this._facade);
		toolbar.registryChanged(this._facade.getPluginsData());
		this._registerPluginsOnKeyEvents();
		this._initEventListener();

		this.add(toolbar);

		this._buildLayout();
		this._createDecisionTable();

		//this._importJSON(context.data.rules);
		//this._facade.update();
	},

	properties: {
		conditionColumns: {
			check: 'Array'
		},
		actionColumns: {
			check: 'Array'
		},
		currentRule: {
			check: 'Integer'
		},
		decisionTable: {
			check: 'Object'
		}
	},

	events: {
		"save": "qx.event.type.Data"
	},
	members: {
		init:function(rules){
			this._importJSON(rules);
			this._facade.update();
		},

		_buildLayout: function () {
			this.setLayout(new qx.ui.layout.VBox());
			var varInput = this._createVariablesPanel();

			var titleArr = new Array();
			titleArr.push(this.tr("ruleseditor.variable"));
			titleArr.push("variable");

			var extraCols = new Array();
			var col = {
				label: this.tr('ruleseditor.vartype'),
				selectable_items: ["string", "integer", "date", "boolean", "decimal"],
				editable: true,
				defaultValue: "string",
				edittype: "select",
				name: 'vartype'
			};
			extraCols.push(col);

			this._inputVariableList = new ms123.form.MutableList(this.tr("ruleseditor.inputvariables"), titleArr, extraCols);
			this._outputVariableList = new ms123.form.MutableList(this.tr("ruleseditor.outputvariables"), titleArr, extraCols);

			varInput.add(this._inputVariableList, {
				width: "50%"
			});
			varInput.add(this._outputVariableList, {
				width: "50%"
			});
			this.add(varInput);

			this._windowTableContainer = new qx.ui.container.Composite(new qx.ui.layout.VBox());
			this.add(this._windowTableContainer, {
				flex: 1
			});

		},

		_createVariablesPanel: function () {
			var panel = new ms123.widgets.CollapsablePanel(this.tr("ruleseditor.variables"), new qx.ui.layout.HBox(4));
			panel.setValue(false);
			return panel;
		},

		_fireDataEvent: function (table, col, row, value, oldValue) {
			if (value == oldValue) return;
			table.fireDataEvent("dataEdited", {
				row: row,
				col: col,
				oldValue: oldValue,
				value: value
			});
		},


		_importJSON: function (json) {
			console.log("import:" + json);
			if( !json || json.length< 2 ) return;
			var data = qx.lang.Json.parse(json);
			this._setConditionVariables(data.variables.input);
			this._setActionVariables(data.variables.output);
			this._executeImportJSON( data.columns );
		},

		_executeImportJSON: function (columns) {
			var CommandClass = Clazz.extend({
				construct: function (facade, columns) {
					this.facade = facade;
					this.cColumns = columns.conditions;
					this.aColumns = columns.actions;
				},
				execute: function () {
					var maxRules = 0;
					var columns = this.facade.getConditionColumns();
					this.cColumns.each((function(col){
						var cc = new ms123.ruleseditor.ConditionColumn(col);
						maxRules = Math.max( cc.getData().length, maxRules);
						columns.push(cc);	
					}).bind(this));
					columns = this.facade.getActionColumns();
					this.aColumns.each((function(col){
						var ac = new ms123.ruleseditor.ActionColumn(col);
						maxRules = Math.max( ac.getData().length, maxRules);
						columns.push(ac);	
					}).bind(this));
					this.facade.setCountRules(maxRules);
					this.facade.columnsChanged();
					this.facade.getDecisionTable().updateRules();
				},
				rollback: function () {
					this.facade.setActionColumns(new Array());
					this.facade.setConditionColumns(new Array());
					this.facade.setCountRules(0);
					this.facade.columnsChanged();
					this.facade.getDecisionTable().updateRules();
				}
			})
			var command = new CommandClass(this._facade, columns);
			this._facade.executeCommands([command]);
			this._facade.update()
		},

		_getJSON: function () {
			var cc = qx.lang.Json.parse(qx.util.Serializer.toJson(this.getConditionColumns()));
			var ac = qx.lang.Json.parse(qx.util.Serializer.toJson(this.getActionColumns()));
			var data = {
				variables: {
					input: this._getConditionVariables(),
					output: this._getActionVariables()
				},
				columns: {
					conditions: cc,
					actions: ac
				}/*,
				ruledata: this.getDecisionTable().getModel().getDataAsMapArray()*/
			}
			return data;
		},

		_createDecisionTable: function () {
			//if( this.getConditionColumns().length == 0) return;
			var decisionTable = new ms123.ruleseditor.DecisionTable(this._facade);
			decisionTable.getTable().addListener('dataEdited', function (e) {
				console.log("dataEdited.data:" + qx.util.Serializer.toJson(e.getData()));

				var CommandClass = Clazz.extend({
					construct: function (facade, data, colDataArray) {
						this.facade = facade;
						this.data = data;
						this.colDataArray = colDataArray;
					},
					execute: function () {
						this.facade.getDecisionTable().getModel().setValue(this.data.col, this.data.row, this.data.value);
						this.colDataArray[this.data.row] = this.data.value;
					},
					rollback: function () {
						this.facade.getDecisionTable().getModel().setValue(this.data.col, this.data.row, this.data.oldValue);
						this.colDataArray[this.data.row] = this.data.oldValue;
					}
				})
				var colDataArray;
				var col = e.getData().col;
				if (col >= this.getConditionColumns().length) {
					colDataArray = this.getActionColumns()[col - this.getConditionColumns().length].getData();
				} else {
					colDataArray = this.getConditionColumns()[col].getData();
				}
				var command = new CommandClass(this._facade, e.getData(), colDataArray);
				this._facade.executeCommands([command]);
				this._facade.update()
			}, this);

			var selModel = decisionTable.getTable().getSelectionModel();
			selModel.addListener("changeSelection", function (e) {
				var index = selModel.getLeadSelectionIndex();
				this.setCurrentRule(index);
				this._update();
			}, this);

			if (this._windowTableContainer.hasChildren()) {
				this._windowTableContainer.removeAll();
			}
			this._windowTableContainer.add(decisionTable.getTable(), {
				flex: 1
			});
			this.setDecisionTable(decisionTable);
			this._handleEvents({
				type: ms123.ruleseditor.Config.EVENT_TABLE_CREATED,
				force: true
			})
		},

		_splitPane: function (left, right) {
			var splitPane = new qx.ui.splitpane.Pane("horizontal").set({
				decorator: null
			});

			splitPane.add(left, 8);
			splitPane.add(right, 3);

			return splitPane;
		},

		_getPropertyPanel: function () {
			return new qx.ui.container.Scroll();
		},
		open: function () {
			//this.setValue(results);
		},

		getPluginFacade: function () {
			if (!(this._pluginFacade)) {
				this._pluginFacade = {
					offer: this._offer.bind(this),
					update: this._update.bind(this),
					columnsChanged: this._columnsChanged.bind(this),
					getActionColumns: this.getActionColumns.bind(this),
					setActionColumns: this.setActionColumns.bind(this),
					getConditionColumns: this.getConditionColumns.bind(this),
					setConditionColumns: this.setConditionColumns.bind(this),
					getDecisionTable: this.getDecisionTable.bind(this),
					getCurrentRule: this.getCurrentRule.bind(this),
					getConditionVariables: this._getConditionVariables.bind(this),
					getActionVariables: this._getActionVariables.bind(this),
					getJSON: this._getJSON.bind(this),
					insertRule: this._insertRule.bind(this),
					removeRule: this._removeRule.bind(this),
					getCountRules: this._getCountRules.bind(this),
					setCountRules: this._setCountRules.bind(this),
					getPluginsData: this._getPluginsData.bind(this),
					executeCommands: this._executeCommands.bind(this),
					isExecutingCommands: this._isExecutingCommands.bind(this),

					registerOnEvent: this._registerOnEvent.bind(this),
					unregisterOnEvent: this._unregisterOnEvent.bind(this),
					raiseEvent: this._handleEvents.bind(this)
				};
			}
			return this._pluginFacade;
		},
		_setCountRules: function (cr) {
			this._countRules=cr;
		},
		_getCountRules: function () {
			return this._countRules;
		},
		_getConditionVariables: function () {
			return this._inputVariableList.getData();
		},
		_setConditionVariables: function (vars) {
			return this._inputVariableList.setData(vars);
		},
		_setActionVariables: function (vars) {
			return this._outputVariableList.setData(vars);
		},
		_getActionVariables: function () {
			return this._outputVariableList.getData();
		},
		_insertRule: function (row, data) {
			if( data) data = data.reverse();
			this._countRules++;
			this.getConditionColumns().each(function (col) {
				var d = data ? data.pop() : null;
				col.insertDataAt(row, d);
			});
			this.getActionColumns().each(function (col) {
				var d = data ? data.pop() : null;
				col.insertDataAt(row, d);
			});
			this.getDecisionTable().updateRules();
		},

		_removeRule: function (row) {
			this._countRules--;
			var savedData = new Array();
			this.getConditionColumns().each(function (col) {
				savedData.push(col.removeDataAt(row));
			});
			this.getActionColumns().each(function (col) {
				savedData.push(col.removeDataAt(row));
			});
			this.getDecisionTable().updateRules();
			return savedData;
		},
		_update: function (pluginData) {
			this._handleEvents({
				type: ms123.ruleseditor.Config.EVENT_CELL_CHANGED,
				force: true
			})
		},
		_columnsChanged: function (pluginData) {
			this._createDecisionTable();
			this._handleEvents({
				type: ms123.ruleseditor.Config.EVENT_COLUMNS_CHANGED,
				force: true
			})
		},
		_offer: function (pluginData) {
			if (!this._pluginsData.member(pluginData)) {
				this._pluginsData.push(pluginData);
			}
		},

		_getPluginsData: function () {
			return this._pluginsData;
		},

		/*Keyboardstuff for plugins*/
		_registerPluginsOnKeyEvents: function () {
			this._pluginsData.each((function (pluginData) {
				if (pluginData.keyCodes) {
					pluginData.keyCodes.each((function (keyComb) {
						var eventName = "key.event";
						eventName += '.' + keyComb.keyAction;
						if (keyComb.metaKeys) {
							if (keyComb.metaKeys.
							indexOf(ms123.ruleseditor.Config.META_KEY_META_CTRL) > -1) {
								eventName += "." + ms123.ruleseditor.Config.META_KEY_META_CTRL;
							}
							if (keyComb.metaKeys.
							indexOf(ms123.ruleseditor.Config.META_KEY_ALT) > -1) {
								eventName += '.' + ms123.ruleseditor.Config.META_KEY_ALT;
							}
							if (keyComb.metaKeys.
							indexOf(ms123.ruleseditor.Config.META_KEY_SHIFT) > -1) {
								eventName += '.' + ms123.ruleseditor.Config.META_KEY_SHIFT;
							}
						}
						if (keyComb.keyCode) {
							eventName += '.' + keyComb.keyCode;
						}
						if (pluginData.toggle === true && pluginData.buttonInstance) {
							this._registerOnEvent(eventName, function () {
								pluginData.buttonInstance.toggle(!pluginData.buttonInstance.pressed);
								pluginData.functionality.call(pluginData, pluginData.buttonInstance, pluginData.buttonInstance.pressed);
							});
						} else {
							this._registerOnEvent(eventName, pluginData.functionality)
						}

					}).bind(this));
				}
			}).bind(this));
		},

		_initEventListener: function () {
			this.addListenerOnce("appear", function () {
				var elem = this.getContentElement().getDomElement();
				elem.addEventListener(ms123.ruleseditor.Config.EVENT_KEYDOWN, this._catchKeyDownEvents.bind(this), false);
				elem.addEventListener(ms123.ruleseditor.Config.EVENT_KEYUP, this._catchKeyUpEvents.bind(this), false);
			}, this);
		},

		_catchKeyUpEvents: function (event) {
			if (!event) event = window.event;
			var keyUpEvent = this._createKeyCombEvent(event, ms123.ruleseditor.Config.KEY_ACTION_UP);
			this._handleEvents({
				type: keyUpEvent,
				event: event
			});
		},

		_catchKeyDownEvents: function (event) {
			if (!event) event = window.event;
			var keyDownEvent = this._createKeyCombEvent(event, ms123.oryx.Config.KEY_ACTION_DOWN);
			this._handleEvents({
				type: keyDownEvent,
				event: event
			});
		},

		_createKeyCombEvent: function (keyEvent, keyAction) {
			var pressedKey = keyEvent.which || keyEvent.keyCode;
			var eventName = "key.event";

			if (keyAction) {
				eventName += "." + keyAction;
			}
			if (keyEvent.ctrlKey || keyEvent.metaKey) {
				eventName += "." + ms123.oryx.Config.META_KEY_META_CTRL;
			}
			if (keyEvent.altKey) {
				eventName += "." + ms123.oryx.Config.META_KEY_ALT;
			}
			if (keyEvent.shiftKey) {
				eventName += "." + ms123.oryx.Config.META_KEY_SHIFT;
			}
			return eventName + "." + pressedKey;
		},

		/**
		 *  Methods for the PluginFacade
		 */
		_registerOnEvent: function (eventType, callback) {
			if (!(this._eventListeners.keys().member(eventType))) {
				this._eventListeners[eventType] = [];
			}
			this._eventListeners[eventType].push(callback);
		},

		_unregisterOnEvent: function (eventType, callback) {
			if (this._eventListeners.keys().member(eventType)) {
				this._eventListeners[eventType] = this._eventListeners[eventType].without(callback);
			} else {}
		},

		_executeEventImmediately: function (eventObj) {
			if (this._eventListeners.keys().member(eventObj.event.type)) {
				this._eventListeners[eventObj.event.type].each((function (value) {
					value(eventObj.event, eventObj.arg);
				}).bind(this));
			}
		},
		_executeEvents: function () {
			this._queueRunning = true;
			while (this._eventsQueue.length > 0) {
				var val = this._eventsQueue.shift();
				this._executeEventImmediately(val);
			}
			this._queueRunning = false;
		},
		_handleEvents: function (event, argObj) {
			console.log("Dispatching event type " + event.type + " on " + argObj);
			switch (event.type) {}
			if (event.forceExecution) {
				this._executeEventImmediately({
					event: event,
					arg: argObj
				});
			} else {
				this._eventsQueue.push({
					event: event,
					arg: argObj
				});
			}

			if (!this._queueRunning) {
				this._executeEvents();
			}

			return false;
		},
		/**
		 * Implementes the command pattern
		 * (The real usage of the command pattern
		 * is implemented and shown in the Plugins/undo.js)
		 *
		 */
		_executeCommands: function (commands) {

			if (!this._commandStack) {
				this._commandStack = [];
			}
			if (!this._commandStackExecuted) {
				this._commandStackExecuted = [];
			}


			this._commandStack = [].concat(this._commandStack).concat(commands);

			// Check if already executes
			if (this._commandExecuting) {
				return;
			}

			// Start execution
			this._commandExecuting = true;

			// Iterate over all commands
			while (this._commandStack.length > 0) {
				var command = this._commandStack.shift();
				// and execute it
				command.execute();
				this._commandStackExecuted.push(command);
			}

			// Raise event for executing commands
			console.log("sendExecuteCommandEvent");
			this._handleEvents({
				type: ms123.ruleseditor.Config.EVENT_EXECUTE_COMMANDS,
				commands: this._commandStackExecuted
			});

			// Remove temporary vars
			delete this._commandStack;
			delete this._commandStackExecuted;
			delete this._commandExecuting;


			//this.updateSelection();
		},
		_isExecutingCommands: function () {
			return !!this._commandExecuting;
		}


	}
});
