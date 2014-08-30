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
*/

qx.Class.define("ms123.datamapper.edit.UploadWindow", {
	extend: qx.core.Object,
	implement: [qx.ui.form.IStringForm, qx.ui.form.IForm],
	include: [qx.ui.form.MForm, qx.locale.MTranslation],
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (facade, config, title) {
		this.base(arguments);
		this._title = title;
		this._config = config;
		if (config) {
			this._format = config.format;
		} else {
			this._format = facade.format;
		}

		this._window = this._createWindow(this._title);

		this._createUploadContainer();
		this._window.add(this._uploadContainer, {
			edge: "center"
		});
		this._window.open();
	},

	/******************************************************************************
	 EVENTS
	 ******************************************************************************/
	events: {
		"changeValue": "qx.event.type.Data",
		"changeEnabled": "qx.event.type.Data",
		"ready": "qx.event.type.Data"
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
					var f = this.file.name;
					var id = self._config.id;
					var rpc = self._getRpcHeader(id);
					var params = rpc.params;
					params["fileContent"] = this.result;

					self.fireDataEvent("ready", {id:id,filename:f}, null);
					self._saveFile(rpc.params);
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
		_saveFile: function (params) {
			try {
				ms123.util.Remote.rpcSync("datamapper:upload", params);
			} catch (e) {
				ms123.form.Dialog.alert("UploadWindow._saveFile:" + e);
				return null;
			}
		},
		_createUploadContainer: function () {
			var container = new qx.ui.container.Composite(new qx.ui.layout.Dock(10));

			var u = ms123.util.Remote._username;
			var p = ms123.util.Remote._password;
			var credentials = ms123.util.Base64.encode(u + ":" + p);

			var id = this._config.id;
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
				this._window.destroy();
				this.fireDataEvent("ready", {id:id,filename:f}, null);
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
		_getRpcHeader: function (id) {
			return {
				"service": "datamapper",
				"method": "upload",
				"id": 31,
				"params": {
					"fileId":id
				}
			};
		},
		_setRpcHeader: function (form, id) {
			var rpcString = qx.util.Serializer.toJson(this._getRpcHeader(id));
			form.setParameter("__rpc__", rpcString);
		},
		setEnabled: function () {},
		getEnabled: function () {return true;},
		resetValue: function () {},
		getValue: function () { return null; },
		setValue: function (value) { },
		_createWindow: function (name) {
			var win = new qx.ui.window.Window(name, "").set({
				resizable: true,
				useMoveFrame: true,
				useResizeFrame: true
			});
			win.setLayout(new qx.ui.layout.Dock(2,2));
			win.setWidth(450);
			win.setHeight(400);
			win.setAllowMaximize(false);
			win.setAllowMinimize(false);
			win.setModal(true);
			win.setActive(false);
			win.minimize();
			win.center();
			return win;
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
