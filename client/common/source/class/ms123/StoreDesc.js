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
qx.Class.define("ms123.StoreDesc", {
	extend: qx.core.Object,


	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (context) {
		context = context || {};
		var namespace = context.namespace;
		var repository = context.repository;
		if( namespace === undefined || namespace === null){
			namespace = ms123.StoreDesc.getCurrentNamespace();
		}
		if( repository === undefined || repository === null){
			repository = ms123.StoreDesc.getCurrentNamespace();
		}
		this.__namespace = namespace;
		this.__repository = repository;

		var pack = context.pack;
		if( pack === undefined || pack === null){
			pack = ms123.StoreDesc.PACK_DEFAULT;
		}
		this.__pack = pack;
		this.__storeId = context.storeId;
		if( !this.__storeId ){
			this.__storeId = this.__namespace +"_"+this.__pack;
		}
	},

	statics: {
		__namespaceDataStoreDesc: {},
		__namespaceMetaStoreDesc: {},
		__namespaceConfigStoreDesc: {},
		__globalMetaStoreDesc: null,
		__globalDataStoreDesc: null,

		setCurrentNamespace:function(ns){
			ms123.StoreDesc.__currentNamespace = ns;
		},
		getCurrentNamespace:function(){
			return ms123.StoreDesc.__currentNamespace;
		},

		setNamespaceDataStoreDesc: function (sdesc) {
			ms123.StoreDesc.__namespaceDataStoreDesc[ms123.StoreDesc.__currentNamespace] = sdesc;
		},
		setNamespaceMetaStoreDesc: function (sdesc) {
			ms123.StoreDesc.__namespaceMetaStoreDesc[ms123.StoreDesc.__currentNamespace] = sdesc;
		},
		setNamespaceConfigStoreDesc: function (sdesc) {
			ms123.StoreDesc.__namespaceConfigStoreDesc[ms123.StoreDesc.__currentNamespace] = sdesc;
		},
		setGlobalMetaStoreDesc: function (sdesc) {
			ms123.StoreDesc.__globalMetaStoreDesc = sdesc;
		},

		setGlobalDataStoreDesc: function (sdesc) {
			ms123.StoreDesc.__globalDataStoreDesc = sdesc;
		},
		getNamespaceDataStoreDescForNS: function (ns) {
			return ms123.StoreDesc.__namespaceDataStoreDesc[ns];
		},

		getNamespaceDataStoreDesc: function () {
			return ms123.StoreDesc.__namespaceDataStoreDesc[ms123.StoreDesc.__currentNamespace];
		},
		getNamespaceConfigStoreDesc: function () {
			return ms123.StoreDesc.__namespaceConfigStoreDesc[ms123.StoreDesc.__currentNamespace];
		},
		getNamespaceMetaStoreDesc: function () {
			return ms123.StoreDesc.__namespaceMetaStoreDesc[ms123.StoreDesc.__currentNamespace];
		},
		getGlobalMetaStoreDesc: function (sdesc) {
			return ms123.StoreDesc.__globalMetaStoreDesc;
		},
		getGlobalDataStoreDesc: function (sdesc) {
			return ms123.StoreDesc.__globalDataStoreDesc;
		},
		NAMESPACE: "namespace",
		STORE_ID: "storeId",
		PACK_DEFAULT: "data"
	},

	properties: {},


	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		getNamespaceDataStoreDesc: function () {
			return ms123.StoreDesc.__namespaceDataStoreDesc[this.__namespace];
		},
		getNamespaceMetaStoreDesc: function () {
			return ms123.StoreDesc.__namespaceMetaStoreDesc[this.__namespace];
		},
		getNamespaceConfigStoreDesc: function () {
			return ms123.StoreDesc.__namespaceConfigStoreDesc[this.__namespace];
		},
		getNamespace: function () {
			return this.__namespace;
		},
		getPack: function () {
			return this.__pack;
		},
		getRepository: function () {
			return this.__repository;
		},
		getStoreId: function () {
			return this.__storeId;
		},

		toString: function () {
			return "[" + this.getStoreId() + "/" + this.getNamespace() + "," + this.getPack() + "]";
		}

	}
});
