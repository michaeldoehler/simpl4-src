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

qx.Mixin.define("ms123.util.MFillIn", {

	/**
	 *****************************************************************************
	 MEMBERS
	 *****************************************************************************
	 */

	members: {
		_setSelectedValues: function (sourceEntity, sourceFields, sourceData, sourceIdField,destinationFields, destinationData,destinationProperties,) {
			var fillinArr = [];
			for (var i = 0; i < destinationFields.length; i++) {
				var f = destinationFields[i];
				if (f.fillin) {
					var m = this._parseFillin(f.fillin);
					if (m[sourceEntity]) {
						var x = m[sourceEntity];
						x.name = f.name;
						fillinArr.push(x);
					}
				}
			}
			var sourceFieldMap = {};
			for (var i = 0; i < sourceFields.length; i++) {
				sourceFieldMap[sourceFields[i].name] = sourceFields[i];
			}
			var destinationFieldMap = {};
			for (var i = 0; i < destinationFields.length; i++) {
				destinationFieldMap[destinationFields[i].name] = destinationFields[i];
			}
			if (fillinArr.length > 0) {
				for (var i = 0; i < fillinArr.length; i++) {
					var f = fillinArr[i];
					if (f.expr == "false") {
						continue;
					}
					var name = f.name;
					if (sourceFieldMap[f.expr]) {
						name = f.expr;
					} else if (f.expr == "true") {} else {
						var res = this.__maskedEval(f.expr, sourceData);
						if (res != null && res !== true) {
							destinationData[name] = res;
							continue;
						}
					}
					try {
						this._setValue(destinationFieldMap, destinationData, name, sourceData);
					} catch (e) {
						console.error("_setValue(" + name + "):" + e);
					}
				}
			} else {
				for (var i = 0; i < destinationFields.length; i++) {
					var fieldName = destinationFields[i].name;
					if (fieldName != "name" && fieldName != "id" && sourceData[fieldName] != undefined) {
						var fieldMetaData = destinationFieldMap[fieldName];
						if (!fieldMetaData.readonly && fieldName.match(/^[a-z]/)) {
							this._setValue(destinationFieldMap, destinationData, fieldName, sourceData);
						}
					}
				}
			}

			var key = this.getUserData("key");
			var mainValue = sourceData[key];
			var title = this._getRecordTitle(sourceData);
			var relatedToField = "_relatedto";
			if (destinationProperties.title_expression) {
				var v = this.__maskedEval(destinationProperties.title_expression, sourceData, "Id");
				this._setValue(destinationFieldMap,destinationData,relatedToField,v+"/"+sourceData[sourceIdField] );
			} else if (mainValue) {
				this._setValue(destinationFieldMap,destinationData,relatedToField,sourceData[sourceIdField] + "/" + mainValue);
			} else if (title) {
				this._setValue(destinationFieldMap,destinationData,relatedToField,sourceData[sourceIdField] + "/" + title);
			} else {
				this._setValue(destinationFieldMap,destinationData,relatedToField,sourceData[sourceIdField] + "/Id");
			}
		},
		_setValue: function (destinationFieldMap, destinationData, name, sourceData) {
			var fieldMetaData = destinationFieldMap[name] || {};
			console.debug("setting:" + name + "->" + (typeof sourceData == "string" ? sourceData : sourceData[name]) + "/" + fieldMetaData.readonly);
			if (fieldMetaData.type == "date") {
				var d = new Date();
				d.setTime(sourceData[name]);
				destinationData[name] = d;
			} else {
				destinationData[name] = typeof sourceData == "string" ? sourceData : sourceData[name];
			}
		},
		__maskedEval: function (scr, env, def) {
			try {
				return (new Function("with(this) { return " + scr + "}")).call(env);
			} catch (e) {
				console.log("RelatedTo.__maskedEval:" + scr);
				console.log("\t:" + e);
			}
			return def;
		},
		_getRecordTitle: function (map) {
			var names = ["name", "title", "shortname", "shortname_company", "shortname_person", "name1"];
			for (var i = 0; i < names.length; i++) {
				if (map[names[i]]) {
					return map[names[i]];
				}
			}
			return null;
		},

		_parseFillin: function (str) {
			var line = [];
			var quote = false;

			for (var col = 0, c = 0; c < str.length; c++) {
				var cc = str[c],
					nc = str[c + 1];
				line[col] = line[col] || '';
				if (cc == '"' && quote && nc == '"') {
					line[col] += cc;
					++c;
					continue;
				}
				if (cc == '"') {
					quote = !quote;
					continue;
				}
				if (cc == ',' && !quote) {
					++col;
					continue;
				}
				line[col] += cc;
			}
			var ret = {};
			for (var i = 0; i < line.length; i++) {
				var col = line[i];
				var k;
				if ((k = col.indexOf(":")) != -1) {
					ret[col.substring(0, k)] = {
						expr: col.substring(k + 1)
					};
				} else {
					ret[col] = {
						expr: true
					};
				}
			}
			return ret;
		}
	}
});
