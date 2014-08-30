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
qx.Theme.define("ms123.theme.brown.Decoration", {
	extend: qx.theme.simple.Decoration,

	decorations: {
	  "hover-css" : {
      decorator : [
        qx.ui.decoration.MLinearBackgroundGradient
      ],

      style : {
        startColorPosition : 0,
        endColorPosition : 100,
        startColor : "#CCCCCC",
        endColor : "#CCCCCC"
      }
    },

	  "pressed-css" : {
      decorator : [
        qx.ui.decoration.MLinearBackgroundGradient
      ],

      style : {
        startColorPosition : 0,
        endColorPosition : 100,
        startColor : "#f87925",
        endColor : "#f87925"
      }
    },

    "button-box" :
    {
      decorator : [
        qx.ui.decoration.MLinearBackgroundGradient,
        qx.ui.decoration.MBorderRadius,
        qx.ui.decoration.MSingleBorder,
        qx.ui.decoration.MBackgroundColor
      ],

      style :
      {
        radius : 1,
        width : 1,
        color : "button-border",
        gradientStart : ["button-box-bright", 40],
        gradientEnd : ["button-box-bright", 70],
        backgroundColor : "button-box-bright"
      }
    },
    "button-box-pressed" :
    {
      include : "button-box",

      style :
      {
        gradientStart : ["button-border", 40],
        gradientEnd : ["button-box-bright-pressed", 70],
        backgroundColor : "button-box-dark-pressed"
      }
    },
    "button-box-hovered" :
    {
      include : "button-box",

      style :
      {
        gradientStart : ["#CCCCCC", 40],
        gradientEnd : ["#CCCCCC", 70],
        color : "button-border-hovered"
      }
    },
    "border-blue" :
    {
      decorator: qx.ui.decoration.Decorator,

      style :
      {
        width : 1,
        color : "background-selected"
      }
    },
    "window" :
    {
      decorator: [
        qx.ui.decoration.MDoubleBorder,
        qx.ui.decoration.MBoxShadow,
        qx.ui.decoration.MBackgroundColor
      ],

      style :
      {
        width : 1,
        color : "window-border",
        innerWidth : 1,
        innerColor: "window-border-inner",
        shadowLength : 1,
        shadowBlurRadius : 3,
        shadowColor : "shadow",
        backgroundColor : "background"
      }
    },
    "grid-header-cell-first" :
    {
      include : "grid-header-cell",
      style : {
        widthLeft : 1
      }
    },
    "grid-header-cell" :
    {
      decorator : qx.ui.decoration.Decorator,

      style :
      {
        widthRight : 1,
        color : "button-border"
      }
    },

    /*
    ---------------------------------------------------------------------------
      TEXT FIELD
    ---------------------------------------------------------------------------
    */
    "inset" :
    {
      style :
      {
        width : 1,
        radius: 3,
        color : [ "border-light-shadow", "border-light", "border-light", "border-light" ]
      }
    },

    "focused-inset" :
    {
      style :
      {
        width : 2,
        radius: 3,
        color : "background-selected"
      }
    }


	}
});
