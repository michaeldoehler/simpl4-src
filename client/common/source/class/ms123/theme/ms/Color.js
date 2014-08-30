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
qx.Theme.define("ms123.theme.ms.Color",
{
  extend : qx.theme.modern.Color,

  colors :
  {
    //"window-caption-active-start" : "#f87925",
    //"window-caption-active-end" : "#f87925",
    "window-caption-active-start" : "brown",
    "window-caption-active-end" : "brown",
    //"window-border" : "#f87925",


    "window-border" : "#909090",


    "selected-start" : "#CCCCCC",
    "selected-end" : "#CCCCCC",



    /*
    ---------------------------------------------------------------------------
      BACKGROUND COLORS
    ---------------------------------------------------------------------------
    */

    // application, desktop, ...
    "background-application" : "#DFDFDF",

    // pane color for windows, splitpanes, ...
    //"background-pane" : "#F3F3F3",
  	"background-pane" : "white",

    // textfields, ...
    "background-light" : "#FCFCFC",

    // headers, ...
    "background-medium" : "#EEEEEE",

    // splitpane
    "background-splitpane" : "#AFAFAF",

    // tooltip, ...
    "background-tip" : "#ffffdd",

    // error tooltip
    "background-tip-error": "#C72B2B",

    // tables, ...
    "background-odd" : "#E4E4E4",

    // html area
    "htmlarea-background" : "white",

    // progress bar
    "progressbar-background" : "white",




    /*
    ---------------------------------------------------------------------------
      TEXT COLORS
    ---------------------------------------------------------------------------
    */

    // other types
    "text-light" : "#909090",
    "text-gray" : "#4a4a4a",

    // labels
    "text-label" : "#000080",

    // group boxes
    "text-title" : "#314a6e",

    // text fields
    "text-input" : "#000000",

    // states
    "text-hovered"  : "#001533",
    "text-disabled" : "#7B7A7E",
    //"text-selected" : "#fffefe",
    "text-selected" : "black",
    "text-active"   : "#26364D",
    "text-inactive" : "#404955",
    "text-placeholder" : "#CBC8CD",






    /*
    ---------------------------------------------------------------------------
      BORDER COLORS
    ---------------------------------------------------------------------------
    */

    "border-inner-scrollbar" : "white",

    // menus, tables, scrollbars, list, etc.
    //"border-main" : "#4d4d4d",
    "border-main" : "white",
    "menu-separator-top" : "#C5C5C5",
    "menu-separator-bottom" : "#FAFAFA",

    // between toolbars
    "border-separator" : "#808080",
    "border-toolbar-button-outer" : "#b6b6b6",
    "border-toolbar-border-inner" : "#f8f8f8",
    "border-toolbar-separator-right" : "#f4f4f4",
    "border-toolbar-separator-left" : "#b8b8b8",

    // text fields
    "border-input" : "#334866",
    "border-inner-input" : "white",

    // disabled text fields
    "border-disabled" : "#B6B6B6",

    // tab view, window
    "border-pane" : "#00204D",

    // buttons
    "border-button" : "#666666",

    // tables (vertical line)
    "border-column" : "#CCCCCC",

    // focus state of text fields
    "border-focused" : "#99C3FE",

    // invalid form widgets
    "invalid" : "#990000",
    "border-focused-invalid" : "#FF9999",

    // drag & drop
    "border-dragover" : "#33508D",

    "keyboard-focus" : "black",


    /*
    ---------------------------------------------------------------------------
      TABLE COLORS
    ---------------------------------------------------------------------------
    */

    // equal to "background-pane"
    "table-pane" : "white",

    // own table colors
    // "table-row-background-selected" and "table-row-background-focused-selected"
    // are inspired by the colors of the selection decorator
    //"table-focus-indicator" : "#0880EF",
    "table-focus-indicator" : "black",
    //"table-row-background-focused-selected" : "#084FAB",
    //"table-row-background-focused" : "#80B4EF",
    //"table-row-background-selected" : "#084FAB",
    "table-row-background-focused-selected" : "#a09f9c",
 		"table-row-background-focused" : "#f87925",
    "table-row-background-selected" : "#CCCCCC",

    // equal to "background-pane" and "background-odd"
    "table-row-background-even" : "white",
    "table-row-background-odd" : "#E4E4E4",

    // equal to "text-selected" and "text-label"
    "table-row-selected" : "#fffefe",
    "table-row" : "#1a1a1a",

    // equal to "border-collumn"
    "table-row-line" : "#CCC",
    "table-column-line" : "#CCC",

    "table-header-hovered" : "white"

  }
});
