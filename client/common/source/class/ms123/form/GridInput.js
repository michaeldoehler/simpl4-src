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
	@lint ignoreDeprecated(alert,eval) 
	@asset(qx/icon/${qx.icontheme}/22/actions/*)
	@asset(qx/icon/${qx.icontheme}/16/places/*)
*/
qx.Class.define("ms123.form.GridInput", {
	extend: qx.ui.container.Composite,
	implement: [qx.ui.form.IStringForm, qx.ui.form.IForm],
	include: [qx.ui.form.MForm],


	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */

	construct: function (context) {
		this.base(arguments);
		var layout = new qx.ui.layout.Dock(3, 3);
		this.setLayout(layout);

		this._context = context;
		this._config = context.config;
		console.error("GridInput:" + qx.util.Serializer.toJson(this._config));

		this.__lockSetValue = false;
		var headerLine = this._createHeader(this._config.items);
		this.add(headerLine, {
			edge: "north"
		});

		this._lineContainer = new qx.ui.container.Composite(new qx.ui.layout.VBox(2));
		this.add(this._lineContainer, {
			edge: "center"
		});

			//var data = qx.lang.Json.stringify(this._config.items, null, 4);console.log("Items:"+data);
		var firstLine = this._createOneLine(this._config.items);
		this._lineContainer.add(firstLine, {
			flex: 0
		});
		this._firstLine= firstLine;
		firstLine.addListener("resize", function () {
			for (var i = 0; i < this._config.items.length; i++) {
				var w1 = headerLine.getChildren()[i];
				var w2 = firstLine.getChildren()[i];
				var w = w2.getBounds().width;
				w1.setMinWidth(w);
				w1.setMaxWidth(w);
			}
		},this);
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
		 *  Event data: The new text value of the table.
		 */
		"changeValue": "qx.event.type.Data"
	},


	/**
	 *****************************************************************************
	 MEMBERS
	 *****************************************************************************
	 */

	members: {
		// overridden
		/**
		 * @lint ignoreReferenceField(_forwardStates)
		 */
		_forwardStates: {
			focused: true
		},


		// interface implementation
		setValue: function (values) {
			if( this.__lockSetValue === true) return;
			if( values==null || values == "") return;
			if( typeof values == "string"){
				values = qx.lang.Json.parse(values);
			}
			console.log("GridInput:"+values+"/"+values.length);

			for(var i=0; i< values.length;i++){
				if( i > 0){
					var line = this._createOneLine(this._config.items);
					this._addLine( line);
					this._setLineValues( line, values[i]);
				}else{
					this._setLineValues( this._firstLine, values[i]);
				}
			}
		},

		// interface implementation
		getValue: function () {
			//var data = this._getValues();
			//data = qx.lang.Json.stringify(data, null, 4);
			//console.log("data:" + data);
			//return data;
			return null;
		},

		// interface implementation
		resetValue: function () {
			alert("resetValue");
		},

		// useit checkbox
		getCheckBox: function () {
			return this.getChildControl("checkbox");
		},
		_createHeader: function (columns) {
			var container = new qx.ui.container.Composite(new qx.ui.layout.HBox(2));

			for (var i = 0; i < columns.length; i++) {
				var col = columns[i];
				var text = col.colname;
				if (col.display) {
					text = col.display;
					if (text.length>1 && text.match(/^@/)) {
						text = this.tr(text.substring(1));
					}
				}
				var l = new qx.ui.basic.Label(text);
				try {
					l.setAppearance("grid-header-cell");
				} catch (e) {
					console.log("e:" + e);
					console.log(e.stack);
				}
				container.add(l, {
					flex: 1
				});
			}
			container.add(new qx.ui.basic.Label(""), {
				flex: 0
			});
			container.setBackgroundColor("#EEEEEE");
			return container;
		},

		_createOneLine: function (columns) {
			var container = new qx.ui.container.Composite(new qx.ui.layout.HBox(2));
			for (var i = 0; i < columns.length; i++) {
				var col = columns[i];
				var fe = this._createFormElement(col);
				fe.setUserData("key", col.colname);
				container.add(fe, {
					flex: 1
				});
			}
			container.setUserData("name", "line1");
			container.add(this._createToolbar(), {
				flex: 0
			});
			return container;
		},
		_createFormElement: function (col) {
			var type = col.type.toLowerCase();
			var readonly = col.readonly;
			var lines = col.lines;
			var formElement = null;
			switch (type.toLowerCase()) {
			case "textarea":
				formElement = new ms123.form.TextArea();
				//formElement._setHeight(lines * 16);
				formElement.setAutoSize(true);
				formElement.setAllowStretchY(false);
				break;
			case "decimal":
				formElement = new ms123.form.DecimalField();
				if (readonly && readonly === true) {
					formElement.setReadOnly(true);
				}
				formElement.setFilter(/[0-9.,]/);
				formElement.setMaxHeight(22);
				break;

			case "number":
				formElement = new ms123.form.NumberField();
				if (readonly && readonly === true) {
					formElement.setReadOnly(true);
				}
				formElement.setFilter(/[0-9]/);
				formElement.setMaxHeight(22);
				break;

			case "text":
				formElement = new ms123.form.TextField();
				if (readonly && readonly === true) {
					formElement.setReadOnly(true);
				}
				formElement.setMaxHeight(22);
				break;
			case "boolean":
				formElement = new qx.ui.form.CheckBox();
				formElement.setMinWidth(40);
				formElement.setMaxWidth(40);
				formElement.setPaddingLeft(15);
				formElement.setAllowGrowX(true);
				formElement.setMaxHeight(20);
				break;
			case "date":
			case "datetime":
				var m = qx.locale.Manager.getInstance();
				var lang = m.getLanguage();
				if (type == "date") {
					formElement = new qx.ui.form.DateField();
				} else {
					formElement = new ms123.form.DateTimeField();
					if (lang == "de") {
						formElement.setTimeFormat("24");
					} else {
						formElement.setTimeFormat("12ampm");
					}
				}
				var format = new qx.util.format.DateFormat("MM-dd-yyyy");
				if (lang == "de") {
					format = new qx.util.format.DateFormat("dd.MM.yyyy");
				}
				formElement.setDateFormat(format);
				formElement.setMaxHeight(22);
				break;
			case "search":
				var context = {}
				context.storeDesc = this._context.storeDesc;
				formElement = new ms123.form.GridInputSelector(context);
				formElement.setModule( col.constraints.entity );
				formElement.setMinWidth(20);
				formElement.setMaxWidth(20);
				formElement.setMaxHeight(22);
				break;
			case "resourceselector":
				formElement = new ms123.form.ResourceSelectorField(col.config,"key",{storeDesc:this._context.storeDesc});
				break;
			}
			this._setValidator(formElement,col);
			formElement.setUserData("type",type);
			formElement.addListener('changeValue', (function (e) {
				console.log("Target:"+e.getTarget());
				if( e.getTarget().getUserData("type") == "search"){
					var data = qx.lang.Json.stringify(e.getData(), null, 4);
					console.log("data_from_selector:" + data);
					this._setLineValues(e.getTarget(),e.getData());
				}

				var odata = this.data;	
				this.data = this._getValues();
				//this.data = qx.lang.Json.stringify(this.data, null, 4);
				console.log("data:" + this.data);
				this.__lockSetValue = true;
				this.fireDataEvent("changeValue", this.data, odata);
				this.__lockSetValue = false;
			}).bind(this))
			return formElement;
		},
		_createToolbar: function () {
			var toolbar = new qx.ui.container.Composite(new qx.ui.layout.VBox());
			toolbar.setUserData("key", "toolbar");
			var hor = new qx.ui.container.Composite(new qx.ui.layout.HBox());

			var bAdd = new ms123.form.GridInputButton("icon/16/actions/list-add.png");
			bAdd.addListener("execute", function (e) {
				var ln = this._getLineNumber(e.getTarget());
				this._addLine(this._createOneLine(this._config.items), ln + 1, {
					flex: 1
				});
			}, this);
			hor.add(bAdd);

			var bDel = new ms123.form.GridInputButton("icon/16/actions/list-remove.png");
			bDel.addListener("execute", function (e) {
				var count = this._getLineCount();
				if (count < 2) return;
				var ln = this._getLineNumber(e.getTarget());
				this._removeLine(ln);
				var odata = this.data;	
				this.data = this._getValues();
				this.__lockSetValue = true;
				this.fireDataEvent("changeValue", this.data, odata);
				this.__lockSetValue = false;
			}, this);
			hor.add(bDel);
			toolbar.add(hor);


			var hor = new qx.ui.container.Composite(new qx.ui.layout.HBox());
			var bUp = new ms123.form.GridInputButton("icon/16/actions/go-up.png");
			bUp.addListener("execute", function (e) {
				var count = this._getLineCount();
				if (count < 2) return;
				var ln = this._getLineNumber(e.getTarget());
				if (ln == 0) return;
				this._swap(ln - 1);
			}, this);
			hor.add(bUp);

			var bDown = new ms123.form.GridInputButton("icon/16/actions/go-down.png");
			bDown.addListener("execute", function (e) {
				var count = this._getLineCount();
				if (count < 2) return;
				var ln = this._getLineNumber(e.getTarget());
				if (ln >= (count - 1)) return;
				this._swap(ln);
			}, this);
			hor.add(bDown);
			toolbar.add(hor);

			return toolbar;
		},
		_addLine: function (w, index) {
			this._lineContainer.addAt(w, index);
		},
		_removeLine: function (index) {
			return this._lineContainer.removeAt(index);
		},
		_getLineCount: function (w) {
			var lines = this._lineContainer.getChildren();
			return lines.length;
		},
		_getLineNumber: function (w) {
			var lines = this._lineContainer.getChildren();
			for (var i = 0; i < lines.length; i++) {
				var line = lines[i];
				var b = this._findWidget(line, w);
				if (b) return i;
			}
		},
		_getValues: function () {
			var lines = this._lineContainer.getChildren();
			var ret = [];
			for (var i = 0; i < lines.length; i++) {
				var line = lines[i];
				var map = {};
				ret.push(map);

				for (var j = 0; j < this._config.items.length; j++) {
					var col = this._config.items[j];
					var w = this._findWidgetByKey(line, col.colname);
					if( w.getUserData("type").match(/^date/)){
						try{
							map[col.colname] = w.getValue().getTime();
						}catch(e){}
					}else{
						map[col.colname] = w.getValue();
					}
				}
			}
			return ret;
		},
		_getLine: function (w) {
			var lines = this._lineContainer.getChildren();
			for (var i = 0; i < lines.length; i++) {
				var line = lines[i];
				var b = this._findWidget(line, w);
				if (b) return line;
			}
		},
		_setLineValues:function(w,map){
			var line = this._getLine(w);
			console.log("line:"+line);
			var keys = Object.keys(map);
			for (var j = 0; j < this._config.items.length; j++) {
				var col = this._config.items[j];
				var w = this._findWidgetByKey(line, col.colname);
				if( keys.indexOf(col.colname)!=-1 || keys.indexOf(col.mapping)!=-1){
					var value = map[col.colname];
					if(col.mapping && col.mapping.length>0){
						value = map[col.mapping];
					}
					console.log("setValue:"+col.colname+"/"+col.mapping+"="+value);
					w.setValue(value);
				}
			}
		},
		_findWidget: function (widget, w) {
			if (widget == w) {
				return true;
			}
			if (widget instanceof qx.ui.container.Composite) {
				var childs = widget.getChildren();
				for (var i = 0; i < childs.length; i++) {
					var b = this._findWidget(childs[i], w);
					if (b) return b;
				}
			}
			return false;
		},
		_findWidgetByKey: function (widget, key) {
			if (widget.getUserData("key") == key) {
				return widget;
			}
			if (widget instanceof qx.ui.container.Composite) {
				var childs = widget.getChildren();
				for (var i = 0; i < childs.length; i++) {
					var w = this._findWidgetByKey(childs[i], key);
					if (w) return w;
				}
			}
			return null;
		},
		_swap: function (from) {
			var c1 = this._removeLine(from);
			var c2 = this._removeLine(from);
			this._addLine(c1, from);
			this._addLine(c2, from);
		},
		_setValidator:function(formElement,fieldData){
			var validator = null;
			if (formElement && fieldData.validation) {

				if (fieldData.validation.required) {
					formElement.setRequired(true);
				}
				if (fieldData.validation.filter) {
					formElement.setFilter(fieldData.validation.filter);
				}

				if (fieldData.validation.validator) {
					var validator = fieldData.validation.validator;
					if (typeof validator == "string") {
						if (qx.util.Validate[validator]) {
							validator = qx.util.Validate[validator]();
						} else if (validator.charAt(0) == "/") {
							if (fieldData.validation.invalidMessage) {
								validator = qx.util.Validate.regExp(new RegExp(validator.substr(1, validator.length - 2)),fieldData.validation.invalidMessage);
							}else{
								validator = qx.util.Validate.regExp(new RegExp(validator.substr(1, validator.length - 2)));
							}
						} else {
							this.error("Invalid string validator.");
						}
					} else if (!(validator instanceof qx.ui.form.validation.AsyncValidator) && typeof validator != "function") {
						this.error("Invalid validator.");
					}
				}
			}
		}
	}
});
