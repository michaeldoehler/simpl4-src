/*
 * This file is part of SIMPL4(http://simpl4.org).
 *
 *  Copyright [2014] [Manfred Sattler] <manfred@ms123.org>
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
 */
can.Construct.extend( "simpl4.util.Rx", {
	observableFromWebSocket: function( url, openObserver, closingObserver ) {
		if ( !WebSocket ) {
			throw new TypeError( 'WebSocket not implemented in your runtime.' );
		}

		var socket;

		var socketClose = function( code, reason ) {
			console.log( "socketClose:" + code + "/" + reason + "/" + socket + "/" + closingObserver );
			if ( socket ) {
				if ( closingObserver ) {
					closingObserver.onNext();
					closingObserver.onCompleted();
				}
				if ( !code ) {
					socket.close();
				} else {
					socket.close( code, reason );
				}
			}
		};

		var observable = new Rx.AnonymousObservable( function( obs ) {
			socket = new ReconnectingWebSocket( url, null, {
				debug: false,
				reconnectInterval: 50
			} );

			var openHandler = function( e ) {
				openObserver.onNext( e );
				openObserver.onCompleted();
				socket.removeEventListener( 'open', openHandler, false );
			};
			var messageHandler = function( e ) {
				obs.onNext( e );
			};
			var errHandler = function( e ) {
				obs.onError( e );
			};
			var closeHandler = function( e ) {
				console.log( "closeHandler.code", e.code );
				if ( e.code == null ) return;
				if ( e.code === 4001 ) {
					obs.onCompleted();
				} else if ( e.code !== 1000 || !e.wasClean ) {
					return obs.onError( e );
				} else {
					obs.onCompleted();
				}
			};

			openObserver && socket.addEventListener( 'open', openHandler, false );
			socket.addEventListener( 'message', messageHandler, false );
			socket.addEventListener( 'error', errHandler, false );
			socket.addEventListener( 'close', closeHandler, false );

			return function() {
				socketClose();

				socket.removeEventListener( 'message', messageHandler, false );
				socket.removeEventListener( 'error', errHandler, false );
				socket.removeEventListener( 'close', closeHandler, false );
			};
		} );

		var observer = Rx.Observer.create( function( data ) {
				socket.readyState === WebSocket.OPEN && socket.send( data );
			},
			function( e ) {
				if ( !e.code ) {
					throw new Error( 'no code specified. be sure to pass { code: ###, reason: "" } to onError()' );
				}

				socketClose( e.code, e.reason || '' );
			},
			function() {
				socketClose( 1000, '' );
			} );

		return Rx.Subject.create( observer, observable );
	}
}, {} );
