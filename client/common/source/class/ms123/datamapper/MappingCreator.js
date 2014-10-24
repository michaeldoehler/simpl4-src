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
qx.Class.define('ms123.datamapper.MappingCreator', {
	extend: qx.ui.container.Composite,

	construct: function (facade) {
		this.base(arguments);
		this._facade = facade;

		this.setLayout(new qx.ui.layout.Dock());

		var container = new qx.ui.container.Composite(new qx.ui.layout.HBox(2));
		var input = this._createGroupBox(this.tr("datamapper.input"),ms123.datamapper.Config.INPUT);
		var output = this._createGroupBox(this.tr("datamapper.output"),ms123.datamapper.Config.OUTPUT);
		if( this._facade.mainEntity){
			console.error("Step1");
			output.formatSelector.setEnabled(false);
			var pf = new ms123.datamapper.create.PojoFieldsEditor(this._facade,ms123.datamapper.Config.OUTPUT);
			var data = pf.createModel(this._facade.mainEntity);
			data.format= ms123.datamapper.Config.FORMAT_POJO;
			this._outputValue=data;
			this._outputValue.type = ms123.datamapper.Config.NODETYPE_COLLECTION;
		}

		container.add(input.groupBox, {
			width: "50%"
		});
		container.add(output.groupBox, {
			width: "50%"
		});
		this.add(container, {
			edge: "center"
		});
		input.formatSelector.addListener("changeValue", function (ev) {
			this._inputValue = ev.getData();
			if (this._inputValue && this._outputValue) {
				this._createButton.setEnabled(true);
				this._createButton.setBackgroundColor(ms123.datamapper.Config.BG_COLOR_READY);
			}else{
				this._createButton.setEnabled(false);
				this._createButton.setBackgroundColor(ms123.datamapper.Config.BG_COLOR_NOTREADY);
			}
			console.log("MappingCreator.input:"+JSON.stringify(this._inputValue,null,2));
		}, this);
		output.formatSelector.addListener("changeValue", function (ev) {
			this._outputValue = ev.getData();
			if (this._inputValue && this._outputValue) {
				this._createButton.setEnabled(true);
				this._createButton.setBackgroundColor(ms123.datamapper.Config.BG_COLOR_READY);
			}else{
				this._createButton.setEnabled(false);
				this._createButton.setBackgroundColor(ms123.datamapper.Config.BG_COLOR_NOTREADY);
			}
			console.log("MappingCreator.output:",this._outputValue);
		}, this);

		this._createButton = new qx.ui.form.Button(this.tr("datamapper.create_mapping"), "icon/16/actions/dialog-ok.png");
		this._createButton.setDecorator(null);
		this._createButton.addListener("execute", function () {
			if( this._outputValue && this._outputValue.kind == ms123.datamapper.Config.KIND_LIKE_INPUT){
				var format = this._outputValue.format;
				ms123.util.Clone.merge(this._outputValue, this._inputValue);
				this._outputValue.format = format;
				if( this._inputValue.format == ms123.datamapper.Config.FORMAT_CSV ||
						this._outputValue.format == ms123.datamapper.Config.FORMAT_POJO){
					this._outputValue.type = ms123.datamapper.Config.NODETYPE_COLLECTION;
				}
			}
			var data = {
				input: this._inputValue,
				output: this._outputValue
			};
			ms123.datamapper.BaseTree.prepareTreeData(data.input,"");
			ms123.datamapper.BaseTree.prepareTreeData(data.output,"");
			if( this._inputValue.fileId){
				data.fileId = this._inputValue.fileId;
				delete this._inputValue.fileId;
			}
			console.log("MappingCreator.ready:"+JSON.stringify(data,null,2));
			this.fireDataEvent("ready", data, null);
		}, this);
		this._createButton.setEnabled(false);

		this.add(this._createButton, {
			edge: "south"
		});
	},

	events: {
		"ready": "qx.event.type.Data"
	},
	properties: {},

	members: {
		_createGroupBox: function (legend,side) {
			var gb = new qx.ui.groupbox.GroupBox(legend);
			this.add(gb, {
				edge: "center"
			});
			gb.setLayout(new qx.ui.layout.Dock());

			var fs = new ms123.datamapper.create.FormatSelector(this._facade,side,false);
			gb.add(fs, {
				edge: "center"
			});
			return {
				groupBox: gb,
				formatSelector: fs
			};
		}
	}
});
