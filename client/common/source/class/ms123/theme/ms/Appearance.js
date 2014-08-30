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
qx.Theme.define("ms123.theme.ms.Appearance",
{
  extend : qx.theme.modern.Appearance,

  appearances :
  {
		"datefield/popup" : {
				alias : "condition/data_date"
		},
    "toolbar-button" :
    {
      alias : "atom",

      style : function(states)
      {
        var decorator;
				var padding = [3,3];
        if (
          states.pressed ||
          (states.checked && !states.hovered) ||
          (states.checked && states.disabled))
        {
          decorator = "pressed-css";
        } else if (states.hovered && !states.disabled) {
          decorator = "hover-css";
        }

        var useCSS = qx.core.Environment.get("css.gradients") &&
          qx.core.Environment.get("css.borderradius");
        if (useCSS && decorator) {
          decorator += "-css";
        }

        return {
          marginTop : 2,
          marginBottom : 2,
          padding : padding,
          decorator : decorator
        };
      }
    },
 		"tabview-page" : {
      alias : "widget",
      include : "widget",

      style : function(states) {
        // is used for the padding of the pane
        var useCSS = qx.core.Environment.get("css.gradient.linear") && qx.core.Environment.get("css.borderradius");
        return {
          //padding : useCSS ? [4, 3] : undefined
					padding : undefined
        }
      }
    },

 
    "collapsable-panel" : {
      style : function(states) {
        return {
          decorator  : "pane",
          padding    : 5,
          allowGrowY : !!states.opened || !!states.horizontal,
          allowGrowX : !!states.opened ||  !states.horizontal
        };
      }
    },

    "collapsable-panel/bar" : {
      include : "groupbox/legend",
      alias   : "groupbox/legend",
      style   : function(states) {
        return {
          icon       :  states.opened ? "decoration/tree/open.png" : "decoration/tree/closed.png",
          allowGrowY : !states.opened && !!states.horizontal,
          allowGrowX :  states.opened ||  !states.horizontal,
          maxWidth   : !states.opened && !!states.horizontal ? 16 : null
        };
      }
    },

    "collapsable-panel/container" : {
      style : function(states) {
        return { padding : [0, 5] };
      }
    },
    "grid-header-cell" :
    {
      alias : "atom",

      style : function(states)
      {
        return {
          decorator : states.first ? "grid-header-cell-first" : "grid-header-cell",
          minWidth: 13,
          font : "bold",
          paddingTop: 3,
          paddingLeft: 5
        }
      }
    }
  }
});
