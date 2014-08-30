qx.Class.define("website.TreeItem", {
  extend : qx.ui.tree.VirtualTreeItem,

  properties :
  { 
		appearance: {
			refine: true,
			init: "tree-file"
		}
	},

  members : {
    _addWidgets : function() {
      this.addSpacer();
      this.addOpenButton();

      this.addIcon();
      //this.setIcon("icon/16/places/user-desktop.png");
      var icon = this.getChildControl("icon");
      icon.setWidth(24);


      // The label
      this.addLabel();


      // All else should be right justified
      this.addWidget(new qx.ui.core.Spacer(), {flex: 1});

      var text = new qx.ui.basic.Label();
      this.addWidget(text);
    }
  }
});
