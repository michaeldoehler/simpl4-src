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
	@asset(qx/icon/${qx.icontheme}/22/actions/*)
	@asset(qx/icon/${qx.icontheme}/16/places/*)
	@asset(qx/icon/${qx.icontheme}/22/status/*)
	@asset(qx/icon/${qx.icontheme}/16/apps/*)
	@asset(qx/icon/${qx.icontheme}/16/mimetypes/*)
	@ignore($)
	@lint ignoreDeprecated(alert,eval) 
*/



qx.Class.define('ms123.widgets.Tree', {
	extend: ms123.widgets.Widget,
	events : {
		 "nodeDeleted" : "qx.event.type.Data"
	},

	construct: function (context) {
		this.base(arguments);

		this._context = context;
		var dock = new qx.ui.layout.Dock();
		this._setLayout(dock);

		this._treeController = this._createTree();

		if( context.baseurl || context.rpcMethod ){
			var url = context.baseurl;
			if( context.modul ){
				url += "/"+ context.modul;
			}
			if( context.queryParams ){
				url += "?" + context.queryParams;
			}
			var tdata = null;
			if( context.rpcMethod ){
				try{
					tdata = ms123.util.Remote.rpcSync(context.rpcMethod, context.rpcParams);
				}catch(e){
					ms123.form.Dialog.alert("widgets.Tree.construct:"+e);
					return;
				}
			}else{
				tdata = ms123.util.Remote.sendSync(url);
			}
			if( tdata.tree && tdata.tree.length){
				tdata = eval(tdata.tree)[0];
			}
			if( tdata.title == null ){
				tdata.title = this.tr( "meta.tree.root."+ tdata.id );
			}else{
				tdata.title = this.tr( "meta.tree.root."+ tdata.title );
			}
			this.setModel(tdata);
		}else if( context.treeModel ){
			this.setModel(context.treeModel);
		}
	},

	members: {
		_createTree: function () {
		  var tree = this.getChildControl("wtree");
			this.add(tree, {
				edge: "center"
			});
			if( this._context.hideRoot !== undefined ){
      	tree.setHideRoot(this._context.hideRoot);
			}
      tree.setRootOpenClose(true);

      tree.addListener("click", this._context.clickListener, tree );
      var treeController = new qx.data.controller.Tree(null, tree, "children", "id");
      return treeController;

		},
		setModel: function(d){
			if( d.children == null ){
				d.children = [];
			}
      this._model = qx.data.marshal.Json.createModel(d);
			this._model.parent = null;
			this._setParentModel( this._model );
      this._treeController.setDelegate(this);
      this._treeController.setModel(this._model);
		},

		// delegate implementation
		bindItem: function (controller, item, id) {
			controller.bindProperty("title", "label", null, item, id);

			var level = id.getLevel();
			if( level == "0") item.setOpen( true );
			var menu = this._context.createMenu(item,level, id, this );
			if( menu != null ){
				item.setContextMenu(menu);
			}
		},
		createItem: function () {
			var item = new ms123.searchfilter.TreeItem();
			item.setOpen(false);

			item.addSpacer();
			item.addOpenButton();
			item.addIcon();
			item.addLabel();

			return item;
		},
    _createTreeNode: function (pmodel, name, meta) {
			var path = this._getModelPath( pmodel );
			var model = this._createModelNode(name,path.length, meta);
			model = qx.data.marshal.Json.createModel(model);
			model.parent = pmodel;
			pmodel.getChildren().push( model );
			return path.join("/") + "/" + name;
    },
    _createModelNode: function (name,plen, meta) {
			var data = {
					"title":name,
					"level":""+plen,
					"children":[],
					"id":name
			}
			if( meta ){
				for (var attrname in meta) { data[attrname] = meta[attrname]; }
			}
      return qx.data.marshal.Json.createModel(data);
		},
		_getModelPath: function (model) {
			var path = [];
			path.push( model.getId() );
			while(model.parent){
				model = model.parent;
				path.push( model.getId() );
			}
			return path.reverse();
		},
		_getChildren: function( model ){
      var children=null;
			try{
      	children = model.getChildren();
			}catch(e){
				return new qx.data.Array();
			}
			return children;
		},
		_setParentModel: function (model) {
      var children=this._getChildren(model);
			if (children.getLength() > 0) {
				for (var i = 0; i < children.getLength(); i++) {
					var c = children.getItem(i);
					c.parent = model;
					this._setParentModel(c);
				}
			}
			return null;
		},
		_createChildControlImpl: function (id) {
			var control;

			switch (id) {
			case "wtree":
				control = this._tree = new qx.ui.tree.Tree().set({
					decorator:null
				});
				break;
			}

			return control || this.base(arguments, id);
		}
	},
	destruct : function() {
  }

});
