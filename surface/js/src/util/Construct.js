clazz = {};
clazz.define = function( moduleName, deps, callback ) {
	var module;
	var args = [];
	args.push( deps );
	var result = callback ? callback.apply( null, args ) : undefined;
};

clazz.simpleExtend = function( d, s ) {
	for ( var prop in s ) {
		d[ prop ] = s[ prop ];
	}
	return d;
};

clazz.makeArray = function( arr ) {
	var ret = [];
	var keys = Object.keys( arr );
	for ( i = 0, len = keys.length; i < len; i++ ) {
		var key = keys[ i ];
		ret[ i ] = arr[ key ];
	}
	return ret;
};

clazz.underscore = function( s ) {
	var strColons = /\=\=/;
	var strWords = /([A-Z]+)([A-Z][a-z])/g;
	strLowUp = /([a-z\d])([A-Z])/g;
	strDash = /([a-z\d])([A-Z])/g;
	strQuote = /"/g;
	strSingleQuote = /'/g;
	return s.replace( strColons, '/' ).replace( strWords, '$1_$2' ).replace( strLowUp, '$1_$2' ).replace( strDash, '_' ).toLowerCase();
};

clazz.getNext = function( obj, prop, add ) {
	var result = obj[ prop ];
	if ( result === undefined && add === true ) {
		result = obj[ prop ] = {};
	}
	return result;
};

clazz.isContainer = function( current ) {
	return /^f|^o/.test( typeof current );
};

clazz.getGlobal = function(){
	try{
		global = window;
	}catch( e){
	}
	return global;
};


clazz.getObject = function( name, roots, add ) {
	var parts = name ? name.split( '.' ) : [],
		length = parts.length,
		current, r = 0,
		i, container, rootsLength;
	roots = [ roots || clazz.getGlobal() ];
	rootsLength = roots.length;
	if ( !length ) {
		return roots[ 0 ];
	}
	for ( r; r < rootsLength; r++ ) {
		current = roots[ r ];
		container = undefined;
		for ( i = 0; i < length && clazz.isContainer( current ); i++ ) {
			container = current;
			current = clazz.getNext( container, parts[ i ] );
		}
		if ( container !== undefined && current !== undefined ) {
			break;
		}
	}
	if ( add === false && current !== undefined ) {
		delete container[ parts[ i - 1 ] ];
	}
	if ( add === true && current === undefined ) {
		current = roots[ 0 ];
		for ( i = 0; i < length && clazz.isContainer( current ); i++ ) {
			current = clazz.getNext( current, parts[ i ], true );
		}
	}
	return current;
};

clazz.define( 'clazz/construct/construct', clazz, function( clazz ) {
	var initializing = 0;
	var canGetDescriptor;
	try {
		Object.getOwnPropertyDescriptor( {} );
		canGetDescriptor = true;
	} catch ( e ) {
		canGetDescriptor = false;
	}
	var getDescriptor = function( newProps, name ) {
		var descriptor = Object.getOwnPropertyDescriptor( newProps, name );
		if ( descriptor && ( descriptor.get || descriptor.set ) ) {
			return descriptor;
		}
		return null;
	};
	var inheritGetterSetter = function( newProps, oldProps, addTo ) {
		addTo = addTo || newProps;
		var descriptor;
		for ( var name in newProps ) {
			if ( descriptor = getDescriptor( newProps, name ) ) {
				this._defineProperty( addTo, oldProps, name, descriptor );
			} else {
				clazz.construct._overwrite( addTo, oldProps, name, newProps[ name ] );
			}
		}
	};
	var simpleInherit = function( newProps, oldProps, addTo ) {
		addTo = addTo || newProps;
		for ( var name in newProps ) {
			clazz.construct._overwrite( addTo, oldProps, name, newProps[ name ] );
		}
	};

	clazz.construct = function() {
		if ( arguments.length ) {
			return clazz.construct.extend.apply( clazz.construct, arguments );
		}
	};
	clazz.simpleExtend( clazz.construct, {
		constructorExtends: true,
		newInstance: function() {
			var inst = this.instance(),
				args;
			if ( inst.setup ) {
				args = inst.setup.apply( inst, arguments );
			}
			if ( inst.init ) {
				inst.init.apply( inst, args || arguments );
			}
			return inst;
		},
		_inherit: canGetDescriptor ? inheritGetterSetter : simpleInherit,
		_defineProperty: function( what, oldProps, propName, descriptor ) {
			Object.defineProperty( what, propName, descriptor );
		},
		_overwrite: function( what, oldProps, propName, val ) {
			what[ propName ] = val;
		},
		setup: function( base, fullName ) {
			this.defaults = clazz.simpleExtend( true, {}, base.defaults, this.defaults );
		},
		instance: function() {
			initializing = 1;
			var inst = new this();
			initializing = 0;
			return inst;
		},
		extend: function( name, staticProperties, instanceProperties ) {
			clazz.extend = this;
			var fullName = name,
				klass = staticProperties,
				proto = instanceProperties;
			if ( typeof fullName !== 'string' ) {
				proto = klass;
				klass = fullName;
				fullName = null;
			}
			if ( !proto ) {
				proto = klass;
				klass = null;
			}
			proto = proto || {};
			var _super_class = this,
				_super = this.prototype,
				Constructor, parts, current, _fullName, _shortName, propName, shortName, namespace, prototype;
			prototype = this.instance();
			clazz.construct._inherit( proto, _super, prototype );
			if ( fullName ) {
				parts = fullName.split( '.' );
				shortName = parts.pop();
			}
			if ( typeof constructorName === 'undefined' ) {
				Constructor = function() {
					return init.apply( this, arguments );
				};
			}

			function init() {
				if ( !initializing ) {
					return this.constructor !== Constructor && arguments.length && Constructor.constructorExtends ? Constructor.extend.apply( Constructor, arguments ) : Constructor.newInstance.apply( Constructor, arguments );
				}
			}
			for ( propName in _super_class ) {
				if ( _super_class.hasOwnProperty( propName ) ) {
					Constructor[ propName ] = _super_class[ propName ];
				}
			}
			clazz.construct._inherit( klass, _super_class, Constructor );
			if ( fullName ) {
				current = clazz.getObject( parts.join( '.' ), clazz.getGlobal(), true );
				namespace = current;
				_fullName = clazz.underscore( fullName.replace( /\./g, '_' ) );
				_shortName = clazz.underscore( shortName );
				current[ shortName ] = Constructor;
			}
			clazz.simpleExtend( Constructor, {
				constructor: Constructor,
				prototype: prototype,
				namespace: namespace,
				_shortName: _shortName,
				fullName: fullName,
				_fullName: _fullName
			} );
			if ( shortName !== undefined ) {
				Constructor.shortName = shortName;
			}
			Constructor.prototype.constructor = Constructor;
			var t = [ _super_class ].concat( clazz.makeArray( arguments ) ),
				args = Constructor.setup.apply( Constructor, t );
			if ( Constructor.init ) {
				Constructor.init.apply( Constructor, args || t );
			}
			return Constructor;
		}
	} );
	clazz.construct.prototype.setup = function() {};
	clazz.construct.prototype.init = function() {};
	return clazz.construct;
} );
