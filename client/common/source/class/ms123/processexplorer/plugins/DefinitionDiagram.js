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
	* @ignore(Clazz)
*/
qx.Class.define("ms123.processexplorer.plugins.DefinitionDiagram", {
	extend: qx.ui.tabview.Page,
	include: [qx.locale.MTranslation],

	/**
	 * Constructor
	 */
	construct: function (facade) {
		this.base(arguments,this.tr("processexplorer.definition_diagramm"),"icon/16/actions/format-justify-fill.png");
		this.facade = facade;
		this.setLayout(new qx.ui.layout.HBox());

		this.scroll = new qx.ui.container.Scroll();
		this.add(this.scroll,{flex:1});


		this.facade.registerOnEvent(ms123.processexplorer.Config.EVENT_PROCESSDEFINITION_CHANGED, this._handleEvent.bind(this));
		this.facade.registerOnEvent(ms123.processexplorer.Config.EVENT_PROCESSDEPLOYMENT_CHANGED, this._handleEventDeplomentChanged.bind(this));
		this.setEnabled(false);
	},

	/**
	 * ****************************************************************************
	 * MEMBERS
	 * ****************************************************************************
	 */
	members: {
		_showDiagram:function(){
			var source = ms123.util.Remote.rpcSync("activiti:getDefinitionDiagram", {
					processDefinitionId:this._processDefinition.id
				});
			var image = new qx.ui.basic.Image(source);
			this.scroll.add(image);
		},
		_handleEventDeplomentChanged: function (e) {
			this.setEnabled(false);
		},
		_handleEvent: function (e) {
			this._processDefinition = e.processDefinition;
			this._showDiagram();
			this.setEnabled(true);
		}
	}
});
