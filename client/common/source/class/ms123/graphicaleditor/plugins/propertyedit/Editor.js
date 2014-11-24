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
	* @ignore($H)
	* @ignore(Clazz)
	* @ignore(Clazz.extend)
*/
qx.Class.define("ms123.graphicaleditor.plugins.propertyedit.Editor", {
	extend: qx.ui.core.Widget,
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (facade, diagramName, direction) {
		this.base(arguments);
		this.facade = facade;
		this.direction = direction;
		this.diagramName = diagramName;
		this._setLayout(new qx.ui.layout.Dock());
		this.facade.getPropertyValue=this._getPropertyValue.bind(this);

		//without this, it is not in the js-file
		ms123.graphicaleditor.plugins.propertyedit.ShapeSelect;

		if (this.direction == "horizontal") {
			this.panelSpace = new qx.ui.container.Composite(new qx.ui.layout.VBox()).set({});
			this.panelSpace.setPadding(0);
			this._add(this.panelSpace, {
				edge: "center"
			});
		} else {
			this.tabView = new qx.ui.tabview.TabView().set({
				contentPadding: 2
			});
			this._add(this.tabView, {
				edge: "center"
			});
		}
		this.title = new qx.ui.basic.Label().set({
			allowGrowY: false
		});

		this.title.setRich(true);
		this._add(this.title, {
			edge: "north"
		});
		this.mainAttributePanel = this.createMainAttributePanel();
		this.moreAttributePanel = this.createMoreAttributePanel();
		this._oldValues={};
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
		edit: function (shapeSelection, isSameSelection) {
			if (!isSameSelection) {
				if (this.mainAttributePanel.hasChildren()) {
					this.mainAttributePanel.removeAll();
				}
				if (this.moreAttributePanel.hasChildren()) {
					this.moreAttributePanel.removeAll();
				}
			}
			this.shapeSelection = shapeSelection;
			if(  this.shapeSelection.shapes.first() instanceof ms123.oryx.core.Shape ){
				var parent = this.shapeSelection.shapes.first().getParentShape();
				if( parent != undefined ){
					this.parentID = parent.getStencil().id().split("#")[1].toLowerCase();
				}
			}
			this.isBPMNRoot = (this.shapeSelection.shapes.first().getStencil().id() == "http://b3mn.org/stencilset/bpmn2.0#BPMNDiagram"); //@@@MS  TODO
			this.setPropertyWindowTitle();
			this.identifyCommonProperties();
			this.setCommonPropertiesValues();

			// Create the Properties
			this.__propagateChanges = false;
			if (isSameSelection) {
				this.updateProperties();
			} else {
				this.keyFormElementMap = {};
				this.keyFormLabelMap = {};
				this.createProperties();
			}
			this.__propagateChanges = true;
		},

		createMainAttributePanel: function () {
			if (this.direction == "horizontal") {
				var panel = new ms123.widgets.CollapsablePanel(this.tr("ge.PropertyWindow.oftenUsed"), this.createSingleGridLayout());
				this.panelSpace.add(panel);
				panel.setValue(true);
				return panel;
			} else {
				var page = new qx.ui.tabview.Page(this.tr("ge.PropertyWindow.oftenUsed")).set({
					showCloseButton: false
				});
				page.setLayout(new qx.ui.layout.Grow());
				this.tabView.add(page);
				var scroll = new qx.ui.container.Scroll();
				page.add(scroll);

				var content = new qx.ui.container.Composite();
				content.setLayout(this.createDoubleGridLayout());
				scroll.add(content);
				return content;
			}
		},
		createMoreAttributePanel: function () {
			if (this.direction == "horizontal") {
				var panel = new ms123.widgets.CollapsablePanel(this.tr("ge.PropertyWindow.moreProps"), this.createSingleGridLayout());
				this.panelSpace.add(panel);
				panel.setValue(true);
				return panel;
			} else {
				var page = new qx.ui.tabview.Page(this.tr("ge.PropertyWindow.moreProps")).set({
					showCloseButton: false
				});
				page.setLayout(new qx.ui.layout.Grow());
				this.tabView.add(page);
				var scroll = new qx.ui.container.Scroll();
				page.add(scroll);

				var content = new qx.ui.container.Composite();
				content.setLayout(this.createDoubleGridLayout());
				scroll.add(content);
				return content;
			}
		},

		createSingleGridLayout: function () {
			var layout = new qx.ui.layout.Grid();
			layout.setSpacing(2);
			layout.setColumnFlex(0, 3);
			layout.setColumnFlex(1, 12);
			layout.setColumnWidth(0, 80);
			layout.setColumnWidth(1, 80);
			layout.setColumnMinWidth(0, 80);
			layout.setColumnMinWidth(1, 80);
			layout.setColumnAlign(1, "right", "top");
			return layout;
		},


		createDoubleGridLayout: function () {
			var layout = new qx.ui.layout.Grid();
			layout.setSpacing(4);
			layout.setColumnAlign(0, "left", "top");
			layout.setColumnAlign(1, "left", "top");
			layout.setColumnFlex(1, 1);
			layout.setColumnAlign(2, "left", "top");
			layout.setColumnAlign(3, "left", "top");
			layout.setColumnFlex(3, 1);
			return layout;
		},

		createLabelText: function (name, item) {
			var required = "";
			if (item.getRequired && item.getRequired()) {
				required = " <span style='color:red'>*</span> ";
			}

			var colon = name.length > 0 || item.getRequired() ? " :" : "";
			if( name.length>1 && name.match(/^@/)){
				name = this.tr(name.substring(1));
			}
			var text = name + required + colon;
			return "<div style='white-space:nowrap;overflow:hidden;font-family:arial;font-size:9px'>" + text + "</div>";
		},
		createTitleLabelText: function (text) {
			return "<div style='white-space:nowrap;overflow:hidden;color:#314A6E;padding:3px;margin:3px;font-size:12px'>" + text + "</div>";
		},

		beforeEdit: function (e) {
			var formElement = e.getTarget();
			var key = formElement.getUserData("key");
			this._oldValues[key] = new Hash();
			this.shapeSelection.shapes.each((function (shape) {
				this._oldValues[key][shape.getId()] = shape.properties[key];
			}).bind(this));
		},


		afterEdit: function (e) {
			if (this.__propagateChanges == false) return;

			var formElement = e.getTarget();
			var key = formElement.getUserData("key");
			var selectedElements = this.shapeSelection.shapes;
			var oldValues = this._oldValues[key];
			var oldValuesClone = ms123.util.Clone.clone(oldValues);
			var newValue = formElement.getValue();
			if( selectedElements.length==1){
				if(oldValues[selectedElements[0].getId()]==newValue){
					return;
				}
				oldValues[selectedElements[0].getId()]= newValue;
			}
			var facade = this.facade;
			var self=this;
			// Implement the specific command for property change
			var commandClass = Clazz.extend({
				construct: function () {
					this.key = key;
					this.selectedElements = selectedElements;
					this.oldValues = oldValuesClone;
					this.newValue = newValue;
					this.facade = facade;
				},
				execute: function () {
					this.selectedElements.each((function (shape) {
//					if (!shape.getStencil().property(this.key).readonly()) {
							shape.setProperty(this.key, this.newValue);
//					}
					}).bind(this));
					this.facade.setSelection(this.selectedElements);
					this.facade.getCanvas().update();
					this.facade.updateSelection();
				},
				rollback: function () {
					this.selectedElements.each((function (shape) {
						shape.setProperty(this.key, this.oldValues[shape.getId()]);
					}).bind(this));
					this.facade.setSelection(this.selectedElements);
					this.facade.getCanvas().update();
					this.facade.updateSelection();
					self._updateVisible();
				}
			})
			var command = new commandClass();
			this.facade.executeCommands([command]);

			this.facade.raiseEvent({
				type: ms123.oryx.Config.EVENT_PROPWINDOW_PROP_CHANGED,
				elements: selectedElements,
				key: key,
				value: newValue
			});
		},

		// Changes made in the property window will be shown directly
		editDirectly: function (key, value) {
			if (this.__propagateChanges == false) return;
			this._updateVisible();
			this.shapeSelection.shapes.each((function (shape) {
//				if (!shape.getStencil().property(key).readonly()) {
					shape.setProperty(key, value);
					//shape.update();
				//}
			}).bind(this));

			/* Propagate changed properties */
			var selectedElements = this.shapeSelection.shapes;

			this.facade.raiseEvent({
				type: ms123.oryx.Config.EVENT_PROPWINDOW_PROP_CHANGED,
				elements: selectedElements,
				key: key,
				value: value
			});

			this.facade.getCanvas().update();

		},
		_updateVisible:function(){
			var env = this._getEnvFromFormElements();
			this.shapeSelection.commonProperties.each((function (pair, index) {

				var key = pair.prefix() + "-" + pair.id();
				var attribute = this.shapeSelection.commonPropertiesValues[key];
				var formElement = this.keyFormElementMap[key];
				var label = this.keyFormLabelMap[key];
				if (formElement) {
					var b = this.__maskedEval(pair.visible(),env);
					formElement.setVisibility(b ? "visible" : "excluded");
					label.setVisibility(b ? "visible" : "excluded");

					var b = this.__maskedEval(pair.readonly(),env);
					if( formElement.setReadOnly){
						formElement.setReadOnly(b);
					}
					formElement.setEnabled(!b);
				}
			}).bind(this));
		},
		__maskedEval: function (scr, env) {
			if( scr === false) return false;
			if( scr === true) return true;
			env["parent"] = this.parentID;
			return (new Function("with(this) { return " + scr + "}")).call(env);
		},

		// if a field becomes invalid after editing the shape must be restored to the old value
		updateAfterInvalid: function (key) {
			//console.log("updateAfterInvalid:" + key);
			var oldValues = this._oldValues[key];
			this.shapeSelection.shapes.each((function (shape) {
//				if (!shape.getStencil().property(key).readonly()) {
					shape.setProperty(key, oldValues[shape.getId()]);
					shape.update();
//				}
			}).bind(this));

			this.facade.getCanvas().update();
		},

		/**
		 * Changes the title of the property window panel according to the selected shapes.
		 */
		setPropertyWindowTitle: function () {
			var title = "";
			if (this.shapeSelection.shapes.length == 1) {
				title = this.tr("ge.PropertyWindow.title") + ' (' + this.shapeSelection.shapes.first().getStencil().title() + ')';
			} else {
				title = this.tr("ge.PropertyWindow.title") + ' (' + this.shapeSelection.shapes.length + ' ' + this.tr("ge.PropertyWindow.selected") + ')';
			}
			this.title.setValue(this.createTitleLabelText(title));
		},
		/**
		 * Sets this.shapeSelection.commonPropertiesValues.
		 * If the value for a common property is not equal for each shape the value
		 * is left empty in the property window.
		 */
		setCommonPropertiesValues: function () {
			this.shapeSelection.commonPropertiesValues = new Hash();
			this.shapeSelection.commonProperties.each((function (property) {
				var key = property.prefix() + "-" + property.id();
				var emptyValue = false;
				var firstShape = this.shapeSelection.shapes.first();

				this.shapeSelection.shapes.each((function (shape) {
					if (firstShape.properties[key] != shape.properties[key]) {
						emptyValue = true;
					}
				}).bind(this));

				/* Set property value */
				if (!emptyValue) {
					this.shapeSelection.commonPropertiesValues[key] = this.__supplant(firstShape.properties[key]);
				}
			}).bind(this));
		},

		/**
		 * Returns the set of stencils used by the passed shapes.
		 */
		getStencilSetOfSelection: function () {
			var stencils = new Hash();

			this.shapeSelection.shapes.each(function (shape) {
				stencils[shape.getStencil().id()] = shape.getStencil();
			})
			return stencils;
		},

		/**
		 * Identifies the common Properties of the selected shapes.
		 */
		identifyCommonProperties: function () {
			this.shapeSelection.commonProperties.clear();

			/** 
			 * A common property is a property, that is part of 
			 * the stencil definition of the first and all other stencils.
			 */
			var stencils = this.getStencilSetOfSelection();
			var firstStencil = stencils.values().first();
			var comparingStencils = stencils.values().without(firstStencil);


			if (comparingStencils.length == 0) {
				this.shapeSelection.commonProperties = firstStencil.properties();
			} else {
				var properties = new Hash();

				/* put all properties of on stencil in a Hash */
				firstStencil.properties().each(function (property) {
					properties[property.namespace() + '-' + property.id() + '-' + property.type()] = property;
				});

				/* Calculate intersection of properties. */

				comparingStencils.each(function (stencil) {
					var intersection = new Hash();
					stencil.properties().each(function (property) {
						if (properties[property.namespace() + '-' + property.id() + '-' + property.type()]) {
							intersection[property.namespace() + '-' + property.id() + '-' + property.type()] = property;
						}
					});
					properties = intersection;
				});

				this.shapeSelection.commonProperties = properties.values();
			}
			//	console.log("identifyCommonProperties:" + this.shapeSelection.commonProperties);
		},

		/**
		 * Creates the properties for from the properties of the  selected shapes.
		 */
		createProperties: function () {
			var properties = {};
			var form = new qx.ui.form.Form();
			var env = this._getEnvFromProperties();
			if (this.shapeSelection.commonProperties) {

				var prow = 0;
				var mrow = 0;
				var pcol = 0;
				var mcol = 0;
				var formcols = this.direction == "horizontal" ? 1 : 2;
				this.shapeSelection.commonProperties.each((function (pair, index) {
					var key = pair.prefix() + "-" + pair.id();
					var name = pair.title();
					var attribute = this.shapeSelection.commonPropertiesValues[key];
					var refToViewFlag = false;

					var formElement;
					var isBPMNDiagramName = (this.isBPMNRoot && pair.id() == 'name');
					if( this.isBPMNRoot ){
					}
					if (isBPMNDiagramName) {
						console.log("setProperty:"+key+"="+this.diagramName);
						this.shapeSelection.shapes.first().setProperty(key, this.diagramName);
					}
					var isTargetNamespace = (this.isBPMNRoot && pair.id() == 'targetnamespace');
					if (isTargetNamespace) {
						this.shapeSelection.shapes.first().setProperty(key, this.facade.storeDesc.getNamespace());
					}
					if (pair.include()) {
						switch (pair.type()) {
						case ms123.oryx.Config.TYPE_TEXT:
							formElement = new ms123.graphicaleditor.plugins.propertyedit.TextAreaField(key, pair.config(),this.facade);
							formElement.addListener('changeValue', (function (e) {
								this.editDirectly(key, e.getData());
								this.afterEdit(e);
							}).bind(this))
							break;
						case ms123.oryx.Config.TYPE_HTML:
							formElement = new ms123.graphicaleditor.plugins.propertyedit.HtmlAreaField(key, pair.config(),this.facade);
							formElement.addListener('changeValue', (function (e) {
								this.editDirectly(key, e.getData());
								this.afterEdit(e);
							}).bind(this))
							break;
						case ms123.oryx.Config.TYPE_STRING:
							var formElement = new qx.ui.form.TextField();
							formElement.setRequired(!pair.optional());
							formElement.setMaxLength(pair.length());
							formElement.setLiveUpdate(false);
							var filter = pair.filter() || this._getFilterForValidator(pair.validator()) || this._getFilterForKey(key);
							if( filter ){
								formElement.setFilter(new RegExp(filter));
							}
							formElement.addListener('keyup', (function (e) {
								var value = formElement.getValue();
								if( (value == null || value == "") && pair.optional()){
									this.editDirectly(key, value);
									formElement.setValid(true);
								}else{
									var valid = this._validate(key, pair.validator(),value);
									if( valid ){
										formElement.setValid(true);
										this.editDirectly(key, value);
									}else{
										formElement.setValid(false);
									}
								}
							}).bind(this));

							if (isBPMNDiagramName) formElement.setEnabled(false);
							if (isTargetNamespace) formElement.setEnabled(false);
							break;
						case ms123.oryx.Config.TYPE_INTEGER:
							var formElement = new ms123.graphicaleditor.plugins.propertyedit.NumberField();
							formElement.setWidth(50);
							formElement.setFilter(/[0-9]/);
							formElement.setLiveUpdate(false);
							formElement.setRequired(!pair.optional());
							formElement.addListener('changeValue', (function (e) {
								console.log("NumberField.changeValue:" + formElement.getValue() + "/" + key);
								this.editDirectly(key, formElement.getValue());
							}).bind(this));
							break;
						case ms123.oryx.Config.TYPE_BOOLEAN:
							formElement = new qx.ui.form.CheckBox();
							formElement.addListener('changeValue', (function (e) {
								this.editDirectly(key, formElement.getValue());
								this.afterEdit(e);
							}).bind(this));
							break;
						case ms123.oryx.Config.TYPE_MODULE_SELECTOR:
							formElement = new ms123.graphicaleditor.plugins.propertyedit.ModuleSelectorField(pair.config(), key, this.facade);
							formElement.addListener('changeValue', (function (e) {
								console.log("changeValue.moduleselector:" + e.getData() + "/" + formElement.getValue());
								this.editDirectly(key, e.getData());
								this.afterEdit(e);
							}).bind(this))
							break;
						case ms123.oryx.Config.TYPE_RESOURCE_SELECTOR:
							formElement = new ms123.form.ResourceSelectorField(pair.config(), key, this.facade);
							formElement.addListener('changeValue', (function (e) {
								console.log("changeValue.resourceselector:" + e.getData() + "/" + formElement.getValue());
								this.editDirectly(key, e.getData());
								this.afterEdit(e);
							}).bind(this))
							break;
						case ms123.oryx.Config.TYPE_MULTISELECT:
							formElement = new ms123.graphicaleditor.plugins.propertyedit.MultiSelectField(pair.config(), key, this.facade,pair.title());
							formElement.addListener('changeValue', (function (e) {
								console.log("changeValue.multiselect:" + e.getData() + "/" + formElement.getValue());
								this.editDirectly(key, e.getData());
								this.afterEdit(e);
							}).bind(this))
							break;
						case ms123.oryx.Config.TYPE_COLOR:
							formElement = new ms123.graphicaleditor.plugins.propertyedit.ColorPopup();
							formElement.addListener('changeValue', (function (e) {
								this.editDirectly(key, formElement.getValue());
								this.afterEdit(e);
							}).bind(this));
							break;
						case ms123.oryx.Config.TYPE_COMBO:
							var items = pair.items();
							var options = [];
							formElement = new qx.ui.form.ComboBox();
							items.each((function (value) {
								var option = [value.title(), null, value.value()];
								//if (value.value() == attribute) attribute = value.title();
								if (value.refToView()[0]) refToViewFlag = true;
								var item = new qx.ui.form.ListItem(value.title(), this.__getResourceUrl(value.icon()), value.value());
								formElement.add(item);
							}).bind(this));
							formElement.addListener('changeSelection', (function (e) {
								this.editDirectly(key, formElement.getValue());
							}).bind(this))
							break;
						case ms123.oryx.Config.TYPE_CHOICE:
							var items = pair.items();
							var options = [];
							formElement = new ms123.graphicaleditor.plugins.propertyedit.SelectBox();
							var config = pair.config();
							if( config ){
								items.each((function (value) {
									if (value.refToView()[0]) refToViewFlag = true;
									var title = this._notEmpty(value.title()) ?  value.title() : value.value();
									var item = new qx.ui.form.ListItem(title, this.__getResourceUrl(value.icon()), value.value());
									//this._createTooltip(item,"Shit");
									formElement.add(item);
								}).bind(this));
								try{
									var obj = window;
									var parts = config.type.split("\.");
									for(var i=0; i< parts.length;i++){
										obj = obj[parts[i]];
									}
									new obj( this.facade, formElement, config );								
								}catch(e){
									console.error("Editor.Ex:"+e);
								}
							}else{
								items.each((function (value) {
									if (value.refToView()[0]) refToViewFlag = true;
									var item = new qx.ui.form.ListItem(value.title(), this.__getResourceUrl(value.icon()), value.value());
									formElement.add(item);
								}).bind(this));
							}
							formElement.addListener('changeSelection', (function (e) {
								this.editDirectly(key, formElement.getValue());
							}).bind(this))
							break;
						case ms123.oryx.Config.TYPE_COMPLEX:
							var config = pair.config() || {};
							if( config.kind == "enum" ){
								formElement = new ms123.graphicaleditor.plugins.propertyedit.EnumField( pair.config(), pair.title(), pair.complexItems(), key, this.facade);
							}else if( config.kind == "filterparam" ){
								formElement = new ms123.graphicaleditor.plugins.propertyedit.FilterParamField( pair.config(), pair.title(), pair.complexItems(), key, this.facade);
							}else{
								formElement = new ms123.graphicaleditor.plugins.propertyedit.ComplexListField( pair.config(), pair.title(), pair.complexItems(), key, this.facade);
							}
							formElement.addListener('changeValue', (function (e) {
								this.editDirectly(key, e.getData());
								this.afterEdit(e);
							}).bind(this))
							break;
						case ms123.oryx.Config.TYPE_CONSTRAINTS:
							var config = pair.config() || {};
							formElement = new ms123.graphicaleditor.plugins.propertyedit.ConstraintsField( pair.config(), pair.title(), pair.complexItems(), key, this.facade);
							formElement.addListener('changeValue', (function (e) {
								this.editDirectly(key, e.getData());
								this.afterEdit(e);
							}).bind(this))
							break;
						case ms123.oryx.Config.TYPE_FIELD_SELECTOR:
							formElement = new ms123.graphicaleditor.plugins.propertyedit.FieldSelectorField(pair.config(), pair.title(), key, this.facade);
							formElement.addListener('changeValue', (function (e) {
								this.editDirectly(key, e.getData());
								this.afterEdit(e);
							}).bind(this))
							break;
						}
					}
					if (formElement && pair.include()) {
						formElement.setUserData("key", key);
						if (attribute !== undefined) formElement.setValue(attribute);

						formElement.addListener('focusin', this.beforeEdit, this);
						if (!(formElement instanceof ms123.graphicaleditor.plugins.propertyedit.ComplexListField) && 
								!(formElement instanceof ms123.graphicaleditor.plugins.propertyedit.ConstraintsField) && 
								!(formElement instanceof ms123.graphicaleditor.plugins.propertyedit.EnumField) && 
								!(formElement instanceof ms123.graphicaleditor.plugins.propertyedit.FilterParamField) && 
								!(formElement instanceof ms123.graphicaleditor.plugins.propertyedit.MultiSelectField) && 
								!(formElement instanceof ms123.graphicaleditor.plugins.propertyedit.ModuleSelectorField) && 
								!(formElement instanceof ms123.form.ResourceSelectorField) && 
								!(formElement instanceof ms123.graphicaleditor.plugins.propertyedit.FieldSelectorField) && 
								!(formElement instanceof ms123.graphicaleditor.plugins.propertyedit.TextAreaField) && 
								!(formElement instanceof ms123.graphicaleditor.plugins.propertyedit.HtmlAreaField) && 
								!(formElement instanceof qx.ui.form.CheckBox) && 
								!(formElement instanceof ms123.graphicaleditor.plugins.propertyedit.ColorPopup)) {
							formElement.addListener('blur', (function (e) {
								if (!formElement.isValid(false)) {
									this.updateAfterInvalid(key);
								} else {
									this.afterEdit(e);
								}
							}).bind(this));
						}

						this.keyFormElementMap[key] = formElement;

						var name = pair.title();
						var icons = [];
						var attribute = this.shapeSelection.commonPropertiesValues[key];

						if (pair.refToView()[0] || refToViewFlag || pair.popular()) {
							pair.setPopular();
						}
						var labeltxt = this.createLabelText(pair.title(), formElement);
						var label = new qx.ui.basic.Label(labeltxt);
						this.keyFormLabelMap[key] = label;
						label.setRich(true);
						if (pair.description()) {
							var tt = new qx.ui.tooltip.ToolTip(pair.description());
							tt.setRich(true);
							tt.setShowTimeout(200);
							tt.setHideTimeout(100000);
							label.setToolTip(tt);
						}

						if( this.__maskedEval(pair.readonly(),env)){
							 if( formElement.setReadOnly){
							 	formElement.setReadOnly(true);
							 }
							 formElement.setEnabled(false);
						}
						if (!this.__maskedEval(pair.visible(),env)) {
							formElement.setVisibility("excluded");
							label.setVisibility("excluded");
						}


						properties[key] = attribute;
						if (pair.popular()) {
							this.mainAttributePanel.add(label, {
								row: prow,
								column: pcol
							});
							this.mainAttributePanel.add(formElement, {
								row: prow,
								column: pcol + 1
							});
							pcol += 2;
							if (pcol >= (formcols * 2)) {
								prow++;
								pcol = 0;
							}
						} else {
							this.moreAttributePanel.add(label, {
								row: mrow,
								column: mcol
							});
							this.moreAttributePanel.add(formElement, {
								row: mrow,
								column: mcol + 1
							});
							mcol += 2;
							if (mcol >= (formcols * 2)) {
								mrow++;
								mcol = 0;
							}
						}
					}
				}).bind(this));
			}
		},

		__supplant: function (scr, env) {
			if( scr == "null") return null;
			var ok = false;
			try {
				if (scr.indexOf("${") != -1) ok = true;
			} catch (e) {
				return scr;
			}
			if (ok == false) return scr;
			if (!env) env = {};
			env.namespace = this.facade.storeDesc.getNamespace();
			env.name = this.diagramName;
			env.diagramname = this.diagramName;
			return scr.replace(/[\\$@]{([^{}]*)}/g, function (a, b) {
				var r = env[b];
				return typeof r === 'string' || typeof r === 'number' ? r : a;
			});
		},

		updateProperties: function () {
			if (this.shapeSelection.commonProperties) {
				this.shapeSelection.commonProperties.each((function (pair, index) {

					var key = pair.prefix() + "-" + pair.id();
					var attribute = this.shapeSelection.commonPropertiesValues[key];
					var formElement = this.keyFormElementMap[key];
					if (formElement) {
						if (attribute !== undefined) formElement.setValue(attribute);
					}
				}).bind(this));
			}
		},
		_getEnvFromFormElements: function () {
			var env = {};
			if (this.shapeSelection.commonProperties) {
				this.shapeSelection.commonProperties.each((function (pair, index) {

					var key = pair.prefix() + "-" + pair.id();
					var formElement = this.keyFormElementMap[key];
					if (formElement) {
						env[pair.id()] = formElement.getValue();
					}
				}).bind(this));
			}
//			var value = qx.lang.Json.stringify(env, null, 4); console.log("ENV1:" + value);
			return env;
		},
		_getPropertyValue: function (id) {
			var value = null;
			if (this.shapeSelection.commonProperties) {
				this.shapeSelection.commonProperties.each((function (pair, index) {
					if( id == pair.id()){
						var key = pair.prefix() + "-" + pair.id();
						value = this.shapeSelection.commonPropertiesValues[key];
						return;
					}
				}).bind(this));
			}
			return value;
		},
		_getEnvFromProperties: function () {
			var env = {};
			if (this.shapeSelection.commonProperties) {
				this.shapeSelection.commonProperties.each((function (pair, index) {

					var key = pair.prefix() + "-" + pair.id();
					var attribute = this.shapeSelection.commonPropertiesValues[key];
					env[pair.id()]=attribute;
				}).bind(this));
			}
//			var value = qx.lang.Json.stringify(env, null, 4); console.log("ENV2:" + value);
			return env;
		},
		_notEmpty:function(s){
			return s != null && s.length>0;
		},
		_createTooltip:function(item,desc){
			var tt = new qx.ui.tooltip.ToolTip(desc);
			tt.setRich(true);
			tt.setShowTimeout(200);
			tt.setHideTimeout(100000);
			item.setToolTip(tt);
		},
		_getFilterForValidator:function(validator, value){
			if( validator == null){
				 return null;
			}
			if( validator=="isId()"){
				return "[0-9A-Za-z_]";
			}
			return null;
		},
		_getFilterForKey:function(key){
			if( key =="oryx-ws_id" || key == "oryx-xf_id"){
				return "[0-9A-Za-z_]";
			}
			return null;
		},
		_getValidatorForKey:function(key){
			if( key =="oryx-ws_id" || key == "oryx-xf_id"){
				return "isId()";
			}
			return null;
		},
		_validate:function(key, validator, value){
			if( validator == null){
				validator = this._getValidatorForKey(key);
				if( validator == null){
				 return true;
				}
			}
			if( validator=="isId()"){
				return value.match("^[A-Za-z]([0-9A-Za-z_]){0,48}$");
			}
			return value.match(validator);
		},
		__getResourceUrl: function (name) {
			if (name == undefined || !name || name == "") return null;
			if( name.match(/^data:image/)) return name;
			var am = qx.util.AliasManager.getInstance();
			var x = am.resolve("resource/ms123/stencilsets/bpmn/" + name);
			return x;
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
