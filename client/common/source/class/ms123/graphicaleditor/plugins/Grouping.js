/**
 * Copyright (c) 2006
 * Martin Czuchra, Nicolas Peters, Daniel Polak, Willi Tscheschner
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 **/

/**
	* @ignore(Hash)
	* @ignore(Clazz.extend)
	* @ignore($)
*/
qx.Class.define("ms123.graphicaleditor.plugins.Grouping", {
	extend: qx.core.Object,

	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function(facade) {
		this.base(arguments,facade);
		this.facade = facade;

		this.facade.offer({
			'name':ms123.oryx.Translation.Grouping.group,
			'functionality': this.createGroup.bind(this),
			'group': ms123.oryx.Translation.Grouping.grouping,
			'icon': this.__getResourceUrl("shape_group.png"),
			'description': ms123.oryx.Translation.Grouping.groupDesc,
			'index': 1,
			'minShape': 2,
			'isEnabled': this.isEnabled.bind(this, false)});

		this.facade.offer({
			'name':ms123.oryx.Translation.Grouping.ungroup,
			'functionality': this.deleteGroup.bind(this),
			'group': ms123.oryx.Translation.Grouping.grouping,
			'icon': + this.__getResourceUrl("shape_ungroup.png"),
			'description': ms123.oryx.Translation.Grouping.ungroupDesc,
			'index': 2,
			'minShape': 2,
			'isEnabled': this.isEnabled.bind(this, true)});
			
		this.selectedElements = [];
		this.groups = [];
	},

	/******************************************************************************
	 STATICS
	 ******************************************************************************/
	statics: {

	},

	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},

	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {

	isEnabled: function(handles) {
		
		var selectedEl = this.selectedElements;

		return	handles === this.groups.any(function(group) {
					return 		group.length === selectedEl.length &&
								group.all(function(grEl) { return selectedEl.member(grEl)})
								});
	},

	onSelectionChanged: function(event) {

		// Get the new selection
		var newSelection = event.elements;
		
		// Find all groups with these selection
		this.selectedElements = this.groups.findAll(function(group) {
				return group.any(function(grEl) { return newSelection.member(grEl)})
		});
		
		// Add the selection to them
		this.selectedElements.push(newSelection)
		
		// Do all in one level and unique
		this.selectedElements = this.selectedElements.flatten().uniq();
		
		// If there are more element, set new selection in the editor
		if(this.selectedElements.length !== newSelection.length) {
			this.facade.setSelection(this.selectedElements);
		}
	},
	
	createGroup: function() {
	
		var selectedElements = this.facade.getSelection();
		
		var commandClass = Clazz.extend({
			construct: function(selectedElements, groups, setGroupsCB, facade){
				this.selectedElements = selectedElements;
				this.groups = groups;
				this.callback = setGroupsCB;
				this.facade = facade;
			},			
			execute: function(){
				var g = this.groups.findAll(function(group) {
					return !group.any(function(grEl) { return selectedElements.member(grEl)})
				});
				
				g.push(selectedElements);

				this.callback(g.clone());
				
				this.facade.setSelection(this.selectedElements);
			},
			rollback: function(){
				this.callback(this.groups.clone());
				
				this.facade.setSelection(this.selectedElements);
			}
		})
		
		var command = new commandClass(selectedElements, this.groups.clone(), this.setGroups.bind(this), this.facade);
		
		this.facade.executeCommands([command]);
	},
	
	deleteGroup: function() {
		
		var selectedElements = this.facade.getSelection();
		
		var commandClass = Clazz.extend({
			construct: function(selectedElements, groups, setGroupsCB, facade){
				this.selectedElements = selectedElements;
				this.groups = groups;
				this.callback = setGroupsCB;
				this.facade = facade;
			},			
			execute: function(){
				// Delete all groups where all these elements are member and where the elements length the same
				var groupPartition = this.groups.partition(function(group) {
						return 		group.length !== selectedElements.length ||
									!group.all(function(grEl) { return selectedElements.member(grEl)})
					});

				this.callback(groupPartition[0]);
				
				this.facade.setSelection(this.selectedElements);
			},
			rollback: function(){
				this.callback(this.groups.clone());
				
				this.facade.setSelection(this.selectedElements);
			}
		})
		
		var command = new commandClass(selectedElements, this.groups.clone(), this.setGroups.bind(this), this.facade);
		
		this.facade.executeCommands([command]);	
	},
	
	setGroups: function(groups) {
		this.groups = groups;
	},
		__getResourceUrl: function (name) {
			var am = qx.util.AliasManager.getInstance();
			return am.resolve("resource/ms123/" + name);
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
