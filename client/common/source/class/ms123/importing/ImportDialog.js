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
  @ignore(FileReader) 
	@asset(qx/icon/${qx.icontheme}/16/actions/*)
	@asset(qx/icon/${qx.icontheme}/16/places/*)
	@asset(ms123/icons/*)
	@asset(ms123/*)
*/

qx.Class.define("ms123.importing.ImportDialog", {
	extend: qx.core.Object,
	include: qx.locale.MTranslation,

	statics: {
		_top: 35,
		_left: 100,
		html5drop: function (e) {
			console.error("html5drop:"+this);
			this.html5dropfiles(e.dataTransfer.files);

			e.stopPropagation();
			e.preventDefault();
		}
	},

	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (context) {
		this.base(arguments);

		this.__storeDesc = context.storeDesc;
		this.__id = context.id;
		this.__prefix = context.prefix;
		this.__mainEntity = context.mainEntity;
		this.__fileType = context.fileType;
		this.__configManager = new ms123.config.ConfigManager();

		var win = context.parentWidget;
		if (win.hasChildren()) {
			win.removeAll();
		}
		//this.__user = ms123.util.Remote.rpcSync("auth:getUserProperties");
		this.__user = ms123.config.ConfigManager.getUserProperties();
		this._mainTabs = new qx.ui.tabview.TabView().set({
			contentPadding: 0
		});
		win.add(this._mainTabs, {
			edge: "center"
		});
		win.add(this._createToolbar(), {
			edge: "south"
		});
		this._window = win;

		this._mainTabs.addListener("changeSelection", function (e) {
			var pid = e._target.getSelection()[0].getUserData("id");
			if (pid == "upload") {
				this.setRpcHeader(this._uploadForm, this.__id);
			}
		}, this);

		this._makeTabs();
		this._createSourceSetupPage();
		this._inputModel = this._getInputModel();
		this._loadSetup();
		this._setUploadUrl();
		this.setRpcHeader(this._uploadForm, this.__id);

		if (this.__fileType == "csv") {
			this._mainTabs.setSelection([this._sourceSetupPage]);
		} else {
			this._mainTabs.setSelection([this._uploadPage]);
		}
	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},


	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		html5dropfiles: function (files) {
			for (var i = 0, f; f = files[i]; i++) {//here only one file allowed
				var filereader = new FileReader();
				filereader.file = f;
				var self = this;
				filereader.onloadend = (function (evt) {
					console.log("onloadend.total:" + evt.total+"/"+evt.loaded+"/"+evt.returnValue);
					console.log("onloadend.file.name:" + this.file.name);
					if( evt.total==0 ){
						ms123.form.Dialog.alert(self.tr("data.document.cannot_read")+":"+this.file.name);
						return;
					}
					//self.__id = this.file.name;
					var rpc = self._getRpcHeader();
					var params = rpc.params;
					params["fileContent"] = this.result;
					self._saveFile(rpc.params);
					self._getInputModel();
					self._loadSetup();
				});
				filereader.readAsDataURL(f);
			}
		},
		_createDropArea:function(){
			var l = new qx.ui.basic.Label(this.tr("import.drop_file")).set({
				allowGrowX:true,
				allowGrowY:true,
				rich:true,
				backgroundColor:"#dddddd"
			});
			l.addListener('appear', function () {
				var element = l.getContentElement().getDomElement();
				element.ondrop = ms123.importing.ImportDialog.html5drop.bind(this);
				element.ondragover = function () {
					return false;
				}
				element.ondragover = function () {
					return false;
				}
				
			}, this);
			this._dropArea = l;
			return l;
		},
		_saveFile: function (params) {
			try {
				ms123.util.Remote.rpcSync("importing:upload", params);
			} catch (e) {
				ms123.form.Dialog.alert("ImportDialog._saveFile:" + e);
				return null;
			}
		},

		_makeTabs: function () {
			this._sourceSetupPage = new qx.ui.tabview.Page(this.tr("import.select_the_source"), "resource/ms123/settings_icon.png").set({
				showCloseButton: false
			});
			this._sourceSetupPage.setUserData("id", "sourceSetup");
			this._sourceSetupPage.setDecorator(null);
			this._sourceSetupPage.setLayout(new qx.ui.layout.Dock());
			var page = null;
			this._sourceSetupPage.setEnabled(false);
			if (this.__fileType == "csv") {
				page = this._createCSVSourcePage();
				this._sourceSetupPage.setEnabled(true);
			}
			if (page != null) {
				this._sourceSetupPage.add(page, {
					edge: "center"
				});
			}
			this._mainTabs.add(this._sourceSetupPage, {
				edge: 0
			});

			this._uploadPage = new qx.ui.tabview.Page(this.tr("import.upload_exportfile"), "resource/ms123/upload_icon.gif").set({
				showCloseButton: false
			});
			this._uploadPage.setDecorator(null);
			this._uploadPage.setLayout(new qx.ui.layout.Dock());
			this._uploadPage.setUserData("id", "upload");
			this._uploadPage.add(this._createUploadPage(), {
				edge: "center"
			});
			this._mainTabs.add(this._uploadPage, {
				edge: 0
			});

			this._mappingPage = new qx.ui.tabview.Page(this.tr("import.map_fields"), "resource/ms123/mapping_icon.png").set({
				showCloseButton: false
			});
			this._mappingPage.setDecorator(null);
			this._mappingPage.setLayout(new qx.ui.layout.Dock());
			this._mappingPage.setUserData("id", "mapping");
			this._mappingPage.add(this._createMappingPage(), {
				edge: "center"
			});
			this._mappingPage.setEnabled(false);
			this._mainTabs.add(this._mappingPage, {
				edge: 0
			});

			this._specifyDefaultsPage = new qx.ui.tabview.Page(this.tr("import.specify_defaults"), "resource/ms123/defaults_icon.gif").set({
				showCloseButton: false
			});
			this._specifyDefaultsPage.setDecorator(null);
			this._specifyDefaultsPage.setLayout(new qx.ui.layout.Dock());
			this._specifyDefaultsPage.setUserData("id", "defaults");
			var defaultPage = this._createDefaultPage();
			defaultPage.setup(this.__mainEntity, 1);
			this._specifyDefaultsPage.add(defaultPage, {
				edge: "center"
			});
			this._specifyDefaultsPage.setEnabled(false);
			this._mainTabs.add(this._specifyDefaultsPage, {
				edge: 0
			});

			this._resultPage = new qx.ui.tabview.Page(this.tr("import.result"), "resource/ms123/upload_icon.gif").set({
				showCloseButton: false
			});
			this._resultPage.setDecorator(null);
			this._resultPage.setLayout(new qx.ui.layout.Dock());
			this._resultPage.setUserData("id", "result");
			this._resultPage.add(this._createResultPage(), {
				edge: "center"
			});
			this._resultPage.setEnabled(false);
			this._mainTabs.add(this._resultPage, {
				edge: 0
			});
		},
		_createWindow: function (root, name) {
			var win = new qx.ui.window.Window(name, "").set({
				resizable: true,
				useMoveFrame: true,
				useResizeFrame: true
			});
			win.setLayout(new qx.ui.layout.Dock());
			win.setWidth(650);
			win.setHeight(590);
			win.setAllowMaximize(false);
			win.moveTo(ms123.exporter.ExportDialog._left, ms123.exporter.ExportDialog._top);
			win.open();
			win.addListener("changeActive", function (e) {
				console.log("changeActive:" + e.getData());
			}, this);
			win.setModal(true);
			return win;
		},
		_createCSVSourcePage: function () {
			var formData = {
				"quote": {
					'type': "ComboBox",
					'label': this.tr("export.csv.quote"),
					'value': '\"',
					'options': [{
						'label': "\""
					},
					{
						'label': "'"
					}]
				},
				"columnDelim": {
					'type': "ComboBox",
					'label': this.tr("export.csv.col_delimeter"),
					'value': ',',
					'options': [{
						'label': ","
					},
					{
						'label': "TAB"
					},
					{
						'label': ";"
					}]
				},
				"header": {
					'type': "CheckBox",
					'label': this.tr("export.csv.include_column_header"),
					'value': true
				},
				"excel": {
					'type': "CheckBox",
					'label': this.tr("export.csv.excel_compatible"),
					'value': true
				}
			}
			this._csvForm = new ms123.form.Form({
				"tabs": [{
					id: "tab1",
					layout: "single",
					lineheight: 20
				}],
				"formData": formData,
				"allowCancel": true,
				"inWindow": false,
				"buttons": [],
				"callback": function (m, v) {},
				"context": null
			});
			return this._csvForm;
		},
		_createSourceSetupPage: function () {
			if (this._sourceSetupPage.hasChildren()) {
				this._sourceSetupPage.removeAll();
			}
			if (this.__fileType == "csv") {
				var page = this._createCSVSourcePage();
				this._sourceSetupPage.setEnabled(true);
				this._sourceSetupPage.add(page, {
					edge: "center",
					left: 50,
					top: 50
				});
			} else {
				this._sourceSetupPage.setEnabled(false);
			}
		},
		_setUploadUrl: function () {
			var u = ms123.util.Remote._username;
			var p = ms123.util.Remote._password;
			var credentials = ms123.util.Base64.encode(u + ":" + p);
			var url = "/rpc/xyz/" + this.__id + "?credentials=" + credentials;
			this._uploadForm.setUrl(url);
		},
		_createUploadPage: function () {
			var container = new qx.ui.container.Composite(new qx.ui.layout.Dock(10));

			var u = ms123.util.Remote._username;
			var p = ms123.util.Remote._password;
			var credentials = ms123.util.Base64.encode(u + ":" + p);

			var uploadForm = new ms123.upload.UploadForm('uploadFrm', 'meta/userdata/' + this.__user.userid + '/imports?credentials=' + credentials + "&module=import&method=put");
			uploadForm.setParameter('credentials', credentials);
			uploadForm.setPadding(8);

			this._uploadForm = uploadForm;

			var vb = new qx.ui.layout.VBox(10)
			uploadForm.setLayout(vb);
			container.add(uploadForm,{edge:"north"});

			var l = new qx.ui.basic.Label(this.tr("import.select_file"));
			l.setRich(true);
			uploadForm.add(l);

			var uploadField = new ms123.upload.UploadField('importfile', this.tr("import.select_button"), 'icon/16/actions/document-save.png');
			uploadForm.add(uploadField);

			uploadForm.addListener('sending', function (e) {
				this.debug('sending');
			});

			var bt = new qx.ui.form.Button(this.tr("import.upload_button"), "icon/16/actions/dialog-ok.png");
			bt.set({
				marginTop: 10,
				allowGrowX: true
			});
			uploadForm.add(bt);

			var _this = this;
			this._withMapping = false;
			uploadForm.addListener('completed', function (e) {
				_this._getInputModel();
				_this._loadSetup();
				ms123.form.Dialog.alert(_this.tr("import.uploading_complete"));
				uploadField.setFileName('');
				bt.setEnabled(true);
			});

			var resetButton = new qx.ui.form.Button(this.tr("import.reset_button"), "icon/16/actions/document-revert.png");
			resetButton.set({
				width: 120,
				allowGrowX: true
			});
			resetButton.addListener("execute", function (e) {
				uploadField.setFileName("");
			}, this);
			uploadForm.add(resetButton);

			bt.addListener('execute', function (e) {
				_this._withMapping = false;
				var f = uploadField.getFileName();
				if (f == null || f == '') {
					ms123.form.Dialog.alert(_this.tr("import.no_filename"));
					return;
				}
				uploadForm.send();
				this.setEnabled(false);
			});
			if(ms123.config.ConfigManager.hasDocumentDD()){
				container.add(this._createDropArea(),{edge:"center"});
			}
			
			return container;
		},

		_setMapping: function (mapping) {
			return;
		},

		_getRpcHeader: function () {
			var rpc = {
				"service": "importing",
				"method": "upload",
				"id": 31,
				"params": {
					"importingid": this.__prefix + "/" + this.__id,
					"settings": this._getSettings(),
					"withoutImport": true,
					"storeId": this.__storeDesc.getStoreId()
				}
			};
			return rpc;
		},
		setRpcHeader: function (form) {
			var rpc = this._getRpcHeader();
			var rpcString = qx.util.Serializer.toJson(rpc);
			form.setParameter("__rpc__", rpcString);
		},
		_getInputModel: function () {
			var postData = {};
			this._inputModel = this.__getFileModel(this.__id);
			if (!this._inputModel) {
				this._mappingPage.setEnabled(false);
				this._specifyDefaultsPage.setEnabled(false);
				return null;
			}
			if (this._inputModel.value == "csv-record") {
				this.__fileType = "csv";
			} else {
				this.__fileType = "xml";
			}
			console.log("filetype:" + this.__fileType);
			this._mappingPage.setEnabled(true);
			this._specifyDefaultsPage.setEnabled(true);
			this._resultPage.setEnabled(true);
			return this._inputModel;
		},
		__getFileModel: function (name) {
			console.log("getFileModel.name:" + name);
			var fileModel = null;
			try {
				fileModel = ms123.util.Remote.rpcSync("importing:getFileModel", {
					namespace: this.__storeDesc.getNamespace(),
					importingid: this.__prefix + "/" + name
				});
			} catch (e) {
				ms123.form.Dialog.alert("ImportDialog.__getFileModel:" + e);
				return null;
			}
			return fileModel;
		},
		_loadSettings: function (name) {
			console.log("getSettings.name:" + name);
			var settings = null;
			try {
				settings = ms123.util.Remote.rpcSync("importing:getSettings", {
					namespace: this.__storeDesc.getNamespace(),
					importingid: this.__prefix + "/" + name
				});
			} catch (e) {
				ms123.form.Dialog.alert("ImportDialog._loadSettings:" + e);
				return null;
			}
			return settings;
		},
		__updateImporting: function (name, settings) {
			console.log("__updateImporting.name:" + name);
			var ret = null;
			try {
				ret = ms123.util.Remote.rpcSync("importing:updateImporting", {
					namespace: this.__storeDesc.getNamespace(),
					settings: settings,
					importingid: this.__prefix + "/" + name
				});
			} catch (e) {
				ms123.form.Dialog.alert("ImportDialog.__updateImporting:" + e);
				return null;
			}
			return ret;
		},
		_createMappingPage: function () {
			this._modelMapping = new ms123.importing.ModelMapping({
				storeDesc: this.__storeDesc
			});
			return this._modelMapping;
		},
		_createDefaultPage: function () {
			if (this._specifyDefaultsPage.hasChildren()) {
				this._specifyDefaultsPage.removeAll();
			}
			var defaults = new ms123.importing.Defaults({
				storeDesc: this.__storeDesc
			});
			this._specifyDefaultsPage.add(defaults, {
				edge: "center"
			});
			this._defaults = defaults;
			return defaults;
		},
		_createResultRefForm: function (module) {
			var context = {};
			context.buttons = {};
			context.model = this.__configManager.getEntityModel(this.__mainEntity, this.__storeDesc, "main-form", "properties");
			context.config = this.__mainEntity;
			var form = new ms123.widgets.Form(context);
			this._resultRefForm = form;
			return form;
		},
		_createResultTable: function (result, preview) {
			if (this._resultRefForm == null) {
				this._resultPageBottomWidget.add(this._createResultRefForm(this.__mainEntity));
			} else {
				this._resultRefForm.fillForm({});
			}
			this._resultPage.setEnabled(true);
			if (this._resultTable) {
				this._resultUpperWidget.remove(this._resultTable);
				this._resultTable = null;
			}
			var fields = result.fields;
			var result = result.result;
			var cmd = preview ? "PREVIEW" : "IMPORT";
			this._resultDescription.setValue("<span style='color:red'>" + cmd + ":" + this.tr("import.result_rec") + ":\t" + result.length + ",\t" + this.tr("import.duplicated") + ":\t" + this._countDupletten(result) + ",\t" + this.tr("import.constraintViolationsLong") + ":\t" + this._countConstraintViolations(result) + "</span>");

			var columns = [];
			var col = {};
			col.name = col.id = "_duplicated_";
			col.label = this.tr("import.duplicated");
			columns.push(col);

			col = {};
			col.name = col.id = "_constraintViolations_";
			col.label = this.tr("import.constraintViolations");
			columns.push(col);

			var model = this.__configManager.getEntityModel(this.__mainEntity, this.__storeDesc, "main-grid", "properties");
			var cols = model.attr("colModel");
			var configData = {}
			configData.gridProps = model.attr("gridProps");
			configData.colModel = columns;
			for (var i = 0; i < cols.length; i++) {
				configData.colModel.push(cols[i]);
			}
			var gridConfig = {};
			gridConfig.model = this.__configManager.buildModel(configData.colModel, configData.gridProps);
			gridConfig.modelForm = this.__configManager.getEntityModel(this.__mainEntity, this.__storeDesc, "main-form", "properties");
			gridConfig.user = this.__user;
			gridConfig.storeDesc = this.__storeDesc;
			gridConfig.isMaster = true;
			gridConfig.loadBeforeEdit = false;
			gridConfig.unit_id = "importdialog/"+new Date().getTime()+"";
			gridConfig.config = this.__mainEntity;
			gridConfig.disable = ["export", "import", "add", "del"];

			var table = new ms123.widgets.Table(gridConfig);
			this._resultTable = table;
			this._resultUpperWidget.add(table, {
				edge: "center"
			});
			var tcm = table.getTable().getTableColumnModel();
			tcm.setDataCellRenderer(1, new ms123.util.TooltipRenderer());


			var eventBus = qx.event.message.Bus;
			eventBus.subscribe(gridConfig.unit_id + ".table.row.selected", function (msg) {
				var data = msg.getData();
				var refid = data.row._duplicated_id_;
				console.log("refid:"+refid);
				if (refid == null) {
					this._resultRefForm.fillForm({});
					return;
				}
				//var filter = this._createIdFilter([refid]);
				var records =  this._getRecordById( refid );
				if( records != null){
					this._resultRefForm.fillForm(records);
				}
			}, this);

			for (var i = 0; i < result.length; i++) {
				if (result[i].constraintViolations) {
					var cv = result[i].constraintViolations;
					var message = "";
					for (var j = 0; j < cv.length; j++) {
						var c = cv[j];
						message += c.path + ":" + c.message + " / ";
					}
					result[i]._constraintViolations_ = message;
				}
			}
			table.setData(result);
		},
		_countDupletten: function (result) {
			var dup = 0;
			for (var i = 0; i < result.length; i++) {
				var m = result[i];
				if (m._duplicated_id_) {
					dup++
				}
			}
			return dup;
		},
		_countConstraintViolations: function (result) {
			var cv = 0;
			for (var i = 0; i < result.length; i++) {
				var m = result[i];
				if (m.constraintViolations) {
					cv++
				}
			}
			return cv;
		},
		_createResultPage: function () {
			if (this._resultPage.hasChildren()) {
				this._resultPage.removeAll();
			}
			this._resultPage.setEnabled(false);
			var splitpane = new qx.ui.splitpane.Pane("vertical");
			splitpane.setDecorator(null);

			var upperWidget = new qx.ui.container.Composite();
			upperWidget.setLayout(new qx.ui.layout.Dock());
			upperWidget.setDecorator(null);
			splitpane.add(upperWidget, 2);
			this._resultUpperWidget = upperWidget;
			this._resultDescription = new qx.ui.basic.Label().set({
				decorator: "",
				allowGrowX: true,
				paddingTop: 2,
				paddingBottom: 2,
				value: "",
				rich: true
			});
			upperWidget.add(this._resultDescription, {
				edge: "north"
			});

			var bottomWidget = new qx.ui.container.Composite();
			bottomWidget.setLayout(new qx.ui.layout.Dock());
			bottomWidget.setDecorator(null);
			splitpane.add(bottomWidget, 2);
			this._resultPageBottomWidget = bottomWidget;
			this._resultPage.add(splitpane, {
				edge: "center"
			});
			return splitpane;
		},
		_createToolbar: function () {
			var toolbar = new qx.ui.toolbar.ToolBar();
			toolbar.setSpacing(5);
			toolbar.addSpacer();
			toolbar.addSpacer();
			var buttonSave = new qx.ui.toolbar.Button(this.tr("import.save_import"), "icon/16/actions/dialog-ok.png");
			buttonSave.addListener("execute", function () {
				this._modelMapping.prepareForSave();
				this._saveSetup();
			}, this);
			toolbar._add(buttonSave)
			this._buttonSave = buttonSave;

			var buttonImport = new qx.ui.toolbar.Button(this.tr("import.import_button"), "icon/16/actions/document-revert.png");
			buttonImport.addListener("execute", function () {
				this._mainTabs.setSelection([this._resultPage]);
				this._doPreviewOrImport(false, true, false, -1);
			}, this);
			toolbar._add(buttonImport)
			this._buttonImport = buttonImport;
			this._buttonImport.setEnabled(false);

			var buttonPreview = new qx.ui.toolbar.Button(this.tr("import.preview_button"), "icon/16/actions/edit-find.png");
			buttonPreview.addListener("execute", function () {
				this._mainTabs.setSelection([this._resultPage]);
				this._doPreviewOrImport(true, true, false, -1);
			}, this);
			toolbar._add(buttonPreview)
			this._buttonPreview = buttonPreview;
			this._buttonPreview.setEnabled(false);

			var buttonClose = new qx.ui.toolbar.Button(this.tr("import.close_button"), "icon/16/actions/dialog-close.png");
			buttonClose.addListener("execute", function () {
				this._window.close();
			}, this);
			toolbar._add(buttonClose)
			return toolbar;
		},
		_doPreviewOrImport: function (preview, showInTable, pretty, max) {
			var rpcParams = {
				storeId: this.__storeDesc.getStoreId(),
				importingid: this.__prefix + "/" + this.__id,
				max: max,
				withoutSave: preview
			}

			var params = {
				service: "importing",
				method: "doImport",
				parameter: rpcParams,
				async: false,
				context: this
			}
			var result = null;
			try {
				result = ms123.util.Remote.rpcAsync(params);
			} catch (e) {
				ms123.form.Dialog.alert("ImportDialog._doPreviewOrImport:" + e);
				return;
			}

			if (showInTable) {
				this._createResultTable(result, preview);
			}
			return result;
		},
		_getRecordById: function (id) {
			var rpcParams = {
				storeId: this.__storeDesc.getStoreId(),
				entity: this.__mainEntity,
				id: id
			}

			var params = {
				service: "data",
				method: "queryOne",
				parameter: rpcParams,
				async: false,
				context: this
			}
			var result = null;
			try {
				result = ms123.util.Remote.rpcAsync(params);
			} catch (e) {
				ms123.form.Dialog.alert("ImportDialog._getRecordById:" + e);
				return null;
			}
			return result;
		},
		_getRecordsByFilter: function (filter) {
			var rpcParams = {
				storeId: this.__storeDesc.getStoreId(),
				entity: this.__mainEntity,
				filter: filter
			}

			var params = {
				service: "data",
				method: "query",
				parameter: rpcParams,
				async: false,
				context: this
			}
			var result = null;
			try {
				result = ms123.util.Remote.rpcAsync(params);
			} catch (e) {
				ms123.form.Dialog.alert("ImportDialog._getRecordsByFilter:" + e);
				return null;
			}
			return result.rows;
		},
		_createIdFilter: function (idArray) {
			var idFilter = {
				label: "1",
				connector: "or",
				children: []
			};
			for (var i = 0; i < idArray.length; i++) {
				var node = new ms123.searchfilter.Node();
				node.setField("id");
				node.setOp("eq");
				node.setData(idArray[i]);
				idFilter.children.push(node);
			}
			return qx.lang.Json.parse(qx.util.Serializer.toJson(idFilter));
		},
		_getMappingSetup: function () {
			return this._modelMapping.getValue();
		},
		_setMappingSetup: function (data) {
			if (this._inputModel) {
				this._modelMapping.setup(this, this._inputModel, this.__mainEntity, this.__fileType, this.__user);
				this._modelMapping.setValue(data);
			}
		},
		_getSourceSetup: function () {
			var m = {};
			if (this._csvForm) {
				m = this._csvForm.getModel();
			}
			return qx.lang.Json.parse(qx.util.Serializer.toJson(m));
		},
		_setSourceSetup: function (map) {
			if (!this._csvForm) return;
			var model = this._csvForm.getModel();
			var props = qx.Class.getProperties(model.constructor);
			for (var i = 0, l = props.length; i < l; i++) {
				var p = props[i];
				if (map[p] != undefined) {
					model.set(p, map[p]);
				}
			}
		},
		_getDefaultsSetup: function () {
			if (this._defaults) {
				return this._defaults.getValue();
			}
		},
		_setDefaultsSetup: function (data) {
			if (this._inputModel) {
				this._defaults.setup(this.__mainEntity, this.__fileType == "csv" ? 1 : 3);
				this._defaults.setValue(data);
			}
		},
		_getSettings: function () {
			var settings = {};
			try {
				settings.mapping = this._getMappingSetup();
			} catch (e) {
				settings.mapping = [];
			}
			settings.sourceSetup = this._getSourceSetup();
			try {
				settings.defaults = this._getDefaultsSetup();
			} catch (e) {
				settings.defaults = [];
			}
			settings.mainEntity = this.__mainEntity;
			settings.fileType = this.__fileType;
			settings.inputModel = this._inputModel;
			//console.log("_getSettings:" + qx.util.Serializer.toJson(settings));
			if (settings.mapping.length > 0) {
				this._buttonImport.setEnabled(true);
				this._buttonPreview.setEnabled(true);
			}
			return settings;
		},
		_saveSetup: function () {
			var ret = this.__updateImporting(this.__id, this._getSettings());
			ms123.form.Dialog.alert(this.tr("import.import_saved"));
		},
		_loadSetup: function () {
			var data = this._loadSettings(this.__id);

			console.log("_loadSettings:" + qx.util.Serializer.toJson(data));
			this._setSourceSetup(data.sourceSetup ? data.sourceSetup : {});
			this._setMappingSetup(data.mapping ? data.mapping : []);
			this._setDefaultsSetup(data.defaults ? data.defaults : []);
			this._inputModel = data.inputModel;
		}

	}
});
