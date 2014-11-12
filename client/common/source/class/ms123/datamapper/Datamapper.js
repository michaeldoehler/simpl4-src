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
 */
qx.Class.define('ms123.datamapper.Datamapper', {
	extend: ms123.datamapper.BaseManager,

	construct: function (context) {
		this.base(arguments, context);
		this._use = context.use;
		this._facade.use = this._use;
		this._facade.importingid = context.importingid;
		this._facade.mainEntity = context.mainEntity;

console.log("datamapper.importingid:"+this._facade.importingid);
		var deco = new ms123.util.RoundSingleBorder(1, "solid", "white", 1);
		this.setLayout(new qx.ui.layout.Dock(0, 1, null, deco));
		this._disposeList = [];
	},

	events: {
		"save": "qx.event.type.Data",
		"save2": "qx.event.type.Data"
	},
	properties: {},

	members: {
		init: function (content) {
			this._facade.update = this._update.bind(this);
			this._facade.save = this._save.bind(this);
			this._facade.getConfig = this._getConfig.bind(this);
			if (this._isEmpty(content)) {
				var mc = new ms123.datamapper.MappingCreator(this._facade);
				this.add(mc, {
					edge: "center"
				});
				mc.addListener("ready", function (ev) {
					this._facade.save(ev.getData(),null,2);
					this.removeAll();
					this.initMapper(ev.getData());
				}, this);
			} else {
				var config = content;
				if( typeof config == 'string'){
					config = qx.lang.Json.parse(content);
				}
				this.initMapper(config);
			}
		},
		_isEmpty: function (content) {
			if (!content || content == "") return true;
			return false;
		},
		initMapper: function (config) {
			new ms123.baseeditor.Undo(this._facade);
			var isDefaultsEnabled = config.output.format == ms123.datamapper.Config.FORMAT_POJO;
			if( isDefaultsEnabled){
				var mainEntity = this._getMainEntity(config.output);
				if( mainEntity==null){
					 isDefaultsEnabled = false;
				}else{
					config.output.cleanName = mainEntity;
				}
			}
			new ms123.datamapper.plugins.ViewSwitch(this._facade, isDefaultsEnabled);
			new ms123.datamapper.plugins.Save(this._facade);
			var toolbar = new ms123.datamapper.plugins.GlobalToolbar(this._facade);
			toolbar.setBackgroundColor("white");
			toolbar.registryChanged(this.getPluginsData());
			this.add(toolbar, {
				edge: "north"
			});
			var stack = new qx.ui.container.Stack();
			var editor = this._createEditor(config);
			var preview = this._createPreview(config);
			stack.add(preview);
			stack.add(editor);
			var defaults = null;
			if( isDefaultsEnabled ){
				defaults = this._createDefaults(config);
				stack.add(defaults);	
			}

			this.add(stack, {
				edge: "center"
			});
			this._facade.viewStack = stack;
			this._facade.editorView = editor;
			this._facade.previewView = preview;
			this._facade.defaultsView = defaults;
			this._facade.viewStack.setSelection([this._facade.editorView]);
		},
		_createPreview:function(config){
			var container = new qx.ui.container.Composite(new qx.ui.layout.Dock(3,3));
			this.resetPluginsData();
			var p = null;
			if( this._use == ms123.datamapper.Config.USE_IMPORT){
				p = new ms123.datamapper.plugins.Import(this._facade,config);
			}else{
				p = new ms123.datamapper.plugins.Preview(this._facade,config);
			}
			this._facade.preview = p;
			var toolbar = new ms123.datamapper.plugins.PreviewToolbar(this._facade);
			toolbar.registryChanged(this.getPluginsData());
			this._previewToolbar=toolbar;
			container.add(toolbar, {
				edge: "north"
			});
			container.add(p, {
				edge: "center"
			});
			return container;
		},
		_createDefaults:function(config){
			var container = new qx.ui.container.Composite(new qx.ui.layout.Dock(3,3));
			this.resetPluginsData();
			var mainEntity = config.output.cleanName;
			var d = new ms123.datamapper.plugins.Defaults(this._facade,config);
			d.setup(mainEntity,3);
			d.setValue( config.defaults);
			this._facade.defaults = d;
			container.add(d, {
				edge: "center"
			});
			return container;
		},
		_createEditor:function(config){
			var inputTreeContainer = this._createEditorArea(ms123.datamapper.Config.INPUT, config.input);
			var outputTreeContainer = this._createEditorArea(ms123.datamapper.Config.OUTPUT, config.output);

			var attributeScriptEdit = this._createAttributeScriptEdit(config);
			var attributeLookupEdit = this._createAttributeLookupEdit(config);
			var attributeContainer = new ms123.datamapper.Split2(attributeScriptEdit,attributeLookupEdit);
			var outputContainer = new ms123.datamapper.Split2(outputTreeContainer,attributeContainer);

			this.resetPluginsData();
			var mappingEdit = new ms123.datamapper.plugins.MappingEdit(this._facade,config);
			mappingEdit.registryChanged(this.getPluginsData());
			this._disposeList.push(mappingEdit);

			var center = new qx.ui.container.Composite(new qx.ui.layout.Dock());
			center.add(mappingEdit, {
				edge: "north"
			});
			this._facade.mappingEdit = mappingEdit;
			
			var container = new ms123.datamapper.Split3(inputTreeContainer, center, outputContainer);
			this._facade.jsPlumbContainer = container;
			return container;
		},
		_createAttributeScriptEdit: function (config) {
			var script = new ms123.datamapper.plugins.AttributeScriptEdit(this._facade,config);
			this._facade.attributeScriptEdit = script;
			return script;
		},
		_createAttributeLookupEdit: function (config) {
			var container = new qx.ui.container.Composite(new qx.ui.layout.Dock(1,1));
			 var l = new qx.ui.basic.Label("Lookup").set({});
			var lookup = new ms123.datamapper.plugins.AttributeLookupEdit(this._facade,config);
			container.add(l,{edge:"west"});
			container.add(lookup,{edge:"center"});
			this._facade.attributeLookupEdit = lookup;
			return container;
		},
		_createEditorArea: function (side, config) {
			var container = new qx.ui.container.Composite();
			var layout = new qx.ui.layout.Dock();
			container.setLayout(layout);

			var tree;
			if (side == ms123.datamapper.Config.INPUT) {
				this._facade.inputTree = tree = new ms123.datamapper.InputTree(this._facade, config);
			} else {
				this._facade.outputTree = tree = new ms123.datamapper.OutputTree(this._facade, config);
			}
			var context = {
				config: config,
				side: side,
				tree: tree
			}

			this.resetPluginsData();
			var toolbar = new ms123.datamapper.plugins.AreaToolbar(this._facade,context);
			if (side == ms123.datamapper.Config.INPUT) {
				this._inputAreaToolbar=toolbar;
			}else{
				this._outputAreaToolbar=toolbar;
			}
			new ms123.datamapper.plugins.ContextMenu(this._facade, context);
			var edit = new ms123.datamapper.plugins.TreeEdit(this._facade, context);
			new ms123.datamapper.plugins.MetadataEdit(this._facade,context);
			new ms123.datamapper.plugins.EntityCreate(this._facade,context);
			this._disposeList.push(edit);
			toolbar.registryChanged(this.getPluginsData());
			container.add(toolbar, {
				edge: "north"
			});

			container.add(tree, {
				edge: "center"
			});
			this._facade.raiseEvent({
				type: side == ms123.datamapper.Config.INPUT ? ms123.datamapper.Config.EVENT_INPUTTREE_CREATED : 
																													 ms123.datamapper.Config.EVENT_OUTPUTTREE_CREATED,
				force: true
			})
			return container;
		},
		_getConfig: function () {
			var data = {};
			data.input = qx.lang.Json.parse(qx.util.Serializer.toJson(this._facade.inputTree.getModel()));
			data.output = qx.lang.Json.parse(qx.util.Serializer.toJson(this._facade.outputTree.getModel()));
			data.mapping = this._facade.mappingEdit.getCleanMappings();
			data.fileId = this._facade.preview.getFileId();
			return data;
		},
		_getMainEntity:function(tree){
			var mainEntity = tree.cleanName;
			var cm = new ms123.config.ConfigManager();
			if( cm.getEntity(mainEntity, this._facade.storeDesc)){
				return mainEntity;
			}
			for( var i=0; i < tree.children.length;i++){
				var me = this._getMainEntity(tree.children[i]);	
				if( me) return me;
			}
			return null;
		},
		_save: function (data) {
			if( !data){
				data = this._getConfig();
			}
			if( this._facade.defaults){
				data.defaults = this._facade.defaults.getValue();
				console.debug("Def:"+JSON.stringify(this._facade.defaults.getValue(),null,2));
			}
			this.fireDataEvent("save2", data);
			this.fireDataEvent("save", JSON.stringify(data,null,2));
		},
		_update: function () {
			if(this._inputAreaToolbar)this._inputAreaToolbar.onUpdate();
			if(this._outputAreaToolbar)this._outputAreaToolbar.onUpdate();
			if(this._previewToolbar)this._previewToolbar.onUpdate();
		},
		_destroy:function(){
			console.log("datamapper._destroy");
			this._disposeList.each( function(o){
				o.dispose();	
			});
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {
		console.error("datamapper.Destruct");
	}
});
