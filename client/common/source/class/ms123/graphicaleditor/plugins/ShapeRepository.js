/**
 * Copyright (c) 2009
 * Willi Tscheschner
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
	* @ignore(Clazz)
*/
qx.Class.define("ms123.graphicaleditor.plugins.ShapeRepository", {
	extend: qx.ui.container.Composite,
	include: [qx.locale.MTranslation],

	/**
	 * Constructor
	 */
	construct: function (facade,editorType) {
		this.base(arguments);
		this.facade = facade;
		this.editorType = editorType;
		this.setLayout(new qx.ui.layout.Grow());
		this.__createPanelSpace();
		this.__setStencilSets(facade);
		this.facade.getWidget().setDroppable(true);
	},

	/**
	 * ****************************************************************************
	 * MEMBERS
	 * ****************************************************************************
	 */
	members: {
		__getStencilIconUrl: function (name) {
			if( name.match(/^data:image/)) return name;
			var am = qx.util.AliasManager.getInstance(name);
			return am.resolve("resource/ms123/stencilsets/"+this.editorType+"/" + name);
		},
		__createPanelSpace: function () {
			var panels = [];
			var buttons = [];

			var scroll = new qx.ui.container.Scroll();
			this.add(scroll);

			var panelSpace = new qx.ui.container.Composite(new qx.ui.layout.VBox()).set({
				allowShrinkY: false,
				allowGrowX: true
			});

			panelSpace.setPadding(0);
			scroll.add(panelSpace);
			this.__panelSpace = panelSpace;
		},
		/**
		 * Load all stencilsets in the shaperepository
		 */
		__setStencilSets: function (facade) {
			var first = true;
			facade.getStencilSets().values().each((function (sset) {

				// For each Stencilset create and add a new Tree-Node
				var stencilSetNode;

				var typeTitle = sset.title();
				var extensions = sset.extensions();
				if (extensions && extensions.size() > 0) {
					typeTitle += " / " + ms123.oryx.core.StencilSet.getTranslation(extensions.values()[0], "title");
				}

				var stencilSetPanel = new ms123.widgets.CollapsablePanel(typeTitle, new qx.ui.layout.VBox());
				this.__panelSpace.add(stencilSetPanel);


				// Get Stencils from Stencilset
				var stencils = sset.stencils(facade.getCanvas().getStencil(), facade.getRules());
				var panelGroups = new Hash();

				// Sort the stencils according to their position and add them to the repository
				stencils = stencils.sortBy(function (value) {
					return value.position();
				});
				stencils.each((function (value) {

					// Show stencils in no group if there is less than 10 shapes
					if (stencils.length <= ms123.oryx.Config.MAX_NUM_SHAPES_NO_GROUP) {
						this.__createStencilEntry(stencilSetPanel, value);
						return;
					}

					// Get the groups name
					var groups = value.groups();

					// For each Group-Entree
					groups.each((function (group) {

						// If there is a new group
						if (!panelGroups[group]) {
							// Create a new group
							panelGroups[group] = new ms123.widgets.CollapsablePanel(group, new qx.ui.layout.VBox());
							panelGroups[group].setValue(first ? true : false);
							first = false;

							// Add the Group to the ShapeRepository
							stencilSetPanel.add(panelGroups[group]);
						}

						// Create the Stencil-Tree-Node
						this.__createStencilEntry(panelGroups[group], value);

					}).bind(this));


					// If there is no group
					if (groups.length == 0) {
						// Create the Stencil-Entry
						this.__createStencilEntry(stencilSetPanel, value);
					}

				}).bind(this));
			}).bind(this));
		},

		__createStencilEntry: function (parentPanel, stencil) {
			var b = new ms123.graphicaleditor.DraggableButton(stencil.title(), this.__getStencilIconUrl(stencil.icon()));
			b.setUserData("stencil", stencil);
			b.setDraggable(true);
			b.addListener("drag", this.__dragover, this);

			b.addListener("dragstart", function (e) {
				e.addAction("move");
				console.log("DRAG:dragstart:" + b);
				this.facade.getWidget().addListener("drop", this.__drop, this);
			},this);

			b.addListener("dragend", function (e) {
				console.log("DRAG:dragend:" + b);
				this.facade.getWidget().removeListener("drop", this.__drop, this);
				b.releaseCapture();
			},this);

			b.addListener("execute", function (e) {
				console.log("execute:" + b);
			});
			b.setPaddingTop(2);
			b.setPaddingBottom(2);
			b.setMargin(1);
			parentPanel.add(b);
		},

		__drop: function (e) {
			this._lastOverElement = undefined;

			// Hide the highlighting
			this.facade.raiseEvent({
				type: ms123.oryx.Config.EVENT_HIGHLIGHT_HIDE,
				highlightId: 'shapeRepo.added'
			});
			this.facade.raiseEvent({
				type: ms123.oryx.Config.EVENT_HIGHLIGHT_HIDE,
				highlightId: 'shapeRepo.attached'
			});

			// Check if there is a current Parent
			if (!this._currentParent) {
				console.log("NO current parent");
				return
			}

			var stencil = e.getRelatedTarget().getUserData("stencil");
			console.log("stencil:" + stencil);
			var option = {}; //Ext.dd.Registry.getHandle(target.DDM.currentTarget);
			option.type = stencil.id();
			option.namespace = stencil.namespace();

			var pos = {
				x: e.getDocumentLeft(),
				y: e.getDocumentTop()
			};

			var a = this.facade.getCanvas().node.getScreenCTM();
			console.log("a:" + a + "/" + option.namespace + "/" + option.type);

			// Correcting the UpperLeft-Offset
			pos.x -= a.e;
			pos.y -= a.f;
			// Correcting the Zoom-Faktor
			pos.x /= a.a;
			pos.y /= a.d;
			// Correting the ScrollOffset
			pos.x -= document.documentElement.scrollLeft;
			pos.y -= document.documentElement.scrollTop;
			// Correct position of parent
			var parentAbs = this._currentParent.absoluteXY();
			pos.x -= parentAbs.x;
			pos.y -= parentAbs.y;

			// Set position
			option['position'] = pos

			// Set parent
			if (this._canAttach && this._currentParent instanceof ms123.oryx.core.Node) {
				option['parent'] = undefined;
			} else {
				option['parent'] = this._currentParent;
			}


			var commandClass = Clazz.extend({
				construct: function (option, currentParent, canAttach, position, facade) {
					console.log("Vlazz:" + this + "/" + option);
					this.option = option;
					this.currentParent = currentParent;
					this.canAttach = canAttach;
					this.position = position;
					this.facade = facade;
					this.selection = this.facade.getSelection();
					this.shape;
					this.parent;
				},
				execute: function () {
					console.log("execute:" + this.option + "/" + option);
					if (!this.shape) {
						this.shape = this.facade.createShape(option);
						this.parent = this.shape.parent;
					} else {
						this.parent.add(this.shape);
					}
					console.log("execute:" + this.shape + "/" + this._canAttach + "/this:" + this);

					if (this.canAttach && this.currentParent instanceof ms123.oryx.core.Node && this.shape.dockers.length > 0) {
						var docker = this.shape.dockers[0];

						if (this.currentParent.parent instanceof ms123.oryx.core.Node) {
							this.currentParent.parent.add(docker.parent);
						}

						docker.bounds.centerMoveTo(this.position);
						docker.setDockedShape(this.currentParent);
						//docker.update();	
					}

					//this.currentParent.update();
					//this.shape.update();
					this.facade.setSelection([this.shape]);
					this.facade.getCanvas().update();
					this.facade.updateSelection();

				},
				rollback: function () {
					this.facade.deleteShape(this.shape);

					//this.currentParent.update();
					this.facade.setSelection(this.selection.without(this.shape));
					this.facade.getCanvas().update();
					this.facade.updateSelection();

				}
			});

			var event = {};
			event.clientX = e.getDocumentLeft();
			event.clientY = e.getDocumentTop();
			var position = this.facade.eventCoordinates(event);

			console.log("commandClass:" + commandClass + "/o:" + option);
			var command = new commandClass(option, this._currentParent, this._canAttach, position, this.facade);
			console.log("command:" + command);

			this.facade.executeCommands([command]);

			this._currentParent = undefined;
		},
		__dragover: function (e) {
			var cursor = qx.ui.core.DragDropCursor.getInstance();
			var event = {};
			event.clientX = e.getDocumentLeft();
			event.clientY = e.getDocumentTop();
			var coord = this.facade.eventCoordinates(event);
			var aShapes = this.facade.getCanvas().getAbstractShapesAtPosition(coord);
			if (aShapes.length <= 0) {
				cursor.setAction(null);
				return false;
			}

			var el = aShapes.last();

			if (aShapes.lenght == 1 && aShapes[0] instanceof ms123.oryx.core.Canvas) {
				console.log("is canvas");
				cursor.setAction(null);
				return false;
			} else {
				var stencil = e.getTarget().getUserData("stencil");
				// check containment rules
				if (stencil.type() === "node") {
					var parentCandidate = aShapes.reverse().find(function (candidate) {
						return (candidate instanceof ms123.oryx.core.Canvas || candidate instanceof ms123.oryx.core.Node || candidate instanceof ms123.oryx.core.Edge);
					});

					if (parentCandidate !== this._lastOverElement) {
						this._canAttach = undefined;
						this._canContain = undefined;
					}

					if (parentCandidate) {
						//check containment rule					
						if (!(parentCandidate instanceof ms123.oryx.core.Canvas) && parentCandidate.isPointOverOffset(coord.x, coord.y) && this._canAttach == undefined) {

							this._canAttach = this.facade.getRules().canConnect({
								sourceShape: parentCandidate,
								edgeStencil: stencil,
								targetStencil: stencil
							});

							console.log("canAttach:" + this._canAttach);
							if (this._canAttach) {
								// Show Highlight
								this.facade.raiseEvent({
									type: ms123.oryx.Config.EVENT_HIGHLIGHT_SHOW,
									highlightId: "shapeRepo.attached",
									elements: [parentCandidate],
									style: ms123.oryx.Config.SELECTION_HIGHLIGHT_STYLE_RECTANGLE,
									color: ms123.oryx.Config.SELECTION_VALID_COLOR
								});

								this.facade.raiseEvent({
									type: ms123.oryx.Config.EVENT_HIGHLIGHT_HIDE,
									highlightId: "shapeRepo.added"
								});

								this._canContain = undefined;
							}

						}

						if (!(parentCandidate instanceof ms123.oryx.core.Canvas) && !parentCandidate.isPointOverOffset(coord.x, coord.y)) {
							this._canAttach = this._canAttach == false ? this._canAttach : undefined;
						}

						if (this._canContain == undefined && !this._canAttach) {

							this._canContain = this.facade.getRules().canContain({
								containingShape: parentCandidate,
								containedStencil: stencil
							});

							// Show Highlight
							this.facade.raiseEvent({
								type: ms123.oryx.Config.EVENT_HIGHLIGHT_SHOW,
								highlightId: 'shapeRepo.added',
								elements: [parentCandidate],
								color: this._canContain ? ms123.oryx.Config.SELECTION_VALID_COLOR : ms123.oryx.Config.SELECTION_INVALID_COLOR
							});
							this.facade.raiseEvent({
								type: ms123.oryx.Config.EVENT_HIGHLIGHT_HIDE,
								highlightId: "shapeRepo.attached"
							});
						}



						this._currentParent = this._canContain || this._canAttach ? parentCandidate : undefined;
						this._lastOverElement = parentCandidate;
						//		var pr = dragZone.getProxy();
						//		pr.setStatus(this._currentParent ? pr.dropAllowed : pr.dropNotAllowed);
						//		pr.sync();
						if (this._currentParent) {
							cursor.setAction("move");
						} else {
							cursor.setAction(null);
						}
					}
				} else { //Edge
					this._currentParent = this.facade.getCanvas();
					cursor.setAction("move");
					//var pr = dragZone.getProxy();
					//pr.setStatus(pr.dropAllowed);
					//pr.sync();
				}
			}
		}
	}
});
