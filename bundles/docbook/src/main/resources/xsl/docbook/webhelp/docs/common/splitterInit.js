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

	var myLayout;

	jQuery(document).ready(function ($) {

                myLayout = $('body').layout({
                //Defining panes    
                        west__paneSelector:         "#sidebar"
                ,       north__paneSelector:        "#header"
                ,       center__paneSelector:       "#content"
		
		//	some resizing/toggling settings
		,	north__resizable:			false	// OVERRIDE the pane-default of 'resizable=true'
                ,	north__spacing_open:                    0		// no resizer-bar when open (zero height)
		,	west__slideable:			false
                ,	west__spacing_closed:		6		
		,	west__spacing_open:		4		

		,
		//	some pane-size settings
			west__minSize:				280
		,       north__minSize:                          99	

		//	some pane animation settings
		,	west__animatePaneSizing:	false
		,	west__fxSpeed_size:			"normal"	
		,	west__fxSpeed_open:			10	
		,	west__fxSettings_open:		{easing: ""} 
		,	west__fxName_close:			"none"	

	
		,	stateManagement__enabled:	true // automatic cookie load & save enabled by default
                ,       stateManagement__cookie__name:    "sidebar_state" 
		});




 	});

