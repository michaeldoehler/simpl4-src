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
qx.Class.define("ms123.graphicaleditor.Split3", {
	extend: qx.ui.container.Composite,

	construct: function (shapeRepository, editor, propertyPanel,container,direction,place) {
		this.base(arguments);

		this.setLayout(new qx.ui.layout.HBox());
		container.dialogPanel=propertyPanel;

		var mainSplitPane = new qx.ui.splitpane.Pane("horizontal").set({
			decorator: null
		});
		var rightSplitPane = editor;
		var leftSplitPane = shapeRepository;
		if( ["right", "center_bottom"].indexOf(place) != -1){
			rightSplitPane = new qx.ui.splitpane.Pane(direction).set({
				decorator: null
			});
		}else{
			leftSplitPane = new qx.ui.splitpane.Pane("vertical").set({
				decorator: null
			});
			if( ["right_top"].indexOf(place) != -1){
				leftSplitPane.add(propertyPanel, 8);
				leftSplitPane.add(shapeRepository, 5);
			}else{
				leftSplitPane.add(shapeRepository, 5);
				leftSplitPane.add(propertyPanel, 8);
			}
			container.dialogPanel=leftSplitPane;
		}

		if( ["right_bottom", "right_top"].indexOf(place) != -1){
			mainSplitPane.add(rightSplitPane, 10);
			mainSplitPane.add(leftSplitPane, place=="right" ? 2 : 4);
		}else{
			mainSplitPane.add(leftSplitPane, place=="right" ? 2 : 4);
			mainSplitPane.add(rightSplitPane, 10);
		}

		if( ["right", "center_bottom"].indexOf(place) != -1){
			rightSplitPane.add(editor, 6);
			rightSplitPane.add(propertyPanel, place=="right"? 3 : 2);
		}

		this.add(mainSplitPane, {
			flex: 1
		});
	}
});
