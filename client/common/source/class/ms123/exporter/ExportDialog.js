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
	@asset(qx/icon/${qx.icontheme}/16/actions/*)
	@asset(qx/icon/${qx.icontheme}/16/places/*)
	@asset(ms123/icons/*)
	@asset(ms123/*)
*/

qx.Class.define("ms123.exporter.ExportDialog", {
 extend: qx.core.Object,
 include : qx.locale.MTranslation,


	statics : {
    _top : 35,
    _left : 100
  },

	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (context) {
		this.base(arguments);

		this._context = context;

		this._init(context);
	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},


	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		_init: function (context) {
			var app = qx.core.Init.getApplication();
			var win = this._createWindow(app.getRoot(), "Export" );
			this._mainTabs = new qx.ui.tabview.TabView().set({
				contentPadding: 0
			});
			win.add(this._mainTabs, { edge: "center", left: 0, top:0 });
			win.add(this._createToolbar(), { edge: "south", left: 0, top:0 });
			this._window = win;
			this._withXML = true;
			if( context.noXML && context.noXML == true){
				this._withXML = false;
			}

    	this._fieldsArray = qx.lang.Json.parse(context.fields);
			this._makeOrderbyOptions( this._fieldsArray );
			this._makeTabs();
			this._form = this._csvForm;
			this._format = "csv";
			this._mainTabs.addListener("changeSelection", function (e) {
				var pid = e._target.getSelection()[0].getUserData("id");
				this._format = pid;
				if( pid == "csv" ){
					this._form = this._csvForm;
				}else if( pid == "pdf" ){
					this._form = this._pdfForm;
				}else if( pid == "html" ){
					this._form = this._htmlForm;
				}else if( pid == "xml" ){
					this._form = this._xmlForm;
				}else if( pid == "xls" ){
					this._form = this._xlsForm;
				}
      }, this);
		},
		_createWindow: function (root, name) {
			var win = new qx.ui.window.Window(name, "").set({
				resizable: true,
				useMoveFrame: true,
				useResizeFrame: true
			});
			win.setLayout(new qx.ui.layout.Dock());
			win.setWidth(550);
			win.setHeight(490);
			win.setAllowMaximize(false);
			win.moveTo( ms123.exporter.ExportDialog._left, ms123.exporter.ExportDialog._top );
			win.open();
			win.addListener("changeActive", function (e) {
				console.log("changeActive:" + e.getData());
			}, this);
//			root.add(win, {
//				left: ms123.exporter.ExportDialog._left,
//				top: ms123.exporter.ExportDialog._top
//			});
			win.setModal( true );
			return win;
		},
		_makeTabs: function () {
			this._csvPage = new qx.ui.tabview.Page("CSV","ms123/csv_icon.png").set({
				showCloseButton: false
			});
			this._csvPage.setUserData("id", "csv");
			this._csvPage.setDecorator(null);
			this._csvPage.setLayout(new qx.ui.layout.Dock());
			this._csvPage.add( this._createCSVControls(), { edge:"center", left: 50, top: 50 });
			this._csvPage.add( this._createCSVHiddenForm(), { edge:"south", left: 0, top: 0 });
			this._mainTabs.add(this._csvPage, {
				edge: 0
			});


			this._xlsPage = new qx.ui.tabview.Page("XLS","ms123/excel_icon.png").set({
				showCloseButton: false
			});
			this._xlsPage.setDecorator(null);
			this._xlsPage.setLayout(new qx.ui.layout.Dock());
			this._xlsPage.setUserData("id", "xls");
			this._xlsPage.add( this._createXLSControls(), { edge:"center", left: 50, top: 50 });
			this._xlsPage.add( this._createXLSHiddenForm(), { edge:"south", left: 0, top: 0 });
			this._mainTabs.add(this._xlsPage, {
				edge: 0
			});

			this._pdfPage = new qx.ui.tabview.Page("PDF","ms123/pdf_icon.gif").set({
				showCloseButton: false
			});
			this._pdfPage.setDecorator(null);
			this._pdfPage.setLayout(new qx.ui.layout.Dock());
			this._pdfPage.setUserData("id", "pdf");
			this._pdfPage.add( this._createPDFControls(), { edge:"center", left: 50, top: 50 });
			this._pdfPage.add( this._createPDFHiddenForm(), { edge:"south", left: 0, top: 0 });
			this._mainTabs.add(this._pdfPage, {
				edge: 0
			});

			this._htmlPage = new qx.ui.tabview.Page("HTML","ms123/html_icon.gif").set({
				showCloseButton: false
			});
			this._htmlPage.setDecorator(null);
			this._htmlPage.setLayout(new qx.ui.layout.Dock());
			this._htmlPage.setUserData("id", "html");
			this._htmlPage.add( this._createHTMLControls(), { edge:"center", left: 50, top: 50 });
			this._htmlPage.add( this._createHTMLHiddenForm(), { edge:"south", left: 0, top: 0 });
			this._mainTabs.add(this._htmlPage, {
				edge: 0
			});
			if( this._withXML == true ){
				this._xmlPage = new qx.ui.tabview.Page("XML","ms123/xml_icon.gif").set({
					showCloseButton: false
				});
				this._xmlPage.setDecorator(null);
				this._xmlPage.setLayout(new qx.ui.layout.Dock());
				this._xmlPage.setUserData("id", "xml");
				this._xmlPage.add( this._createXMLControls(), { edge:"center", left: 50, top: 50 });
				this._xmlPage.add( this._createXMLHiddenForm(), { edge:"south", left: 0, top: 0 });
				this._mainTabs.add(this._xmlPage, {
					edge: 0
				});
			}
		},
		_hideTabs: function () {
			this._mainTabs.setEnabled(false);
			this._mainTabs.setVisibility("hidden");
		},
		_showTabs: function () {
			this._mainTabs.setEnabled(true);
			this._mainTabs.setVisibility("visible");
		},

		_makeOrderbyOptions: function (fields) {
			this._orderbyOptions = [];
			for(var i=0; i < fields.length;i++){
				var o = {};
				o.label = (i+1)+"";
				o.value = (i+1);
				this._orderbyOptions.push( o );
			}
		},
		_createCSVControls: function () {
			var formData = {
				"quote" : {
					'type'  : "ComboBox",
					'label' : this.tr("export.csv.quote"),
					'value' : '\"',
					'options' : [
						{ 'label' : "\"" },
						{ 'label' : "'" }
					]
				},
				"columnDelim" : {
					'type'  : "ComboBox",
					'label' : this.tr("export.csv.col_delimeter"),
					'value' : ',',
					'options' : [
						{ 'label' : "," },
						{ 'label' : "TAB" },
						{ 'label' : ";" }
					]
				},
				"rowDelim" : {
					'type'  : "SelectBox",
					'label' : this.tr("export.csv.row_delimeter"),
					'value' : 1,
					'options' : [
						{ label: "Windows - CR/LF",   value: "\r\n"},
						{ label: "Unix - LF",   value: "\n"},
						{ label: "CR", value: "\r"}
					]
				},
				"header" : {
					'type'  : "CheckBox",
					'label' : this.tr("export.csv.include_column_header"),
					'value' : false
				},
				"alwaysQuote" : {
					'type'  : "CheckBox",
					'label' : this.tr("export.csv.always_quote"),
					'value' : true
				},
				"excel" : {
					'type'  : "CheckBox",
					'label' : this.tr("export.csv.excel_compatible"),
					'value' : true
				},
				"orderby" : {
					'type'  : "SelectBox",
					'label' : this.tr("export.csv.order_first_n_field"),
					'value' : 1,
					'options' : this._orderbyOptions
				},
				"filename" : {
					'type'  : "TextField",
					'label' : this.tr("export.csv.filename"),
					'validation': {
						required: true,
						validator: "/^[A-Za-z]([0-9A-Za-z_.]){2,20}$/"
					},
					'value' : "download.csv"
				}
			}
      this._csvForm = new ms123.form.Form({
				"tabs": [ { id:"tab1",layout:"single", lineheight:20 } ],
        "formData": formData,
        "allowCancel": true,
        "inWindow": false,
				"buttons":[],
        "callback": function (m,v) {
        },
        "context": null
      });
			return this._csvForm;
		},
		_createPDFControls: function () {
			var formData = {
				"header" : {
					'type'  : "CheckBox",
					'label' : this.tr("export.csv.include_column_header"),
					'value' : true
				},
				"landscape" : {
					'type'  : "CheckBox",
					'label' : this.tr("export.csv.landscape"),
					'value' : false
				},
				"orderby" : {
					'type'  : "SelectBox",
					'label' : this.tr("export.csv.order_first_n_field"),
					'value' : 1,
					'options' : this._orderbyOptions
				},
				"filename" : {
					'type'  : "TextField",
					'label' : this.tr("export.csv.filename"),
					'validation': {
						required: true,
						validator: "/^[A-Za-z]([0-9A-Za-z_.]){2,20}$/"
					},
					'value' : "download.pdf"
				}
			}
      this._pdfForm = new ms123.form.Form({
				"tabs": [ { id:"tab1",layout:"single", lineheight:20 } ],
        "formData": formData,
        "allowCancel": true,
        "inWindow": false,
				"buttons":[],
        "callback": function (m,v) {
        },
        "context": null
      });
			return this._pdfForm;
		},
		_createHTMLControls: function () {
			var formData = {
				"header" : {
					'type'  : "CheckBox",
					'label' : this.tr("export.csv.include_column_header"),
					'value' : true
				},
				"orderby" : {
					'type'  : "SelectBox",
					'label' : this.tr("export.csv.order_first_n_field"),
					'value' : 1,
					'options' : this._orderbyOptions
				},
				"filename" : {
					'type'  : "TextField",
					'label' : this.tr("export.csv.filename"),
					'validation': {
						required: true,
						validator: "/^[A-Za-z]([0-9A-Za-z_.]){2,20}$/"
					},
					'value' : "download.html"
				}
			}
      this._htmlForm = new ms123.form.Form({
				"tabs": [ { id:"tab1",layout:"single", lineheight:20 } ],
        "formData": formData,
        "allowCancel": true,
        "inWindow": false,
				"buttons":[],
        "callback": function (m,v) {
        },
        "context": null
      });
			return this._htmlForm;
		},
		_createXMLControls: function () {
			var formData = {
				"filename" : {
					'type'  : "TextField",
					'label' : this.tr("export.csv.filename"),
					'validation': {
						required: true,
						validator: "/^[A-Za-z]([0-9A-Za-z_.]){2,20}$/"
					},
					'value' : "download.xml"
				},
				"withNullValues" : {
					'type'  : "CheckBox",
					'label' : this.tr("export.xml.with_null_values"),
					'value' : false
				}
			}
      this._xmlForm = new ms123.form.Form({
				"tabs": [ { id:"tab1",layout:"single", lineheight:20 } ],
        "formData": formData,
        "allowCancel": true,
        "inWindow": false,
				"buttons":[],
        "callback": function (m,v) {
        },
        "context": null
      });
			return this._xmlForm;
		},
		_createXLSControls: function () {
			var formData = {
				"header" : {
					'type'  : "CheckBox",
					'label' : this.tr("export.csv.include_column_header"),
					'value' : false
				},
				"orderby" : {
					'type'  : "SelectBox",
					'label' : this.tr("export.csv.order_first_n_field"),
					'value' : 1,
					'options' : this._orderbyOptions
				},
				"filename" : {
					'type'  : "TextField",
					'label' : this.tr("export.csv.filename"),
					'validation': {
						required: true,
						validator: "/^[A-Za-z]([0-9A-Za-z_.]){2,20}$/"
					},
					'value' : "download.xls"
				}
			}
      this._xlsForm = new ms123.form.Form({
				"tabs": [ { id:"tab1",layout:"single", lineheight:20 } ],
        "formData": formData,
        "allowCancel": true,
        "inWindow": false,
				"buttons":[],
        "callback": function (m,v) {
        },
        "context": null
      });
			return this._xlsForm;
		},
		_createCSVHiddenForm: function () {
			var htmlForm =
				'<form id="downloadFormPOST-csv" action="" method="post" enctype="application/json" accept-charset="UTF-8" target="_top">' +
				'<input type="hidden" name="__rpc__" value="">' +
				'</form>';
			var hiddenForm = new qx.ui.embed.Html(htmlForm); 
			hiddenForm.setHeight(0);
			return hiddenForm;
		},
		_createHTMLHiddenForm: function () {
			var htmlForm =
				'<form id="downloadFormPOST-html" action="" method="post" accept-charset="UTF-8" target="_blank">' +
				'<input type="hidden" name="__rpc__" value="">' +
				'</form>';
			var hiddenForm = new qx.ui.embed.Html(htmlForm); 
			hiddenForm.setHeight(0);
			return hiddenForm;
		},
		_createXMLHiddenForm: function () {
			var xmlForm =
				'<form id="downloadFormPOST-xml" action="" method="post" accept-charset="UTF-8" target="_blank">' +
				'<input type="hidden" name="__rpc__" value="">' +
				'</form>';
			var hiddenForm = new qx.ui.embed.Html(xmlForm); 
			hiddenForm.setHeight(0);
			return hiddenForm;
		},
		_createXLSHiddenForm: function () {
			var htmlForm =
				'<form id="downloadFormPOST-xls" action="" method="post" accept-charset="UTF-8" target="_top">' +
				'<input type="hidden" name="__rpc__" value="">' +
				'</form>';
			var hiddenForm = new qx.ui.embed.Html(htmlForm); 
			hiddenForm.setHeight(0);
			return hiddenForm;
		},
		_createPDFHiddenForm: function () {
			var htmlForm =
				'<form id="downloadFormPOST-pdf" action="" method="post" accept-charset="UTF-8" target="_top">' +
				'<input type="hidden" name="__rpc__" value="">' +
				'</form>';
			var hiddenForm = new qx.ui.embed.Html(htmlForm); 
			hiddenForm.setHeight(0);
			return hiddenForm;
		},
		_createToolbar: function () {
      var toolbar = new qx.ui.toolbar.ToolBar();
      toolbar.setSpacing(5);
      toolbar.addSpacer();
      toolbar.addSpacer();
			var buttonExport = new qx.ui.toolbar.Button(this.tr("export.export_button"), "icon/16/actions/document-revert.png");
			buttonExport.addListener("execute", function () {
				var m = this._form.getModel();
				var options = qx.util.Serializer.toJson(m);
				var orderby="";
				if( this._format != "xml" ){
					var komma = "";
					for( var i=0; i < m.getOrderby();i++){
							orderby+= komma + this._fieldsArray[i];
							komma = ",";
					}
				}
				var u = ms123.util.Remote._username;
				var p = ms123.util.Remote._password;
				var credentials = ms123.util.Base64.encode( u + ":"+ p );
				var downloadForm = window.document.getElementById("downloadFormPOST-"+this._format);
				var aliases=null;
				if( this._context.aliases){
						aliases=qx.lang.Json.parse(this._context.aliases);
				}
				var rpc = {
					"service": "exporting",
					"method": "exportData",
					"id": 31,
					"params": {
						storeId: this._context.storeDesc.getStoreId(),
						filters:qx.lang.Json.parse(this._context.filter),
						fields:qx.lang.Json.parse(this._context.fields),
						aliases:aliases,
						options:qx.lang.Json.parse(options),
						orderby:orderby,
						mainEntity:this._context.mainModule,
						format:this._format
					}
				};
				downloadForm.action = "/rpc/__rpcForm__?credentials=" + credentials;
				var rpcString = qx.util.Serializer.toJson(rpc);
				downloadForm["__rpc__"].value = rpcString;
				downloadForm.submit(); 

			}, this);
			toolbar._add( buttonExport )

			var buttonClose = new qx.ui.toolbar.Button(this.tr("export.close_button"), "icon/16/actions/dialog-close.png");
			buttonClose.setToolTipText(this.tr("meta.lists.fs.down"));
			buttonClose.addListener("execute", function () {
				this._window.close();
			}, this);
			toolbar._add( buttonClose )
			return toolbar;
		}
	}
});
