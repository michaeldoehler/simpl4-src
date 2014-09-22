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
 * @ignore(Clazz)
 * @ignore($A)
 * @ignore($H)
 */

qx.Class.define("ms123.graphicaleditor.plugins.SyntaxChecker", {
	extend: ms123.oryx.core.AbstractPlugin,
	include: [qx.locale.MTranslation],
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (facade) {
		this.base(arguments, facade);
		this.facade = facade;


		this.active = false;
		this.raisedEventIds = [];

		this.facade.offer({
			'name': this.tr("ge.SyntaxChecker.name"),
			'functionality': this.perform.bind(this),
			'group': this.tr("ge.SyntaxChecker.group"),
			'icon': this.__getResourceUrl("checker_syntax.png"),
			'description': this.tr("ge.SyntaxChecker.desc"),
			'index': 0,
			'toggle': true,
			'minShape': 0,
			'maxShape': 0
		});

		this.facade.registerOnEvent(ms123.graphicaleditor.plugins.SyntaxChecker.CHECK_FOR_ERRORS_EVENT, this.checkForErrors.bind(this));
		this.facade.registerOnEvent(ms123.graphicaleditor.plugins.SyntaxChecker.RESET_ERRORS_EVENT, this.resetErrors.bind(this));
		this.facade.registerOnEvent(ms123.graphicaleditor.plugins.SyntaxChecker.SHOW_ERRORS_EVENT, this.doShowErrors.bind(this));




	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},

	/******************************************************************************
	 STATICS
	 ******************************************************************************/
	statics: {
		CHECK_FOR_ERRORS_EVENT: "checkForErrors",
		RESET_ERRORS_EVENT: "resetErrors",
		SHOW_ERRORS_EVENT: "showErrors"
	},
	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		perform: function (e) {
			var button = e.getTarget();
			this.button = button;
			var pressed = button.getValue();
			if (!pressed) {
				this.resetErrors();
			} else {
				this.checkForErrors({
					onNoErrors: (function () {
						this.setActivated(false);
						this.facade.raiseEvent({
							type: ms123.oryx.Config.EVENT_LOADING_STATUS,
							text: this.tr("ge.SyntaxChecker.noErrors"),
							timeout: 10000
						});
						//Ext.Msg.alert(this.tr("ge.Oryx.title"), this.tr("ge.SyntaxChecker.noErrors"));
					}).bind(this),
					onErrors: (function () {
						this.enableDeactivationHandler(button);
					}).bind(this),
					onFailure: (function () {
						this.setActivated(false);
						//Ext.Msg.alert(this.tr("ge.Oryx.title"), this.tr("ge.SyntaxChecker.invalid"));
					}).bind(this)
				});
			}
		},

		/**
		 * Registers handler for deactivating syntax checker as soon as somewhere is clicked...
		 * param {Ext.Button} Toolbar button
		 */
		enableDeactivationHandler: function (button) {
			var deactivate = function () {
				this.setActivated(false);
				this.resetErrors();
				this.facade.unregisterOnEvent(ms123.oryx.Config.EVENT_MOUSEDOWN, deactivate);
			};
			this.facade.registerOnEvent(ms123.oryx.Config.EVENT_MOUSEDOWN, deactivate.bind(this));
		},

		/**
		 * Sets the activated state of the plugin
		 * param {Ext.Button} Toolbar button
		 * param {Object} activated
		 */
		setActivated: function (activated) {
			this.button.setValue(activated);
			if (activated === undefined) {
				this.active = !this.active;
			} else {
				this.active = activated;
			}
		},

		checkForErrors: function (options) {
			var outgoings = [];
			var shapeList = [];

			var shape = this.facade.getJSON();
			var childShapes = shape.childShapes;
			for (var i = 0; i < childShapes.length; i++) {
				var child = childShapes[i];
				shapeList.push(child.resourceId);
				for (var j = 0; j < child.outgoing.length; j++) {
					outgoings.push(child.outgoing[j].resourceId);
				}
			}
			var errors={};
			for (var i = 0; i < shapeList.length; i++) {
				var sh = this.facade.getCanvas().getChildShapeByResourceId(shapeList[i]);
				if (sh instanceof ms123.oryx.core.Edge) {
					var errormsg = null;
					if (sh.outgoing.length == 0) {
						errormsg = "END";
					}
					if (outgoings.indexOf(shapeList[i]) == -1) {
						errormsg = "START";
					}
					if( errormsg){
						errors[shapeList[i]] = errormsg;
					}
				}
			}
			if( Object.keys(errors).length>0){
				this.showErrors(errors);
			}else{
				options.onNoErrors();
			}
		},

		/** Called on SHOW_ERRORS_EVENT.
		 * 
		 * param {Object} event
		 * param {Object} args
		 */
		doShowErrors: function (event, args) {
			this.showErrors(event.errors);
		},

		/**
		 * Shows overlays for each given error
		 * @methodOf ORYX.Plugins.SyntaxChecker.prototype
		 * param {Hash|Object} errors
		 * @example
		 * showErrors({
		 *     myShape1: "This has an error!",
		 *     myShape2: "Another error!"
		 * })
		 */
		showErrors: function (errors) {
			// If normal object is given, convert to hash
			if (!(errors instanceof Hash)) {
				errors = new Hash(errors);
			}

			// Get all Valid ResourceIDs and collect all shapes
			errors.keys().each((function (value) {
				var sh = this.facade.getCanvas().getChildShapeByResourceId(value);
				if (sh) {
					this.raiseOverlay(sh, this.parseCodeToMsg(errors[value]));
				}
			}).bind(this));
			this.active = !this.active;

			//show a status message with a hint to the error messages in the tooltip
			this.facade.raiseEvent({
				type: ms123.oryx.Config.EVENT_LOADING_STATUS,
				text: this.tr("ge.SyntaxChecker.notice"),
				timeout: 10000
			});
		},
		parseCodeToMsg: function (code) {
			var msg = code.replace(/: /g, "<br />").replace(/, /g, "<br />");
			var codes = msg.split("<br />");
			for (var i = 0; i < codes.length; i++) {
				var singleCode = codes[i];
				var replacement = this.parseSingleCodeToMsg(singleCode);
				if (singleCode != replacement) {
					msg = msg.replace(singleCode, replacement);
				}
			}

			return msg;
		},

		parseSingleCodeToMsg: function (code) {
			return this.tr("ge.SyntaxChecker")[code] || code;
		},
		/**
		 * Resets all (displayed) errors
		 * @methodOf ORYX.Plugins.SyntaxChecker.prototype
		 */
		resetErrors: function () {
			this.raisedEventIds.each((function (id) {
				this.facade.raiseEvent({
					type: ms123.oryx.Config.EVENT_OVERLAY_HIDE,
					id: id
				});
			}).bind(this))

			this.raisedEventIds = [];
			this.active = false;
		},

		raiseOverlay: function (shape, errorMsg) {
			console.log("raiseOverlay:" + shape + "/" + errorMsg);
			var id = "syntaxchecker." + this.raisedEventIds.length;
			var crossId = ms123.oryx.Editor.provideId();
			var cross = ms123.oryx.Editor.graft("http://www.w3.org/2000/svg", null, ['path',
			{
				"id": crossId,
				"title": "",
				"stroke-width": 5.0,
				"stroke": "red",
				"d": "M20,-5 L5,-20 M5,-5 L20,-20",
				"line-captions": "round"
			}]);

			this.facade.raiseEvent({
				type: ms123.oryx.Config.EVENT_OVERLAY_SHOW,
				id: id,
				shapes: [shape],
				node: cross,
				nodePosition: shape instanceof ms123.oryx.core.Edge ? errorMsg : "NW"
			});

/*var tooltip = new Ext.ToolTip({
				showDelay: 50,
				html: errorMsg,
				target: crossId
			});*/

			this.raisedEventIds.push(id);

			return cross;
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
