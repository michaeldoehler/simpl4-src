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
 * @ignore(jQuery)
 */

qx.Class.define( "ms123.processexplorer.plugins.JsonDisplay", {
	extend: qx.core.Object,
	include: [ qx.locale.MTranslation ],
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function( facade ) {
		this.base( arguments );
		this.facade = facade;
		this.facade.registerOnEvent( ms123.processexplorer.Config.EVENT_SHOWDETAILS, this._handleShowWindowEvent.bind( this ) );
		this.facade.registerOnEvent( ms123.processexplorer.Config.EVENT_HIDEDETAILS, this._handleHideWindowEvent.bind( this ) );

	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},

	/******************************************************************************
	 STATICS
	 ******************************************************************************/
	statics: {},
	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		_handleHideWindowEvent: function( e ) {
			//this._window.close();
		},
		_handleShowWindowEvent: function( e ) {
			var value = e.value;
			var win = this._createWindow( e.name, value );
			win.open();
		},
		_createWindow: function( name, value ) {
			var win = new ms123.desktop.Window( null, name, "" ).set( {
				resizable: true,
				useMoveFrame: false,
				contentPadding: 4,
				useResizeFrame: false
			} );

			win.setCaption( name );
			win.setLayout( new qx.ui.layout.Dock );
			win.setWidth( 600 );
			win.setHeight( 500 );
			win.setAllowMaximize( false );
			win.setAllowMinimize( true );
			win.setModal( false );
			win.setActive( false );
			win.minimize();
			win.center();

			var tab = new qx.ui.tabview.TabView().set( {
				contentPadding: 0,
				minHeight: 150
			} );
			var treePage = new qx.ui.tabview.Page( this.tr( "jsondisplay.formatted" ), "resource/ms123/view.png" ).set( {
				showCloseButton: false
			} );
			treePage.setLayout( new qx.ui.layout.Dock() );
			tab.add( treePage, {
				edge: 0
			} );
			var jsonPage = new qx.ui.tabview.Page( this.tr( "jsondisplay.plain" ), "resource/ms123/json.png" ).set( {
				showCloseButton: false
			} );
			jsonPage.setLayout( new qx.ui.layout.Grow() );
			tab.add( jsonPage, {
				edge: 0
			} );


			var embed = new qx.ui.embed.Html().set( {
				overflowY: "auto",
				overflowX: "auto"
			} );
			embed.addListenerOnce( "appear", function() {
				var el = embed.getContentElement().getDomElement();
				treePage.add( this.__createToolbar(el), {
					edge: "north"
				} );
				jQuery( el ).JSONView( qx.lang.Json.parse( value ), {
					collapsed: false
				} );
				jQuery( el ).JSONView( 'toggle', 2 );
			}, this );
			treePage.add( embed, {
				edge: "center"
			} );

			var msgArea = new qx.ui.form.TextArea();
			msgArea.setFont( qx.bom.Font.fromString( "Mono, 9px" ) );
			msgArea.setValue( value );
			jsonPage.add( msgArea );
			win.add( tab, {
				edge: "center"
			} );

			var app = qx.core.Init.getApplication();
			var ns = this.facade.storeDesc.getNamespace();
			var tb = app.getTaskbar( ns );
			var dt = app.getDesktop( ns );
			tb.addWindow( win );
			dt.add( win );
			return win;
		},
		__createToolbar: function( el ) {
			var toolbar = new qx.ui.toolbar.ToolBar();
			var collapse = new qx.ui.toolbar.Button( this.tr("jsondisplay.collapse"), "icon/16/actions/list-remove.png" );
			collapse.addListener( "execute", function() {
				jQuery( el ).JSONView( 'collapse' );
			}, this );
			toolbar._add( collapse );

			var expand = new qx.ui.toolbar.Button( this.tr("jsondisplay.expand"), "icon/16/actions/list-add.png" );
			expand.addListener( "execute", function() {
				jQuery( el ).JSONView( 'expand' );
			}, this );
			toolbar._add( expand );

			var toggle = new qx.ui.toolbar.Button( this.tr("jsondisplay.toggle"), "icon/16/actions/object-flip-vertical.png" );
			toggle.addListener( "execute", function() {
				jQuery( el ).JSONView( 'toggle' );
			}, this );
			toolbar._add( toggle );

			var level2 = new qx.ui.toolbar.Button( this.tr("jsondisplay.level2"), "icon/16/actions/object-flip-vertical.png" );
			level2.addListener( "execute", function() {
				jQuery( el ).JSONView( 'toggle', 2 );
			}, this );
			toolbar._add( level2 );
			return toolbar;
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function() {}

} );



/*!
jQuery JSONView.
Licensed under the MIT License.
 */
( function( jQuery ) {
	var $, Collapser, JSONFormatter, JSONView;
	JSONFormatter = ( function() {
		function JSONFormatter( options ) {
			if ( options == null ) {
				options = {};
			}
			this.options = options;
		}

		JSONFormatter.prototype.htmlEncode = function( html ) {
			if ( html !== null ) {
				return html.toString().replace( /&/g, "&amp;" ).replace( /"/g, "&quot;" ).replace( /</g, "&lt;" ).replace( />/g, "&gt;" );
			} else {
				return '';
			}
		};

		JSONFormatter.prototype.jsString = function( s ) {
			s = JSON.stringify( s ).slice( 1, -1 );
			return this.htmlEncode( s );
		};

		JSONFormatter.prototype.decorateWithSpan = function( value, className ) {
			return "<span class=\"" + className + "\">" + ( this.htmlEncode( value ) ) + "</span>";
		};

		JSONFormatter.prototype.valueToHTML = function( value, level ) {
			var valueType;
			if ( level == null ) {
				level = 0;
			}
			valueType = Object.prototype.toString.call( value ).match( /\s(.+)]/ )[ 1 ].toLowerCase();
			return this[ "" + valueType + "ToHTML" ].call( this, value, level );
		};

		JSONFormatter.prototype.nullToHTML = function( value ) {
			return this.decorateWithSpan( 'null', 'null' );
		};

		JSONFormatter.prototype.numberToHTML = function( value ) {
			return this.decorateWithSpan( value, 'num' );
		};

		JSONFormatter.prototype.stringToHTML = function( value ) {
			var multilineClass, newLinePattern;
			if ( /^(http|https|file):\/\/[^\s]+$/i.test( value ) ) {
				return "<a href=\"" + ( this.htmlEncode( value ) ) + "\"><span class=\"q\">\"</span>" + ( this.jsString( value ) ) + "<span class=\"q\">\"</span></a>";
			} else {
				multilineClass = '';
				value = this.jsString( value );
				if ( this.options.nl2br ) {
					newLinePattern = /([^>\\r\\n]?)(\\r\\n|\\n\\r|\\r|\\n)/g;
					if ( newLinePattern.test( value ) ) {
						multilineClass = ' multiline';
						value = ( value + '' ).replace( newLinePattern, '$1' + '<br />' );
					}
				}
				return "<span class=\"string" + multilineClass + "\">\"" + value + "\"</span>";
			}
		};

		JSONFormatter.prototype.booleanToHTML = function( value ) {
			return this.decorateWithSpan( value, 'bool' );
		};

		JSONFormatter.prototype.arrayToHTML = function( array, level ) {
			var collapsible, hasContents, index, numProps, output, value, _i, _len;
			if ( level == null ) {
				level = 0;
			}
			hasContents = false;
			output = '';
			numProps = array.length;
			for ( index = _i = 0, _len = array.length; _i < _len; index = ++_i ) {
				value = array[ index ];
				hasContents = true;
				output += '<li>' + this.valueToHTML( value, level + 1 );
				if ( numProps > 1 ) {
					output += ',';
				}
				output += '</li>';
				numProps--;
			}
			if ( hasContents ) {
				collapsible = level === 0 ? '' : ' collapsible';
				return "[<ul class=\"array level" + level + collapsible + "\">" + output + "</ul>]";
			} else {
				return '[ ]';
			}
		};

		JSONFormatter.prototype.objectToHTML = function( object, level ) {
			var collapsible, hasContents, numProps, output, prop, value;
			if ( level == null ) {
				level = 0;
			}
			hasContents = false;
			output = '';
			numProps = 0;
			for ( prop in object ) {
				numProps++;
			}
			for ( prop in object ) {
				value = object[ prop ];
				hasContents = true;
				output += "<li><span class=\"prop\"><span class=\"q\">\"</span>" + ( this.jsString( prop ) ) + "<span class=\"q\">\"</span></span>: " + ( this.valueToHTML( value, level + 1 ) );
				if ( numProps > 1 ) {
					output += ',';
				}
				output += '</li>';
				numProps--;
			}
			if ( hasContents ) {
				collapsible = level === 0 ? '' : ' collapsible';
				return "{<ul class=\"obj level" + level + collapsible + "\">" + output + "</ul>}";
			} else {
				return '{ }';
			}
		};

		JSONFormatter.prototype.jsonToHTML = function( json ) {
			return "<div class=\"jsonview\">" + ( this.valueToHTML( json ) ) + "</div>";
		};

		return JSONFormatter;

	} )();
	//( typeof module !== "undefined" && module !== null ) && ( module.exports = JSONFormatter );
	Collapser = ( function() {
		function Collapser() {}

		Collapser.bindEvent = function( item, options ) {
			var collapser;
			collapser = document.createElement( 'div' );
			collapser.className = 'collapser';
			collapser.innerHTML = options.collapsed ? '+' : '-';
			collapser.addEventListener( 'click', ( function( _this ) {
				return function( event ) {
					return _this.toggle( event.target, options );
				};
			} )( this ) );
			item.insertBefore( collapser, item.firstChild );
			if ( options.collapsed ) {
				return this.collapse( collapser );
			}
		};

		Collapser.expand = function( collapser ) {
			var ellipsis, target;
			target = this.collapseTarget( collapser );
			if ( target.style.display === '' ) {
				return;
			}
			ellipsis = target.parentNode.getElementsByClassName( 'ellipsis' )[ 0 ];
			target.parentNode.removeChild( ellipsis );
			target.style.display = '';
			return collapser.innerHTML = '-';
		};

		Collapser.collapse = function( collapser ) {
			var ellipsis, target;
			target = this.collapseTarget( collapser );
			if ( target.style.display === 'none' ) {
				return;
			}
			target.style.display = 'none';
			ellipsis = document.createElement( 'span' );
			ellipsis.className = 'ellipsis';
			ellipsis.innerHTML = ' &hellip; ';
			target.parentNode.insertBefore( ellipsis, target );
			return collapser.innerHTML = '+';
		};

		Collapser.toggle = function( collapser, options ) {
			var action, collapsers, target, _i, _len, _results;
			if ( options == null ) {
				options = {};
			}
			target = this.collapseTarget( collapser );
			action = target.style.display === 'none' ? 'expand' : 'collapse';
			if ( options.recursive_collapser ) {
				collapsers = collapser.parentNode.getElementsByClassName( 'collapser' );
				_results = [];
				for ( _i = 0, _len = collapsers.length; _i < _len; _i++ ) {
					collapser = collapsers[ _i ];
					_results.push( this[ action ]( collapser ) );
				}
				return _results;
			} else {
				return this[ action ]( collapser );
			}
		};

		Collapser.collapseTarget = function( collapser ) {
			var target, targets;
			targets = collapser.parentNode.getElementsByClassName( 'collapsible' );
			if ( !targets.length ) {
				return;
			}
			return target = targets[ 0 ];
		};

		return Collapser;

	} )();
	$ = jQuery;
	JSONView = {
		collapse: function( el ) {
			if ( el.innerHTML === '-' ) {
				return Collapser.collapse( el );
			}
		},
		expand: function( el ) {
			if ( el.innerHTML === '+' ) {
				return Collapser.expand( el );
			}
		},
		toggle: function( el ) {
			return Collapser.toggle( el );
		}
	};
	return $.fn.JSONView = function() {
		var args, defaultOptions, formatter, json, method, options, outputDoc;
		args = arguments;
		if ( JSONView[ args[ 0 ] ] != null ) {
			method = args[ 0 ];
			return this.each( function() {
				var $this, level;
				$this = $( this );
				if ( args[ 1 ] != null ) {
					level = args[ 1 ];
					return $this.find( ".jsonview .collapsible.level" + level ).siblings( '.collapser' ).each( function() {
						return JSONView[ method ]( this );
					} );
				} else {
					return $this.find( '.jsonview > ul > li .collapsible' ).siblings( '.collapser' ).each( function() {
						return JSONView[ method ]( this );
					} );
				}
			} );
		} else {
			json = args[ 0 ];
			options = args[ 1 ] || {};
			defaultOptions = {
				collapsed: false,
				nl2br: false,
				recursive_collapser: false
			};
			options = $.extend( defaultOptions, options );
			formatter = new JSONFormatter( {
				nl2br: options.nl2br
			} );
			if ( Object.prototype.toString.call( json ) === '[object String]' ) {
				json = JSON.parse( json );
			}
			outputDoc = formatter.jsonToHTML( json );
			return this.each( function() {
				var $this, item, items, _i, _len, _results;
				$this = $( this );
				$this.html( outputDoc );
				items = $this[ 0 ].getElementsByClassName( 'collapsible' );
				_results = [];
				for ( _i = 0, _len = items.length; _i < _len; _i++ ) {
					item = items[ _i ];
					if ( item.parentNode.nodeName === 'LI' ) {
						_results.push( Collapser.bindEvent( item.parentNode, options ) );
					} else {
						_results.push( void 0 );
					}
				}
				return _results;
			} );
		}
	};
} )( jQuery );
