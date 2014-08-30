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
qx.Class.define("ms123.datamapper.TreeItem", {
  extend : qx.ui.tree.core.AbstractTreeItem,
	construct: function (side) {
		this.base(arguments);
		this._side = side;
		this.addListenerOnce("changeModel",function(ev){
			this._setId();
		},this);
	},

  properties : {
    appearance : {
      refine : true,
      init : "tree-folder"
    }
  },

  events : {
    open : "qx.event.type.Event"
	},
	members: {
		_addWidgets: function () {
			this.addSpacer();
			this.addOpenButton();
			this.addIcon();
			this.addLabel();
		},
		setTitle:function(name){
			var label = this.getChildControl("label");
			if( this.getModel().getFieldType ){
				label.setValue( name + " : "+ this.getModel().getFieldType() );
			}else{
				label.setValue( name );
			}
		},
		_setId: function () {
			this.getContentElement().addClass(this._side+'TreeItem');
			var label = this.getChildControl("label");
			label.setTextColor(ms123.datamapper.Config.TREE_LABEL_COLOR);
			label.getContentElement().addClass(ms123.datamapper.Config.IDPREFIX+this._side+'TreeItemLabel');
			//this.setTitle( this.getModel().getName() );

			var open = this.getChildControl("open");
			open.getContentElement().addClass('treeItemOpener');
    	open.addListener("click", function(e){
				var data = {
					item:this,
					open:open.hasState("opened")
				};
      	this.fireDataEvent("open", data);
			},this);
		}
	}
});
