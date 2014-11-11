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
 * @ignore(jQuery)
 * @ignore(jsPlumb.*)
 * @ignore(Clazz.extend)
 */

qx.Class.define("ms123.datamapper.plugins.TreeEdit", {
	extend: qx.core.Object,
	include: [qx.locale.MTranslation],
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (facade, context) {
		this.base(arguments);
		this._facade = facade;
		this._context = context;
		this._tree = context.tree;
		this._side = context.side;

		var up_msg = this.tr("datamapper.up");
		var down_msg = this.tr("datamapper.down");
		var add_msg = this.tr("datamapper.add");
		var remove_msg = this.tr("datamapper.remove");
		var edit_msg = this.tr("datamapper.edit");
		var group = "2";
		this._facade.offer({
			name: add_msg,
			description: add_msg,
			icon: "icon/16/actions/list-add.png",
			functionality: this.insertNode.bind(this),
			group: group,
			index: 1,
			isEnabled: qx.lang.Function.bind(function () {
				return this.isElement();
			}, this)
		});
		this._facade.offer({
			name: remove_msg,
			description: remove_msg,
			icon: "icon/16/actions/list-remove.png",
			functionality: this.removeNode.bind(this),
			group: group,
			index: 2,
			isEnabled: qx.lang.Function.bind(function () {
				var t1 = (this.isAttribute() || this.isCollection())  && this._currentId && !this._facade.mappingEdit.isMapped(this._currentId,this._side).hit;
				var t2 = (this.isElement() && this._currentId && !this._facade.mappingEdit.isElementMapped(this._currentId,this._side));
				return t1 || t2;
			}, this)
		});

		this._facade.offer({
			name: edit_msg,
			description: edit_msg,
			icon: "resource/ms123/edit.png",
			functionality: this.editNode.bind(this),
			group: group,
			index: 2,
			isEnabled: qx.lang.Function.bind(function () {
				return this.isAttribute() || this.isCollection() || this.isElement();
			}, this)
		});

		this._facade.offer({
			name: up_msg,
			description: up_msg,
			icon: "icon/16/actions/go-up.png",
			functionality: this.upNode.bind(this),
			group: group,
			index: 3,
			isEnabled: qx.lang.Function.bind(function () {
				return (this.isAttribute()||this.isCollection()) && this._canUp();
			}, this)
		});

		this._facade.offer({
			name: down_msg,
			description: down_msg,
			icon: "icon/16/actions/go-down.png",
			functionality: this.downNode.bind(this),
			group: group,
			index: 4,
			isEnabled: qx.lang.Function.bind(function () {
				return (this.isAttribute()||this.isCollection()) && this._canDown();
			}, this)
		});


/*this._facade.offer({
			name: change_column_msg,
			description: change_column_msg,
			icon: "icon/16/actions/document-new.png",
			functionality: this.changeColumn.bind(this),
			target:ms123.datamapper.plugins.ContextMenu,
			group: group,
			index: 1,
			isEnabled: qx.lang.Function.bind(function () {
				return true;
			}, this)
		});
		this._facade.offer({
			name: delete_column_msg,
			description: delete_column_msg,
			icon: "icon/16/actions/list-remove.png",
			functionality: this.deleteColumn.bind(this),
			target:ms123.datamapper.plugins.ContextMenu,
			group: group,
			index: 2
		});*/
		this._currentId = null;

		this._tree.getTree().addListener("click", function(e){
		}, this);



		this._tree.getTree().addListener("changeSelection", function (e) {
			var selItem = this._tree.getSelection()[0];
			
			var items = this._tree.getItems();
			var self = this;
			items.each(function (item) {
				var label = item.getChildControl("label");
				if( selItem == item){
					self._currentId = selItem.getModel().getId();
					label.setTextColor(ms123.datamapper.Config.TREE_LABEL_SELECTED_COLOR);	
				}else{
					label.setTextColor(ms123.datamapper.Config.TREE_LABEL_COLOR);	
				}
			});
			this._facade.update();

		}, this);
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
		isElement: function () {
			var item = this._tree.getModelSelection().getItem(0);
			if (item == null) return false;
			return item.getType() != ms123.datamapper.Config.NODETYPE_ATTRIBUTE;
		},
		isCollection: function () {
			var item = this._tree.getModelSelection().getItem(0);
			if (item == null) return false;
			return item.getType() == ms123.datamapper.Config.NODETYPE_COLLECTION;
		},
		isAttribute: function () {
			var item = this._tree.getModelSelection().getItem(0);
			if (item == null) return false;
			return item.getType() == ms123.datamapper.Config.NODETYPE_ATTRIBUTE;
		},
		_getParentModel:function(item){
			return item.getParent() ? item.getParent().getModel() :this._tree.getRootModel();
		},
		insertNode: function (e) {
			var tree = this._context.tree;
			var fe = new ms123.datamapper.edit.FieldsEditor(this._facade, this._context);
			fe.addListener("changeValue", function (e) {
				var sel = this._tree.getSelection()[0];
				var pmodel =this._getParentModel(sel);
				var model = tree.getModelSelection().getItem(0);
				this._correctTypes(e.getData());
				this.executeCommandInsertRemoveNode(pmodel, model, e.getData());
			}, this);
		},
		removeNode: function (e) {
			var sel = this._tree.getSelection()[0];
			var pmodel =this._getParentModel(sel);
			console.log("sel:%o" , pmodel);
			this.executeCommandInsertRemoveNode(pmodel, sel.getModel(), null);
		},
		editNode: function (e) {
			var selModel = this._tree.getModelSelection().getItem(0);
			var selItem = this._tree.getSelection()[0];
			var f = qx.lang.Json.parse(qx.util.Serializer.toJson(selModel));
			var fe = new ms123.datamapper.edit.AttributeEditor(this._facade, this._context, f);
			fe.addListener("changeValue", function (e) {
				this.executeCommandEditNode(selItem, selModel, e.getData());
			}, this);
		},
		_raiseTreeChanged: function () {
			qx.event.Timer.once(function () {
				this._facade.raiseEvent({
					type: ms123.datamapper.Config.EVENT_TREE_CHANGED
				});
			}, this, 200);
		},
		executeCommandEditNode: function (selItem, selectedModel, data) {
			var self = this;
			var CommandClass = Clazz.extend({
				construct: function (selItem, selectedModel,data) {
					this.selectedModel = selectedModel;
					this.parentModel = self._tree.getParent(selectedModel);
					this.selectedItem = selItem;
					this.data = data;
					this.data.fieldFormat = this.data.fieldFormat||null
				},
				execute: function () {
					console.log("Data:"+JSON.stringify(this.data,null,2));
					this.nodeName = this.selectedModel.getName();
					if( this.selectedModel.getFieldType){
						this.fieldType = this.selectedModel.getFieldType();
					}
					if( this.selectedModel.getType){
						this.type = this.selectedModel.getType();
					}
					if( this.selectedModel.getFieldFormat){
						this.fieldFormat = this.selectedModel.getFieldFormat();
					}
					this.selectedModel.setName(this.data[ms123.datamapper.Config.NODENAME]);
					this.cleanName = this.selectedModel.getCleanName();
					this.selectedModel.setCleanName(null);
					if( this.selectedModel.setFieldType && this.data.fieldType!=null){
						this.selectedModel.setFieldType(this.data.fieldType);
					}
					if( this.selectedModel.setType && this.data.type!=null){
						this.selectedModel.setType(this.data.type);
					}
					if( this.selectedModel.setFieldFormat){
						this.selectedModel.setFieldFormat(this.data.fieldFormat);
					}
					this.selectedItem.setTitle(this.data[ms123.datamapper.Config.NODENAME]);
					this.makeCleanNames();
				},
				rollback: function () {
					this.selectedModel.setName(this.nodeName);
					this.selectedModel.setCleanName(this.cleanName);
					if( this.selectedModel.setFieldType && this.data.fieldType!=null){
						this.selectedModel.setFieldType(this.fieldType);
					}
					if( this.selectedModel.setType && this.data.type!=null){
						this.selectedModel.setType(this.type);
					}
					if( this.selectedModel.setFieldFormat){
						this.selectedModel.setFieldFormat(this.fieldFormat);
					}
					this.selectedItem.setTitle(this.nodeName);
				},
				makeCleanNames:function(){
					if( this.parentModel){
						var childs = this.parentModel.getChildren();
						var nameList = [];
						for( var i=0; i < childs.getLength(); i++){
							var item = childs.getItem(i);
							if( item.getCleanName()) nameList.push(item.getCleanName());
						}
						for( var i=0; i < childs.getLength(); i++){
							var item = childs.getItem(i);
							if( item.getCleanName() == null){
								var cleanName = ms123.datamapper.BaseTree.makeCleanName( item.getName(), nameList);
								item.setCleanName(cleanName);	
							}
						}
					}else{
						var cleanName = ms123.datamapper.BaseTree.makeCleanName( this.selectedModel.getName(), []);
						this.selectedModel.setCleanName(cleanName);	
					}
				}
			})
			var command = new CommandClass(selItem, selectedModel, data);
			this._facade.executeCommands([command]);
		},
		executeCommandUpDown: function (parentModel, nodeModel, up) {
			var tree = this._tree;
			var self = this;
			var CommandClass = Clazz.extend({
				construct: function (parentModel, nodeModel,up) {
					this.parentModel = parentModel;
					this.nodeModel = nodeModel;
					this.up = up;
				},
				execute: function () {
					if (this.up) {
						var index = this.parentModel.getChildren().indexOf(this.nodeModel);
						tree.enableChangeEvents(false);
						this.parentModel.getChildren().remove(this.nodeModel);
						tree.enableChangeEvents(true);
						this.parentModel.getChildren().insertAt(index-1,this.nodeModel);
						tree.setModelSelection(this.nodeModel);
					} else {
						var index = this.parentModel.getChildren().indexOf(this.nodeModel);
						tree.enableChangeEvents(false);
						this.parentModel.getChildren().remove(this.nodeModel);
						tree.enableChangeEvents(true);
						this.parentModel.getChildren().insertAt(index+1,this.nodeModel);
						tree.setModelSelection(this.nodeModel);
					}
					self._facade.mappingEdit.repaint();
				},
				rollback: function () {
					if (this.up) {
						var index = this.parentModel.getChildren().indexOf(this.nodeModel);
						console.log("index:"+index);
						tree.enableChangeEvents(false);
						this.parentModel.getChildren().remove(this.nodeModel);
						tree.enableChangeEvents(true);
						this.parentModel.getChildren().insertAt(index+1,this.nodeModel);
						tree.setModelSelection(this.nodeModel);
					} else {
						var index = this.parentModel.getChildren().indexOf(this.nodeModel);
						console.log("index:"+index);
						tree.enableChangeEvents(false);
						this.parentModel.getChildren().remove(this.nodeModel);
						tree.enableChangeEvents(true);
						this.parentModel.getChildren().insertAt(index-1,this.nodeModel);
						tree.setModelSelection(this.nodeModel);
					}
					self._facade.mappingEdit.repaint();
				}
			})
			var command = new CommandClass(parentModel, nodeModel, up);
			this._facade.executeCommands([command]);
			this._facade.update();
		},
		executeCommandInsertRemoveNode: function (parentModel, nodeModel, data) {
			var tree = this._tree;
			var self = this;
			var CommandClass = Clazz.extend({
				construct: function (parentModel, nodeModel,data) {
					this.parentModel = parentModel;
					this.nodeModel = nodeModel;
					this.data = data;
				},
				execute: function () {
					if (this.data) {
						this.newModel = tree.insertDataNode(this.nodeModel, this.data);
					} else {
						this.savedNode = tree.removeNode(this.parentModel, this.nodeModel);
					}
					self._raiseTreeChanged();
					self._facade.mappingEdit.repaint();
				},
				rollback: function () {
					if (this.data) {
						tree.removeModel(this.newModel);
					} else {
						tree.insertSavedNode(this.savedNode);
					}
					self._raiseTreeChanged();
					self._facade.mappingEdit.repaint();
				}
			})
			var command = new CommandClass(parentModel, nodeModel, data);
			this._facade.executeCommands([command]);
			this._facade.update();
		},
		_correctTypes:function(model){
			if( model.fieldType){
				if( model.fieldType == ms123.datamapper.Config.NODETYPE_COLLECTION){
					model.type=ms123.datamapper.Config.NODETYPE_COLLECTION;
					delete model.fieldType;
				}
				if( model.fieldType == ms123.datamapper.Config.NODETYPE_ELEMENT){
					model.type=ms123.datamapper.Config.NODETYPE_ELEMENT;
					delete  model.fieldType;
				}
			}
			var children = model.children || [];
			for (var i = 0; i < children.length; i++) {
				var c = children[i];
				this._correctTypes(c);
			}
		},
		_canUp: function (e) {
			var sel = this._tree.getSelection()[0];
			var pmodel =this._getParentModel(sel);
			return pmodel.getChildren().indexOf(sel.getModel()) > 0;
		},
		_canDown: function (e) {
			var sel = this._tree.getSelection()[0];
			var pmodel =this._getParentModel(sel);
			var count = pmodel.getChildren().getLength();
			return pmodel.getChildren().indexOf(sel.getModel()) < count-1;
		},
		upNode: function (e) {
			var sel = this._tree.getSelection()[0];
			var pmodel =this._getParentModel(sel);
			this.executeCommandUpDown(pmodel,sel.getModel(),true);
		},
		downNode: function (e) {
			var sel = this._tree.getSelection()[0];
			var pmodel =this._getParentModel(sel);
			this.executeCommandUpDown(pmodel,sel.getModel(),false);
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {
	}

});
