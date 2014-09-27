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
 * @ignore(jQuery)
 * @ignore($A)
 * @ignore(jsPlumb.*)
 * @ignore(Clazz.extend)
 */
qx.Class.define("ms123.datamapper.plugins.MappingEdit", {
	extend: ms123.baseeditor.Toolbar,
	include: [qx.locale.MTranslation],

	/**
	 * Constructor
	 */
	construct: function (facade, config) {
		this.base(arguments, facade);
		this._facade = facade;
		this._config = config;
		this._inputTree = facade.inputTree;
		this._outputTree = facade.outputTree;
		var group = "5";
		var add_msg = this.tr("datamapper.add");
		var remove_msg = this.tr("datamapper.remove");
		var edit_msg = this.tr("datamapper.edit");
		this._facade.offer({
			name: add_msg,
			description: add_msg,
			icon: "icon/16/actions/list-add.png",
			functionality: this._createStructureMapping.bind(this),
			group: group,
			index: 1,
			isEnabled: qx.lang.Function.bind(function () {
				return true;
			}, this)
		});
		this._facade.offer({
			name: remove_msg,
			description: remove_msg,
			icon: "icon/16/actions/list-remove.png",
			functionality: this._removeStructureMapping.bind(this),
			group: group,
			index: 2,
			isEnabled: qx.lang.Function.bind(function () {
				var sel = this._structureSelectBox.getSelection();
				var ms = this._structureSelectBox.getModelSelection();
				return ms && ms.getLength() > 0;
			}, this)
		});

		this._facade.offer({
			name: edit_msg,
			description: edit_msg,
			icon: "resource/ms123/edit.png",
			functionality: this._editStructureMappingName.bind(this),
			group: group,
			index: 2,
			isEnabled: qx.lang.Function.bind(function () {
				var ms = this._structureSelectBox.getModelSelection();
				return ms && ms.getLength() > 0;
			}, this)
		});

		this._facade.registerOnEvent(ms123.datamapper.Config.EVENT_TREE_CHANGED, this._updateMappings.bind(this));
		this._jsPlumb = jsPlumb.getInstance();

		this.addListener("resize", function (ev) {
			this.repaint();
		}, this);

		this.addListenerOnce("appear", function (ev) {
			this._initJsPlumb(this._jsPlumb);
			this._makeAllSourcesAndTargets();
			this._enableAllSourcesAndTargets(true);
			this._noUpdate = true;
			this.setMappings(config.mapping);
			this._noUpdate = false;
			this._updateMappings();
		}, this);

		var scrollbarYOutput = this._outputTree.getTree().getChildControl("scrollbar-y");
		scrollbarYOutput.addListener("scroll", function (e) {
			qx.event.Timer.once(function () {
				this._jsPlumb.repaintEverything();
			}, this, 200);
		}, this);
		var scrollbarXOutput = this._outputTree.getTree().getChildControl("scrollbar-x");
		scrollbarXOutput.addListener("scroll", function (e) {
			qx.event.Timer.once(function () {
				this._jsPlumb.repaintEverything();
			}, this, 200);
		}, this);

		var scrollbarY = this._inputTree.getTree().getChildControl("scrollbar-y");
		scrollbarY.addListener("scroll", function (e) {
			qx.event.Timer.once(function () {
				this._jsPlumb.repaintEverything();
			}, this, 200);
		}, this);

		var scrollbarX = this._inputTree.getTree().getChildControl("scrollbar-x");
		scrollbarX.addListener("scroll", function (e) {
			qx.event.Timer.once(function () {
				this._jsPlumb.repaintEverything();
			}, this, 200);
		}, this);

		this._inputTree.addListener("open", function (e) {
			var data = e.getData();
			this.executeCommandTreeOpenCloseCommand(data.item, data.open);
			qx.event.Timer.once(function () {
				this._jsPlumb.repaintEverything();
			}, this, 200);
		}, this);
		this._outputTree.addListener("open", function (e) {
			var data = e.getData();
			this.executeCommandTreeOpenCloseCommand(data.item, data.open);
			qx.event.Timer.once(function () {
				this._jsPlumb.repaintEverything();
			}, this, 200);
		}, this);
	},

	/**
	 * ****************************************************************************
	 * MEMBERS
	 * ****************************************************************************
	 */
	members: {
		_init: function () {
			this._mappings = {};
			this.setLayout(new qx.ui.layout.VBox());

			var upper = new qx.ui.container.Composite(new qx.ui.layout.HBox());
			var l = new qx.ui.basic.Label().set({
				rich: true
			});
			l.setValue("<div style='border-top:1px solid #c2c2c2;padding:3px;'><b>" + this.tr("datamapper.element_mapping") + "</b></div>");
			upper.add(l);

			var toolbar = new qx.ui.toolbar.ToolBar().set({});
			var sb = this._createElementMappingSelectBox();
			upper.add(toolbar, {
				flex: 1
			});
			this.add(upper);
			this.add(sb);
			this.setToolbar(toolbar);
			toolbar.setSpacing(2);
			this.groupIndex = new Hash();
		},
		_createElementMappingSelectBox: function () {
			var sb = new qx.ui.form.SelectBox();
			sb.addListener("changeSelection", function (e) {
				var item = this._structureSelectBox.getSelection()[0];
				if (item) {
					console.log("changeSelection:" + item.getModel());
				} else {
					console.log("changeSelection:to null");
				}
				if (this._internalUse === true) return;
				var item = this._structureSelectBox.getSelection()[0];
				if (item == null) {
					return;
				}
				this.executeCommandStructureSelectionChanged(e.getData()[0], e.getOldData()[0]);
			}, this);
			this._structureSelectBox = sb;
			return sb;
		},
		_getSelectedStructureMapping: function () {
			var item = this._structureSelectBox.getSelection()[0];
			if (item) {
				return item.getUserData(ms123.datamapper.Config.MAPPING_PARAM);
			}
			console.error("_getSelectedStructureMapping:null");
			return null;
		},
		_removeStructureMapping: function () {
			var item = this._structureSelectBox.getSelection()[0];
			var mapping = item.getUserData(ms123.datamapper.Config.MAPPING_PARAM);
			this.executeCommandInsertRemoveMapping(mapping, true);
		},
		executeCommandTreeOpenCloseCommand: function (item, open) {
			var self = this;
			var CommandClass = Clazz.extend({
				construct: function (item, open) {
					this.item = item;
					this.open = open;
				},
				execute: function () {
					this.item.setOpen(this.open);
					self._correctEndpointElements();
					self.repaint();
				},
				rollback: function () {
					this.item.setOpen(!this.open);
					self._correctEndpointElements();
					self.repaint();
				},
				toString: function () {
					return "TreeOpenClose";
				}
			})
			var command = new CommandClass(item, open);
			this._facade.executeCommands([command]);
			this._facade.update();
		},
		_editStructureMappingName: function () {
			var self = this;
			var CommandClass = Clazz.extend({
				construct: function (item, data, smapping) {
					this.item = item;
					this.mapping = smapping;
					this.data = data;
					this.oldName = this.item.getLabel();
					this.oldMap2parent = smapping.map2parent;
				},
				execute: function () {
					console.log("Setting:" + this.newName);
					this.mapping.name = this.data.name;
					this.mapping.map2parent = this.data.map2parent;
					this.item.setLabel(this.data.name);
				},
				rollback: function () {
					this.mapping.name = this.oldName;
					this.mapping.map2parent = this.oldMap2parent;
					this.item.setLabel(this.oldName);
				},
				toString: function () {
					return "EditStructureMapping";
				}
			})
			var item = this._structureSelectBox.getSelection()[0];
			var smapping = item.getUserData(ms123.datamapper.Config.MAPPING_PARAM);
			var fe = new ms123.datamapper.edit.StructureNameEditor(this._facade, this._context, {
				name: item.getLabel(),
				map2parent: smapping.map2parent
			});
			fe.addListener("changeValue", function (e) {
				var command = new CommandClass(item, e.getData(), smapping);
				this._facade.executeCommands([command]);
				this._facade.update();
			}, this);
		},
		executeCommandStructureSelectionChanged: function (newItem, oldItem) {
			var command = this.createCommandStructureSelectionChanged(newItem,oldItem);
			this._facade.executeCommands([command]);
			this._facade.update();
		},
		createCommandStructureSelectionChanged: function (newItem, oldItem) {
			var self = this;
			var CommandClass = Clazz.extend({
				construct: function (newItem, oldItem) {
					this.oldItem = oldItem;
					this.newItem = newItem;
				},
				execute: function () {
					self._internalUse = true;
					self._structureSelectBox.setSelection([newItem]);
					self._internalUse = false;
					var mapping = newItem.getUserData(ms123.datamapper.Config.MAPPING_PARAM);
					mapping.conn.toggleType("structureSelected");
					if (oldItem) {
						var mapping = oldItem.getUserData(ms123.datamapper.Config.MAPPING_PARAM);
						mapping.conn.setType("structure");
					}
					self._updateMappings();
				},
				rollback: function () {
					self._internalUse = true;
					self._structureSelectBox.setSelection(oldItem ? [oldItem] : []);
					self._internalUse = false;
					var mapping = newItem.getUserData(ms123.datamapper.Config.MAPPING_PARAM);
					mapping.conn.setType("structure");
					if (oldItem) {
						var mapping = oldItem.getUserData(ms123.datamapper.Config.MAPPING_PARAM);
						mapping.conn.setType("structureSelected");
					}
					self._updateMappings();
				},
				toString: function () {
					return "StructureSelectionChanged";
				}
			})
			var command = new CommandClass(newItem, oldItem);
			return command;
		},
		executeCommandInsertRemoveMapping: function (mapping, remove) {
			var command = this.createCommandInsertRemoveMapping(mapping, remove);
			this._facade.executeCommands([command]);
			this._facade.update();
		},
		createCommandInsertRemoveMapping: function (mapping, remove) {
			var structureMapping = this._mappings;
			var structureSelectBox = this._structureSelectBox;
			var self = this;
			var CommandClass = Clazz.extend({
				construct: function (mapping) {
					this.mappings = [];
					this.mappings.push(mapping);
					if (mapping.type == ms123.datamapper.Config.STRUCTURE_MAPPING) {
						var ckeys = Object.keys(mapping.children);
						var _this = this;
						ckeys.each(function (k) {
							_this.mappings.push(mapping.children[k]);
						});
					}
					this.smappingOld = self._getSelectedStructureMapping();
				},
				execute: function () {
					self._internalUse = true;
					if (remove) {
						var smapping = self._getSelectedStructureMapping();
						for (var i = 0; i < this.mappings.length; i++) {
							var m = this.mappings[i];
							if (m.type == ms123.datamapper.Config.STRUCTURE_MAPPING) {
								delete structureMapping[m.id];
								structureSelectBox.remove(m.item);
							} else {
								delete smapping.children[m.id];
							}
							if (m.conn) {
								self._internalUse = true;
								self._jsPlumb.detach(m.conn);
								self._internalUse = false;
								m.conn = null;
							}
						}
					} else {
						for (var i = 0; i < this.mappings.length; i++) {
							var m = this.mappings[i];
							if (m.conn == null) {
								var connMap = {
									source: self._getEndpointElementById(m.input.id, ms123.datamapper.Config.INPUT),
									target: self._getEndpointElementById(m.output.id, ms123.datamapper.Config.OUTPUT),
									anchors: ["RightMiddle", "LeftMiddle"]
								}
								m.conn = self._jsPlumb.connect(connMap);
								m.input.ep = self._getEndpointByElAndConn(connMap.source, m.conn);
								m.output.ep = self._getEndpointByElAndConn(connMap.target, m.conn);
							}
							m.conn.setParameter(ms123.datamapper.Config.MAPPING_PARAM, m);
							if (m.type == ms123.datamapper.Config.STRUCTURE_MAPPING) {
								structureMapping[m.id] = m;
								m.conn.setType("structure");
								m.conn.setDetachable(false);
								if (m.item == null) {
									m.item = new qx.ui.form.ListItem(m.name, null, m.id);
									m.item.setUserData(ms123.datamapper.Config.MAPPING_PARAM, m);
								}
								structureSelectBox.add(m.item);
								structureSelectBox.setSelection([m.item]);
							} else {
								m.conn.setType("attribute");
								var smapping = self._getSelectedStructureMapping();
								smapping.children[m.id] = m;
							}
						}
					}
					self._internalUse = false;
					self.onUpdate();
					self._updateMappings();
					self._raiseMappingChanged();
				},
				rollback: function () {
					self._internalUse = true;
					if (remove) {
						for (var i = 0; i < this.mappings.length; i++) {
							var m = this.mappings[i];
							var connMap = {
								source: self._getEndpointElementById(m.input.id, ms123.datamapper.Config.INPUT),
								target: self._getEndpointElementById(m.output.id, ms123.datamapper.Config.OUTPUT),
								anchors: ["RightMiddle", "LeftMiddle"]
							}
							m.conn = self._jsPlumb.connect(connMap);
							m.input.ep = self._getEndpointByElAndConn(connMap.source, m.conn);
							m.output.ep = self._getEndpointByElAndConn(connMap.target, m.conn);
							m.conn.setParameter(ms123.datamapper.Config.MAPPING_PARAM, m);
							if (m.type == ms123.datamapper.Config.STRUCTURE_MAPPING) {
								structureMapping[m.id] = m;
								m.conn.setType("structure");
								m.conn.setDetachable(false);
							} else {
								m.conn.setType("attribute");
								var smapping = self._getSelectedStructureMapping();
								smapping.children[m.id] = m;
							}
							if (m.type == ms123.datamapper.Config.STRUCTURE_MAPPING) {
								structureSelectBox.add(m.item);
								structureSelectBox.setSelection([m.item]);
								self._updateMappings();
							}
						}
					} else {
						if( this.smappingOld ){
							 structureSelectBox.setSelection([this.smappingOld.item]);
						}
						for (var i = 0; i < this.mappings.length; i++) {
							var m = this.mappings[i];
							self._jsPlumb.detach(m.conn);
							if (m.type == ms123.datamapper.Config.STRUCTURE_MAPPING) {
								delete structureMapping[m.id];
								structureSelectBox.remove(m.item);
							} else {
								var smapping = self._getSelectedStructureMapping();
								delete smapping.children[m.id];
							}
							m.conn = null;
						}
					}
					self._internalUse = false;
					self.onUpdate();
					self._updateMappings();
					self._raiseMappingChanged();
				},
				toString: function () {
					return "InsertRemoveMapping";
				}
			})
			var command = new CommandClass(mapping);
			return command;
		},
		_executeCommandListAsOneCommand: function (commandList) {
			var self = this;
			var CommandClass = Clazz.extend({
				construct: function (commands) {
					this.commands = commands;
					this.reverseCommands = ([].concat(this.commands)).reverse();
				},
				execute: function () {
					this.commands.each(function (command) {
						command.execute();
					});
				},
				rollback: function () {
					this.reverseCommands.each(function (command) {
						command.rollback();
					});
				},
				toString: function () {
					return "CommandListAsOneCommand";
				}
			})
			var command = new CommandClass(commandList);
			this._facade.executeCommands([command]);
			this._facade.update();
		},
		_createStructureMapping: function (e) {
			this._config.input = qx.lang.Json.parse(qx.util.Serializer.toJson(this._facade.inputTree.getModel()));
			this._config.output = qx.lang.Json.parse(qx.util.Serializer.toJson(this._facade.outputTree.getModel()));
			var se = new ms123.datamapper.edit.StructureEditor(this._facade, this._config, this,this.tr("datamapper.structure_editor"));
			se.addListener("changeValue", function (ev) {
				var mapping = ev.getData();
				console.log("mapping:" + JSON.stringify(mapping, null, 2));
				mapping.type = ms123.datamapper.Config.STRUCTURE_MAPPING;
				mapping.id = ms123.util.IdGen.id();
				mapping.children = {};
				this.executeCommandInsertRemoveMapping(mapping, false);
			}, this);
		},
		__createCleanMapping: function (isStructure, m, k, selected) {
			if (!m.input) return null;
			var ret = {
				id: k,
				name: m.name,
				type: m.type,
				map2parent: m.map2parent,
				selected: selected,
				input: {
					id: m.input.id,
					path: m.input.path,
					type: m.input.type
				},
				output: {
					id: m.output.id,
					path: m.output.path,
					type: m.output.type
				}
			};
			if (selected == null) {
				delete ret.selected;
			}
			if (isStructure) {
				if (ret.map2parent == null) {
					ret.map2parent = true;
				}
			} else {
				delete ret.map2parent;
			}
			return ret;
		},
		getMappings: function () {
			return this._mappings;
		},
		_getMappingBySourceAndTarget: function (inputId, outputId) {
			if (inputId == null || outputId == null) return null;
			var keys = Object.keys(this._mappings);
			var self = this;
			var result = null;
			keys.each(function (k) {
				var mapping = self._mappings[k];
				if (inputId == self._getId(mapping, ms123.datamapper.Config.INPUT) && outputId == self._getId(mapping, ms123.datamapper.Config.OUTPUT)) {
					result = mapping;
					return;
				}
				Object.keys(mapping.children).each(function (k) {
					var m = mapping.children[k];
					if (inputId == self._getId(m, ms123.datamapper.Config.INPUT) && outputId == self._getId(m, ms123.datamapper.Config.OUTPUT)) {
						result = m;
						return;
					}
				});
			});
			console.log("result:" + result);
			return result;
		},
		_getMappingByConnection: function (conn) {
			var keys = Object.keys(this._mappings);
			var self = this;
			var rmapping = null;
			keys.each(function (k) {
				var m = self._mappings[k];
				if (conn == m.conn) {
					rmapping = m;
				}
			});
			return rmapping;
		},
		_updateMappings: function () {
			if (this._noUpdate === true) return;
			this._removeBackgroundColor();
			this._setConnectionDisabledColor();
			this._makeAllSourcesAndTargets();
			this._enableAllSourcesAndTargets(false);

			this._enableItems();
		},
		_removeBackgroundColor: function () {
			var inputItems = this._inputTree.getItems();
			inputItems.each(function (item) {
				item.setBackgroundColor(null);
			});

			var outputItems = this._outputTree.getItems();
			outputItems.each(function (item) {
				item.setBackgroundColor(null);
			});
		},
		setMappings: function (mappings) {
			if (!mappings) return;
			var self = this;
			var selectedModel = null;
			var commandList = [];
			mappings.each(function (m, i) {
				self._mappings[m.id] = m;
				if (m.selected) {
					selectedModel = m.id;
				}
				var children = m.children;
				m.children = {};
				commandList.push(self.createCommandInsertRemoveMapping(m, false));
				console.log("children2:" + children);
				children.each(function (ck) {
					commandList.push(self.createCommandInsertRemoveMapping(ck, false));
				});
			});
			var listItems = this._structureSelectBox.getSelectables();
			listItems.each(function (listItem) {
				if (listItem.getModel() == selectedModel) {
					commandList.push(self.createCommandStructureSelectionChanged(listItem, null));
				}
			});
			this._executeCommandListAsOneCommand(commandList);
			this._raiseResetUndo();
		},
		getCleanMappings: function () {
			var item = this._structureSelectBox.getSelection()[0];
			var selectedModel = item ? item.getModel() : null;
			var keys = Object.keys(this._mappings);
			var mappings = [];
			var self = this;
			keys.each(function (k, index) {
				var m = self._mappings[k];
				var selected = false;
				if (m.id == selectedModel) {
					selected = true;
				}
				var cleanMapping = self.__createCleanMapping(true, m, k, selected);
				cleanMapping.children = [];
				var ckeys = Object.keys(m.children);
				ckeys.each(function (k) {
					var cm = m.children[k];
					var cleanChildMapping = self.__createCleanMapping(false, cm, k, null);
					if (cleanChildMapping) {
						cleanMapping.children.push(cleanChildMapping);
					}
				});
				mappings.push(cleanMapping)
			});
			return mappings;
		},
		isElementMapped:function(id, side){
			var item = this.getTreeItemById(id, side);
			if( item ){
				var m = item.getModel();
				var ret = this._isElementMapped(m,side);
				return ret;
			}
			return false;
		},
		_isElementMapped:function(m,side){
			var children = m.getChildren();
			for (var i = 0; i < children.getLength(); i++) {
				var c = children.getItem(i);
				if( c.getType() == ms123.datamapper.Config.NODETYPE_ELEMENT){
					var ret = this._isElementMapped(c,side);
					if( ret) return true;
				}else{
					var isMapped = this.isMapped(c.getId(),side);
					if( isMapped.hit == true ) return true;
				}
			}
			return false;
		},
		isMapped: function (id, side) {
			if (id == null) return false;
			var smapping = this._getSelectedStructureMapping();
			if (smapping == null) return false;
			var sid = this._getId(smapping,side);
			var keys = Object.keys(this._mappings);
			var self = this;
			var hit = false;
			var structure = false;
			var item = this._structureSelectBox.getSelection()[0];
			var selectedModel = item ? item.getModel() : null;

			var inSelectedCollection = false;
			var attrMapping =[];
			var sid = null;
			keys.each(function (k) {
				var mapping = self._mappings[k];
				var current_sid = self._getId(mapping,side);
				if (id == current_sid) {
					hit = true;
					sid = mapping.id;
					structure = true;
					return;
				}

				var isSelected=false;
				if (mapping.id == selectedModel) {
					isSelected = true;
					sid = mapping.id;
				}
				Object.keys(mapping.children).each(function (k) {
					var m = mapping.children[k];
					var mid = self._getId(m,side);
					if (id == mid) {
						hit = true;
						if (isSelected) {
							attrMapping.push(m);
						}
					}
				});
			});
			console.log("HIT:" + hit+"/"+id+"/"+side+"/"+attrMapping.length);
			return {
				hit: hit,
				sid: sid,
				attrMappings:attrMapping,
				structure: structure
			}
		},
		_correctEndpointElements: function () {
			var keys = Object.keys(this._mappings);
			var self = this;
			keys.each(function (k) {
				var mapping = self._mappings[k];

				var inputItem = self.getTreeItemById(mapping.input.id, ms123.datamapper.Config.INPUT);
				var outputItem = self.getTreeItemById(mapping.output.id, ms123.datamapper.Config.OUTPUT);
				var el = self._getVisibleElement(inputItem, ms123.datamapper.Config.INPUT);
				mapping.input.ep.setElement(el);
				inputItem.setUserData("epel", el);

				var el = self._getVisibleElement(outputItem, ms123.datamapper.Config.OUTPUT);
				mapping.output.ep.setElement(el);
				outputItem.setUserData("epel", el);

				Object.keys(mapping.children).each(function (k) {
					var m = mapping.children[k];
					var inputItem = self.getTreeItemById(m.input.id, ms123.datamapper.Config.INPUT);
					var outputItem = self.getTreeItemById(m.output.id, ms123.datamapper.Config.OUTPUT);

					var el = self._getVisibleElement(inputItem, ms123.datamapper.Config.INPUT);
					m.input.ep.setElement(el);
					inputItem.setUserData("epel", el);

					var el = self._getVisibleElement(outputItem, ms123.datamapper.Config.OUTPUT);
					m.output.ep.setElement(el);
					outputItem.setUserData("epel", el);
				});
			});
		},
		_getVisibleElement: function (item, side) {
			var pitem = this._getVisibleParentItem(item, side);
			var el = null;
			if (pitem == null) {
				el = item.getContentElement().getDomElement();
			} else {
				el = pitem.getContentElement().getDomElement();
			}
			return el;
		},
		_getVisibleParentItem: function (item, side) {
			var tree = this._getTree(side).getTree();
			var p = item.getParent();
			if (!p) {
				return null;
			}

			if (p.isVisible() && !p.isOpen() && this._getVisibleParentItem(p) == null) {
				return p;
			} else {
				return this._getVisibleParentItem(p, side);
			}
		},
		_setConnectionDisabledColor: function () {
			var keys = Object.keys(this._mappings);
			var self = this;
			keys.each(function (k) {
				var mapping = self._mappings[k];
				console.log("Disable:" + mapping.type + "/" + mapping.conn.id);
				mapping.conn.setType("structure");
				mapping.conn.setDetachable(false);
				mapping.conn.endpoints[0].removeClass("endpointEnabled");
				mapping.conn.endpoints[1].removeClass("endpointEnabled");
				Object.keys(mapping.children).each(function (k) {
					var m = mapping.children[k];
					console.log("\t----->Disable:" + m.type + "/" + m.conn.id);
					m.conn.setType("attribute");
					m.conn.setDetachable(false);
					m.conn.endpoints[0].removeClass("endpointEnabled");
					m.conn.endpoints[1].removeClass("endpointEnabled");
				});
			});
		},
		_enableItems: function () {
			var inputItems = this._inputTree.getItems();
			var outputItems = this._outputTree.getItems();
			var self = this;

			inputItems.each(function (item) {
				var model = item.getModel();
				var id = model.getId();
				var el = item.getUserData("epel");
				var type = model.getType();
				if (type == ms123.datamapper.Config.NODETYPE_COLLECTION || (model.getRoot && model.getRoot())) {
					if (self._jsPlumb.isSource(el)) {
						self._jsPlumb.setSourceEnabled(el, true);
					}
				}
			});
			outputItems.each(function (item) {
				var model = item.getModel();
				var id = model.getId();
				var el = item.getUserData("epel");
				var type = model.getType();
				if (type == ms123.datamapper.Config.NODETYPE_COLLECTION || (model.getRoot && model.getRoot())) {
					if (self._jsPlumb.isTarget(el)) {
						try {
							jQuery(el).droppable("option", "disabled", false)
						} catch (e) {
							console.error("shit:" + e);
						}
						self._jsPlumb.setTargetEnabled(el, true);
					}
				}
			});
			var selectedStructureMapping = this._getSelectedStructureMapping();
			if (selectedStructureMapping) {
				this._handleCollection(selectedStructureMapping);
			}
		},
		_handleCollection: function (mapping) {
			var inputItem = this.getTreeItemById(mapping.input.id, ms123.datamapper.Config.INPUT);
			var outputItem = this.getTreeItemById(mapping.output.id, ms123.datamapper.Config.OUTPUT);

			console.log("inputItem is:" + inputItem.isOpen());
			inputItem.setBackgroundColor(ms123.datamapper.Config.BG_COLOR_STRUCTURE_SELECTED);
			outputItem.setBackgroundColor(ms123.datamapper.Config.BG_COLOR_STRUCTURE_SELECTED);
			mapping.conn.setType("structureSelected");
			mapping.conn.setDetachable(true);
			mapping.conn.endpoints[0].addClass("endpointEnabled");
			mapping.conn.endpoints[1].addClass("endpointEnabled");
			console.log("\nEnable:" + mapping.type + "/" + mapping.conn.id);
			Object.keys(mapping.children).each(function (k) {
				var m = mapping.children[k];
				console.log("\t----->Enable:" + m.type + "/" + m.conn.id);
				m.conn.setType("attributeSelected");
				m.conn.setDetachable(true);
				m.conn.endpoints[0].addClass("endpointEnabled");
				m.conn.endpoints[1].addClass("endpointEnabled");
			});
			this._enableAttributeItems(inputItem, outputItem);
		},
		_enableAttributeItems: function (inputItem, outputItem) {
			var self = this;
			var children = inputItem.getChildren();
			children.each(function (item) {
				if (item.getModel().getType() == ms123.datamapper.Config.NODETYPE_ATTRIBUTE) {
					self._handleAttribute(item, ms123.datamapper.Config.INPUT);
				}
				self._handleElements(item, ms123.datamapper.Config.INPUT);
			});
			var children = outputItem.getChildren();
			children.each(function (item) {
				if (item.getModel().getType() == ms123.datamapper.Config.NODETYPE_ATTRIBUTE) {
					self._handleAttribute(item, ms123.datamapper.Config.OUTPUT);
				}
				self._handleElements(item, ms123.datamapper.Config.OUTPUT);
			});

		},
		_handleAttribute: function (item, side) {
			item.setBackgroundColor(ms123.datamapper.Config.BG_COLOR_ATTRIBUTE_CONNECTED);
			var el = item.getUserData("epel");
			if (side == ms123.datamapper.Config.INPUT) {
				this._jsPlumb.setSourceEnabled(el, true);
				this._jsPlumb.setTargetEnabled(el, true);
				try {
					jQuery(el).droppable("option", "disabled", false)
				} catch (e) {}
			} else {
				this._jsPlumb.setTargetEnabled(el, true);
				try {
					jQuery(el).droppable("option", "disabled", false)
				} catch (e) {
					console.log("_handleAttribute:" + e + "/" + el);
				}
			}
		},
		_handleElements: function (item, side) {
			var self = this;
			if (item.getModel().getType() == ms123.datamapper.Config.NODETYPE_ELEMENT) {
				var children = item.getChildren();
				children.each(function (item) {
					if (item.getModel().getType() == ms123.datamapper.Config.NODETYPE_ATTRIBUTE) {
						self._handleAttribute(item, side);
					}
					if (item.getModel().getType() == ms123.datamapper.Config.NODETYPE_ELEMENT) {
						self._handleElements(item, side);
					}
				});
			}
		},
		_makeAllSourcesAndTargets: function () {
			var inputItems = this._inputTree.getItems();
			var ouputItems = this._outputTree.getItems();
			var self = this;
			inputItems.each(function (item) {
				var model = item.getModel();
				var type = model.getType();
				var scope = ms123.datamapper.Config.STRUCTURE_SCOPE;
				var strokeColor = ms123.datamapper.Config.STRUCTURE_LINECOLOR;
				if (type == ms123.datamapper.Config.NODETYPE_ATTRIBUTE) {
					scope = ms123.datamapper.Config.ATTRIBUTE_SCOPE;
					strokeColor = ms123.datamapper.Config.ATTRIBUTE_LINECOLOR;
				}
				if (type != ms123.datamapper.Config.NODETYPE_ELEMENT || (model.getRoot && model.getRoot())) {
					var el = item.getContentElement().getDomElement();
					var isSource = self._jsPlumb.isSource(el);
					if (!isSource) {
						console.log("makeSource:" + el + "/" + item.getModel().getPath());
						self._jsPlumb.makeSource(el, {
							anchor: "RightMiddle",
							filter: function (evt, el) {
								var t = evt.target || evt.srcElement;
								return t.className !== "treeItemOpener";
							},
							connectorStyle: {
								strokeStyle: strokeColor,
								lineWidth: 1
							},
							scope: scope,
							isTarget: true,
							isSource: true
						});
						if (type == ms123.datamapper.Config.NODETYPE_ATTRIBUTE) {
							self._jsPlumb.makeTarget(el, {
								anchor: "RightMiddle",
								isTarget: true,
								scope: scope,
								beforeDrop: function (p) {
									console.log("INPUT.Target.beforeDrop:%o", p);
									var conn = p.connection;
									var el0 = conn.endpoints[0].getElement();
									var itemSource = self._getTreeItemByEl(el0, ms123.datamapper.Config.INPUT);
									var itemTarget = self._getTreeItemByEl(jQuery("#" + p.targetId).get(0), ms123.datamapper.Config.INPUT);
									console.log("beforeDrop.itemTarget:%o", itemTarget);
									console.log("beforeDrop.itemSource:%o", itemSource);
									if (itemSource != null || itemTarget != null) return false;
									return true;
								},
								dropOptions: {
									hoverClass: "dropHoverClass"
								}
							});
						}
						item.setUserData("epel", el);
					}
				}
			});
			ouputItems.each(function (item) {
				var model = item.getModel();
				var type = model.getType();
				var scope = ms123.datamapper.Config.STRUCTURE_SCOPE;
				var strokeColor = ms123.datamapper.Config.STRUCTURE_LINECOLOR;
				if (type == ms123.datamapper.Config.NODETYPE_ATTRIBUTE) {
					scope = ms123.datamapper.Config.ATTRIBUTE_SCOPE;
					strokeColor = ms123.datamapper.Config.ATTRIBUTE_LINECOLOR;
				}
				if (type != ms123.datamapper.Config.NODETYPE_ELEMENT || (model.getRoot && model.getRoot())) {
					var el = item.getContentElement().getDomElement();
					var isTarget = self._jsPlumb.isTarget(el);
					if (!isTarget) {
						console.log("makeTarget:" + el + "/" + item.getModel().getPath() + "/" + jQuery(el).size());
						self._jsPlumb.makeTarget(el, {
							anchor: "LeftMiddle",
							isTarget: true,
							isSource: false,
							scope: scope,
							beforeDrop: function (p) {
								console.log("OUTPUT.TARGET.beforeDrop:%o", p);
								var conn = p.connection;
								var el0 = conn.endpoints[0].getElement();
								var itemSource = self._getTreeItemByEl(el0, ms123.datamapper.Config.INPUT);
								var itemTarget = self._getTreeItemByEl(jQuery("#" + p.targetId).get(0), ms123.datamapper.Config.OUTPUT);
								console.log("beforeDrop.itemSource:%o", itemSource);
								console.log("beforeDrop.itemTarget:%o", itemTarget);
								if (itemSource == null || itemTarget == null) return false;
								var error = self.validateConnection(itemSource.getModel(), itemTarget.getModel());
								if( error ){
									ms123.form.Dialog.alert(error);
									return false;
								}
								var mapping = self._getMappingBySourceAndTarget(itemSource.getModel().getId(), itemTarget.getModel().getId());
								if (mapping == null) {
									return true;
								}
								if (mapping.type == ms123.datamapper.Config.STRUCTURE_MAPPING) {
									return true;
								}
								return true;
							},
							dropOptions: {
								hoverClass: "dropHoverClass"
							}
						});
						item.setUserData("epel", el);
					}
				}
			});
		},

		validateConnection: function (source, target) {
			if (!this._isCollection(source)) return null;
			if( this._isRootAndElement(target)){
				var mapped = this.isMapped(target.getId(), ms123.datamapper.Config.OUTPUT);
				if( mapped.hit && mapped.structure){
					return "Root-Element as target supports only one Mapping";
				}
			}
			var self = this;
			var error = null;
			var keys = Object.keys(this._mappings);
			keys.each(function (k) {
				var mapping = self._mappings[k];
				var inputModel = self.getTreeItemById(mapping.input.id, ms123.datamapper.Config.INPUT).getModel();
				var outputModel = self.getTreeItemById(mapping.output.id, ms123.datamapper.Config.OUTPUT).getModel();

				var sourceAncestor = self._nearestCommonAncestor(source, inputModel, self._inputTree);
				var targetAncestor = self._nearestCommonAncestor(target, outputModel, self._outputTree);

				var sourcesInRelation = (sourceAncestor === source) || (sourceAncestor === inputModel);
				var targetsInRelation = (targetAncestor === target) || (targetAncestor === outputModel);

				console.log("sourcesInRelation:" + sourcesInRelation);
				console.log("targetsInRelation:" + targetsInRelation);

				if ((source.getId() != mapping.input.id) && (target.getId() != mapping.output.id)) {
					if (sourcesInRelation && targetsInRelation) {
						if (((sourceAncestor == source) && (targetAncestor != target)) || ((sourceAncestor.getId() == mapping.input.id) && (targetAncestor.getId() != mapping.output.id))) {
							error = "Combination of requested mapping and existing mapping <br /><b>\"" + mapping.name + "\"</b><br /> would cause illegal rotation of the tree structure.";
							return;
						}
					} else if (!sourcesInRelation && targetsInRelation) {
						error = "Target of the mapping is in child relationship with existing mapping <br /><b>\"" + mapping.name + "\"</b><br /> but there is no adequate relationship in source tree structure<br /> between these two mappings.";
						return;
					}
				}
			});
			return error;
		},
		_nearestCommonAncestor: function (model1, model2, tree) {
			var ancestors = [];
			var ancestor = model1;
			while (ancestor != null) {
				ancestors.push(ancestor);
				ancestor = tree.getParent(ancestor);
			}
			ancestor = model2;
			while (ancestor != null) {
				if (ancestors.indexOf(ancestor) != -1) {
					return ancestor;
				}
				ancestor = tree.getParent(ancestor);
			}
			return null;
		},

		_enableAllSourcesAndTargets: function (b) {
			var inputItems = this._inputTree.getItems();
			var ouputItems = this._outputTree.getItems();
			var self = this;

			inputItems.findAll(function (item) {
				return item.getModel().getType() != ms123.datamapper.Config.NODETYPE_ELEMENT;
			}).each(function (item) {
				var el = item.getUserData("epel");
				if (self._jsPlumb.isSource(el)) {
					self._jsPlumb.setSourceEnabled(el, b);
					self._jsPlumb.setTargetEnabled(el, b);
					try {
						jQuery(el).droppable("option", "disabled", !b)
					} catch (e) {}
				}
			});

			ouputItems.findAll(function (item) {
				return item.getModel().getType() != ms123.datamapper.Config.NODETYPE_ELEMENT;
			}).each(function (item) {
				var el = item.getUserData("epel");
				if (self._jsPlumb.isTarget(el)) {
					try {
						jQuery(el).droppable("option", "disabled", !b)
					} catch (e) {
						console.error("shit:" + e);
					}
					self._jsPlumb.setTargetEnabled(el, b);
				}
			});

		},
		_getEndpointByElAndConn: function (el, conn) {
			var epList = this._jsPlumb.getEndpoints(el);
			if (!epList || epList.length == 0) {
				return null;
			}
			for (var i = 0; i < epList.length; i++) {
				var ep = epList[i];
				for (var j = 0; j < ep.connections.length; j++) {
					if (ep.connections[j] == conn) {
						return ep;
					}
				}
			}
			return null;
		},
		_getTree: function (side) {
			var tree = (side == ms123.datamapper.Config.INPUT) ? this._inputTree : this._outputTree;
			return tree;
		},
		_getTreeItemByEl: function (el, side) {
			var tree = this._getTree(side);
			var items = tree.getItems();
			var retItem = null;
			var self = this;
			items.each(function (item) {
				var _el = item.getUserData("epel");
				if (_el === el) {
					retItem = item;
				}
			});
			return retItem;
		},
		getTreeItemById: function (id, side) {
			var tree = this._getTree(side);
			var items = tree.getItems();
			var retItem = null;
			var self = this;
			items.each(function (item) {
				var _id = item.getModel().getId();
				if (_id == id) {
					retItem = item;
				}
			});
			return retItem;
		},
		_getEndpointElementById: function (id, side) {
			var tree = this._getTree(side);
			var items = tree.getItems();
			var retItem = null;
			var self = this;
			items.each(function (item) {
				var _id = item.getModel().getId();
				if (_id == id) {
					retItem = item;
				}
			});
			return retItem.getUserData("epel");
		},
		_raiseResetUndo: function () {
			this._facade.raiseEvent({
				type: ms123.baseeditor.Config.EVENT_RESET_UNDO
			});
		},
		_raiseMappingChanged: function () {
console.log("_raiseMappingChanged");
			this._facade.raiseEvent({
				type: ms123.datamapper.Config.EVENT_MAPPING_CHANGED
			});
		},
		_getId: function (mapping, side) {
			return (side == ms123.datamapper.Config.INPUT) ? mapping.input.id : mapping.output.id;
		},
		_isCollection: function (model) {
			return (model.getType() == ms123.datamapper.Config.NODETYPE_COLLECTION || model.getRoot && model.getRoot());
		},
		_isRootAndElement: function (model) {
			return (model.getType() == ms123.datamapper.Config.NODETYPE_ELEMENT && model.getRoot && model.getRoot());
		},
		repaint: function () {
			qx.event.Timer.once(function () {
				this._jsPlumb.repaintEverything();
			}, this, 200);
		},
		_initJsPlumb: function (_jsPlumb) {
			_jsPlumb.Defaults.Container = this._facade.jsPlumbContainer.getContentElement().getDomElement();
			var self = this;
			_jsPlumb.bind("connection", function (info, originalEvent) {
				if (self._internalUse === true) return;
				console.log("New Connection:%o", info);
				var itemSource = self._getTreeItemByEl(info.source, ms123.datamapper.Config.INPUT);
				var itemTarget = self._getTreeItemByEl(info.target, ms123.datamapper.Config.OUTPUT);

				console.log("\titemSource:%o", itemSource);
				console.log("\titemTarget:%o", itemTarget);
				if (itemSource == null || itemTarget == null) {
					return;
				}
				var sourceModel = itemSource.getModel();
				var targetModel = itemTarget.getModel();
				var sourceEndpoint = self._getEndpointByElAndConn(info.sourceId, info.connection);
				var targetEndpoint = self._getEndpointByElAndConn(info.targetId, info.connection);
				var type = sourceModel.getType();
				if (
				type == ms123.datamapper.Config.NODETYPE_COLLECTION || type == ms123.datamapper.Config.NODETYPE_ATTRIBUTE || (sourceModel.getRoot && sourceModel.getRoot())) {
					var mapping = {
						input: {},
						output: {}
					};
					mapping.input.id = sourceModel.getId();
					mapping.output.id = targetModel.getId();
					mapping.input.path = sourceModel.getPath();
					mapping.output.path = targetModel.getPath();
					mapping.input.ep = sourceEndpoint;
					mapping.output.ep = targetEndpoint;
					if (type == ms123.datamapper.Config.NODETYPE_ATTRIBUTE) {
						mapping.id = ms123.util.IdGen.id();
						mapping.name = "";
						mapping.type = ms123.datamapper.Config.ATTRIBUTE_MAPPING;
					} else {
						mapping.name = self.tr("datamapper.foreach") + " " + sourceModel.getName() + " -> " + targetModel.getName();
						mapping.id = ms123.util.IdGen.id();
						mapping.type = ms123.datamapper.Config.STRUCTURE_MAPPING;
						mapping.map2parent = true;
						mapping.children = {};
						info.connection.setDetachable(false);
					}
					console.log("-->setting mapping conn:" + mapping.input.path + "/" + info.connection.id);
					mapping.conn = info.connection;
					self.executeCommandInsertRemoveMapping(mapping, false);
				}
			});
			_jsPlumb.bind("connectionDetached", function (info, originalEvent) {
				if (self._internalUse === true) return;
				try {
					var mapping = info.connection.getParameter(ms123.datamapper.Config.MAPPING_PARAM);
					console.log("Detach Connection:%o,%o", info, mapping);
					if (mapping) {
						mapping.conn = null;
						self.executeCommandInsertRemoveMapping(mapping, true);
					}
				} catch (e) {
					console.error(e + "/" + e.stack);
				}
			});

			_jsPlumb.bind("click", function (conn, originalEvent) {
				//if (confirm("Delete connection from " + conn.sourceId + " to " + conn.targetId + "?")) _jsPlumb.detach(conn);
			});

			_jsPlumb.registerConnectionTypes({
				"structure": {
					paintStyle: {
						strokeStyle: "lightgray",
						lineWidth: 1
					},
					hoverPaintStyle: {
						strokeStyle: "lightgray",
						lineWidth: 2
					}
				},
				"structureSelected": {
					paintStyle: {
						strokeStyle: "red",
						lineWidth: 1
					},
					hoverPaintStyle: {
						strokeStyle: "red",
						lineWidth: 2
					}
				},
				"attribute": {
					paintStyle: {
						strokeStyle: "lightgray",
						lineWidth: 1
					},
					hoverPaintStyle: {
						strokeStyle: "lightgray",
						lineWidth: 2
					}
				},
				"attributeSelected": {
					paintStyle: {
						strokeStyle: "black",
						lineWidth: 1
					},
					hoverPaintStyle: {
						strokeStyle: "black",
						lineWidth: 2
					}
				}
			});

			_jsPlumb.importDefaults({
				Anchors: ["RightMiddle", "LeftMiddle"],
				DragOptions: {
					cursor: 'pointer',
					zIndex: 2000
				},
				EndpointStyles: [{
					fillStyle: "#0d78bc"
				},
				{
					fillStyle: "#558822"
				}],
				Endpoints: [
					["Dot",
					{
						radius: 4
					}],
					["Dot",
					{
						radius: 4
					}]
				],
				PaintStyle: {
					strokeStyle: "blue",
					lineWidth: 4
				}
			});
		},
		editNode: function (e) {}

	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {
		console.log("MappingEdit.Destruct");
		this._jsPlumb.unbind("connection");
		this._jsPlumb.unbind("connectionDetached");
		this._jsPlumb.reset();
	}
});
