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
 qooxdoo dialog library
 
 http://qooxdoo.org/contrib/project#dialog
 
 Copyright:
 2007-2010 Christian Boulanger
 
 License:
 LGPL: http://www.gnu.org/licenses/lgpl.html
 EPL: http://www.eclipse.org/org/documents/epl-v10.php
 See the LICENSE file in the project's top-level directory for details.
 
 Authors:
 *  Christian Boulanger (cboulanger)
 ************************************************************************ */

/**
 @ignore(moment)
 @ignore(Exception)
 */

/**
 @lint ignoreDeprecated(alert,eval) 
 */

/** **********************************************************************
 @require(qx.util.Serializer)
 @require(qx.util.Validate)
 ************************************************************************ */

/**
 * A dialog with a form that is constructed on-the-fly
 */
qx.Class.define("ms123.form.Form", {
	extend: ms123.form.Dialog,

	/**
	 *****************************************************************************
	 PROPERTIES
	 *****************************************************************************
	 */
	properties: {
		/**   
		 * Data to create a form with multiple fields. 
		 * So far implemented: 
		 *   TextField / TextArea 
		 *   ComboBox
		 *   SelectBox
		 *   RadioGroup
		 * 
		 * <pre>
		 * {
		 *  "username" : {   
		 *     'type'  : "TextField",
		 *     'label' : "User Name", 
		 *     'value' : ""
		 *   },
		 *   "address" : {
		 *     'type'  : "TextArea",
		 *     'label' : "Address",
		 *     'lines' : 3
		 *   },
		 *   "domain" : {
		 *     'type'  : "SelectBox", 
		 *     'label' : "Domain",
		 *     'value' : 1,
		 *     'options' : [
		 *       { 'label' : "Company", 'value' : 0 }, 
		 *       { 'label' : "Home",    'value' : 1 }
		 *     ]
		 *   },
		 *   "commands" : {
		 *    'type'  : "ComboBox", 
		 *     'label' : "Shell command to execute",
		 *     'options' : [
		 *       { 'label' : "ln -s *" }, 
		 *       { 'label' : "rm -Rf /" }
		 *     ]
		 *   }   
		 * }
		 * </pre>
		 */
		formData: {
			check: "Map",
			nullable: true,
			event: "changeFormData",
			apply: "_applyFormData"
		},

		/**
		 * The model of the result data
		 */
		model: {
			check: "qx.core.Object",
			nullable: true,
			event: "changeModel"
		},
		tabs: {
			check: "Array",
			init: null,
			nullable: true
		},
		useitCheckboxes: {
			init: false
		},
		entityName: {
			init: ""
		},
		useScroll: {
			init: true
		},
		buttons: {
			init: null,
			nullable: true
		},


		/**
		 * The default width of the column with the field labels
		 */
		labelColumnWidth: {
			check: "Integer",
			nullable: false,
			init: 100
		}
	},

	/**
	 *****************************************************************************
	 EVENTS
	 *****************************************************************************
	 */
	events: {
		"tabChanged": "qx.event.type.Data"
	},

	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */
	construct: function (properties) {
		this._layoutDescription = properties["tabs"];
		this._useitCheckboxes = properties["useitCheckboxes"];
		this._useScroll = properties["useScroll"];
		this._entityName = properties["entityName"];
		if (this._entityName === undefined) properties["entityName"] = "";
		if (this._useScroll === undefined) this._useScroll = true;
		this._buttons = properties["buttons"];
		this.__storeDesc = properties["storeDesc"] || ms123.StoreDesc.getNamespaceDataStoreDesc();
		delete properties.storeDesc;
		this.base(arguments, properties);
	},

	/**
	 *****************************************************************************
	 MEMBERS
	 *****************************************************************************
	 */
	members: {

		/**
		 ---------------------------------------------------------------------------
		 PRIVATE MEMBERS
		 ---------------------------------------------------------------------------
		 */

		_formContainer: null,
		_form: null,
		_formValidator: null,
		_formController: null,
		_layoutDescription: null,
		_useitCheckboxes: false,
		_useScroll: true,
		_entityName: null,
		_buttons: null,

		/**
		 ---------------------------------------------------------------------------
		 WIDGET LAYOUT
		 ---------------------------------------------------------------------------
		 */

		/**
		 * Create the main content of the widget
		 */
		_createWidgetContent: function () {

			/**
			 * groupbox
			 */
			var contentPadding;
			if (this._inWindow) {
				contentPadding = [16, 16, 16, 16];
			} else {
				contentPadding = [0, 0, 0, 0];
			}
			var groupboxContainer = new qx.ui.groupbox.GroupBox().set({
				contentPadding: contentPadding
			});
			groupboxContainer.setLayout(new qx.ui.layout.VBox(this._hasMessage ? 10 : 2));
			this.add(groupboxContainer);

			var hbox = new qx.ui.container.Composite;
			hbox.setLayout(new qx.ui.layout.HBox(10));
			if (this._hasMessage) {
				groupboxContainer.add(hbox);
			}
			/**
			 * Add message label
			 */
			if (this._hasMessage) {
				this._message = new qx.ui.basic.Label();
				this._message.setRich(true);
				this._message.setMinWidth(this.getWindowWidth());
				this._message.setAllowStretchX(true);
				hbox.add(this._message, {
					flex: 1
				});
			}

			/**
			 * Form container  
			 */
			this._formContainer = new qx.ui.container.Composite;
			this._formContainer.setLayout(new qx.ui.layout.Grow());
			if (!this._inWindow) {
				groupboxContainer.setAppearance("");
			}
			groupboxContainer.add(this._formContainer, {
				flex: 1
			});

			/**
			 * buttons pane
			 */
			var buttonPane = new qx.ui.container.Composite;
			var bpLayout = new qx.ui.layout.HBox(0);
			bpLayout.setAlignX("center");
			buttonPane.setLayout(bpLayout);
			this.buttonPane = buttonPane;
			groupboxContainer.add(buttonPane);

			if (this._buttons) {
				for (var i = 0; i < this._buttons.length; i++) {
					var b = this._createButton(this._buttons[i]);
					buttonPane.add(b);
				}
			} else {
				/**
				 * Ok Button 
				 */
				var okButton = this._createOkButton();
				buttonPane.add(okButton);

				/** 
				 * Cancel Button 
				 */
				var cancelButton = this._createCancelButton();
				buttonPane.add(cancelButton);
			}

		},

		/**
		 ---------------------------------------------------------------------------
		 APPLY METHODS
		 ---------------------------------------------------------------------------
		 */

		/**
		 * Constructs the form on-the-fly
		 */
		_applyFormData: function (formData, old) {
			this.formData = formData;
			/**
			 * remove container content, form, controller
			 */
			if (this._formController) {
				this.getModel().removeAllBindings();
				this._formController.dispose();
			}
			if (this._form) {
				this._form.getValidationManager().removeAllBindings();
				this._form.dispose();
			}
			this._formContainer.removeAll();

			/**
			 * if form is to be deleted
			 */
			if (!formData) {
				return;
			}

			/**
			 * if a model doesn't exist, create it from the
			 * form data
			 */
			if (!this.getModel()) {
				var modelData = {};
				for (var key in formData) {
					modelData[key] = formData[key].value !== undefined ? formData[key].value : null;
				}
				var model = qx.data.marshal.Json.createModel(modelData, true);
				this.setModel(model);
			}


			/**
			 * create new form and form controller
			 */
			this._form = new qx.ui.form.Form();
			this._formController = new qx.data.controller.Object(this.getModel());

			/**
			 * hook for subclasses to do something with the new form
			 */
			this._onFormReady(this._form);

			/**
			 * loop through form data array
			 */
			var startime = new Date().getTime();
			console.error("StartTime in Form:" + (startime));

			var formDataArr = null;
			if (Array.isArray(formData)) {
				formDataArr = formData;
			} else {
				formDataArr = [];
				for (var key in formData) {
					formDataArr.push([key, formData[key]]);
				}
			}
			if (formDataArr.length > 0 && formDataArr[0].rownum) {
				formDataArr.sort(function (a, b) {
					var rnA = 1000;
					var rnB = 1000;
					if (a[1].rownum) {
						rnA = a[1].rownum;
					}
					if (b[1].rownum) {
						rnB = b[1].rownum;
					}
					return rnA > rnB;
				});
			}

			this._formElementMap = {};
			for (var k = 0; k < formDataArr.length; k++) {
				var key = formDataArr[k][0];
				var fieldData = formDataArr[k][1];
				var formElement = null;

				switch (fieldData.type.toLowerCase()) {
				case "groupheader":
					this._form.addGroupHeader(fieldData.value);
					break;

				case "textarea":
					formElement = new ms123.form.TextArea(this._useitCheckboxes);
					formElement._setHeight(fieldData.lines * 16);
					if (this._useitCheckboxes) {
						var cb = formElement.getCheckBox();
						formElement.setUserData("useit", cb);
					}
					formElement.setUserData("single", true);
					break;
				case "decimalfield":
					formElement = new ms123.form.DecimalField(this._useitCheckboxes);
					if (fieldData.readonly && fieldData.readonly === true) {
						formElement.setReadOnly(true);
					}
					formElement.setFilter(/[0-9.,]/);
					if (this._useitCheckboxes) {
						var cb = formElement.getCheckBox();
						formElement.setUserData("useit", cb);
					}
					break;

				case "numberfield":
					formElement = new ms123.form.NumberField(this._useitCheckboxes);
					if (fieldData.readonly && fieldData.readonly === true) {
						formElement.setReadOnly(true);
					}
					formElement.setFilter(/[0-9]/);
					if (this._useitCheckboxes) {
						var cb = formElement.getCheckBox();
						formElement.setUserData("useit", cb);
					}
					break;

				case "textfield":
					formElement = new ms123.form.TextField(this._useitCheckboxes);
					if (fieldData.readonly && fieldData.readonly === true) {
						formElement.setReadOnly(true);
					}
					if (this._useitCheckboxes) {
						var cb = formElement.getCheckBox();
						formElement.setUserData("useit", cb);
					}
					break;

				case "uploadfield":
					formElement = new ms123.form.UploadField(this._useitCheckboxes);
					if (fieldData.readonly && fieldData.readonly === true) {
						formElement.setReadOnly(true);
					}
					if (this._useitCheckboxes) {
						var cb = formElement.getCheckBox();
						formElement.setUserData("useit", cb);
					}
					break;
				case "spinner":
					formElement = new qx.ui.form.Spinner();
					formElement.set({
						maximum: 1000,
						minimum: 0
					});

					//					if( fieldData.readonly && fieldData.readonly === true ){
					//						formElement.setReadOnly(true);
					//					}
					//					if (this._useitCheckboxes) {
					//						var cb = formElement.getCheckBox();
					//						formElement.setUserData("useit", cb);
					//					}
					break;

				case "actionbutton":
					formElement = new ms123.form.ActionButton(this._useitCheckboxes);
					var label = fieldData.buttonlabel || fieldData.label;
					formElement.setParams(label, fieldData.iconname, fieldData.action, this);
					if (fieldData.callback) {
						formElement.setCallback(fieldData.callback);
					} else {
						formElement.setCallback(this.getActionCallback());
					}
					if (this._useitCheckboxes) {
						var cb = formElement.getCheckBox();
						formElement.setUserData("useit", cb);
					}
					break;
				case "button":
					formElement = new ms123.form.Button(this._useitCheckboxes);
					formElement.setClazzName(fieldData.clazz);
					if (this._useitCheckboxes) {
						var cb = formElement.getCheckBox();
						formElement.setUserData("useit", cb);
					}
					break;
				case "relatedto":
					var context = {}
					context.storeDesc = this.__storeDesc;
					formElement = new ms123.form.RelatedTo(context, this._useitCheckboxes);
					formElement.setModule(fieldData.module);
					formElement.setFormData(formData, this._entityName);
					if (this._useitCheckboxes) {
						var cb = formElement.getCheckBox();
						formElement.setUserData("useit", cb);
					}
					break;

				case "selector":
					var context = {}
					context.storeDesc = this.__storeDesc;
					context.config = fieldData.config;
					context.form = this;
					formElement = new ms123.form.Selector(context, this._useitCheckboxes);
					if (this._useitCheckboxes) {
						var cb = formElement.getCheckBox();
						formElement.setUserData("useit", cb);
					}
					break;
				case "alert":
					formElement = new ms123.form.AlertOut(fieldData.message);
					break;
				case "passwordfield":
					formElement = new qx.ui.form.PasswordField();
					break;

				case "datefield":
				case "datetimefield":
					var m = qx.locale.Manager.getInstance();
					var lang = m.getLanguage();
					if (fieldData.type.toLowerCase() == "datefield") {
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
					if (this._useitCheckboxes) {
						var cb = new qx.ui.form.CheckBox();
						cb.setToolTipText(this.tr("usebox.use"));
						cb.addListener("click", function (e) {
							e.stop();
						}, this);
						formElement._add(cb, {
							flex: 0
						});
						formElement.setUserData("useit", cb);
					}
					break;

				case "combobox":
					//@todo use data model for list
					formElement = new qx.ui.form.ComboBox();
					fieldData.options.forEach(function (item) {
						var listItem = new qx.ui.form.ListItem(item.label, item.icon);
						formElement.add(listItem);
					});
					if (this._useitCheckboxes) {
						var cb = new qx.ui.form.CheckBox();
						cb.setToolTipText(this.tr("usebox.use"));
						cb.addListener("click", function (e) {
							e.stop();
						}, this);
						formElement._add(cb, {
							flex: 0
						});
						formElement.setUserData("useit", cb);
					}
					break;

				case "selectbox":
					if (!fieldData.selectable_items) {
						formElement = new ms123.form.SelectBox();
						formElement.createList(fieldData.options);
					} else {
						formElement = new ms123.form.SelectBox(fieldData.selectable_items);
						formElement.createList([]);
						fieldData.options = null;
					}

					if (this._useitCheckboxes) {
						var cb = new qx.ui.form.CheckBox();
						cb.setToolTipText(this.tr("usebox.use"));
						cb.addListener("click", function (e) {
							e.stop();
						}, this);
						cb.setCenter(false);
						formElement._add(cb, {
							flex: 1
						});
						formElement.setUserData("useit", cb);
					}
					break;

				case "checkbox":
					formElement = new qx.ui.form.CheckBox();
					if (this._useitCheckboxes) {
						var cb = new qx.ui.form.CheckBox();
						cb.setToolTipText(this.tr("usebox.use"));
						formElement._add(cb);
						formElement.setUserData("useit", cb);
					}
					break;

				case "gridinput":
					var context = {}
					context.storeDesc = this.__storeDesc;
					context.config = fieldData.config;
					formElement = new ms123.form.GridInput(context);
					if (this._useitCheckboxes) {
						var cb = formElement.getCheckBox();
						formElement.setUserData("useit", cb);
					}
					break;

				case "complexedit":
					var context = {}
					context.storeDesc = this.__storeDesc;
					context.config = fieldData.config;
					context.caption = fieldData.caption;
					context.toolbar = fieldData.toolbar;
					formElement = new ms123.form.ComplexEdit(context, key);
					if (this._useitCheckboxes) {
						var cb = formElement.getCheckBox();
						formElement.setUserData("useit", cb);
					}
					break;

				case "resourceselector":
					formElement = new ms123.form.ResourceSelectorField(fieldData.config, key, {
						storeDesc: this.__storeDesc
					});
					if (this._useitCheckboxes) {
						var cb = formElement.getCheckBox();
						formElement.setUserData("useit", cb);
					}
					break;

				case "tableedit":
					formElement = new ms123.form.TableEdit(fieldData.config);
					if (this._useitCheckboxes) {
						var cb = formElement.getCheckBox();
						formElement.setUserData("useit", cb);
					}
					break;

				case "tableselect":
					fieldData.config.storeDesc = this.__storeDesc;
					formElement = new ms123.form.TableSelect(fieldData.options, fieldData.config, fieldData.multiselection, key);
					if (this._useitCheckboxes) {
						var cb = new qx.ui.form.CheckBox();
						formElement._add(cb);
						formElement.setUserData("useit", cb);
					}
					break;

				case "doubleselectbox":
					formElement = new ms123.form.DoubleSelectBox();
					var model = qx.data.marshal.Json.createModel(fieldData.options);
					formElement.setModel(model);
					//new qx.data.controller.List(model, formElement, "value1");
					if (this._useitCheckboxes) {
						var cb = new ms123.form.MultiUseBox();
						cb.setValue("ignore");
						formElement._add(cb);
						formElement.setUserData("useit", cb);
					}
					break;
				case "traitdoubleselectbox":
					formElement = new ms123.form.TraitDoubleSelectBox({
						entityName: this._entityName
					});
					var model = qx.data.marshal.Json.createModel(fieldData.options);
					formElement.setModel(model);
					//new qx.data.controller.List(model, formElement, "value1");
					if (this._useitCheckboxes) {
						var cb = new ms123.form.MultiUseBox();
						cb.setValue("ignore");
						formElement._add(cb);
						formElement.setUserData("useit", cb);
					}
					break;

				case "treemultiselectbox":
				case "treeselectbox":
				case "treeselector":
					if (fieldData.type.toLowerCase() == "treeselector") {
						formElement = new ms123.form.TreeSelector();
					} else if (fieldData.type.toLowerCase() == "treeselectbox") {
						formElement = new ms123.form.TreeSelectBox();
					} else {
						formElement = new ms123.form.TreeMultiSelectBox();
					}
					fieldData.options.label = "ROOT";
					if (typeof fieldData.options == 'string') {
						formElement.setModel(fieldData.options);
					} else {
						var model = qx.data.marshal.Json.createModel(fieldData.options);
						formElement.setModel(model);
					}
					if (this._useitCheckboxes) {
						var cb = new ms123.form.MultiUseBox();
						cb.setValue("ignore");
						formElement._add(cb);
						formElement.setUserData("useit", cb);
					}
					break;
				case "traittreemultiselectbox":
					formElement = new ms123.form.TraitTreeMultiSelectBox({
						entityName: this._entityName
					});
					fieldData.options.label = "ROOT";
					var model = qx.data.marshal.Json.createModel(fieldData.options);
					formElement.setModel(model);
					if (this._useitCheckboxes) {
						var cb = new ms123.form.MultiUseBox();
						cb.setValue("ignore");
						formElement._add(cb);
						formElement.setUserData("useit", cb);
					}
					break;

				case "radiogroup":
					formElement = new qx.ui.form.RadioGroup();
					if (fieldData.orientation) {
						formElement.setUserData("orientation", fieldData.orientation);
					}
					var selected = null;
					fieldData.options.forEach(function (item) {
						var radioButton = new qx.ui.form.RadioButton(item.label);
						radioButton.setUserData("value", item.value !== undefined ? item.value : item.label);
						formElement.add(radioButton);
					}, this);
					break;

				case "label":
					formElement = new qx.ui.form.TextField(); // dummy
					formElement.setUserData("excluded", true);
					break;

				default:
					var ok = false;
					try {
						if (fieldData.type.indexOf(".") != -1) {
							var parts = fieldData.type.split("\.");
							var obj = window;
							for (var i = 0; i < parts.length; i++) {
								obj = obj[parts[i]];
							}
							formElement = new obj(fieldData.config, fieldData.options, fieldData.label);
						} else {
							formElement = new ms123.form[fieldData.type](fieldData.config, fieldData.options, fieldData.label);
						}
						if (this._useitCheckboxes) {
							var cb = new qx.ui.form.CheckBox();
							formElement._add(cb);
							formElement.setUserData("useit", cb);
						}
						ok = true;
					} catch (e) {
						console.log(e.stack);
						alert("EVAL.FORM:(" + fieldData.type + "):" + e);
					}
					if (!ok) {
						this.error("Invalid form field type:" + fieldData.type);
					}

				}

				/**
				 * Add form element to controller so that result data
				 * model is updated when form element value changes
				 */
				formElement.setUserData("formmodel", this.getModel());
				formElement.setUserData("key", key);
				formElement.setUserData("position", fieldData.position);
				formElement.setUserData("height", fieldData.height);
				formElement.setUserData("enabled", fieldData.enabled);
				formElement.setUserData("exclude", fieldData.exclude);
				formElement.setUserData("requiredExpr", fieldData.requiredExpr);

				if (fieldData.type.toLowerCase() != "textfield" && fieldData.readonly && fieldData.readonly === true) {
					formElement.setEnabled(false);
					var cb = formElement.getUserData("useit")
					if (cb) {
						cb.setEnabled(true);
					}
				}
				if (fieldData.resourceId) { //GraphicalEditor resourceId
					this._formElementMap[fieldData.resourceId] = formElement;
				}

				var self = this;
				switch (fieldData.type.toLowerCase()) {

					/**
					 * simple form elements
					 */
				case "textarea":
				case "textfield":
				case "uploadfield":
				case "numberfield":
				case "relatedto":
				case "selector":
				case "tableselect":
				case "complexedit":
				case "resourceselector":
				case "gridinput":
				case "button":
				case "alert":
				case "actionbutton":
				case "passwordfield":
				case "datefield":
				case "datetimefield":
				case "combobox":
				case "checkbox":
				case "spinner":
					this._formController.addTarget(
					formElement, "value", key, true, null, {
						"converter": function (value) {
							self._form.getValidationManager().validate();
							return value;
						}
					});
					if (fieldData.defaultValue) {
						if ((fieldData.type.toLowerCase() == "datefield" || fieldData.type.toLowerCase() == "datetimefield") && typeof fieldData.defaultValue == "string") {
							if (fieldData.defaultValue.indexOf(".") != -1) {
								var dateParts = fieldData.defaultValue.split(".");
								if (dateParts.length > 2) {
									var date = new Date(dateParts[2], (dateParts[1] - 1), dateParts[0]);
									formElement.setValue(date);
								}
							} else {
								formElement.setValue(new Date(fieldData.defaultValue));
							}
						} else {
							formElement.setValue(fieldData.defaultValue);
						}
					}
					break;

					/**
					 * single selection form elements
					 */
				case "selectbox":
					//if (fieldData.options == null || fieldData.options.length == 0) break; //@@@MS NO option
					this._formController.addTarget(
					formElement, "selection", key, true, {
						"converter": qx.lang.Function.bind(function (value) {
							var selected = null;
							var selectables = this.getSelectables();
							selectables.forEach(function (selectable) {
								var key = this.getUserData("key");
								try {
									if (selectable.getModel() && selectable.getModel().getValue() == value) {
										selected = selectable;
									}
								} catch (e) {
									console.error("Form.selectbox(" + key + "):" + e + "/model:" + selectable.getModel());
								}
							}, this);

							if (!selected) {
								if (selectables && selectables.length > 0) {
									return [selectables[0]];
								} else {
									return [];
								}
							}
							return [selected];
						}, formElement)
					}, {
						"converter": qx.lang.Function.bind(function (selection) {
							var value = null;
							try {
								if (selection && selection.length > 0) {
									if (selection[0].getModel()) {
										value = selection[0].getModel().getValue();
									}
								}
							} catch (e) {
								console.error("form.Form.selectbox:" + e);
								console.debug(e.stack);
							}
							var key = this.getUserData("key");
							return value;
						}, formElement)
					});

					break;

					/**
					 * multi edit table elements
					 */
				case "tableedit":
					this._formController.addTarget(formElement, "value", key, true, null, {
						"converter": function (value) {
							return value;
						}
					});
					break;

				case "doubleselectbox":
				case "traitdoubleselectbox":
				case "treemultiselectbox":
				case "traittreemultiselectbox":
					this._formController.addTarget(
					formElement, "selection", key, true, null, null);
					break;

				case "radiogroup":
					this._formController.addTarget(
					formElement, "selection", key, true, {
						"converter": qx.lang.Function.bind(function (value) {
							var selectables = this.getSelectables();
							var selection = [];
							if (value) {
								selectables.forEach(function (selectable) {
									var sValue = selectable.getUserData("value");
									if (sValue === value) {
										selection = [selectable];
									}
								}, this);
							}
							return selection;
						}, formElement)
					}, {
						"converter": function (selection) {
							var value = selection[0].getUserData("value");
							return value;
						}
					});
					break;

				default:
					this._formController.addTarget(formElement, "value", key, true, null, {
						"converter": function (value) {
							self._form.getValidationManager().validate();
							return value;
						}
					});
				}

				/**
				 * form element validation
				 */
				var validator = null;
				if (formElement && fieldData.validation) {

					/**
					 * is field required?
					 */
					if (fieldData.validation.required) {
						formElement.setRequired(true);
					}
					if (fieldData.validation.filter) {
						formElement.setFilter(fieldData.validation.filter);
					}

					/**
					 * is there a validator?
					 */
					if (fieldData.validation.validator) {
						var validator = fieldData.validation.validator;

						/**
						 * if validator is a string ...
						 */
						if (typeof validator == "string") {
							/**
							 * if a validation factory exists, use this
							 */
							if (qx.util.Validate[validator]) {
								validator = qx.util.Validate[validator]();
							}

							/**
							 * else, is it a regular expression?
							 */
							else if (validator.charAt(0) == "/") {
								if (fieldData.validation.invalidMessage) {
									validator = qx.util.Validate.regExp(new RegExp(validator.substr(1, validator.length - 2)), fieldData.validation.invalidMessage);
								} else {
									validator = qx.util.Validate.regExp(new RegExp(validator.substr(1, validator.length - 2)));
								}
							}

							/**
							 * error
							 */
							else {
								this.error("Invalid string validator.");
							}
						}

						/**
						 * in all other cases, it must be a funciton or an async validation
						 * object
						 */
						else if (!(validator instanceof qx.ui.form.validation.AsyncValidator) && typeof validator != "function") {
							this.error("Invalid validator.");
						}
					}

					/**
					 * Server validation?
					 */
					if (fieldData.validation.service) {
						var service = fieldData.validation.service;
						var self = this;
						validator = new qx.ui.form.validation.AsyncValidator(

						function (validatorObj, value) {
							if (!validatorObj.__asyncInProgress) {
								validatorObj.__asyncInProgress = true;
								qx.core.Init.getApplication().getRpcManager().execute(
								service.name, service.method, [value], function (response) {
									try {
										var valid = (response && typeof response == "object" && response.data) ? response.data : response;
										validatorObj.setValid(valid);
										validatorObj.__asyncInProgress = false;
									} catch (e) {
										alert(e)
									};
								});
							}
						});
					}
				}

				/**
				 * if field width is specified
				 */
				if (fieldData.width !== undefined) {
					formElement.setWidth(fieldData.width);
				}

				/**
				 * placeholder
				 */
				if (fieldData.placeholder !== undefined) {
					formElement.setPlaceholder(fieldData.placeholder);
				}

				/**
				 * events
				 */
				if (qx.lang.Type.isObject(fieldData.events)) {
					for (var type in fieldData.events) {
						try {
							var func = eval("(" + fieldData.events[type] + ")"); // eval is evil, I know.
							if (!qx.lang.Type.isFunction(func)) {
								throw new Exception();
							}
							//							alert("addListener:"+type+",formElement:"+formElement+",func:"+func);
							formElement.addListener(type, func, formElement);
						}
						catch (e) {
							this.warn("Invalid '" + type + "' event handler for form element '" + key + "'.");
						}
					}
				}

				/**
				 * add label and form element to form
				 */
				var label = fieldData.label;
				//formElement.setUserData("validator",validator);
				this._form.add(formElement, label, validator, key);
			}
			var endtime = new Date().getTime();
			console.error("Time in Form:" + (endtime - startime) + "/" + this._render + "/" + this._inWindow);

			/**
			 * render the form
			 */
			var view = null;
			if (this._render) {
				if (this._layoutDescription) {
					if (this._layoutDescription.resourceId) { //Graphicaldescription
						view = new ms123.form.FormRendererGE(this, this._layoutDescription, this._useScroll, this.inWindow, formData);
					} else {
						view = new ms123.form.FormTabRenderer(this._form, this._layoutDescription, this._useScroll, this.inWindow, formData);
						view.addListener("tabChanged", function (e) {
							this.fireDataEvent("tabChanged", e.getData(), null);
						}, this);
					}
				} else {
					view = new ms123.form.FormRenderer(this._form);
				}
				view.setAllowGrowX(true);
				this.view = view;
				this._formContainer.add(view);
			}

			/**
			 * validate the form
			 */
			this._form.getValidationManager().validate();

		},

		/**
		 * Hook for subclasses to do something with the form, for example
		 * in order to attach bindings to the validation manager.
		 * Default behavior: bind the enabled state of the "OK" button to the 
		 * validity of the current form.
		 * 
		 */
		_onFormReady: function (form) {
			if (this._okButton) {
				form.getValidationManager().bind("valid", this._okButton, "enabled");
			}
		},

		/**
		 * Create a button
		 * @return {qx.ui.form.Button}
		 */
		_createButton: function (b) {
			var button = new qx.ui.form.Button(b.label);
			if (b.enabled != null) {
				button.setEnabled(b.enabled);
			}
			button.setIcon(b.icon);
			button.setAllowStretchX(false);
			button.addListener("execute", function () {
				if (this._hide) this.hide();
				if (this.getCallback()) {
					this.getCallback().call(this.getContext(), this.getModel(), b.value);
				}
			}, this);
			b.button = button;
			return button;
		},

		setReadonly: function (readonly) {
			if (this.view) {
				this.view.setReadonly(readonly);
				this.buttonPane.setEnabled(!readonly);
			}
		},
		setButtonsReadonly: function (readonly) {
			var childs = this.buttonPane.getChildren();
			for (var i = 0; i < childs.length; i++) {
				childs[i].setEnabled(!readonly);
			}
		},

		beforeEdit: function (context) {
			var m = this.getModel();
			var props = qx.Class.getProperties(m.constructor);
			var items = this.getItems();
			for (var i = 0, l = props.length; i < l; i++) {
				var p = props[i];
				if (items[p]) {
					var clazz = items[p].constructor;
					var hasConfig = qx.Class.hasInterface(clazz, ms123.form.IConfig);
					if (hasConfig) {
						items[p].beforeEdit(context);
					}
				}
			}
		},
		beforeAdd: function (context) {
			var m = this.getModel();
			var props = qx.Class.getProperties(m.constructor);
			var items = this.getItems();
			for (var i = 0, l = props.length; i < l; i++) {
				var p = props[i];
				if (items[p]) {
					var clazz = items[p].constructor;
					var hasConfig = qx.Class.hasInterface(clazz, ms123.form.IConfig);
					if (hasConfig) {
						items[p].beforeAdd(context);
					}
				}
			}
		},
		beforeSave: function (context) {
			var m = this.getModel();
			var props = qx.Class.getProperties(m.constructor);
			var items = this.getItems();
			for (var i = 0, l = props.length; i < l; i++) {
				var p = props[i];
				if (items[p]) {
					var clazz = items[p].constructor;
					var hasConfig = qx.Class.hasInterface(clazz, ms123.form.IConfig);
					if (hasConfig) {
						items[p].beforeSave(context);
					}
				}
			}
		},
		afterSave: function (context) {
			var m = this.getModel();
			var props = qx.Class.getProperties(m.constructor);
			var items = this.getItems();
			for (var i = 0, l = props.length; i < l; i++) {
				var p = props[i];
				if (items[p]) {
					var clazz = items[p].constructor;
					var hasConfig = qx.Class.hasInterface(clazz, ms123.form.IConfig);
					if (hasConfig) {
						items[p].afterSave(context);
					}
				}
			}
		},
		fillForm: function (map) {
			if (map == null) return;
			var m = this.getModel();
			var props = qx.Class.getProperties(m.constructor);
			var items = this.getItems();
			for (var i = 0, l = props.length; i < l; i++) {
				try {
					var p = props[i];
					if (map[p] != undefined && map[p] != null) {
						if (this.formData[p].type == "DateField" || this.formData[p].type == "DateTimeField") {
							var d = new Date();
							if (map[p] != "") {
								if (!isNaN(map[p] - 1)) {
									d.setTime(map[p]);
									m.set(p, d);
								} else {
									var d = new Date(moment(map[p]));
									m.set(p, d);
								}
							} else {
								m.set(p, null);
							}
						} else if (this.formData[p].type == "DoubleSelectBox") {
							if (typeof map[p] === 'string') {
								map[p] = qx.lang.Json.parse(map[p], false);
							}
							m.set(p, map[p]);
						} else {
							m.set(p, map[p]);
						}
					} else {
						if (this.formData[p].type == "SelectBox") { //@@@MS First value as default
							if (this.formData[p].defaultValue) {
								m.set(p, this.formData[p].defaultValue);
							} else if (this.formData[p].options.length > 0) {
								var o = this.formData[p].options[0];
								m.set(p, o.value);
							}
						} else {
							if (this.formData[p].defaultValue) {
								m.set(p, this.formData[p].defaultValue);
							} else {
								m.set(p, null);
							}
						}
					}
					if (items[p]) items[p].setValid(true);
				} catch (e) {
					console.error("form.Form.fillForm:" + e + ",(" + p + "=" + map[p] + ")");
				}
			}
		},



		getValidationManager: function () {
			return this._form.getValidationManager();
		},
		validate: function () {
			return this._form.getValidationManager().validate();
		},
		getItems: function () {
			return this._form.getItems();
		},
		setData: function (map) {
			this.fillForm(map);
		},
		_printData: function () {
			var m = this.getModel();
			var items = this.getItems();

			var props = qx.Class.getProperties(m.constructor);
			for (var i = 0; i < props.length; i++) {
				var p = props[i];
				var val = m.get(p);
				var fd = this.formData[p];
				if ((fd.type == "DateField" || fd.type == "DateTimeField") && val != "" && typeof val == 'object' && val.constructor == Date) {
					val = val.getTime();
				}
				console.log("\tdata:" + p + "=" + val);
			}
		},
		getData: function () {
			var m = this.getModel();
			var items = this.getItems();

			var props = qx.Class.getProperties(m.constructor);
			var map = {};
			for (var i = 0; i < props.length; i++) {
				var p = props[i];
				var val = m.get(p);
				var fd = this.formData[p];
				if ((fd.type == "DateField" || fd.type == "DateTimeField") && val != "" && typeof val == 'object' && val.constructor == Date) {
					val = val.getTime();
				}
				map[p] = val;
			}
			return map;
		},

		getLabels: function () {
			var labels = {};
			var groups = this._form.getGroups();
			for (var i = 0; i < groups.length; i++) {
				var group = groups[i];
				for (var j = 0; j < group.names.length; j++) {
					var name = group.names[j];
					labels[name] = group.labels[j];
				}
			}
			return labels;
		},

		getFormElementByKey: function (key) {
			return this.getItems()[key];
		},
		getFormElement: function (resourceId) {
			return this._formElementMap[resourceId];
		},

		selectTab: function (tabid) {
			if (this.view instanceof ms123.form.FormTabRenderer) {
				this.view.selectTab(tabid);
			}
		},

		/**
		 ---------------------------------------------------------------------------
		 EVENT HANDLERS
		 ---------------------------------------------------------------------------
		 */

		/**
		 * Handle click on ok button. Calls callback with the result map
		 * @override
		 */
		_handleOk: function () {
			if (this._hide) this.hide();
			if (this.getCallback()) {
				this.getCallback().call(this.getContext(), this.getModel());
			}
			this.resetCallback();
		}
	}
});
