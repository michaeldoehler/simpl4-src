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
	@asset(qx/icon/${qx.icontheme}/48/status/dialog-warning.png)
*/

/**
 * Confirmation popup singleton
 */
qx.Class.define("ms123.form.Confirm",
{
  extend : ms123.form.Dialog,
  
  /*
  *****************************************************************************
     STATIC METHODS
  *****************************************************************************
  */     
  statics:
  {
    /**
     * Returns singleton instance of this class. This method has to
     * be part of any subclass extending this widget.
     */
    getInstance : function()
    {
      return this.superclass.getInstance(this.classname);
    }
  },
  /*
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */     
  properties :
  {
    yesButtonLabel :
    {
      check : "String",
      nullable : false,
      init : "Yes",
      event : "changeYesButtonLabel"
    },

    yesButtonIcon :
    {
      check : "String",
      nullable : true,
      init : "icon/22/actions/dialog-ok.png",
      event : "changeYesButtonIcon"
    },    
    
    noButtonLabel :
    {
      check : "String",
      nullable : false,
      init : "No",
      event : "changeNoButtonLabel"
    },

    noButtonIcon :
    {
      check : "String",
      nullable : true,
      init : "icon/22/actions/dialog-cancel.png",
      event : "changeNoButtonIcon"
    },
    
    allowCancel :
    {
      refine : true,
      init : false
    }
  },
  
  /*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */     
  members :
  {
    
    /*
    ---------------------------------------------------------------------------
       PRIVATE MEMBERS
    ---------------------------------------------------------------------------
    */  
    _yesButton : null,
    _noButton  : null,
    
    /*
    ---------------------------------------------------------------------------
       WIDGET LAYOUT
    ---------------------------------------------------------------------------
    */     
    
    /**
     * Create the main content of the widget
     */
    _createWidgetContent : function()
    {      

      /*
       * groupbox
       */
      var groupboxContainer = new qx.ui.groupbox.GroupBox().set({
        contentPadding: [16, 16, 16, 16]
      });
      groupboxContainer.setLayout( new qx.ui.layout.VBox(10) );
      this.add( groupboxContainer );

      var hbox = new qx.ui.container.Composite;
      hbox.setLayout( new qx.ui.layout.HBox(10) );
      groupboxContainer.add( hbox );
      
      /*
       * add image 
       */
			console.log("warning:"+this._warn);
			if( this._warn ){
      	this._image = new qx.ui.basic.Image("icon/48/status/dialog-warning.png");
			}else{
				this._image = new qx.ui.basic.Image();
				this._image.setVisibility("excluded");
			}
				hbox.add( this._image );
      
      /*
       * Add message label
       */
      this._message = new qx.ui.basic.Label();
      this._message.setRich(true);
      this._message.setWidth(200);
      this._message.setAllowStretchX(true);
      hbox.add( this._message, {flex:1} );    
      
      var _this = this;
      
      /* 
       * Yes button 
       */
      var yesButton = this._yesButton =  new qx.ui.form.Button;
      yesButton.setAllowStretchX(true);
      yesButton.addListener("execute", this._handleYes, this );
      //this.bind("yesButtonLabel", yesButton, "label");
			yesButton.setLabel(this.tr("yes"));
      this.bind("yesButtonIcon",  yesButton, "icon");
      
      /* 
       * No button 
       */
      var noButton = this._noButton = new qx.ui.form.Button;
      noButton.setAllowStretchX(true);
      noButton.addListener("execute", this._handleNo, this );
			noButton.setLabel(this.tr("no"));
      //this.bind("noButtonLabel",noButton, "label" );
      this.bind("noButtonIcon", noButton, "icon" );
      
      /* 
       * Cancel Button 
       */
      var cancelButton = this._createCancelButton();
      
      /*
       * buttons pane
       */
      var buttonPane = new qx.ui.container.Composite;
      var bpLayout = new qx.ui.layout.HBox(5)
      bpLayout.setAlignX("center");
      buttonPane.setLayout( bpLayout );
      buttonPane.add( yesButton );
      buttonPane.add( noButton );
      buttonPane.add( cancelButton );
      groupboxContainer.add(buttonPane);
        
    },
    
    /*
    ---------------------------------------------------------------------------
       EVENT HANDLERS
    ---------------------------------------------------------------------------
    */     
    
    /**
     * Handle click on yes button. Calls callback with
     * a "true" value
     */
    _handleYes : function()
    {
      this.hide();
      if( this.getCallback() )
      {
        this.getCallback().call(this.getContext(),true);
      }
      this.resetCallback();
    },  

    /**
     * Handle click on no button. Calls callback with 
     * a "false" value
     */
    _handleNo : function()
    {
      this.hide();
      if( this.getCallback() )
      {
        this.getCallback().call(this.getContext(),false);
      }
    } 
  }
});
