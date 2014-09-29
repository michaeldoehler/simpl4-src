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
  @ignore(FileReader) 
 */
qx.Class.define('ms123.datamapper.create.FormatSelector', {
	extend: qx.ui.container.Composite,

	construct: function (facade,side,formatOnly) {
		this.base(arguments);
		this._facade = facade;
		this._use = facade.use;
		this._side = side;
		this._formatOnly = formatOnly === true;
		this.setLayout(new qx.ui.layout.VBox(10));
		if( this._formatOnly === false){
			var statusLabel = this._createStatusDisplay();
			this.add(statusLabel);
		}
		var sb = this._createFormatSelectBox();
		this.add(sb);
		if( this._formatOnly === false){
			var rg = this._createKindRadioGroup();
			this.add(rg);
			this._createEditFieldButton();
			this.add(this._editFiedsButton);

			var csv = this._createCSVSpec();
			this.add(csv);

			this._createUploadContainer();
			this.add(this._uploadContainer,{flex:1});
		}
		this._enableDisable(false);
		this._cache={};
		this._currentFormat = this._formatSelectBox.getSelection()[0].getModel();
		this._currentKind;
		this._setStatus();
		this.addListener("changeValue", function(e){
			var data = e.getData();
			console.log("ChangeValue("+this._side+"):"+data);
			this._setStatus();
		},this);
	},

	properties: {},
	events: {
		"changeValue": "qx.event.type.Data",
		"formatChanged": "qx.event.type.Data"
	},
	/******************************************************************************
	 STATICS
	 ******************************************************************************/
	statics: {
		html5drop: function (e) {
			this.html5dropfiles(e.dataTransfer.files);

			e.stopPropagation();
			e.preventDefault();
		}
	},

	members: {
		_createFormatSelectBox: function () {
			var container = new qx.ui.container.Composite(new qx.ui.layout.HBox(5));
			container.add(new qx.ui.basic.Label(this.tr("datamapper.type")));

			var selectBox = new qx.ui.form.SelectBox();
			var tempItem = new qx.ui.form.ListItem(this.tr("CSV"), "ms123/csv_icon.png", ms123.datamapper.Config.FORMAT_CSV);
			if( this._use != ms123.datamapper.Config.USE_IMPORT  || this._side == ms123.datamapper.Config.INPUT){
				selectBox.add(tempItem);
			}
			//var tempItem = new qx.ui.form.ListItem(this.tr("datamapper.fixed_width"), "ms123/fixedwidth.gif", ms123.datamapper.Config.FORMAT_FW);
			//selectBox.add(tempItem);
			var tempItem = new qx.ui.form.ListItem(this.tr("XML"), "ms123/xml_icon.gif", ms123.datamapper.Config.FORMAT_XML);
			if( this._use != ms123.datamapper.Config.USE_IMPORT  || this._side == ms123.datamapper.Config.INPUT){
				selectBox.add(tempItem);
			}
			var tempItem = new qx.ui.form.ListItem(this.tr("JSON"), "ms123/json.png", ms123.datamapper.Config.FORMAT_JSON);
			if( this._use != ms123.datamapper.Config.USE_IMPORT  || this._side == ms123.datamapper.Config.INPUT){
				selectBox.add(tempItem);
			}
			var tempItem = new qx.ui.form.ListItem(this.tr("MAP(k,v)"), "ms123/map_icon.png", ms123.datamapper.Config.FORMAT_MAP);
			if( this._use != ms123.datamapper.Config.USE_IMPORT ){
				selectBox.add(tempItem);
			}
			var tempItem = new qx.ui.form.ListItem(this.tr("POJO"), "ms123/table.png", ms123.datamapper.Config.FORMAT_POJO);
			if( this._use != ms123.datamapper.Config.USE_IMPORT || this._side == ms123.datamapper.Config.OUTPUT){
				selectBox.add(tempItem);
			}
			//var tempItem = new qx.ui.form.ListItem(this.tr("Excel"), "ms123/excel_icon.png", ms123.datamapper.Config.FORMAT_EXCEL);
			//selectBox.add(tempItem);

			this._formatSelectBox = selectBox;
			selectBox.addListener("changeSelection",function(e){
				var format = this._formatSelectBox.getSelection()[0].getModel();
				this._currentFormat=format;
				this.fireDataEvent("formatChanged", format, null);

				var data = 	this._cache[this._getCacheKey()];
				this.fireDataEvent("changeValue", data, null);
				this._enableDisable();
				this._setRpcHeader(this._uploadForm, this.__id);
			},this);
			container.add(selectBox);
			return container;
		},
		selectFormat:function(format){
			var self = this;
			var selectables = this._formatSelectBox.getSelectables();
			selectables.each( function( s ){
				console.log("model="+s.getModel()+"/"+format);
				if( s.getModel()== format){
					this._currentFormat=format;
					self._formatSelectBox.setSelection([s]);
				}
			});
		},
		_createKindRadioGroup: function () {
			this._setInternal = true;
			var container = new qx.ui.container.Composite(new qx.ui.layout.HBox(5));
			var rg = new qx.ui.form.RadioGroup();
			var button1 = new qx.ui.form.RadioButton(this.tr("datamapper.from_example"));
			button1.setUserData("kind",ms123.datamapper.Config.KIND_FROM_SAMPLE);
			container.add(button1);

			var button2 = new qx.ui.form.RadioButton(this.tr("datamapper.user_defined"));
			button2.setUserData("kind",ms123.datamapper.Config.KIND_USER_DEFINED);
			button2.setValue(true);
			this._currentKind = ms123.datamapper.Config.KIND_USER_DEFINED;
			container.add(button2);

			if( this._use == ms123.datamapper.Config.USE_IMPORT && this._side == ms123.datamapper.Config.OUTPUT){
				var button3 = new qx.ui.form.RadioButton(this.tr("datamapper.like_input"));
				button3.setUserData("kind",ms123.datamapper.Config.KIND_LIKE_INPUT);
				button3.setValue(false);
				container.add(button3);
			}

			if( button3){
				rg.add(button1, button2, button3);
			}else{
				rg.add(button1, button2);
			}

			rg.addListener("changeSelection", function (ev) {
				this._currentKind=rg.getSelection()[0].getUserData("kind");
				var data = 	this._cache[this._getCacheKey()];
				if( this._currentKind == ms123.datamapper.Config.KIND_LIKE_INPUT){
					data = {
							kind: ms123.datamapper.Config.KIND_LIKE_INPUT,
							format: this._currentFormat
					}
				}
				this._enableDisable();
				this.fireDataEvent("changeValue", data, null);
			}, this);


			this._buttonFromExample = button1;
			this._setInternal = false;
			return container;
		},
		_createCSVSpec:function(){
			var f = new ms123.datamapper.create.CSVInlineFieldsEditor(this._facade,this._side);
			var form = f.getForm();
			this._csvForm = form;
			form.setVisibility("excluded");
			return form;
		},
		_isPOJO:function(){
			var format = this._formatSelectBox.getSelection()[0].getModel();
			if( format == ms123.datamapper.Config.FORMAT_POJO ){
				return true;
			}
			return false;
		},
		_enableDisable: function (flag) {
			if( this._setInternal === true ){
				 return;
			}
			this._setInternal = true;
			var uploadEnabled=false;
			var editFieldsEnabled = false;
			var currKind = this._currentKind;
			if (this._editFiedsButton && this._uploadContainer) {
				if( currKind == ms123.datamapper.Config.KIND_FROM_SAMPLE){
					uploadEnabled=true;
				}else if( currKind == ms123.datamapper.Config.KIND_USER_DEFINED){
					editFieldsEnabled=true;
				}else if( currKind == ms123.datamapper.Config.KIND_LIKE_INPUT){
				}
				if( this._isPOJO() && currKind!=ms123.datamapper.Config.KIND_LIKE_INPUT){
					uploadEnabled=false;
					editFieldsEnabled=true;
				}
				this._uploadContainer.setVisibility(uploadEnabled ? "visible" : "excluded");
				this._editFiedsButton.setVisibility(editFieldsEnabled ? "visible" : "excluded");
				
			}
			if( this._csvForm){
				var format = this._formatSelectBox.getSelection()[0].getModel();
				var flag = format == ms123.datamapper.Config.FORMAT_CSV && uploadEnabled === true;
				this._csvForm.setVisibility(!flag ? "excluded" : "visible");
			}
			this._setInternal = false;
		},
		_createEditFieldButton: function () {
			this._editFiedsButton = new qx.ui.form.Button(this.tr("datamapper.edit_fields"));
			this._editFiedsButton.addListener("execute", function (ev) {
				this._facade.format = this._formatSelectBox.getSelection()[0].getModel();
				this._fieldsEditor = this._createFieldsEditor(this._facade.format);
				var data = this._cache[this._getCacheKey()];
				this._fieldsEditor.setValue( data );
				this._fieldsEditor.addListener("changeValue", function (ev) {
					var format = this._formatSelectBox.getSelection()[0].getModel();
					var data = ev.getData();
					this._cache[this._getCacheKey()] = data;
					data.format= format;
					this.fireDataEvent("changeValue", data, null);
				}, this);
			}, this);
		},
		_createFieldsEditor: function (format) {
			switch (format) {
			case ms123.datamapper.Config.FORMAT_CSV:
				return new ms123.datamapper.create.CSVFieldsEditor(this._facade,this._side);
			case ms123.datamapper.Config.FORMAT_XML:
				return new ms123.datamapper.create.XMLFieldsEditor(this._facade,this._side);
			case ms123.datamapper.Config.FORMAT_JSON:
				return new ms123.datamapper.create.JSONFieldsEditor(this._facade,this._side);
			case ms123.datamapper.Config.FORMAT_MAP:
				return new ms123.datamapper.create.MapFieldsEditor(this._facade,this._side,false);
			case ms123.datamapper.Config.FORMAT_EXCEL:
				return new ms123.datamapper.create.ExcelFieldsEditor(this._facade,this._side);
			case ms123.datamapper.Config.FORMAT_FW:
				return new ms123.datamapper.create.FWFieldsEditor(this._facade,this._side);
			case ms123.datamapper.Config.FORMAT_POJO:
				if( this._currentKind==ms123.datamapper.Config.KIND_FROM_SAMPLE){
					return new ms123.datamapper.create.PojoFieldsEditor(this._facade,this._side);
				}else{
					return new ms123.datamapper.create.MapFieldsEditor(this._facade,this._side,true);
				}	
			}
		},
		_createStatusDisplay: function () {
			var container = new qx.ui.container.Composite(new qx.ui.layout.Dock());
			var statusLabel = new qx.ui.basic.Label(this.tr("datamapper.status"));
			this._statusField  = new qx.ui.form.TextField();
			this._statusField.setReadOnly(true);
			this._statusField.setBackgroundColor("gray");
			//container.add(statusLabel,{edge:"west"});
			container.add(this._statusField,{edge:"center"});
			return container;
		},
		_setStatus:function(){
			if( this._formatOnly !== false){
				return;
			}
			if( this._cache[this._getCacheKey()] || this._currentKind == ms123.datamapper.Config.KIND_LIKE_INPUT){
				this._statusField.setValue(this.tr("datamapper.status_ready"));
				this._statusField.setBackgroundColor(ms123.datamapper.Config.BG_COLOR_READY);
			}else{
				this._statusField.setValue(this.tr("datamapper.status_notready"));
				this._statusField.setBackgroundColor(ms123.datamapper.Config.BG_COLOR_NOTREADY);
			}
		},
		_createUploadContainer: function () {
			var container = new qx.ui.container.Composite(new qx.ui.layout.Dock(10));

			var u = ms123.util.Remote._username;
			var p = ms123.util.Remote._password;
			var credentials = ms123.util.Base64.encode(u + ":" + p);

			var id = ms123.util.IdGen.id();
			this.__id = id;
			var url = "/rpc/xyz/" + id + "?credentials=" + credentials;
			var uploadForm = new ms123.upload.UploadForm('uploadFrm', url);
			uploadForm.setParameter('credentials', credentials);
			uploadForm.setPadding(8);

			this._uploadForm = uploadForm;

			this._setRpcHeader(this._uploadForm, id);
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

			uploadForm.addListener('completed', function (e) {
				var f = uploadField.getFileName();
				uploadField.setFileName('');
				bt.setEnabled(true);
				if( this._use == ms123.datamapper.Config.USE_IMPORT){
					var ret =qx.lang.Json.parse( uploadForm.getIframeTextContent()); 
					ret.result.fileId = "internal";
					this._cache[this._getCacheKey()] = ret.result;
					this.fireDataEvent("changeValue", ret.result, null);
				}else{
					this._getMetaData(id);
				}
			},this);

			bt.addListener('execute', function (e) {
				var f = uploadField.getFileName();
				if (f == null || f == '') {
					ms123.form.Dialog.alert(this.tr("import.no_filename"));
					return;
				}
				uploadForm.send();
				bt.setEnabled(false);
			},this);
			this._uploadContainer = container;
			container.add(this._createDropArea(),{edge:"center"});
			return container;
		},
		html5dropfiles: function (files) {
			for (var i = 0, f; f = files[i]; i++) {//here only one file allowed
				var filereader = new FileReader();
				filereader.file = f;
				var self = this;
				filereader.onloadend = (function (evt) {
					if( evt.total==0 ){
						ms123.form.Dialog.alert(self.tr("data.document.cannot_read")+":"+this.file.name);
						return;
					}
					var f = this.file.name;
					var id = self.__id;
					var rpc = self._getRpcHeader(id);
					var params = rpc.params;
					params["fileContent"] = this.result;

					self.fireDataEvent("ready", {id:id,filename:f}, null);
					var data = self._saveFile(rpc);
					if( self._use == ms123.datamapper.Config.USE_IMPORT){
						data.fileId = "internal";
						self.fireDataEvent("changeValue", data, null);
					}else{
						self._getMetaData(id);
					}
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
				element.ondrop = ms123.datamapper.edit.UploadWindow.html5drop.bind(this);
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
		_saveFile: function (rpc) {
			try {
				var data = ms123.util.Remote.rpcSync(rpc.service+":"+rpc.method, rpc.params);
				return data;
			} catch (e) {
				ms123.form.Dialog.alert("UploadWindow._saveFile:" + e);
				return null;
			}
		},
		_getCacheKey:function(){
			return this._currentFormat+"_"+this._currentKind;
		},
		_getConfig:function(){
			var format = this._formatSelectBox.getSelection()[0].getModel();
			var config = {};
			if( format == ms123.datamapper.Config.FORMAT_CSV){
				config = this._csvForm.getData();
			}
			config.format = format;
			config.side = this._side;
			return config;
		},
		_getMetaData:function(id){	
			try{
				var config = this._getConfig();
				var data =  ms123.util.Remote.rpcSync( "datamapper:getMetaData", {fileId:id, config:config });
				var format = this._formatSelectBox.getSelection()[0].getModel();
				data.format= format;
				data.fileId= id;
				this.fireDataEvent("changeValue", data, null);
			}catch(e){
				ms123.form.Dialog.alert("getMetaData:"+e);
			}
		},
		_getRpcHeader: function (id) {
			if( this._use == ms123.datamapper.Config.USE_IMPORT){
				return  {
					"service": "importing",
					"method": "dmUpload",
					"params": {
						"storeId": this._facade.storeDesc.getStoreId(),
						"dmConfig": this._getConfig(),
						"importingid": this._facade.importingid
					}
				}
			}else{
				return  {
					"service": "datamapper",
					"method": "upload",
					"id": 31,
					"params": {
						"fileId":id
					}
				};
			}
		},
		_setRpcHeader: function (form, id) {
			if( !form) return;
			var rpc = this._getRpcHeader(id);
			var rpcString = qx.util.Serializer.toJson(rpc);
			form.setParameter("__rpc__", rpcString);
		}
	}
});
