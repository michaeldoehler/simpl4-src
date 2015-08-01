/*	
 * jQuery mmenu v5.3.4
 * @requires jQuery 1.7.0 or later
 *
 * mmenu.frebsite.nl
 *	
 * Copyright (c) Fred Heusschen
 * www.frebsite.nl
 *
 * Licensed under the MIT license:
 * http://en.wikipedia.org/wiki/MIT_License
 */

(function( $ ) {

	var _PLUGIN_	= 'mmenu',
		_VERSION_	= '5.3.4';


	//	Plugin already excists
	if ( $[ _PLUGIN_ ] )
	{
		return;
	}


	/*
		Class
	*/
	$[ _PLUGIN_ ] = function( $menu, opts, conf )
	{
		this.$menu	= $menu;
		this._api	= [ 'bind', 'init', 'update', 'setSelected', 'getInstance', 'openPanel', 'closePanel', 'closeAllPanels' ];
		this.opts	= opts;
		this.conf	= conf;
		this.vars	= {};
		this.cbck	= {};


		if ( typeof this.___deprecated == 'function' )
		{
			this.___deprecated();
		}

		this._initMenu();
		this._initAnchors();

		var $panels = this.$menu.children( this.conf.panelNodetype );
		
		this._initAddons();
		this.init( $panels );


		if ( typeof this.___debug == 'function' )
		{
			this.___debug();
		}

		return this;
	};

	$[ _PLUGIN_ ].version 	= _VERSION_;
	$[ _PLUGIN_ ].addons 	= {};
	$[ _PLUGIN_ ].uniqueId 	= 0;


	$[ _PLUGIN_ ].defaults 	= {
		extensions		: [],
		navbar 			: {
			add 			: true,
			title			: 'Menu',
			titleLink		: 'panel'
		},
		onClick			: {
//			close			: true,
//			blockUI			: null,
//			preventDefault	: null,
			setSelected		: true
		},
		slidingSubmenus	: true
	};

	$[ _PLUGIN_ ].configuration = {
		classNames			: {
			divider		: 'Divider',
			inset 		: 'Inset',
			panel		: 'Panel',
			selected	: 'Selected',
			spacer		: 'Spacer',
			vertical	: 'Vertical'
		},
		clone				: false,
		openingInterval		: 25,
		panelNodetype		: 'ul, ol, div',
		transitionDuration	: 400
	};

	$[ _PLUGIN_ ].prototype = {

		init: function( $panels )
		{
			$panels = $panels.not( '.' + _c.nopanel );
			$panels = this._initPanels( $panels );

			this.trigger( 'init', $panels );
			this.trigger( 'update' );
		},

		update: function()
		{
			this.trigger( 'update' );
		},

		setSelected: function( $i )
		{
			this.$menu.find( '.' + _c.listview ).children().removeClass( _c.selected );
			$i.addClass( _c.selected );
			this.trigger( 'setSelected', $i );
		},
		
		openPanel: function( $panel )
		{
			var $l = $panel.parent();

			//	Vertical
			if ( $l.hasClass( _c.vertical ) )
			{
				var $sub = $l.parents( '.' + _c.subopened );
				if ( $sub.length )
				{
					return this.openPanel( $sub.first() );
				}
				$l.addClass( _c.opened );
			}

			//	Horizontal
			else
			{
				if ( $panel.hasClass( _c.current ) )
				{
					return;
				}

				var $panels = this.$menu.children( '.' + _c.panel ),
					$current = $panels.filter( '.' + _c.current );

				$panels
					.removeClass( _c.highest )
					.removeClass( _c.current )
					.not( $panel )
					.not( $current )
					.not( '.' + _c.vertical )
					.addClass( _c.hidden );

				if ( !$[ _PLUGIN_ ].support.csstransitions )
				{
					$current.addClass( _c.hidden );
				}

				if ( $panel.hasClass( _c.opened ) )
				{
					$panel.nextAll( '.' + _c.opened )
						.addClass( _c.highest )
						.removeClass( _c.opened )
						.removeClass( _c.subopened );
				}
				else
				{
					$panel.addClass( _c.highest );
					$current.addClass( _c.subopened );
				}

				$panel
					.removeClass( _c.hidden )
					.addClass( _c.current );

				//	Without the timeout, the animation won't work because the element had display: none;
				setTimeout(
					function()
					{
						$panel
							.removeClass( _c.subopened )
							.addClass( _c.opened );

					}, this.conf.openingInterval
				);
			}
			this.trigger( 'openPanel', $panel );
		},

		closePanel: function( $panel )
		{
			var $l = $panel.parent();

			//	Vertical only
			if ( $l.hasClass( _c.vertical ) )
			{
				$l.removeClass( _c.opened );
				this.trigger( 'closePanel', $panel );
			}
		},

		closeAllPanels: function()
		{
			//	Vertical
			this.$menu.find( '.' + _c.listview )
				.children()
				.removeClass( _c.selected )
				.filter( '.' + _c.vertical )
				.removeClass( _c.opened );

			//	Horizontal
			var $pnls = this.$menu.children( '.' + _c.panel ),
				$frst = $pnls.first();

			this.$menu.children( '.' + _c.panel )
				.not( $frst )
				.removeClass( _c.subopened )
				.removeClass( _c.opened )
				.removeClass( _c.current )
				.removeClass( _c.highest )
				.addClass( _c.hidden );

			this.openPanel( $frst );
		},
		
		togglePanel: function( $panel )
		{
			var $l = $panel.parent();

			//	Vertical only
			if ( $l.hasClass( _c.vertical ) )
			{
				this[ $l.hasClass( _c.opened ) ? 'closePanel' : 'openPanel' ]( $panel );
			}
		},

		getInstance: function()
		{
			return this;
		},

		bind: function( event, fn )
		{
			this.cbck[ event ] = this.cbck[ event ] || [];
			this.cbck[ event ].push( fn );
		},

		trigger: function()
		{
			var that = this,
				args = Array.prototype.slice.call( arguments ),
				evnt = args.shift();

			if ( this.cbck[ evnt ] )
			{
				for ( var e = 0, l = this.cbck[ evnt ].length; e < l; e++ )
                {
                    this.cbck[ evnt ][ e ].apply( that, args );
                }
			}
		},

		_initMenu: function()
		{
			var that = this;

			//	Clone if needed
			if ( this.opts.offCanvas && this.conf.clone )
			{
				this.$menu = this.$menu.clone( true );
				this.$menu.add( this.$menu.find( '[id]' ) )
					.filter( '[id]' )
					.each(
						function()
						{
							$(this).attr( 'id', _c.mm( $(this).attr( 'id' ) ) );
						}
					);
			}

			//	Strip whitespace
			this.$menu.contents().each(
				function()
				{
					if ( $(this)[ 0 ].nodeType == 3 )
					{
						$(this).remove();
					}
				}
			);

			this.$menu
				.parent()
				.addClass( _c.wrapper );

			var clsn = [ _c.menu ];

			//	Add direction class
			if ( !this.opts.slidingSubmenus )
			{
				clsn.push( _c.vertical );
			}

			//	Add extensions
			this.opts.extensions = ( this.opts.extensions.length )
				? 'mm-' + this.opts.extensions.join( ' mm-' )
				: '';

			if ( this.opts.extensions )
			{
				clsn.push( this.opts.extensions );
			}

			this.$menu.addClass( clsn.join( ' ' ) );
		},

		_initPanels: function( $panels )
		{
			var that = this;

			//	Add List class
			var $lists = this.__findAddBack( $panels, 'ul, ol' );

			this.__refactorClass( $lists, this.conf.classNames.inset, 'inset' )
				.addClass( _c.nolistview + ' ' + _c.nopanel );

			$lists.not( '.' + _c.nolistview )
				.addClass( _c.listview );

			var $lis = this.__findAddBack( $panels, '.' + _c.listview ).children();

			//	Refactor Selected class
			this.__refactorClass( $lis, this.conf.classNames.selected, 'selected' );

			//	Refactor divider class
			this.__refactorClass( $lis, this.conf.classNames.divider, 'divider' );

			//	Refactor Spacer class
			this.__refactorClass( $lis, this.conf.classNames.spacer, 'spacer' );

			//	Refactor Panel class
			this.__refactorClass( this.__findAddBack( $panels, '.' + this.conf.classNames.panel ), this.conf.classNames.panel, 'panel' );

			//	Create panels
			var $curpanels = $(),
				$oldpanels = $panels
					.add( $panels.find( '.' + _c.panel ) )
					.add( this.__findAddBack( $panels, '.' + _c.listview ).children().children( this.conf.panelNodetype ) )
					.not( '.' + _c.nopanel );

			this.__refactorClass( $oldpanels, this.conf.classNames.vertical, 'vertical' );
			
			if ( !this.opts.slidingSubmenus )
			{
				$oldpanels.addClass( _c.vertical );
			}

			$oldpanels
				.each(
					function()
					{
						var $t = $(this),
							$p = $t;

						if ( $t.is( 'ul, ol' ) )
						{
							$t.wrap( '<div class="' + _c.panel + '" />' );
							$p = $t.parent();
						}
						else
						{
							$p.addClass( _c.panel );
						}

						var id = $t.attr( 'id' );
						$t.removeAttr( 'id' );
						$p.attr( 'id', id || that.__getUniqueId() );

						if ( $t.hasClass( _c.vertical ) )
						{
							$t.removeClass( that.conf.classNames.vertical );
							$p.add( $p.parent() ).addClass( _c.vertical );
						}

						$curpanels = $curpanels.add( $p );
					} 
				);

			var $allpanels = $('.' + _c.panel, this.$menu);

			//	Add open and close links to menu items
			$curpanels
				.each(
					function( i )
					{
						var $t = $(this),
							$p = $t.parent(),
							$a = $p.children( 'a, span' ).first();

						if ( !$p.is( '.' + _c.menu ) )
						{
							$p.data( _d.sub, $t );
							$t.data( _d.parent, $p );
						}

						//	Open link
						if ( !$p.children( '.' + _c.next ).length )
						{
							if ( $p.parent().is( '.' + _c.listview ) )
							{
								var id = $t.attr( 'id' ),
									$b = $( '<a class="' + _c.next + '" href="#' + id + '" data-target="#' + id + '" />' ).insertBefore( $a );

								if ( $a.is( 'span' ) )
								{
									$b.addClass( _c.fullsubopen );
								}
							}
						}

						//	Navbar
						if ( !$t.children( '.' + _c.navbar ).length )
						{
							if ( !$p.hasClass( _c.vertical ) )
							{
								if ( $p.parent().is( '.' + _c.listview ) )
								{
									//	Listview, the panel wrapping this panel
									var $p = $p.closest( '.' + _c.panel );
								}
								else
								{
									//	Non-listview, the first panel that has an anchor that links to this panel
									var $a = $p.closest( '.' + _c.panel ).find( 'a[href="#' + $t.attr( 'id' ) + '"]' ).first(),
										$p = $a.closest( '.' + _c.panel );
								}

								var $navbar = $( '<div class="' + _c.navbar + '" />' );

								if ( $p.length )
								{
									var id = $p.attr( 'id' );
									switch ( that.opts.navbar.titleLink )
									{
										case 'anchor':
											_url = $a.attr( 'href' );
											break;

										case 'panel':
										case 'parent':
											_url = '#' + id;
											break;

										case 'none':
										default:
											_url = false;
											break;
									}

									$navbar
										.append( '<a class="' + _c.btn + ' ' + _c.prev + '" href="#' + id + '" data-target="#' + id + '"></a>' )
										.append( '<a class="' + _c.title + '"' + ( _url ? ' href="' + _url + '"' : '' ) + '>' + $a.text() + '</a>' )
										.prependTo( $t );

									if ( that.opts.navbar.add )
									{
										$t.addClass( _c.hasnavbar );
									}
								}
								else if ( that.opts.navbar.title )
								{
									$navbar
										.append( '<a class="' + _c.title + '">' + that.opts.navbar.title + '</a>' )
										.prependTo( $t );

									if ( that.opts.navbar.add )
									{
										$t.addClass( _c.hasnavbar );
									}
								}
							}
						}
					}
				);


			//	Add opened-classes to parents
			var $s = this.__findAddBack( $panels, '.' + _c.listview )
				.children( '.' + _c.selected )
				.removeClass( _c.selected )
				.last()
				.addClass( _c.selected );

			$s.add( $s.parentsUntil( '.' + _c.menu, 'li' ) )
				.filter( '.' + _c.vertical )
				.addClass( _c.opened )
				.end()
				.not( '.' + _c.vertical )
				.each(
					function()
					{
						$(this).parentsUntil( '.' + _c.menu, '.' + _c.panel )
							.not( '.' + _c.vertical )
							.first()
							.addClass( _c.opened )
							.parentsUntil( '.' + _c.menu, '.' + _c.panel )
							.not( '.' + _c.vertical )
							.first()
							.addClass( _c.opened )
							.addClass( _c.subopened );
					}
				);


			//	Add opened-classes to child
			$s.children( '.' + _c.panel )
				.not( '.' + _c.vertical )
				.addClass( _c.opened )
				.parentsUntil( '.' + _c.menu, '.' + _c.panel )
				.not( '.' + _c.vertical )
				.first()
				.addClass( _c.opened )
				.addClass( _c.subopened );


			//	Set current opened
			var $current = $allpanels.filter( '.' + _c.opened );
			if ( !$current.length )
			{
				$current = $curpanels.first();
			}
			$current
				.addClass( _c.opened )
				.last()
				.addClass( _c.current );


			//	Rearrange markup
			$curpanels
				.not( '.' + _c.vertical )
				.not( $current.last() )
				.addClass( _c.hidden )
				.end()
				.appendTo( this.$menu );
			
			return $curpanels;
		},

		_initAnchors: function()
		{
			var that = this;

			glbl.$body
				.on( _e.click + '-oncanvas',
					'a[href]',
					function( e )
					{
						var $t = $(this),
							fired 	= false,
							inMenu 	= that.$menu.find( $t ).length;

						//	Find behavior for addons
						for ( var a in $[ _PLUGIN_ ].addons )
						{
							if ( fired = $[ _PLUGIN_ ].addons[ a ].clickAnchor.call( that, $t, inMenu ) )
							{
								break;
							}
						}

						//	Open/Close panel
						if ( !fired && inMenu )
						{
							var _h = $t.attr( 'href' );
							if ( _h.length > 1 && _h.slice( 0, 1 ) == '#' )
							{
								try
								{
									var $h = $(_h, that.$menu);
									if ( $h.is( '.' + _c.panel ) )
									{
										fired = true;
										that[ $t.parent().hasClass( _c.vertical ) ? 'togglePanel' : 'openPanel' ]( $h );
									}
								}
								catch( err ) {}
							}
						}

						if ( fired )
						{
							e.preventDefault();
						}


						//	All other anchors in lists
						if ( !fired && inMenu )
						{
							if ( $t.is( '.' + _c.listview + ' > li > a' )
								&& !$t.is( '[rel="external"]' ) 
								&& !$t.is( '[target="_blank"]' ) )
							{

								//	Set selected item
								if ( that.__valueOrFn( that.opts.onClick.setSelected, $t ) )
								{
									that.setSelected( $(e.target).parent() );
								}
	
								//	Prevent default / don't follow link. Default: false
								var preventDefault = that.__valueOrFn( that.opts.onClick.preventDefault, $t, _h.slice( 0, 1 ) == '#' );
								if ( preventDefault )
								{
									e.preventDefault();
								}
		
								//	Block UI. Default: false if preventDefault, true otherwise
								if ( that.__valueOrFn( that.opts.onClick.blockUI, $t, !preventDefault ) )
								{
									glbl.$html.addClass( _c.blocking );
								}

								//	Close menu. Default: true if preventDefault, false otherwise
								if ( that.__valueOrFn( that.opts.onClick.close, $t, preventDefault ) )
								{
									that.close();
								}
							}
						}
					}
				);
		},

		_initAddons: function()
		{
			//	Add add-ons to plugin
			for ( var a in $[ _PLUGIN_ ].addons )
			{
				$[ _PLUGIN_ ].addons[ a ].add.call( this );
				$[ _PLUGIN_ ].addons[ a ].add = function() {};
			}

			//	Setup adds-on for menu
			for ( var a in $[ _PLUGIN_ ].addons )
			{
				$[ _PLUGIN_ ].addons[ a ].setup.call( this );
			}
		},

		__api: function()
		{
			var that = this,
				api = {};

			$.each( this._api, 
				function( i )
				{
					var fn = this;
					api[ fn ] = function()
					{
						var re = that[ fn ].apply( that, arguments );
						return ( typeof re == 'undefined' ) ? api : re;
					}
				}
			);
			return api;
		},

		__valueOrFn: function( o, $e, d )
		{
			if ( typeof o == 'function' )
			{
				return o.call( $e[ 0 ] );
			}
			if ( typeof o == 'undefined' && typeof d != 'undefined' )
			{
				return d;
			}
			return o;
		},

		__refactorClass: function( $e, o, c )
		{
			return $e.filter( '.' + o ).removeClass( o ).addClass( _c[ c ] );
		},

		__findAddBack: function( $e, s )
		{
			return $e.find( s ).add( $e.filter( s ) );
		},

		__filterListItems: function( $i )
		{
			return $i
				.not( '.' + _c.divider )
				.not( '.' + _c.hidden );
		},
		
		__transitionend: function( $e, fn, duration )
		{
			var _ended = false,
				_fn = function()
				{
					if ( !_ended )
					{
						fn.call( $e[ 0 ] );
					}
					_ended = true;
				};
	
			$e.one( _e.transitionend, _fn );
			$e.one( _e.webkitTransitionEnd, _fn );
			setTimeout( _fn, duration * 1.1 );
		},
		
		__getUniqueId: function()
		{
			return _c.mm( $[ _PLUGIN_ ].uniqueId++ );
		}
	};


	/*
		jQuery plugin
	*/
	$.fn[ _PLUGIN_ ] = function( opts, conf )
	{
		//	First time plugin is fired
		initPlugin($(this));

		//	Extend options
		opts = $.extend( true, {}, $[ _PLUGIN_ ].defaults, opts );
		conf = $.extend( true, {}, $[ _PLUGIN_ ].configuration, conf );

		return this.each(
			function()
			{
				var $menu = $(this);
				if ( $menu.data( _PLUGIN_ ) )
				{
					return;
				}
				var _menu = new $[ _PLUGIN_ ]( $menu, opts, conf );
				$menu.data( _PLUGIN_, _menu.__api() );
			}
		);
	};


	/*
		SUPPORT
	*/
	$[ _PLUGIN_ ].support = {
		touch: 'ontouchstart' in window || navigator.msMaxTouchPoints,
		csstransitions: (function()
		{
			//	Use Modernizr test
			if ( typeof Modernizr !== 'undefined' )
			{
				return Modernizr.csstransitions;
			}

			var b = document.body || document.documentElement,
				s = b.style,
				p = 'transition';

			//	Default support
			if ( typeof s[ p ] == 'string' )
			{
				return true;
			}

			//	Vendor specific support
			var v = [ 'Moz', 'webkit', 'Webkit', 'Khtml', 'O', 'ms' ];
			p = p.charAt( 0 ).toUpperCase() + p.substr( 1 );

			for ( var i = 0; i < v.length; i++ )
			{
				if ( typeof s[ v[ i ] + p ] == 'string' )
				{
					return true;
				}
			}

			//	No css transitions
			return false;
		})()
	};


	//	Global variables
	var _c, _d, _e, glbl;

	function initPlugin(t)
	{
		if ( $[ _PLUGIN_ ].glbl )
		{
			return;
		}

		glbl = {
			$wndw : $(window),
			$html : t,//$('html'),
			$body : t//$('body')
		};


		//	Classnames, Datanames, Eventnames
		_c = {};
		_d = {};
		_e = {};
		$.each( [ _c, _d, _e ],
			function( i, o )
			{
				o.add = function( a )
				{
					a = a.split( ' ' );
					for ( var b = 0, l = a.length; b < l; b++ )
					{
						o[ a[ b ] ] = o.mm( a[ b ] );
					}
				};
			}
		);

		//	Classnames
		_c.mm = function( c ) { return 'mm-' + c; };
		_c.add( 'wrapper menu panel nopanel current highest opened subopened navbar hasnavbar title btn prev next listview nolistview inset vertical selected divider spacer hidden fullsubopen' );
		_c.umm = function( c )
		{
			if ( c.slice( 0, 3 ) == 'mm-' )
			{
				c = c.slice( 3 );
			}
			return c;
		};

		//	Datanames
		_d.mm = function( d ) { return 'mm-' + d; };
		_d.add( 'parent sub' );

		//	Eventnames
		_e.mm = function( e ) { return e + '.mm'; };
		_e.add( 'transitionend webkitTransitionEnd mousedown mouseup touchstart touchmove touchend click keydown' );

		$[ _PLUGIN_ ]._c = _c;
		$[ _PLUGIN_ ]._d = _d;
		$[ _PLUGIN_ ]._e = _e;

		$[ _PLUGIN_ ].glbl = glbl;
	}


})( jQuery );
/*	
 * jQuery mmenu autoHeight addon
 * mmenu.frebsite.nl
 *
 * Copyright (c) Fred Heusschen
 */

(function( $ ) {

	var _PLUGIN_ = 'mmenu',
		_ADDON_  = 'autoHeight';


	$[ _PLUGIN_ ].addons[ _ADDON_ ] = {

		//	setup: fired once per menu
		setup: function()
		{
			if ( !this.opts.offCanvas )
			{
				return;
			}
			switch( this.opts.offCanvas.position )
			{
				case 'left':
				case 'right':
					return;
					break;
			}

			var that = this,
				opts = this.opts[ _ADDON_ ],
				conf = this.conf[ _ADDON_ ];

			glbl = $[ _PLUGIN_ ].glbl;


			//	Extend shortcut options
			if ( typeof opts == 'boolean' && opts )
			{
				opts = {
					height: 'auto'
				};
			}
			if ( typeof opts != 'object' )
			{
				opts = {};
			}
			opts = this.opts[ _ADDON_ ] = $.extend( true, {}, $[ _PLUGIN_ ].defaults[ _ADDON_ ], opts );


			if ( opts.height != 'auto' )
			{
				return;
			}

			this.$menu.addClass( _c.autoheight );


			//	Update the height
			var update = function( $panl )
			{
				var $p = this.$menu.children( '.' + _c.current );
					_top = parseInt( $p.css( 'top' )	, 10 ) || 0;
					_bot = parseInt( $p.css( 'bottom' )	, 10 ) || 0;

				this.$menu.addClass( _c.measureheight );

				$panl = $panl || this.$menu.children( '.' + _c.current );
				if ( $panl.is( '.' + _c.vertical ) )
				{
					$panl = $panl
						.parents( '.' + _c.panel )
						.not( '.' + _c.vertical )
						.first();
				}

				this.$menu
					.height( $panl.outerHeight() + _top + _bot )
					.removeClass( _c.measureheight );
			};

			this.bind( 'update', update );
			this.bind( 'openPanel', update );
			this.bind( 'closePanel', update );
			this.bind( 'open', update );

			glbl.$wndw
				.off( _e.resize + '-autoheight' )
				.on( _e.resize + '-autoheight',
					function( e )
					{
						update.call( that );
					}
				);
		},

		//	add: fired once per page load
		add: function()
		{
			_c = $[ _PLUGIN_ ]._c;
			_d = $[ _PLUGIN_ ]._d;
			_e = $[ _PLUGIN_ ]._e;

 			_c.add( 'autoheight measureheight' );
			_e.add( 'resize' );
		},

		//	clickAnchor: prevents default behavior when clicking an anchor
		clickAnchor: function( $a, inMenu ) {}
	};


	//	Default options and configuration
	$[ _PLUGIN_ ].defaults[ _ADDON_ ] = {
		height: 'default' // 'auto'
	};


	var _c, _d, _e, glbl;

})( jQuery );/*	
 * jQuery mmenu backButton addon
 * mmenu.frebsite.nl
 *
 * Copyright (c) Fred Heusschen
 */

(function( $ ) {

	var _PLUGIN_ = 'mmenu',
		_ADDON_  = 'backButton';


	$[ _PLUGIN_ ].addons[ _ADDON_ ] = {

		//	setup: fired once per menu
		setup: function()
		{
			if ( !this.opts.offCanvas )
			{
				return;
			}

			var that = this,
				opts = this.opts[ _ADDON_ ],
				conf = this.conf[ _ADDON_ ];

			glbl = $[ _PLUGIN_ ].glbl;


			//	Extend shortcut options
			if ( typeof opts == 'boolean' )
			{
				opts = {
					close	: opts
				};
			}
			if ( typeof opts != 'object' )
			{
				opts = {};
			}
			opts = $.extend( true, {}, $[ _PLUGIN_ ].defaults[ _ADDON_ ], opts );
			

			//	Close menu
			if ( opts.close )
			{
				var _hash = '#' + that.$menu.attr( 'id' );
				this.bind( 'opened',
					function( e )
					{
						if ( location.hash != _hash )
						{
							history.pushState( null, document.title, _hash );
						}
					}
				);

				$(window).on( 'popstate',
					function( e )
					{
	
						if ( glbl.$html.hasClass( _c.opened ) )
						{
							e.stopPropagation();
							that.close();
						}
						else if ( location.hash == _hash )
						{
							e.stopPropagation();
							that.open();
						}
					}
				);
			}
		},

		//	add: fired once per page load
		add: function()
		{
			if ( !window.history || !window.history.pushState )
			{
				$[ _PLUGIN_ ].addons[ _ADDON_ ].setup = function() {};
				return;
			}

			_c = $[ _PLUGIN_ ]._c;
			_d = $[ _PLUGIN_ ]._d;
			_e = $[ _PLUGIN_ ]._e;
		},

		//	clickAnchor: prevents default behavior when clicking an anchor
		clickAnchor: function( $a, inMenu ) {}
	};


	//	Default options and configuration
	$[ _PLUGIN_ ].defaults[ _ADDON_ ] = {
		close: false
	};


	var _c, _d, _e, glbl;

})( jQuery );/*	
 * jQuery mmenu counters addon
 * mmenu.frebsite.nl
 *
 * Copyright (c) Fred Heusschen
 */

(function( $ ) {

	var _PLUGIN_ = 'mmenu',
		_ADDON_  = 'counters';


	$[ _PLUGIN_ ].addons[ _ADDON_ ] = {

		//	setup: fired once per menu
		setup: function()
		{
			var that = this,
				opts = this.opts[ _ADDON_ ],
				conf = this.conf[ _ADDON_ ];

			glbl = $[ _PLUGIN_ ].glbl;


			//	Extend shortcut options
			if ( typeof opts == 'boolean' )
			{
				opts = {
					add		: opts,
					update	: opts
				};
			}
			if ( typeof opts != 'object' )
			{
				opts = {};
			}
			opts = this.opts[ _ADDON_ ] = $.extend( true, {}, $[ _PLUGIN_ ].defaults[ _ADDON_ ], opts );


			//	Refactor counter class
			this.bind( 'init',
				function( $panels )
				{
					this.__refactorClass( $('em', $panels), this.conf.classNames[ _ADDON_ ].counter, 'counter' );
				}
			);


			//	Add the counters
			if ( opts.add )
			{
				this.bind( 'init',
					function( $panels )
					{
						$panels
							.each(
								function()
								{
									var $prnt = $(this).data( _d.parent );
									if ( $prnt )
									{
										if ( !$prnt.children( 'em.' + _c.counter ).length )
										{
											$prnt.prepend( $( '<em class="' + _c.counter + '" />' ) );
										}
									}
								}
							);
					}
				);
			}

			if ( opts.update )
			{
				this.bind( 'update',
					function()
					{
						this.$menu
							.find( '.' + _c.panel )
							.each(
								function()
								{
									var $panl = $(this),
										$prnt = $panl.data( _d.parent );

									if ( !$prnt )
									{
										return;
									}

									var $cntr = $prnt.children( 'em.' + _c.counter );
									if ( !$cntr.length )
									{
										return;
									}

									$panl = $panl.children( '.' + _c.listview );
									if ( !$panl.length )
									{
										return;
									}

									$cntr.html( that.__filterListItems( $panl.children() ).length );
								}
							);
					}
				);
			}
		},

		//	add: fired once per page load
		add: function()
		{
			_c = $[ _PLUGIN_ ]._c;
			_d = $[ _PLUGIN_ ]._d;
			_e = $[ _PLUGIN_ ]._e;
	
			_c.add( 'counter search noresultsmsg' );
		},

		//	clickAnchor: prevents default behavior when clicking an anchor
		clickAnchor: function( $a, inMenu ) {}
	};


	//	Default options and configuration
	$[ _PLUGIN_ ].defaults[ _ADDON_ ] = {
		add		: false,
		update	: false
	};
	$[ _PLUGIN_ ].configuration.classNames[ _ADDON_ ] = {
		counter: 'Counter'
	};


	var _c, _d, _e, glbl;

})( jQuery );/*	
 * jQuery mmenu dividers addon
 * mmenu.frebsite.nl
 *
 * Copyright (c) Fred Heusschen
 */

(function( $ ) {

	var _PLUGIN_ = 'mmenu',
		_ADDON_  = 'dividers';


	$[ _PLUGIN_ ].addons[ _ADDON_ ] = {

		//	setup: fired once per menu
		setup: function()
		{
			var that = this,
				opts = this.opts[ _ADDON_ ],
				conf = this.conf[ _ADDON_ ];

			glbl = $[ _PLUGIN_ ].glbl;


			//	Extend shortcut options
			if ( typeof opts == 'boolean' )
			{
				opts = {
					add		: opts,
					fixed	: opts
				};
			}
			if ( typeof opts != 'object' )
			{
				opts = {};
			}
			opts = this.opts[ _ADDON_ ] = $.extend( true, {}, $[ _PLUGIN_ ].defaults[ _ADDON_ ], opts );


			//	Refactor collapsed class
			this.bind( 'init',
				function( $panels )
				{
					this.__refactorClass( $('li', this.$menu), this.conf.classNames[ _ADDON_ ].collapsed, 'collapsed' );
				}
			);


			//	Add dividers
			if ( opts.add )
			{
				this.bind( 'init',
					function( $panels )
					{
						switch( opts.addTo )
						{
							case 'panels':
								var $wrapper = $panels;
								break;
			
							default:
								var $wrapper = $(opts.addTo, this.$menu).filter( '.' + _c.panel );
								break;
						}

						$('.' + _c.divider, $wrapper).remove();
						$wrapper
							.find( '.' + _c.listview )
							.not( '.' + _c.vertical )
							.each(
								function()
								{
									var last = '';
									that.__filterListItems( $(this).children() )
										.each(
											function()
											{
												var crnt = $.trim( $(this).children( 'a, span' ).text() ).slice( 0, 1 ).toLowerCase();
												if ( crnt != last && crnt.length )
												{
													last = crnt;
													$( '<li class="' + _c.divider + '">' + crnt + '</li>' ).insertBefore( this );
												}
											}
										);
								}
							);
					}
				);
			}


			//	Toggle collapsed list items
			if ( opts.collapse )
			{
				this.bind( 'init',
					function( $panels )
					{
						$('.' + _c.divider, $panels )
							.each(
								function()
								{
									var $l = $(this),
										$e = $l.nextUntil( '.' + _c.divider, '.' + _c.collapsed );

									if ( $e.length )
									{
										if ( !$l.children( '.' + _c.subopen ).length )
										{
											$l.wrapInner( '<span />' );
											$l.prepend( '<a href="#" class="' + _c.subopen + ' ' + _c.fullsubopen + '" />' );
										}
									}
								}
							);
					}
				);
			}
			
			
			//	Fixed dividers
			if ( opts.fixed )
			{
				var update = function( $panl )
				{
					$panl = $panl || this.$menu.children( '.' + _c.current );
					var $dvdr = $panl
						.find( '.' + _c.divider )
						.not( '.' + _c.hidden );
	
					if ( $dvdr.length )
					{
						this.$menu.addClass( _c.hasdividers );
	
						var scrl = $panl.scrollTop() || 0,
							text = '';
	
						if ( $panl.is( ':visible' ) )
						{
							$panl
								.find( '.' + _c.divider )
								.not( '.' + _c.hidden )
								.each(
									function()
									{
										if ( $(this).position().top + scrl < scrl + 1 )
										{
											text = $(this).text();
										}
									}
								);
						}
	
						this.$fixeddivider.text( text );
					}
					else
					{
						this.$menu.removeClass( _c.hasdividers );
					}
				};


				//	Add the fixed divider
				this.$fixeddivider = $('<ul class="' + _c.listview + ' ' + _c.fixeddivider + '"><li class="' + _c.divider + '"></li></ul>')
					.prependTo( this.$menu )
					.children();

				this.bind( 'openPanel', update );


				//	Set correct value onScroll
				this.bind( 'init',
					function( $panels )
					{
						$panels
							.off( _e.scroll + '-dividers ' + _e.touchmove + '-dividers' )
							.on( _e.scroll + '-dividers ' + _e.touchmove + '-dividers',
								function( e )
								{
									update.call( that, $(this) );
								}
							)
					}
				);

			}
		},

		//	add: fired once per page load
		add: function()
		{
			_c = $[ _PLUGIN_ ]._c;
			_d = $[ _PLUGIN_ ]._d;
			_e = $[ _PLUGIN_ ]._e;
	
			_c.add( 'collapsed uncollapsed fixeddivider hasdividers' );
			_e.add( 'scroll' );
		},

		//	clickAnchor: prevents default behavior when clicking an anchor
		clickAnchor: function( $a, inMenu )
		{
			if ( this.opts[ _ADDON_ ].collapse && inMenu )
			{
				var $l = $a.parent();
				if ( $l.is( '.' + _c.divider ) )
				{
					var $e = $l.nextUntil( '.' + _c.divider, '.' + _c.collapsed );
			
					$l.toggleClass( _c.opened );
					$e[ $l.hasClass( _c.opened ) ? 'addClass' : 'removeClass' ]( _c.uncollapsed );
					
					return true;
				}
			}
			return false;
		}
	};


	//	Default options and configuration
	$[ _PLUGIN_ ].defaults[ _ADDON_ ] = {
		add			: false,
		addTo		: 'panels',
		fixed		: false,
		collapse	: false
	};
	$[ _PLUGIN_ ].configuration.classNames[ _ADDON_ ] = {
		collapsed: 'Collapsed'
	};


	var _c, _d, _e, glbl;

})( jQuery );/*	
 * jQuery mmenu dragOpen addon
 * mmenu.frebsite.nl
 *
 * Copyright (c) Fred Heusschen
 */

(function( $ ) {

	var _PLUGIN_ = 'mmenu',
		_ADDON_  = 'dragOpen';


	$[ _PLUGIN_ ].addons[ _ADDON_ ] = {

		//	setup: fired once per menu
		setup: function()
		{
			if ( !this.opts.offCanvas )
			{
				return;
			}

			var that = this,
				opts = this.opts[ _ADDON_ ],
				conf = this.conf[ _ADDON_ ];

			glbl = $[ _PLUGIN_ ].glbl;


			//	Extend shortcut options
			if ( typeof opts == 'boolean' )
			{
				opts = {
					open: opts
				};
			}
			if ( typeof opts != 'object' )
			{
				opts = {};
			}
			opts = this.opts[ _ADDON_ ] = $.extend( true, {}, $[ _PLUGIN_ ].defaults[ _ADDON_ ], opts );


			//	Drag open			
			if ( opts.open )
			{

				//	Set up variables
				var drag			= {},
					_stage 			= 0,
					_direction 		= false,
					_dimension		= false,
					_distance 		= 0,
					_maxDistance 	= 0;

				var new_distance, drag_distance, css_value,
					doPanstart, getSlideNodes;

				switch( this.opts.offCanvas.position )
				{
					case 'left':
					case 'right':
						drag.events		= 'panleft panright';
						drag.typeLower	= 'x';
						drag.typeUpper	= 'X';
						
						_dimension		= 'width';
						break;
	
					case 'top':
					case 'bottom':
						drag.events		= 'panup pandown';
						drag.typeLower	= 'y';
						drag.typeUpper	= 'Y';
	
						_dimension = 'height';
						break;
				}

				switch( this.opts.offCanvas.position )
				{	
					case 'right':
					case 'bottom':
						drag.negative 	= true;
						doPanstart		= function( pos )
						{
							if ( pos >= glbl.$wndw[ _dimension ]() - opts.maxStartPos )
							{
								_stage = 1;
							}
						};
						break;
					
					default:
						drag.negative 	= false;
						doPanstart		= function( pos )
						{
							if ( pos <= opts.maxStartPos )
							{
								_stage = 1;
							}
						}
						break;
				}

				switch( this.opts.offCanvas.position )
				{
					case 'left':
						drag.open_dir 	= 'right';
						drag.close_dir 	= 'left';
						break;
	
					case 'right':
						drag.open_dir 	= 'left';
						drag.close_dir 	= 'right';
						break;
	
					case 'top':
						drag.open_dir 	= 'down';
						drag.close_dir 	= 'up';
						break;
	
					case 'bottom':
						drag.open_dir 	= 'up';
						drag.close_dir 	= 'down';
						break;
				}

				switch ( this.opts.offCanvas.zposition )
				{
					case 'front':
						getSlideNodes = function()
						{
							return this.$menu;
						};
						break;
		
					default:
						getSlideNodes = function()
						{
							return $('.' + _c.slideout);
						};
						break;
				};

				var $dragNode = this.__valueOrFn( opts.pageNode, this.$menu, glbl.$page );

				if ( typeof $dragNode == 'string' )
				{
					$dragNode = $($dragNode);
				}


				//	Bind events
				var _hammer = new Hammer( $dragNode[ 0 ], opts.vendors.hammer );

				_hammer
					.on( 'panstart',
						function( e )
						{
							doPanstart( e.center[ drag.typeLower ] );
							glbl.$slideOutNodes = getSlideNodes();
							_direction = drag.open_dir;
						}
					)
					.on( drag.events + ' panend',
						function( e )
						{
							if ( _stage > 0 )
							{
								e.preventDefault();
							}
						}
					)
					.on( drag.events,
						function( e )
						{
	
							new_distance = e[ 'delta' + drag.typeUpper ];
							if ( drag.negative )
							{
								new_distance = -new_distance;
							}

							if ( new_distance != _distance )
							{
								_direction = ( new_distance >= _distance )
									? drag.open_dir
									: drag.close_dir;
							}

							_distance = new_distance;
	
							if ( _distance > opts.threshold )
							{
								if ( _stage == 1 )
								{
									if ( glbl.$html.hasClass( _c.opened ) )
									{
										return;
									}
									_stage = 2;
	
									that._openSetup();
									that.trigger( 'opening' );
									glbl.$html.addClass( _c.dragging );
	
									_maxDistance = minMax( 
										glbl.$wndw[ _dimension ]() * conf[ _dimension ].perc, 
										conf[ _dimension ].min, 
										conf[ _dimension ].max
									);
								}
							}
							if ( _stage == 2 )
							{
								drag_distance = minMax( _distance, 10, _maxDistance ) - ( that.opts.offCanvas.zposition == 'front' ? _maxDistance : 0 );
								if ( drag.negative )
								{
									drag_distance = -drag_distance;
								}
								css_value = 'translate' + drag.typeUpper + '(' + drag_distance + 'px )';
	
								glbl.$slideOutNodes.css({
									'-webkit-transform': '-webkit-' + css_value,	
									'transform': css_value
								});
							}
						}
					)
					.on( 'panend',
						function( e )
						{
							if ( _stage == 2 )
							{
								glbl.$html.removeClass( _c.dragging );
								glbl.$slideOutNodes.css( 'transform', '' );
								that[ _direction == drag.open_dir ? '_openFinish' : 'close' ]();
							}
				        	_stage = 0;
					    }
					);
			}
		},

		//	add: fired once per page load
		add: function()
		{
			if ( typeof Hammer != 'function' || Hammer.VERSION < 2 )
			{
				$[ _PLUGIN_ ].addons[ _ADDON_ ].setup = function() {};
				return;
			}

			_c = $[ _PLUGIN_ ]._c;
			_d = $[ _PLUGIN_ ]._d;
			_e = $[ _PLUGIN_ ]._e;

			_c.add( 'dragging' );
		},

		//	clickAnchor: prevents default behavior when clicking an anchor
		clickAnchor: function( $a, inMenu ) {}
	};


	//	Default options and configuration
	$[ _PLUGIN_ ].defaults[ _ADDON_ ] = {
		open		: false,
//		pageNode	: null,
		maxStartPos	: 100,
		threshold	: 50,
		vendors		: {
			hammer		: {}
		}
	};
	$[ _PLUGIN_ ].configuration[ _ADDON_ ] = {
		width	: {
			perc	: 0.8,
			min		: 140,
			max		: 440
		},
		height	: {
			perc	: 0.8,
			min		: 140,
			max		: 880
		}
	};


	var _c, _d, _e, glbl;


	function minMax( val, min, max )
	{
		if ( val < min )
		{
			val = min;
		}
		if ( val > max )
		{
			val = max;
		}
		return val;
	}

})( jQuery );/*	
 * jQuery mmenu fixedElements addon
 * mmenu.frebsite.nl
 *
 * Copyright (c) Fred Heusschen
 */

(function( $ ) {

	var _PLUGIN_ = 'mmenu',
		_ADDON_  = 'fixedElements';


	$[ _PLUGIN_ ].addons[ _ADDON_ ] = {

		//	setup: fired once per menu
		setup: function()
		{
			if ( !this.opts.offCanvas )
			{
				return;
			}

			var that = this,
				opts = this.opts[ _ADDON_ ],
				conf = this.conf[ _ADDON_ ];

			glbl = $[ _PLUGIN_ ].glbl;


			opts = this.opts[ _ADDON_ ] = $.extend( true, {}, $[ _PLUGIN_ ].defaults[ _ADDON_ ], opts );

			var setPage = function( $page )
			{
				//	Refactor fixed classes
				var _fixd = this.conf.classNames[ _ADDON_ ].fixed;

				this.__refactorClass( $page.find( '.' + _fixd ), _fixd, 'slideout' )
					.appendTo( glbl.$body );
			};
			setPage.call( this, glbl.$page );
			this.bind( 'setPage', setPage );
		},

		//	add: fired once per page load
		add: function()
		{
			_c = $[ _PLUGIN_ ]._c;
			_d = $[ _PLUGIN_ ]._d;
			_e = $[ _PLUGIN_ ]._e;
	
			_c.add( 'fixed' );
		},

		//	clickAnchor: prevents default behavior when clicking an anchor
		clickAnchor: function( $a, inMenu ) {}
	};


	//	Default options and configuration
	$[ _PLUGIN_ ].configuration.classNames[ _ADDON_ ] = {
		fixed 	: 'Fixed'
	};


	var _c, _d, _e, glbl;

})( jQuery );/*	
 * jQuery mmenu iconPanels addon
 * mmenu.frebsite.nl
 *
 * Copyright (c) Fred Heusschen
 */

(function( $ ) {

	var _PLUGIN_ = 'mmenu',
		_ADDON_  = 'iconPanels';


	$[ _PLUGIN_ ].addons[ _ADDON_ ] = {

		//	setup: fired once per menu
		setup: function()
		{
			var that = this,
				opts = this.opts[ _ADDON_ ],
				conf = this.conf[ _ADDON_ ];

			glbl = $[ _PLUGIN_ ].glbl;


			//	Extend shortcut options
			if ( typeof opts == 'boolean' )
			{
				opts = {
					add 	: opts
				};
			}
			if ( typeof opts == 'number' )
			{
				opts = {
					add 	: true,
					visible : opts
				};
			}
			if ( typeof opts != 'object' )
			{
				opts = {};
			}
			opts = this.opts[ _ADDON_ ] = $.extend( true, {}, $[ _PLUGIN_ ].defaults[ _ADDON_ ], opts );
			opts.visible++;


			//	Add the iconbars
			if ( opts.add )
			{

				this.$menu.addClass( _c.iconpanel );

				var clsn = [];
				for ( var i = 0; i <= opts.visible; i++ )
				{
					clsn.push( _c.iconpanel + '-' + i );
				}
				clsn = clsn.join( ' ' );

				var update = function( $panl )
				{
					var $allp = that.$menu
						.children( '.' + _c.panel )
						.removeClass( clsn );

					var $curp = $allp
						.filter( '.' + _c.subopened );

					$curp
						.removeClass( _c.hidden )
						.add( $panl )
						.slice( -opts.visible )
						.each(
							function( x )
							{
								$(this).addClass( _c.iconpanel + '-' + x );
							}
						);
				};

				this.bind( 'openPanel', update );
				this.bind( 'init',
					function( $panels )
					{
						update.call( that, that.$menu.children( '.' + _c.current ) );
						if ( opts.hideNavbars )
						{
							$panels.removeClass( _c.hasnavbar )
						}
						$panels
							.each(
								function()
								{
									if ( !$(this).children( '.' + _c.subblocker ).length )
									{
										$(this).prepend( '<a href="#' + $(this).closest( '.' + _c.panel ).attr( 'id' ) + '" class="' + _c.subblocker + '" />' );
									}
								}
							);
					}
				);
			}
		},

		//	add: fired once per page load
		add: function()
		{
			_c = $[ _PLUGIN_ ]._c;
			_d = $[ _PLUGIN_ ]._d;
			_e = $[ _PLUGIN_ ]._e;
	
			_c.add( 'iconpanel subblocker' );
		},

		//	clickAnchor: prevents default behavior when clicking an anchor
		clickAnchor: function( $a, inMenu ) {}
	};


	//	Default options and configuration
	$[ _PLUGIN_ ].defaults[ _ADDON_ ] = {
		add 		: false,
		visible		: 3,
		hideNavbars	: false
	};


	var _c, _d, _e, glbl;

})( jQuery );/*	
 * jQuery mmenu navbar addon
 * mmenu.frebsite.nl
 *
 * Copyright (c) Fred Heusschen
 */

(function( $ ) {

	var _PLUGIN_ = 'mmenu',
		_ADDON_  = 'navbars';


	$[ _PLUGIN_ ].addons[ _ADDON_ ] = {

		//	setup: fired once per menu
		setup: function()
		{
			var that = this,
				navs = this.opts[ _ADDON_ ],
				conf = this.conf[ _ADDON_ ];

			glbl = $[ _PLUGIN_ ].glbl;

			if ( typeof navs == 'undefined' )
			{
				return;
			}

			if ( !( navs instanceof Array ) )
			{
				navs = [ navs ];
			}

			var _pos = {};

			$.each(
				navs,
				function( n )
				{
				
					var opts = navs[ n ];

					//	Extend shortcut options
					if ( typeof opts == 'boolean' && opts )
					{
						opts = {};
					}
					if ( typeof opts != 'object' )
					{
						opts = {};
					}
					if ( typeof opts.content == 'undefined' )
					{
						opts.content = [ 'prev', 'title' ];
					}
					if ( !( opts.content instanceof Array ) )
					{
						opts.content = [ opts.content ];
					}
					opts = $.extend( true, {}, that.opts.navbar, opts );

					var poss = opts.position,
						hght = opts.height;

					if ( typeof hght != 'number' )
					{
						hght = 1;
					}
					hght = Math.min( 4, Math.max( 1, hght ) );

					if ( poss != 'bottom' )
					{
						poss = 'top';
					}
					if ( !_pos[ poss ] )
					{
						_pos[ poss ] = 0;
					}
					_pos[ poss ]++;


					//	Add markup
					var $navbar = $( '<div />' )
						.addClass( 
							_c.navbar + ' ' +
							_c.navbar + '-' + poss + ' ' +
							_c.navbar + '-' + poss + '-' + _pos[ poss ] + ' ' +
							_c.navbar + '-size-' + hght
						);

					_pos[ poss ] += hght - 1;

					for ( var c = 0, l = opts.content.length; c < l; c++ )
					{
						var ctnt = $[ _PLUGIN_ ].addons[ _ADDON_ ][ opts.content[ c ] ] || false;
						if ( ctnt )
						{
							ctnt.call( that, $navbar, opts, conf );
						}
						else
						{
							ctnt = opts.content[ c ];
							if ( !( ctnt instanceof $ ) )
							{
								ctnt = $( opts.content[ c ] );
							}
							ctnt
								.each(
									function()
									{
										$navbar.append( $(this) );
									}
								);
						}
					}

					var _content = Math.ceil( $navbar.children().not( '.' + _c.btn ).length / hght );
					if ( _content > 1 )
					{
						$navbar.addClass( _c.navbar + '-content-' + _content );
					}
					if ( $navbar.children( '.' + _c.btn ).length )
					{
						$navbar.addClass( _c.hasbtns );
					}
					$navbar.prependTo( that.$menu );
				}
			);

			for ( var poss in _pos )
			{
				that.$menu.addClass( _c.hasnavbar + '-' + poss + '-' + _pos[ poss ] );
			}
		},

		//	add: fired once per page load
		add: function()
		{
			_c = $[ _PLUGIN_ ]._c;
			_d = $[ _PLUGIN_ ]._d;
			_e = $[ _PLUGIN_ ]._e;

			_c.add( 'close hasbtns' );
		},

		//	clickAnchor: prevents default behavior when clicking an anchor
		clickAnchor: function( $a, inMenu ) {}
	};


	//	Default options and configuration
	$[ _PLUGIN_ ].configuration[ _ADDON_ ] = {
		breadcrumbSeparator: '/'
	};
	$[ _PLUGIN_ ].configuration.classNames[ _ADDON_ ] = {
		panelTitle	: 'Title',
		panelNext	: 'Next',
		panelPrev	: 'Prev'
	};


	var _c, _d, _e, glbl;

})( jQuery );/*	
 * jQuery mmenu navbar addon breadcrumbs content
 * mmenu.frebsite.nl
 *
 * Copyright (c) Fred Heusschen
 */

(function( $ ) {

	var _PLUGIN_ 	= 'mmenu',
		_ADDON_  	= 'navbars',
		_CONTENT_	= 'breadcrumbs';

	$[ _PLUGIN_ ].addons[ _ADDON_ ][ _CONTENT_ ] = function( $navbar, opts, conf )
	{
		//	Get vars
		var _c = $[ _PLUGIN_ ]._c,
			_d = $[ _PLUGIN_ ]._d;

		_c.add( 'breadcrumbs separator' );


		//	Add content
		$navbar.append( '<span class="' + _c.breadcrumbs + '"></span>' );
		this.bind( 'init',
			function( $panels )
			{
				$panels
					.removeClass( _c.hasnavbar )
					.each(
						function()
						{
							var crumbs = [],
								$panl = $(this),
								$bcrb = $( '<span class="' + _c.breadcrumbs + '"></span>' ),
								$crnt = $(this).children().first(),
								first = true;

							while ( $crnt && $crnt.length )
							{
								if ( !$crnt.is( '.' + _c.panel ) )
								{
									$crnt = $crnt.closest( '.' + _c.panel );
								}

								var text = $crnt.children( '.' + _c.navbar ).children( '.' + _c.title ).text();
								crumbs.unshift( first
									? '<span>' + text + '</span>'
									: '<a href="#' + $crnt.attr( 'id' ) + '">' + text + '</a>' );

								first = false;
								$crnt = $crnt.data( _d.parent );
							}
							$bcrb
								.append( crumbs.join( '<span class="' + _c.separator + '">' + conf.breadcrumbSeparator + '</span>' ) )
								.appendTo( $panl.children( '.' + _c.navbar ) );
						}
					);
			}
		);


		//	Update
		var update = function()
		{
			var $panl = this.$menu.children( '.' + _c.current );

			var $node = $navbar.find( '.' + _c.breadcrumbs ),
				$bcrb = $panl.children( '.' + _c.navbar ).children( '.' + _c.breadcrumbs );

			$node.html( $bcrb.html() );
		};

		this.bind( 'openPanel', update );
		this.bind( 'init', update );
	};

})( jQuery );/*	
 * jQuery mmenu navbar addon close content
 * mmenu.frebsite.nl
 *
 * Copyright (c) Fred Heusschen
 */

(function( $ ) {

	var _PLUGIN_ 	= 'mmenu',
		_ADDON_  	= 'navbars',
		_CONTENT_	= 'close';

	$[ _PLUGIN_ ].addons[ _ADDON_ ][ _CONTENT_ ] = function( $navbar, opts )
	{
		//	Get vars
		var _c = $[ _PLUGIN_ ]._c,
			glbl = $[ _PLUGIN_ ].glbl;


		//	Add content
		$navbar.append( '<a class="' + _c.close + ' ' + _c.btn + '" href="#"></a>' );


		//	Update
		var setPage = function( $page )
		{
			$navbar
				.find( '.' + _c.close )
				.attr( 'href', '#' + $page.attr( 'id' ) );
		};
		setPage.call( this, glbl.$page );
		this.bind( 'setPage', setPage );
	};

})( jQuery );/*	
 * jQuery mmenu navbar addon next content
 * mmenu.frebsite.nl
 *
 * Copyright (c) Fred Heusschen
 */

(function( $ ) {

	var _PLUGIN_ 	= 'mmenu',
		_ADDON_  	= 'navbars',
		_CONTENT_	= 'next';

	$[ _PLUGIN_ ].addons[ _ADDON_ ][ _CONTENT_ ] = function( $navbar, opts )
	{
		//	Get vars
		var _c = $[ _PLUGIN_ ]._c;


		//	Add content
		$navbar.append( '<a class="' + _c.next + ' ' + _c.btn + '" href="#"></a>' );


		//	Update
		var update = function( $panel )
		{
			$panel = $panel || this.$menu.children( '.' + _c.current );

			var $node = $navbar.find( '.' + _c.next ),
				$orgn = $panel.find( '.' + this.conf.classNames[ _ADDON_ ].panelNext );
			
			var _url = $orgn.attr( 'href' ),
				_txt = $orgn.html();

			$node[ _url ? 'attr' : 'removeAttr' ]( 'href', _url );
			$node[ _url || _txt ? 'removeClass' : 'addClass' ]( _c.hidden );
			$node.html( _txt );
		};

		this.bind( 'openPanel', update );
		this.bind( 'init',
			function()
			{
				update.call( this );
			}
		);
	};

})( jQuery );/*	
 * jQuery mmenu navbar addon prev content
 * mmenu.frebsite.nl
 *
 * Copyright (c) Fred Heusschen
 */

(function( $ ) {

	var _PLUGIN_ 	= 'mmenu',
		_ADDON_  	= 'navbars',
		_CONTENT_	= 'prev';

	$[ _PLUGIN_ ].addons[ _ADDON_ ][ _CONTENT_ ] = function( $navbar, opts )
	{
		//	Get vars
		var _c = $[ _PLUGIN_ ]._c;


		//	Add content
		$navbar.append( '<a class="' + _c.prev + ' ' + _c.btn + '" href="#"></a>' );
		this.bind( 'init',
			function( $panl )
			{
				$panl.removeClass( _c.hasnavbar );
			}
		);

		//	Update
		var update = function()
		{
			var $panl = this.$menu.children( '.' + _c.current );

			var $node = $navbar.find( '.' + _c.prev ),
				$orgn = $panl.find( '.' + this.conf.classNames[ _ADDON_ ].panelPrev );

			if ( !$orgn.length )
			{
				$orgn = $panl.children( '.' + _c.navbar ).children( '.' + _c.prev );
			}
			
			var _url = $orgn.attr( 'href' ),
				_txt = $orgn.html();

			$node[ _url ? 'attr' : 'removeAttr' ]( 'href', _url );
			$node[ _url || _txt ? 'removeClass' : 'addClass' ]( _c.hidden );
			$node.html( _txt );
		};

		this.bind( 'openPanel', update );
		this.bind( 'init', update );
	};

})( jQuery );/*	
 * jQuery mmenu navbar addon searchfield content
 * mmenu.frebsite.nl
 *
 * Copyright (c) Fred Heusschen
 */

(function( $ ) {

	var _PLUGIN_ 	= 'mmenu',
		_ADDON_  	= 'navbars',
		_CONTENT_	= 'searchfield';

	$[ _PLUGIN_ ].addons[ _ADDON_ ][ _CONTENT_ ] = function( $navbar, opts )
	{
		var _c = $[ _PLUGIN_ ]._c;

		var $srch = $('<div class="' + _c.search + '" />')
			.appendTo( $navbar );

		if ( typeof this.opts.searchfield != 'object' )
		{
			this.opts.searchfield = {};
		}
		this.opts.searchfield.add = true;
		this.opts.searchfield.addTo = $srch;
	};

})( jQuery );/*	
 * jQuery mmenu navbar addon title content
 * mmenu.frebsite.nl
 *
 * Copyright (c) Fred Heusschen
 */

(function( $ ) {

	var _PLUGIN_ 	= 'mmenu',
		_ADDON_  	= 'navbars',
		_CONTENT_	= 'title';

	$[ _PLUGIN_ ].addons[ _ADDON_ ][ _CONTENT_ ] = function( $navbar, opts )
	{
		//	Get vars
		var _c = $[ _PLUGIN_ ]._c;


		//	Add content
		$navbar.append( '<a class="' + _c.title + '"></a>' );


		//	Update
		var update = function( $panel )
		{
			$panel = $panel || this.$menu.children( '.' + _c.current );

			var $node = $navbar.find( '.' + _c.title ),
				$orgn = $panel.find( '.' + this.conf.classNames[ _ADDON_ ].panelTitle );

			if ( !$orgn.length )
			{
				$orgn = $panel.children( '.' + _c.navbar ).children( '.' + _c.title );
			}

			var _url = $orgn.attr( 'href' ),
				_txt = $orgn.html() || opts.title;

			$node[ _url ? 'attr' : 'removeAttr' ]( 'href', _url );
			$node[ _url || _txt ? 'removeClass' : 'addClass' ]( _c.hidden );
			$node.html( _txt );
		};

		this.bind( 'openPanel', update );
		this.bind( 'init',
			function()
			{
				update.call( this );
			}
		);
	};

})( jQuery );/*	
 * jQuery mmenu offCanvas addon
 * mmenu.frebsite.nl
 *
 * Copyright (c) Fred Heusschen
 */

(function( $ ) {

	var _PLUGIN_ = 'mmenu',
		_ADDON_  = 'offCanvas';


	$[ _PLUGIN_ ].addons[ _ADDON_ ] = {

		//	setup: fired once per menu
		setup: function()
		{
			if ( !this.opts[ _ADDON_ ] )
			{
				return;
			}

			var that = this,
				opts = this.opts[ _ADDON_ ],
				conf = this.conf[ _ADDON_ ];

			glbl = $[ _PLUGIN_ ].glbl;


			//	Add methods to api
			this._api = $.merge( this._api, [ 'open', 'close', 'setPage' ] );


			//	Debug positioning
			if ( opts.position == 'top' || opts.position == 'bottom' )
			{
				opts.zposition = 'front';
			}


			//	Extend configuration
			if ( typeof conf.pageSelector != 'string' )
			{
				conf.pageSelector = '> ' + conf.pageNodetype;
			}


			glbl.$allMenus = ( glbl.$allMenus || $() ).add( this.$menu );


			//	Setup the menu
			this.vars.opened = false;
			
			var clsn = [ _c.offcanvas ];

			if ( opts.position != 'left' )
			{
				clsn.push( _c.mm( opts.position ) );
			}
			if ( opts.zposition != 'back' )
			{
				clsn.push( _c.mm( opts.zposition ) );
			}

			this.$menu
				.addClass( clsn.join( ' ' ) )
				.parent()
				.removeClass( _c.wrapper );


			//	Setup the page
			this.setPage( glbl.$page );


			//	Setup the UI blocker and the window
			this._initBlocker();
			this[ '_initWindow_' + _ADDON_ ]();


			//	Append to the body
			this.$menu[ conf.menuInjectMethod + 'To' ]( conf.menuWrapperSelector );
		},

		//	add: fired once per page load
		add: function()
		{
			_c = $[ _PLUGIN_ ]._c;
			_d = $[ _PLUGIN_ ]._d;
			_e = $[ _PLUGIN_ ]._e;

			_c.add( 'offcanvas slideout modal background opening blocker page' );
			_d.add( 'style' );
			_e.add( 'resize' );
		},

		//	clickAnchor: prevents default behavior when clicking an anchor
		clickAnchor: function( $a, inMenu )
		{
			if ( !this.opts[ _ADDON_ ] )
			{
				return false;
			}

			//	Open menu
			var id = this.$menu.attr( 'id' );
			if ( id && id.length )
			{
				if ( this.conf.clone )
				{
					id = _c.umm( id );
				}
				if ( $a.is( '[href="#' + id + '"]' ) )
				{
					this.open();
					return true;
				}
			}
			
			//	Close menu
			if ( !glbl.$page )
			{
				return;
			}
			var id = glbl.$page.first().attr( 'id' );
			if ( id && id.length )
			{
				if ( $a.is( '[href="#' + id + '"]' ) )
				{
					this.close();
					return true;
				}
			}

			return false;
		}
	};


	//	Default options and configuration
	$[ _PLUGIN_ ].defaults[ _ADDON_ ] = {
		position		: 'left',
		zposition		: 'back',
		modal			: false,
		moveBackground	: true
	};
	$[ _PLUGIN_ ].configuration[ _ADDON_ ] = {
		pageNodetype		: 'div',
		pageSelector		: null,
		wrapPageIfNeeded	: true,
		menuWrapperSelector	: 'body',
		menuInjectMethod	: 'prepend'
	};


	//	Methods
	$[ _PLUGIN_ ].prototype.open = function()
	{
		if ( this.vars.opened )
		{
			return;
		}

		var that = this;

		this._openSetup();

		//	Without the timeout, the animation won't work because the element had display: none;
		setTimeout(
			function()
			{
				that._openFinish();
			}, this.conf.openingInterval
		);
		this.trigger( 'open' );
	};

	$[ _PLUGIN_ ].prototype._openSetup = function()
	{
		var that = this;

		//	Close other menus
		this.closeAllOthers();

		//	Store style and position
		glbl.$page.each(
			function()
			{
				$(this).data( _d.style, $(this).attr( 'style' ) || '' );
			}
		);

		//	Trigger window-resize to measure height
		glbl.$wndw.trigger( _e.resize + '-offcanvas', [ true ] );

		var clsn = [ _c.opened ];

		//	Add options
		if ( this.opts[ _ADDON_ ].modal )
		{
			clsn.push( _c.modal );
		}
		if ( this.opts[ _ADDON_ ].moveBackground )
		{
			clsn.push( _c.background );
		}
		if ( this.opts[ _ADDON_ ].position != 'left' )
		{
			clsn.push( _c.mm( this.opts[ _ADDON_ ].position ) );
		}
		if ( this.opts[ _ADDON_ ].zposition != 'back' )
		{
			clsn.push( _c.mm( this.opts[ _ADDON_ ].zposition ) );
		}
		if ( this.opts.extensions )
		{
			clsn.push( this.opts.extensions );
		}
		glbl.$html.addClass( clsn.join( ' ' ) );

		//	Open
		setTimeout(function(){
            that.vars.opened = true;
        },this.conf.openingInterval);

		this.$menu.addClass( _c.current + ' ' + _c.opened );
	};

	$[ _PLUGIN_ ].prototype._openFinish = function()
	{
		var that = this;

		//	Callback
		this.__transitionend( glbl.$page.first(),
			function()
			{
				that.trigger( 'opened' );
			}, this.conf.transitionDuration
		);

		//	Opening
		glbl.$html.addClass( _c.opening );
		this.trigger( 'opening' );
	};

	$[ _PLUGIN_ ].prototype.close = function()
	{
		if ( !this.vars.opened )
		{
			return;
		}

		var that = this;

		//	Callback
		this.__transitionend( glbl.$page.first(),
			function()
			{
				that.$menu
					.removeClass( _c.current )
					.removeClass( _c.opened );

				glbl.$html
					.removeClass( _c.opened )
					.removeClass( _c.modal )
					.removeClass( _c.background )
					.removeClass( _c.mm( that.opts[ _ADDON_ ].position ) )
					.removeClass( _c.mm( that.opts[ _ADDON_ ].zposition ) );

				if ( that.opts.extensions )
				{
					glbl.$html.removeClass( that.opts.extensions );
				}

				//	Restore style and position
				glbl.$page.each(
					function()
					{
						$(this).attr( 'style', $(this).data( _d.style ) );
					}
				);

				that.vars.opened = false;
				that.trigger( 'closed' );

			}, this.conf.transitionDuration
		);

		//	Closing
		glbl.$html.removeClass( _c.opening );
		this.trigger( 'close' );
		this.trigger( 'closing' );
	};

	$[ _PLUGIN_ ].prototype.closeAllOthers = function()
	{
		glbl.$allMenus
			.not( this.$menu )
			.each(
				function()
				{
					var api = $(this).data( _PLUGIN_ );
					if ( api && api.close )
					{
						api.close();
					}
				}
			);
	}

	$[ _PLUGIN_ ].prototype.setPage = function( $page )
	{
		var that = this,
			conf = this.conf[ _ADDON_ ];

		if ( !$page || !$page.length )
		{
			$page = glbl.$body.find( conf.pageSelector );
			if ( $page.length > 1 && conf.wrapPageIfNeeded )
			{
				$page = $page.wrapAll( '<' + this.conf[ _ADDON_ ].pageNodetype + ' />' ).parent();
			}
		}

		$page.each(
			function()
			{
				$(this).attr( 'id', $(this).attr( 'id' ) || that.__getUniqueId() );		
			}
		);
		$page.addClass( _c.page + ' ' + _c.slideout );
		glbl.$page = $page;

		this.trigger( 'setPage', $page );
	};

	$[ _PLUGIN_ ].prototype[ '_initWindow_' + _ADDON_ ] = function()
	{
		//	Prevent tabbing
		glbl.$wndw
			.off( _e.keydown + '-offcanvas' )
			.on( _e.keydown + '-offcanvas',
				function( e )
				{
					if ( glbl.$html.hasClass( _c.opened ) )
					{
						if ( e.keyCode == 9 )
						{
							e.preventDefault();
							return false;
						}
					}
				}
			);

		//	Set page min-height to window height
		var _h = 0;
		glbl.$wndw
			.off( _e.resize + '-offcanvas' )
			.on( _e.resize + '-offcanvas',
				function( e, force )
				{
					if ( glbl.$page.length == 1 )
					{
						if ( force || glbl.$html.hasClass( _c.opened ) )
						{
							var nh = glbl.$wndw.height();
							if ( force || nh != _h )
							{
								_h = nh;
								glbl.$page.css( 'minHeight', nh );
							}
						}
					}
				}
			);
	};

	$[ _PLUGIN_ ].prototype._initBlocker = function()
	{
		var that = this;

		if ( !glbl.$blck )
		{
			glbl.$blck = $( '<div id="' + _c.blocker + '" class="' + _c.slideout + '" />' );
		}

		glbl.$blck
			.appendTo( glbl.$body )
			.off( _e.touchstart + '-offcanvas ' + _e.touchmove + '-offcanvas' )
			.on( _e.touchstart + '-offcanvas ' + _e.touchmove + '-offcanvas',
				function( e )
				{
					e.preventDefault();
					e.stopPropagation();
					glbl.$blck.trigger( _e.mousedown + '-offcanvas' );
				}
			)
			.off( _e.mousedown + '-offcanvas' )
			.on( _e.mousedown + '-offcanvas',
				function( e )
				{
					e.preventDefault();
					if ( !glbl.$html.hasClass( _c.modal ) )
					{
						that.closeAllOthers();
						that.close();
					}
				}
			);
	};


	var _c, _d, _e, glbl;

})( jQuery );/*	
 * jQuery mmenu searchfield addon
 * mmenu.frebsite.nl
 *
 * Copyright (c) Fred Heusschen
 */

(function( $ ) {

	var _PLUGIN_ = 'mmenu',
		_ADDON_  = 'searchfield';


	$[ _PLUGIN_ ].addons[ _ADDON_ ] = {

		//	setup: fired once per menu
		setup: function()
		{
			var that = this,
				opts = this.opts[ _ADDON_ ],
				conf = this.conf[ _ADDON_ ];

			glbl = $[ _PLUGIN_ ].glbl;


			//	Extend shortcut options
			if ( typeof opts == 'boolean' )
			{
				opts = {
					add: opts
				};
			}
			if ( typeof opts != 'object' )
			{
				opts = {};
			}
			opts = this.opts[ _ADDON_ ] = $.extend( true, {}, $[ _PLUGIN_ ].defaults[ _ADDON_ ], opts );

			this.bind(
				'close',
				function()
				{
					this.$menu
						.find( '.' + _c.search )
						.find( 'input' )
						.blur();
				}
			);


			//	Bind functions to update
			this.bind( 'init',
				function( $panels )
				{
					//	Add the searchfield(s)
					if ( opts.add )
					{
						switch( opts.addTo )
						{
							case 'panels':
								var $wrapper = $panels;
								break;

							default:
								var $wrapper = $(opts.addTo, this.$menu);
								break;
						}

						$wrapper
							.each(
								function()
								{
									//	Add the searchfield
									var $panl = $(this);
									if ( $panl.is( '.' + _c.panel ) && $panl.is( '.' + _c.vertical ) )
									{
										return;
									}

									if ( !$panl.children( '.' + _c.search ).length )
									{
										var _srch = ( conf.form ) 
											? 'form'
											: 'div';

										var $srch = $( '<' + _srch + ' class="' + _c.search + '" />' );
			
										if ( conf.form && typeof conf.form == 'object' )
										{
											for ( var f in conf.form )
											{
												$srch.attr( f, conf.form[ f ] );
											}
										}
										$srch.append( '<input placeholder="' + opts.placeholder + '" type="text" autocomplete="off" />' );

										if ( $panl.hasClass( _c.search ) )
										{
											$panl.replaceWith( $srch );
										}
										else
										{
											$panl
												.prepend( $srch )
												.addClass( _c.hassearch );
										}
									}

									if ( opts.noResults )
									{
										var inPanel = $panl.closest( '.' + _c.panel ).length;

										//	Not in a panel
										if ( !inPanel )
										{
											$panl = that.$menu.children( '.' + _c.panel ).first();
										}

										//	Add no-results message
										if ( !$panl.children( '.' + _c.noresultsmsg ).length )
										{
											var $lst = $panl.children( '.' + _c.listview ).first();

											$( '<div class="' + _c.noresultsmsg + '" />' )
												.append( opts.noResults )
												[ $lst.length ? 'insertAfter' : 'prependTo' ]( $lst.length ? $lst : $panl );
										}
									}
								}
						);


						//	Search through list items
						if ( opts.search )
						{
							$('.' + _c.search, this.$menu)
								.each(
									function()
									{
										var $srch 	= $(this),
											inPanel = $srch.closest( '.' + _c.panel ).length;

										//	In a panel
										if ( inPanel )
										{
											var $pnls = $srch.closest( '.' + _c.panel ),
												$panl = $pnls;
										}

										//	Not in a panel
										else
										{
											var $pnls = $('.' + _c.panel, that.$menu),
												$panl = that.$menu;
										}

										var $inpt = $srch.children( 'input' ),
											$itms = that.__findAddBack( $pnls, '.' + _c.listview ).children( 'li' ),
											$dvdr = $itms.filter( '.' + _c.divider ),
											$rslt = that.__filterListItems( $itms );

										var _anchor = '> a',
											_both = _anchor + ', > span';

										var search = function()
										{

											var query = $inpt.val().toLowerCase();

											//	Scroll to top
											$pnls.scrollTop( 0 );
				
											//	Search through items
											$rslt
												.add( $dvdr )
												.addClass( _c.hidden )
												.find( '.' + _c.fullsubopensearch )
												.removeClass( _c.fullsubopen )
												.removeClass( _c.fullsubopensearch );

											$rslt
												.each(
													function()
													{
														var $item = $(this),
															_search = _anchor;

														if ( opts.showTextItems || ( opts.showSubPanels && $item.find( '.' + _c.next ) ) )
														{
															_search = _both;
														}

														if ( $(_search, $item).text().toLowerCase().indexOf( query ) > -1 )
														{
															$item.add( $item.prevAll( '.' + _c.divider ).first() ).removeClass( _c.hidden );
														}
													}
												);

											//	Update sub items
											if ( opts.showSubPanels )
											{
												$pnls.each(
													function( i )
													{
														var $panl = $(this);
														that.__filterListItems( $panl.find( '.' + _c.listview ).children() )
															.each(
																function()
																{
																	var $li = $(this),
																		$su = $li.data( _d.sub );

																	$li.removeClass( _c.nosubresults );
																	if ( $su )
																	{
																		$su.find( '.' + _c.listview ).children().removeClass( _c.hidden );
																	}
																}
															);
													}
												);
											}

											//	Update parent for submenus
											$( $pnls.get().reverse() )
												.each(
													function( i )
													{
														var $panl = $(this),
															$prnt = $panl.data( _d.parent );

														if ( $prnt )
														{
															if ( that.__filterListItems( $panl.find( '.' + _c.listview ).children() ).length )
															{
																if ( $prnt.hasClass( _c.hidden ) )
																{
																	$prnt.children( '.' + _c.next )
																		.not( '.' + _c.fullsubopen )
																		.addClass( _c.fullsubopen )
																		.addClass( _c.fullsubopensearch );
																}
																$prnt
																	.removeClass( _c.hidden )
																	.removeClass( _c.nosubresults )
																	.prevAll( '.' + _c.divider )
																	.first()
																	.removeClass( _c.hidden );
															}
															else if ( !inPanel )
															{
																if ( $panl.hasClass( _c.opened ) )
																{
																	//	Compensate the timeout for the opening animation
																	setTimeout(
																		function()
																		{
																			that.openPanel( $prnt.closest( '.' + _c.panel ) );
																		}, ( i + 1 ) * ( that.conf.openingInterval * 1.5 )
																	);
																}
																$prnt.addClass( _c.nosubresults );
															}
														}
													}
												);
		
											//	Show/hide no results message
											$panl[ $rslt.not( '.' + _c.hidden ).length ? 'removeClass' : 'addClass' ]( _c.noresults );

											// Update for other addons
											this.update();
										}


										$inpt
											.off( _e.keyup + '-searchfield ' + _e.change + '-searchfield' )
											.on( _e.keyup + '-searchfield',
												function( e )
												{
													if ( !preventKeypressSearch( e.keyCode ) )
													{
														search.call( that );
													}
												}
											)
											.on( _e.change + '-searchfield',
												function( e )
												{
													search.call( that );
												}
											);
									}
								);
						}
					}
				}
		 	);
		},

		//	add: fired once per page load
		add: function()
		{
			_c = $[ _PLUGIN_ ]._c;
			_d = $[ _PLUGIN_ ]._d;
			_e = $[ _PLUGIN_ ]._e;

			_c.add( 'search hassearch noresultsmsg noresults nosubresults fullsubopensearch' );
			_e.add( 'change keyup' );
		},

		//	clickAnchor: prevents default behavior when clicking an anchor
		clickAnchor: function( $a, inMenu ) {}
	};


	//	Default options and configuration
	$[ _PLUGIN_ ].defaults[ _ADDON_ ] = {
		add 			: false,
		addTo			: 'panels',
		search			: true,
		placeholder		: 'Search',
		noResults		: 'No results found.',
		showTextItems	: false,
		showSubPanels	: true
	};
	$[ _PLUGIN_ ].configuration[ _ADDON_ ] = {
		form			: false
	};
	

	var _c, _d, _e, glbl;


	function preventKeypressSearch( c )
	{
		switch( c )
		{
			case 9:		//	tab
			case 16:	//	shift
			case 17:	//	control
			case 18:	//	alt
			case 37:	//	left
			case 38:	//	top
			case 39:	//	right
			case 40:	//	bottom
				return true;
		}
		return false;
	}

})( jQuery );/*	
 * jQuery mmenu sectionIndexer addon
 * mmenu.frebsite.nl
 *
 * Copyright (c) Fred Heusschen
 */

(function( $ ) {

	var _PLUGIN_ = 'mmenu',
		_ADDON_  = 'sectionIndexer';


	$[ _PLUGIN_ ].addons[ _ADDON_ ] = {

		//	setup: fired once per menu
		setup: function()
		{
			var that = this,
				opts = this.opts[ _ADDON_ ],
				conf = this.conf[ _ADDON_ ];

			glbl = $[ _PLUGIN_ ].glbl;


			//	Extend shortcut options
			if ( typeof opts == 'boolean' )
			{
				opts = {
					add: opts
				};
			}
			if ( typeof opts != 'object' )
			{
				opts = {};
			}
			opts = this.opts[ _ADDON_ ] = $.extend( true, {}, $[ _PLUGIN_ ].defaults[ _ADDON_ ], opts );


			this.bind( 'init',
				function( $panels )
				{
					//	Set the panel(s)
					if ( opts.add )
					{
						switch( opts.addTo )
						{
							case 'panels':
								var $wrapper = $panels;
								break;
			
							default:
								var $wrapper = $(opts.addTo, this.$menu).filter( '.' + _c.panel );
								break;
						}

						$wrapper
							.find( '.' + _c.divider )
							.closest( '.' + _c.panel )
							.addClass( _c.hasindexer );
					}


					//	Add the indexer, only if it does not allready excists
					if ( !this.$indexer && 
						this.$menu.children( '.' + _c.hasindexer ).length
					) {
						this.$indexer = $( '<div class="' + _c.indexer + '" />' )
							.prependTo( this.$menu )
							.append( 
								'<a href="#a">a</a>' +
								'<a href="#b">b</a>' +
								'<a href="#c">c</a>' +
								'<a href="#d">d</a>' +
								'<a href="#e">e</a>' +
								'<a href="#f">f</a>' +
								'<a href="#g">g</a>' +
								'<a href="#h">h</a>' +
								'<a href="#i">i</a>' +
								'<a href="#j">j</a>' +
								'<a href="#k">k</a>' +
								'<a href="#l">l</a>' +
								'<a href="#m">m</a>' +
								'<a href="#n">n</a>' +
								'<a href="#o">o</a>' +
								'<a href="#p">p</a>' +
								'<a href="#q">q</a>' +
								'<a href="#r">r</a>' +
								'<a href="#s">s</a>' +
								'<a href="#t">t</a>' +
								'<a href="#u">u</a>' +
								'<a href="#v">v</a>' +
								'<a href="#w">w</a>' +
								'<a href="#x">x</a>' +
								'<a href="#y">y</a>' +
								'<a href="#z">z</a>' );

						//	Scroll onMouseOver
						this.$indexer
							.children()
							.on( _e.mouseover + '-sectionindexer ' + _c.touchstart + '-sectionindexer',
								function( e )
								{
									var lttr = $(this).attr( 'href' ).slice( 1 ),
										$panl = that.$menu.children( '.' + _c.current ),
										$list = $panl.find( '.' + _c.listview );

									var newTop = false,
										oldTop = $panl.scrollTop(),
										lstTop = $list.position().top + parseInt( $list.css( 'margin-top' ), 10 ) + parseInt( $list.css( 'padding-top' ), 10 ) + oldTop;

									$panl.scrollTop( 0 );
									$list
										.children( '.' + _c.divider )
										.not( '.' + _c.hidden )
										.each(
											function()
											{
												if ( newTop === false &&
													lttr == $(this).text().slice( 0, 1 ).toLowerCase()
												) {
													newTop = $(this).position().top + lstTop;
												}
											}
										);

									$panl.scrollTop( newTop !== false ? newTop : oldTop );
								}
							);


						//	Show or hide the indexer
						var update = function( $panl )
						{
							that.$menu[ ( $panl.hasClass( _c.hasindexer ) ? 'add' : 'remove' ) + 'Class' ]( _c.hasindexer );
						};

						this.bind( 'openPanel', update );
						update.call( this, this.$menu.children( '.' + _c.current ) );
					}
				}
			);
		},

		//	add: fired once per page load
		add: function()
		{
			_c = $[ _PLUGIN_ ]._c;
			_d = $[ _PLUGIN_ ]._d;
			_e = $[ _PLUGIN_ ]._e;

			_c.add( 'indexer hasindexer' );
			_e.add( 'mouseover touchstart' );
		},
		
		//	clickAnchor: prevents default behavior when clicking an anchor
		clickAnchor: function( $a, inMenu )
		{
			if ( $a.parent().is( '.' + _c.indexer ) )
			{
				return true;
			}
		}
	};


	//	Default options and configuration
	$[ _PLUGIN_ ].defaults[ _ADDON_ ] = {
		add		: false,
		addTo	: 'panels'
	};


	var _c, _d, _e, glbl;

})( jQuery );/*	
 * jQuery mmenu swipeClose addon
 * mmenu.frebsite.nl
 *
 * Copyright (c) Fred Heusschen
 */

(function( $ ) {

	var _PLUGIN_ = 'mmenu',
		_ADDON_  = 'swipeClose';


	$[ _PLUGIN_ ].addons[ _ADDON_ ] = {

		//	setup: fired once per menu
		setup: function()
		{
			var that = this,
				opts = this.opts[ _ADDON_ ],
				conf = this.conf[ _ADDON_ ];

			glbl = $[ _PLUGIN_ ].glbl;

			//	Extend shortcut options
			if ( typeof opts == 'boolean' )
			{
				opts = {
					close: opts
				};
			}
			if ( typeof opts != 'object' )
			{
				opts = {};
			}
			opts = this.opts[ _ADDON_ ] = $.extend( true, {}, $[ _PLUGIN_ ].defaults[ _ADDON_ ], opts );


			//	Swipe close
			if ( opts.close )
			{

				//	Set up variables
				var closeGesture, prevGesture;

				switch( this.opts.offCanvas.position )
				{
					case 'left':
						closeGesture = 'swipeleft';
						break;
	
					case 'right':
						closeGesture = 'swiperight';
						break;
	
					case 'top':
						closeGesture = 'swipeup';
						break;
	
					case 'bottom':
						closeGesture = 'swipedown';
						break;
				}

				if ( this.opts.extensions.indexOf( 'mm-leftsubpanel' ) != -1 )
				{
					prevGesture = 'swipeleft';
				} else {
					prevGesture = 'swiperight';
				}

				//	Bind events
				var _hammer = new Hammer( this.$menu[0], opts.vendors.hammer );
				_hammer
					.on( closeGesture,
						function( e )
						{
							if ( that.opts.offCanvas ) {
								var prev = that.$menu.find('.' + _c.prev + ':visible');
								if (prev.length == 0) that.close();
								else if (closeGesture != prevGesture) that.close();
							}
						}
					)
					.on( prevGesture,
						function( e )
						{
							var prev = that.$menu.find('.' + _c.prev + ':visible');
							if (prev.length > 0) prev.click();
						}
					);
			}
		},

		//	add: fired once per page load
		add: function()
		{
			if ( typeof Hammer != 'function' || Hammer.VERSION < 2 )
			{
				$[ _PLUGIN_ ].addons[ _ADDON_ ].setup = function() {};
				return;
			}

			_c = $[ _PLUGIN_ ]._c;
			_d = $[ _PLUGIN_ ]._d;
			_e = $[ _PLUGIN_ ]._e;
		},

		//	clickAnchor: prevents default behavior when clicking an anchor
		clickAnchor: function( $a, inMenu ) {}
	};


	//	Default options and configuration
	$[ _PLUGIN_ ].defaults[ _ADDON_ ] = {
		close		: false,
		vendors		: {
			hammer		: {}
		}
	};
	$[ _PLUGIN_ ].configuration[ _ADDON_ ] = {
	};


	var _c, _d, _e, glbl;

})( jQuery );
/*	
 * jQuery mmenu toggles addon
 * mmenu.frebsite.nl
 *
 * Copyright (c) Fred Heusschen
 */

(function( $ ) {

	var _PLUGIN_ = 'mmenu',
		_ADDON_  = 'toggles';


	$[ _PLUGIN_ ].addons[ _ADDON_ ] = {

		//	setup: fired once per menu
		setup: function()
		{
			var that = this,
				opts = this.opts[ _ADDON_ ],
				conf = this.conf[ _ADDON_ ];

			glbl = $[ _PLUGIN_ ].glbl;


			this.bind( 'init',
				function( $panels )
				{

					//	Refactor toggle classes
					this.__refactorClass( $('input', $panels), this.conf.classNames[ _ADDON_ ].toggle, 'toggle' );
					this.__refactorClass( $('input', $panels), this.conf.classNames[ _ADDON_ ].check, 'check' );
			

					//	Add markup
					$('input.' + _c.toggle + ', input.' + _c.check, $panels)
						.each(
							function()
							{
								var $inpt = $(this),
									$prnt = $inpt.closest( 'li' ),
									cl = $inpt.hasClass( _c.toggle ) ? 'toggle' : 'check',
									id = $inpt.attr( 'id' ) || that.__getUniqueId();

								if ( !$prnt.children( 'label[for="' + id + '"]' ).length )
								{
									$inpt.attr( 'id', id );
									$prnt.prepend( $inpt );
			
									$('<label for="' + id + '" class="' + _c[ cl ] + '"></label>')
										.insertBefore( $prnt.children( 'a, span' ).last() );
								}
							}
						);
				}
			);
		},

		//	add: fired once per page load
		add: function()
		{
			_c = $[ _PLUGIN_ ]._c;
			_d = $[ _PLUGIN_ ]._d;
			_e = $[ _PLUGIN_ ]._e;
	
			_c.add( 'toggle check' );
		},

		//	clickAnchor: prevents default behavior when clicking an anchor
		clickAnchor: function( $a, inMenu ) {}
	};


	//	Default options and configuration
	$[ _PLUGIN_ ].configuration.classNames[ _ADDON_ ] = {
		toggle	: 'Toggle',
		check	: 'Check'
	};


	var _c, _d, _e, glbl;

})( jQuery );
