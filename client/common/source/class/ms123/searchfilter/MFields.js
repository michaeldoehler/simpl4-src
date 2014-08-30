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
qx.Mixin.define("ms123.searchfilter.MFields",
{
  members:
  {
    fieldarray: 
			[
				{
					"text": "KurznameFirma", 
					"itemval": "shortname_company", 
					"ops": [
						{
							"text": "enth\u00e4lt", 
							"op": "cn"
						}, 
						{
							"text": "beginnt mit", 
							"op": "bw"
						}, 
						{
							"text": "gleich", 
							"op": "eq"
						}, 
						{
							"text": "ungleich", 
							"op": "ne"
						}
					]
				}, 
				{
					"text": "KurznamePerson", 
					"itemval": "shortname_person", 
					"ops": [
						{
							"text": "enth\u00e4lt", 
							"op": "cn"
						}, 
						{
							"text": "beginnt mit", 
							"op": "bw"
						}, 
						{
							"text": "gleich", 
							"op": "eq"
						}, 
						{
							"text": "ungleich", 
							"op": "ne"
						}
					]
				}, 
				{
					"text": "Firma1", 
					"itemval": "company1", 
					"ops": [
						{
							"text": "enth\u00e4lt", 
							"op": "cn"
						}, 
						{
							"text": "beginnt mit", 
							"op": "bw"
						}, 
						{
							"text": "gleich", 
							"op": "eq"
						}, 
						{
							"text": "ungleich", 
							"op": "ne"
						}
					]
				}, 
				{
					"text": "Firma2", 
					"itemval": "company2", 
					"ops": [
						{
							"text": "enth\u00e4lt", 
							"op": "cn"
						}, 
						{
							"text": "beginnt mit", 
							"op": "bw"
						}, 
						{
							"text": "gleich", 
							"op": "eq"
						}, 
						{
							"text": "ungleich", 
							"op": "ne"
						}
					]
				}, 
				{
					"text": "Vorname", 
					"itemval": "givenname", 
					"ops": [
						{
							"text": "enth\u00e4lt", 
							"op": "cn"
						}, 
						{
							"text": "beginnt mit", 
							"op": "bw"
						}, 
						{
							"text": "gleich", 
							"op": "eq"
						}, 
						{
							"text": "ungleich", 
							"op": "ne"
						}
					]
				}, 
				{
					"text": "Merkmale", 
					"itemval": "traits",
					"ops": [
						{
							"text": "enth\u00e4lt", 
							"op": "cn"
						}, 
						{
							"text": "beginnt mit", 
							"op": "bw"
						}, 
						{
							"text": "gleich", 
							"op": "eq"
						}, 
						{
							"text": "ungleich", 
							"op": "ne"
						}
					],
					"dataValues": [
						{
							"text": "PEV", 
							"value": "100000"
						}, 
						{
							"text": "PEV.0501", 
							"value": "100000.100001"
						}, 
						{
							"text": "PEV.0502", 
							"value": "100000.100002"
						} 
					] 
				}
			]
				}
});
