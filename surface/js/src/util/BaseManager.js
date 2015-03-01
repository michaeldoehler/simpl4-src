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
can.Construct.extend("simpl4.util.BaseManager", {
	namespace:null,
	language:null,
	user:null,
	password:null,
	baseurl:'',
	setBaseUrl:function(baseUrl){
		simpl4.util.BaseManager.baseurl = baseUrl;
	},
	getBaseUrl:function(){
		return simpl4.util.BaseManager.baseurl;
	},
	setNamespace:function(namespace){
		simpl4.util.BaseManager.namespace = namespace;
	},
	getNamespace:function(){
		return simpl4.util.BaseManager.namespace;
	},
	getStoreId:function(){
		return simpl4.util.BaseManager.namespace+"_data";
	},
	setLanguage:function(language){
		simpl4.util.BaseManager.language = language;
	},
	getLanguage:function(language){
		return simpl4.util.BaseManager.language;
	},
	setUser:function(user){
		simpl4.util.BaseManager.user = user;
	},
	getUser:function(user){
		return simpl4.util.BaseManager.user;
	},
	setPassword:function(password){
		simpl4.util.BaseManager.password = password;
	},
	getPassword:function(password){
		return simpl4.util.BaseManager.password;
	},
	getDateFormat:function(){
		return simpl4.util.BaseManager.getLanguage() === 'de' ? 'DD.MM.YYYY' : 'MM/DD/YYYY';
	}
}, {});
