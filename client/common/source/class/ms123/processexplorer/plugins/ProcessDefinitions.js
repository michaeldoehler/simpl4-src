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
qx.Class.define( "ms123.processexplorer.plugins.ProcessDefinitions", {
	extend: qx.ui.container.Composite,
	include: [ qx.locale.MTranslation ],

	/**
	 * Constructor
	 */
	construct: function( facade ) {
		this.base( arguments );
		this.facade = facade;
		this.setLayout( new qx.ui.layout.Dock() );
		this._namespace = facade.storeDesc.getNamespace();

		this.SELECT_COL = 0;
		this.ID_COL = 1;
		this.KEY_COL = 2;
		this.START_COL = 4;
		this._createSearchPanel();
		this._createDefinitionTable();
		this._createToolbar();
		this._onlyLatest = true;
		this._key = "";
		this._id = "";
		this._refreshDefinitions();
		this.facade.registerOnEvent( ms123.processexplorer.Config.EVENT_PROCESSDEPLOYMENT_CHANGED, this._handleEvent.bind( this ) );
	},

	/**
	 * ****************************************************************************
	 * MEMBERS
	 * ****************************************************************************
	 */
	members: {
		_createSearchPanel: function() {
			var formData = {
				"onlyLatest": {
					'type': "CheckBox",
					'label': this.tr( "processexplorer.definition.onlyLatest" ),
					'value': true
				},
				"id": {
					'type': "TextField",
					'label': "Id",
					'value': ""
				},
				"key": {
					'type': "TextField",
					'label': this.tr( "processexplorer.definition.key" ),
					'value': ""
				}
			};

			var self = this;
			var form = new ms123.form.Form( {
				"tabs": [ {
					id: "tab1",
					layout: "single"
				} ],
				"useScroll": false,
				"formData": formData,
				"buttons": [],
				"inWindow": false,
				"callback": function( m, v ) {
					console.error( "callback:" + m + "/" + v );
				},
				"context": self
			} );
			this._searchForm = form;
			this.add( form, {
				edge: "north"
			} );
			var m = form.getModel();
			form.setData( {
				onlyLatest: true
			} );
			m.addListener( "changeBubble", this.__changeListener, this );
			return form;
		},
		__changeListener: function() {
			var m = this._searchForm.getModel();
			var props = qx.Class.getProperties( m.constructor );
			var items = this._searchForm.getItems();
			var map = {};
			for ( var i = 0, l = props.length; i < l; i++ ) {
				var p = props[ i ];
				map[ p ] = m.get( p );
			}
			var onlyLatest = m.get( "onlyLatest" );
			if ( onlyLatest != this._onlyLatest ) {
				this._onlyLatest = onlyLatest;
				this._refreshDefinitions();
			}
			var id = m.get( "id" );
			console.log( "id:" + id );
			if ( id != this._id ) {
				this._id = id;
				this._tableModel.updateView( this._views[ "SearchAsYouType" ].id );
				this._tableModel.setView( this._views[ "SearchAsYouType" ].id );
			}
			var key = m.get( "key" );
			if ( key != this._key ) {
				this._key = key;
				this._tableModel.updateView( this._views[ "SearchAsYouType" ].id );
				this._tableModel.setView( this._views[ "SearchAsYouType" ].id );
			}

		},
		_createDefinitionTable: function() {
			var colIds = new Array();
			var colHds = new Array();
			var colWidth = new Array();
			colIds.push( "selected" );
			colWidth.push( 20 );
			colHds.push( "" );

			//colIds.push("category");
			//colWidth.push("20%");
			//colHds.push(this.tr("processexplorer.definition.category"));

			colIds.push( "id" );
			colWidth.push( "4*" );
			colHds.push( "Id" );

			colIds.push( "key" );
			colWidth.push( "2*" );
			colHds.push( this.tr( "processexplorer.definition.key" ) );

			colIds.push( "version" );
			colWidth.push( 20 );
			colHds.push( "V" );

			//	colIds.push("history");
			//	colWidth.push(30);
			//	colHds.push("");

			colIds.push( "start" );
			colWidth.push( 30 );
			colHds.push( "" );

			this._tableModel = this._createTableModel();
			this._tableModel.setColumns( colHds, colIds );
			var customMap = {
				tableColumnModel: function( obj ) {
					return new qx.ui.table.columnmodel.Resize( obj );
				}
			};
			var table = new qx.ui.table.Table( this._tableModel, customMap );
			table.highlightFocusedRow( false );
			table.addListener( "cellTap", this.onCellClick, this, false );
			var tcm = table.getTableColumnModel();


			this._booleanCols = [];
			var booleanCellRendererFactory = new qx.ui.table.cellrenderer.Dynamic( this._booleanCellRendererFactoryFunc );
			var booleanCellEditorFactory = new qx.ui.table.celleditor.Dynamic( this._booleanCellEditorFactoryFunc );
			tcm.setDataCellRenderer( this.SELECT_COL, booleanCellRendererFactory );
			tcm.setCellEditorFactory( this.SELECT_COL, booleanCellEditorFactory );
			table.getTableModel().setColumnEditable( this.SELECT_COL, true );
			this._booleanCols.push( this.SELECT_COL );



			table.setStatusBarVisible( false );
			var selModel = table.getSelectionModel();
			selModel.setSelectionMode( qx.ui.table.selection.Model.NO_SELECTION );
			selModel.addListener( "changeSelection", function( e ) {
				var index = selModel.getLeadSelectionIndex();
				var count = selModel.getSelectedCount();
				if ( count == 0 ) {
					this._buttonRemove.setEnabled( false );
					return;
				}
				this._buttonRemove.setEnabled( true );
				var map = this._tableModel.getRowDataAsMap( index );
				this.facade.raiseEvent( {
					type: ms123.processexplorer.Config.EVENT_PROCESSDEFINITION_CHANGED,
					processDefinition: map
				} );
			}, this );

			tcm.setDataCellRenderer( this.START_COL, new ms123.processexplorer.plugins.StartImageRenderer() );
			colWidth.each( ( function( w, index ) {
				var resizeBehavior = tcm.getBehavior();
				resizeBehavior.setWidth( index, w );
			} ).bind( this ) );

			this.add( table, {
				edge: "center"
			} );
			this._table = table;
			return table;
		},
		_createToolbar: function() {
			var toolbar = new qx.ui.toolbar.ToolBar();
			this._buttonRemove = new qx.ui.toolbar.Button( "", "icon/16/places/user-trash.png" );
			this._buttonRemove.addListener( "execute", function() {
				ms123.form.Dialog.confirm( this.tr( "processexplorer.definition.confirm_delete" ), function( e ) {
					if ( e ) {
						this._deleteDeployments();
						this._refreshDefinitions();
						this.facade.raiseEvent( {
							type: ms123.processexplorer.Config.EVENT_PROCESSDEPLOYMENT_CHANGED
						} );
					}
				}, this );
			}, this );
			if ( ms123.config.ConfigManager.isAdmin() ) {
				toolbar._add( this._buttonRemove );
				this._buttonRemove.setEnabled( false );
			}


			toolbar.setSpacing( 5 );
			this.add( toolbar, {
				edge: "south"
			} );
			return toolbar;
		},
		_views: {
			"All": {},
			"SearchAsYouType": {
				// All rows matching search string are visible
				filters: function( rowdata ) {
					var id = rowdata[ this.ID_COL ].toLowerCase();
					var key = rowdata[ this.KEY_COL ].toLowerCase();
					var keyB = this._key == null || this._key == '' || ( key.indexOf( this._key ) != -1 );
					var idB = this._id == null || this._id == '' || ( id.indexOf( this._id ) != -1 );
					return keyB && idB;
				}
			}
		},
		_createTableModel: function() {
			var tm = new ms123.widgets.smart.model.Default();
			//			tm.clearSorting();
			var id = 0;
			for ( var view in this._views ) {
				if ( view == 'All' ) {
					this._views[ view ].id = 0;
					continue;
				}
				this._views[ view ].id = ++id;
				tm.addView( this._views[ view ].filters, this, this._views[ view ].conjunction );
			}
			return tm;
		},
		_clearTable: function() {
			try {
				this._tableModel.removeRows( 0, this._tableModel.getRowCount() );
			} catch ( e ) {}
		},
		_refreshDefinitions: function() {
			var rows = this._getProcessDefinitions();
			for ( var i = 0; i < rows.length; i++ ) {
				rows[ i ].selected = false;
			}
			this._clearTable();
			this._tableModel.addRowsAsMapArray( rows, null, true, true );
		},
		_booleanCellRendererFactoryFunc: function( cellInfo ) {
			return new qx.ui.table.cellrenderer.Boolean;
		},
		_booleanCellEditorFactoryFunc: function( cellInfo ) {
			return new qx.ui.table.celleditor.CheckBox;
		},
		onCellClick: function( e ) {
			var rownum = e.getRow();
			var colnum = e.getColumn();
			this._table.stopEditing();
			this._table.setFocusedCell( colnum, rownum );

			if ( this._booleanCols.indexOf( colnum ) >= 0 ) {
				if ( this._tableModel.getValue( colnum, rownum ) === true ) {
					this._tableModel.setValue( colnum, rownum, false );
				} else {
					this._tableModel.setValue( colnum, rownum, true );
				}
				if ( ms123.config.ConfigManager.isAdmin() ) {
					if ( this._isMinOneSeleted() ) {
						this._buttonRemove.setEnabled( true );
					} else {
						this._buttonRemove.setEnabled( false );
					}
				}
			} else if ( colnum == this.ID_COL ) {
				var map = this._tableModel.getRowDataAsMap( rownum );
				this._lastSelect = "id";
				this._changeEvent( map, "id" );
			} else if ( colnum == this.KEY_COL ) {
				var map = this._tableModel.getRowDataAsMap( rownum );
				this._lastSelect = "key";
				this._changeEvent( map, "key" );
			} else if ( colnum == this.START_COL ) {
				var map = this._tableModel.getRowDataAsMap( rownum );
				var pdata = qx.util.Serializer.toJson( map );

				this.facade.raiseEvent( {
					type: ms123.processexplorer.Config.EVENT_PROCESSSTARTED
				} );

				var pc = new ms123.processexplorer.ProcessController( {
					appRoot: this.getApplicationRoot()
				} );
				pc.addListener( "taskCompleted", function( e ) {
					setTimeout( ( function() {
						this._changeEvent( map, this._lastSelect );
					} ).bind( this ), 500 );
				}, this );
				pc.start( map );
			}
		},
		_deleteDeployments: function() {
			try {
				var deploymentIds = this._getDeploymentIds();
				console.log( "_deleteDeployments:" + JSON.stringify( deploymentIds, null, 2 ) );
				ms123.util.Remote.rpcSync( "activiti:deleteDeployments", {
					deploymentIds: deploymentIds,
					cascade: true
				} );
				ms123.form.Dialog.alert( this.tr( "processexplorer.definition.definitions_deleted" ) );
			} catch ( e ) {
				ms123.form.Dialog.alert( "ProcessDefinitions._getDeleteDeployments:" + e );
				return;
			}
		},
		_changeEvent: function( map, select ) {
			if ( select == null ) {
				select = "id";
			}
			this.facade.raiseEvent( {
				type: ms123.processexplorer.Config.EVENT_PROCESSDEFINITION_CHANGED,
				select: select,
				processDefinition: map
			} );
		},
		_getProcessDefinitions: function() {
			var result = null;
			try {
				result = ms123.util.Remote.rpcSync( "activiti:getProcessDefinitions", {
					namespace: this._namespace,
					version: this._onlyLatest ? -1 : null
				} );
			} catch ( e ) {
				ms123.form.Dialog.alert( "ProcessDefinitions._getProcessDefinitions:" + e );
				return;
			}
			return result[ "data" ];
		},
		_isMinOneSeleted: function() {
			this._table.stopEditing();
			var rc = this._tableModel.getRowCount();
			for ( var i = 0; i < rc; i++ ) {
				var rd = this._tableModel.getRowDataAsMap( i );
				if ( rd.selected ) {
					return true;
				}
			}
			return false;
		},
		_getDeploymentIds: function() {
			this._table.stopEditing();
			var rc = this._tableModel.getRowCount();
			var records = [];
			for ( var i = 0; i < rc; i++ ) {
				var rd = this._tableModel.getRowDataAsMap( i );
				if ( rd.selected ) {
					records.push( rd.deploymentId );
				}
			}
			return records;
		},
		_handleEvent: function() {
			this._refreshDefinitions();
		}
	}
} );
