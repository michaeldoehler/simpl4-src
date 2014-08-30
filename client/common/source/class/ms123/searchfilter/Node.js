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

qx.Class.define("ms123.searchfilter.Node", {
  extend : qx.core.Object,
  construct : function () {
    this.base(arguments);
    this.setChildren(new qx.data.Array());
  },
  properties : {
    label : {
      check: "String",
      init : "",
      event: "changeLabel"
    },
    nodeName : {
      check: "String",
      init : ""
    },
    connector : {
      check: "String",
      nullable : true,
      event: "changeConnector"
    },
    field : {
      check: "String",
      nullable : true,
      event: "changeField"
    },
    op : {
      check: "String",
      nullable : true,
      event: "changeOp"
    },
    data : {
      check: "String",
      init : "",
      event: "changeData"
    },
    children : {
      check: "qx.data.Array",
      event: "changeChildren"
    }
  }
});
