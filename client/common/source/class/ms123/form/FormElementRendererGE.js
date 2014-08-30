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
 * Form renderer renderer for {@link qx.ui.form.Form}. 
 */
qx.Class.define("ms123.form.FormElementRendererGE", {
	extend: qx.ui.container.Composite,
	construct: function (formElement, elementDesc) {
		this.base(arguments);
		this._formElement = formElement;
		this._childs = elementDesc.childShapes;
		this.setLayout(new qx.ui.layout.Dock(3, 3));
		var width = elementDesc.bounds.lowerRight.x - elementDesc.bounds.upperLeft.x;
		var height = elementDesc.bounds.lowerRight.y - elementDesc.bounds.upperLeft.y;
		console.log("height:" + height + "/w:" + width + "/" + formElement);
		//	this._formElement.setMinWidth(width);
		this._formElement.setWidth(width);
		this._formElement.setMinHeight(height);

		var stencilId = elementDesc.stencil.id.toLowerCase();
		if (stencilId == "textarea") {
			this._formElement._setHeight(height);
		}
		if (stencilId == "input" || stencilId == "moduleselector") {
			this._formElement.setAllowGrowY(false);
		}
		if (stencilId == "tableselect") {
			this._formElement.setHeight(height);
			this._formElement.setAllowGrowY(true);
		}
		if (stencilId == "alert") {
			this._formElement.setHeight(height);
			this._formElement.setAllowGrowY(true);
		}

		this._formElement.setAllowGrowX(true);
		this.add(this._formElement, {
			edge: "center"
		});
		this._createWidget();
	},

	members: {
		_createWidget: function () {
			for (var i = 0; i < this._childs.length; i++) {
				var child = this._childs[i];
				var stencilId = child.stencil.id.toLowerCase();
				console.log("_createInputWidget:" + stencilId);
				var properties = child.properties;
				switch (stencilId) {
				case "label":
					var text = properties.xf_text;
					if( text.match(/^@/)){
						text = this.tr(text.substring(1));
					}				
					var label = new qx.ui.basic.Label(text);
					this.add(label, {
						edge: "north"
					});
					break;
				}
			}
		}
	}
});
