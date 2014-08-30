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
qx.Theme.define("ms123.theme.ms.Font",
{
  extend : qx.theme.modern.Font,

 fonts :
  { 
    "default" :
    { 
      size : 11,
      lineHeight : 1.4,
      family : [ "Arial", "Tahoma", "Arial", "Liberation Sans" ]
    },
    
    "bold" : 
    { 
      size : 11,
      lineHeight : 1.4,
      family : [ "Arial", "Tahoma", "Arial", "Liberation Sans" ],
      bold : true
    },
      
    "small" :
    {
      size : 10,
      lineHeight : 1.4,
      family : [ "Lucida Sans", "Tahoma", "Arial", "Liberation Sans" ]
    },
      
    "monospace" :
    {
      size : 11,
      lineHeight : 1.4,
      family : [ "DejaVu Sans Mono", "Courier New", "monospace" ]
    } 
  } 


});
