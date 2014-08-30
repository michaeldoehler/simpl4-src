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
 * @ignore(FileReader) 
 */
qx.Class.define("ms123.form.UploadField", {
	extend: ms123.form.AbstractField,
	implement: [ms123.form.IConfig],
	include: [qx.locale.MTranslation],

	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */

	construct: function (useitCheckboxes) {
		this.base(arguments, useitCheckboxes);
		this.addListener('appear', function () {
			if(!ms123.config.ConfigManager.hasDocumentDD()){
				return;
			}
			var element = this.getContentElement().getDomElement();
			element.ondrop = ms123.form.UploadField.html5drop.bind(this);
			element.ondragover = function () {
				return false;
			}
			element.ondragover = function () {
				return false;
			}
			
		}, this);
		if( !this._bgColor){
			this._bgColor = this._uploadForm.getBackgroundColor();
		}

	},

	/**
	 *****************************************************************************
	 PROPERTIES
	 *****************************************************************************
	 */

	properties: {
		placeholder: {
			check: "String",
			nullable: true,
			apply: "_applyPlaceholder"
		},
		// overridden
		focusable: {
			refine: true,
			init: true
		}
	},
	/******************************************************************************
	 STATICS
	 ******************************************************************************/
	statics: {
		html5drop: function (e) {
			console.error("html5drop:"+this);
			this.html5dropfiles(e.dataTransfer.files);

			e.stopPropagation();
			e.preventDefault();
		}
	},

	/**
	 *****************************************************************************
	 MEMBERS
	 *****************************************************************************
	 */

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
					self.setFileNameDrag ( this.file.name);
					self.setContentDrag ( this.result );
					self._uploadField.setEnabled(false);
					self._uploadForm.setBackgroundColor("#cfcfcf");
					self._setText(this.file.name);
				});
				filereader.readAsDataURL(f);
			}
		},
		_setEnabled:function(){
			this._uploadField.setEnabled(true);
			this._uploadField.setBackgroundColor(this._bgColor);
			this._uploadField.getButton().setEnabled(true);
			this._uploadField.getTextField().setValue(null);
		},
		beforeAdd: function (context) {
			this.__isEdit = false;
			if( !this.getContentDrag()){
				this._filename = null;
			}
			this.getChildControl("textfield").setSelection([this._upload]);
			this._setEnabled();
		},
		beforeEdit: function (context) {
			this.__isEdit = true;
			this.getChildControl("textfield").setSelection([this._download]);
			this._filename = context.data["filename"];
			this._id = context.data["id"];
			this._storeId = context.storeDesc.getStoreId();
			this._buttonDownload.setLabel(this.tr("upload.download_file") + ":" + this._filename);
			this._setEnabled();
		},
		afterSave: function (context) {
			if(this.getContentDrag()) return;
			if (context.method == "update") return;
			var f = this._uploadField.getFileName();
			if (f == null || f == '') {
				ms123.form.Dialog.alert(this.tr("data.document.no_filename"));
				return;
			}
			console.log("afterSave:" + context.id);
			this._setUploadUrl("/rpc/xyz", context.id);
			var name = this.getUserData("key");
			console.log("context:" + qx.util.Serializer.toJson(context));
			console.log("key:" + key);
			this._uploadField.setFieldName(name);
			for (var p in context.map) {
				var key = p;
				var val = context.map[p];
				console.log("param:" + key + "=" + val);
				this._uploadForm.setParameter(key, val);
			}
			this.setRpcHeader(this._uploadForm, context);
			qx.event.Timer.once(function () {
				this._uploadForm.send();
				this._uploadField.setFileName("");
			}, this, 50);
		},
		beforeSave: function (context) {
			if( this.getContentDrag()){
				context.data.content=this.getContentDrag();
				context.data.filename=this.getFileNameDrag();
			}
		},
		setRpcHeader: function (form, context) {

			var rpc = {
				"service": context.service,
				"method": "upload",
				"id": 31,
				"params": {
					"entity": context.rpcParams.entity,
					"id": context.id + "",
					"storeId": context.rpcParams.storeId
				}
			};
			var rpcString = qx.util.Serializer.toJson(rpc);
			form.setParameter("__rpc__", rpcString);

		},
		_setText: function (text) {
			this._uploadField.getTextField().setValue(text);
			this._uploadField.getButton().setEnabled(false);
		},
		setReadOnly: function (flag) {
			this._uploadField.getTextField().setReadOnly(flag);
			this._uploadField.getTextField().setBackgroundColor("#cfcfcf");
			this._uploadField.getButton().setReadOnly(flag);
		},

		setValue: function (value) {
			console.error("setValue:" + value);
		},

		getValue: function () {
			if (this.__isEdit && this._filename) {
				return this._filename;
			}
			var f = this._uploadField.getFileName();
			console.error("getValue:" + f);
			f = this._baseName(f);
			console.error("getValue1:" + f);
			return this.getFileNameDrag() ? this.getFileNameDrag() : f;
		},

		setFileNameDrag:function(c){
			this._fileNameDrag = c;
		},
		getFileNameDrag:function(){
			return this._fileNameDrag;
		},

		setContentDrag:function(c){
			this._contentDrag = c;
		},
		getContentDrag:function(){
			return this._contentDrag;
		},

		_baseName: function (f) {
			if (f) {
				var slash = f.lastIndexOf("\\");
				if (slash != -1) {
					f = f.substring(slash + 1);
				} else {
					var slash = f.lastIndexOf("/");
					if (slash != -1) {
						f = f.substring(slash + 1);
					}
				}
			}
			return f;
		},
		_setUploadUrl: function (xurl, id) {
			var u = ms123.util.Remote._username;
			var p = ms123.util.Remote._password;
			var credentials = ms123.util.Base64.encode(u + ":" + p);
			var url = xurl + "/" + id + "?credentials=" + credentials + "&method=put";
			console.log("_setUploadUrl.u:" + url);
			this._uploadForm.setUrl(url);
		},
		_onChangeFileName: function (e) {
			var value = e.getData();
			console.log("_onChangeFileName:" + value);
			this.fireDataEvent("changeValue", this._baseName(value), e.getOldData());
		},

		/**
		 ---------------------------------------------------------------------------
		 WIDGET API
		 ---------------------------------------------------------------------------
		 */

		// overridden
		_createChildControlImpl: function (id, hash) {
			var control;
			switch (id) {
			case "textfield":
				var control = new qx.ui.container.Stack();
				this._download = this._createDownloadForm();
				this._upload = this._createUploadForm();
				control.add(this._download);
				control.add(this._upload);
				this._add(control, {
					flex: 1
				});
				break;
			}
			return control || this.base(arguments, id);
		},
		_createHiddenForm: function () {
			var htmlForm = '<form id="downloadFormPOST" action="" method="post" accept-charset="UTF-8" target="_blank">' +
										 '<input type="hidden" name="credentials" value="">' + 
										 '<input type="hidden" name="__rpc__" value="">' + '</form>';
			var hiddenForm = new qx.ui.embed.Html(htmlForm);
			hiddenForm.setHeight(0);
			hiddenForm.setWidth(0);
			return hiddenForm;
		},
		_createDownloadButton: function () {
			var buttonDownload = new qx.ui.form.Button("", "icon/16/actions/document-revert.png");
			buttonDownload.setHeight(20);
			buttonDownload.addListener("execute", function () {
				var u = ms123.util.Remote._username;
				var p = ms123.util.Remote._password;
				var credentials = ms123.util.Base64.encode(u + ":" + p);
				var downloadForm = window.document.getElementById("downloadFormPOST");
				this._downloadFile(downloadForm, credentials, this._storeId, this._id);
			}, this);
			this._buttonDownload = buttonDownload;
			return buttonDownload;
		},
		_createDownloadForm: function () {
			var formComposite = new qx.ui.container.Composite();
			formComposite.setLayout(new qx.ui.layout.VBox());
			formComposite.add(this._createHiddenForm());
			formComposite.add(this._createDownloadButton());
			return formComposite;
		},
		_downloadFile: function (downloadForm, credentials, storeId, id) {
			var rpc = {
				"service": "data",
				"method": "queryOne",
				"id": 31,
				"params": {
					storeId: storeId,
					entity: "document",
					id: id,
					getContent: true
				}
			};
			downloadForm.action = "/rpc/__rpcForm__?credentials=" + credentials;
			var rpcString = qx.util.Serializer.toJson(rpc);
			downloadForm["__rpc__"].value = rpcString;
			downloadForm.submit();
		},
		_createUploadForm: function () {
			var uploadForm = new ms123.upload.UploadForm('uploadFrm');
			uploadForm.setPadding(8);

			var vb = new qx.ui.layout.VBox(10)
			uploadForm.setLayout(vb);
			this._uploadForm = uploadForm;

			var l = new qx.ui.basic.Label(this.tr("import.select_file"));
			l.setRich(true);
			uploadForm.add(l);


			var uploadField = new ms123.upload.UploadField(null, this.tr("import.select_button"), 'icon/16/actions/document-save.png');
			uploadField.setFileName("");
			uploadForm.add(uploadField);
			this._uploadField = uploadField;
			uploadField.addListener("changeFileName", this._onChangeFileName, this);

			var _this = this;
			uploadForm.addListener('completed', function (e) {
				ms123.form.Dialog.alert(_this.tr("import.uploading_complete"));
			});

			/*var resetButton = new qx.ui.form.Button(this.tr("import.reset_button"), "icon/16/actions/document-revert.png");
			resetButton.set({
				width: 120,
				allowGrowX: true
			});
			resetButton.addListener("execute", function (e) {
				uploadField.setFileName("");
			}, this);
			uploadForm.add(resetButton);*/
			return uploadForm;
		}
	}
});
