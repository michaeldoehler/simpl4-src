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
/**
 */
qx.Class.define("ms123.processexplorer.plugins.ImageCellRenderer", {
	extend: qx.ui.table.cellrenderer.Conditional,

	/**
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */

	properties: {},


/* 
  *****************************************************************************
     MEMBERS 
  *****************************************************************************
  */

	members: {
		createDataCellHtml: function (cellInfo, htmlArr) {
			htmlArr.push(
			'<div class="', this._getCellClass(cellInfo), '" style="', 'left:', cellInfo.styleLeft, 'px;top:0px;', 
			this._getCellSizeStyle(cellInfo.styleWidth, cellInfo.styleHeight, this._insetX, this._insetY), 
			this._getCellStyle(cellInfo), '" ', 
			this._getCellAttributes(cellInfo), '>' + 
			this._getContentHtml(cellInfo), 
			'</div>');
		},
		_getContentHtml: function (cellInfo) {
			if( cellInfo.value == "error" ){
				return '<img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABIAAAATCAYAAACdkl3yAAAABHNCSVQICAgIfAhkiAAAAAlwSFlz'+
				'AAABFQAAARUBKX4CGwAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAAKNSURB'+
				'VDiNrZQ7axRRFMd/s7sxmxg1QkQSIb5BESIKWvj4Akps/BAWWqSwE0zhow0qGFQEGz+DiKQRH4hg'+
				'Y+Ej7ormpTOZdXDvZGfu3HuPxczKugYU9N/c5pzf/M//HMYTEQOU+TfVKm3Ix3qdF/fu0ZumeKUS'+
				'nuflJcXrlUr5CwiwojXlsTHGT5+mWq1WPBGRMAyZvXyZ3QcOUOrry5s9D0R+hbXhgGQZ32ZnuRuG'+
				'XJma+lQBeHz/Psf37qUyNIT1PAzQ222+gBrAAWuqVTZs3Ur/s2ckSVKqALhGA9m8GaMUj4FMhF3A'+
				'9i6WD7wGrAhHPI8+rRlau5ZWq5WDRAQXx+ieHhpFBq+Kr+8onPjA88INgPI8epXCZhmA5I6sxcUx'+
				'lXKZERHeFMVPAAusB2baYznHkNasTxKSICCo1RARcpAx2DQFYD8QAe8L2CMAY1BpitaaQaU44fs0'+
				'v3xBLy4SNBpdjprNn1kcA74Db41BKYXv+wRBwLowZLLZRCcJkiS4NCVz+bB5Rs5h47hjQcKhNOVh'+
				'o8G7Wg3f90lbLS5pTa8x2I4ttlUBsMbglALnwDmilRUml5Z4+fkzURSRZRnOOS4At4A93afRBjlr'+
				'cUohWUYzjjk/N8dL3yeOYwadYxCoAzFwDrizymmU2iCrFGZ5mYv1Ok/n51FKMeAc08DtjsYIOAsk'+
				'q4HEWmwUMb+wwKOvX9Fa0yfCDWAnMAjcBEaKJh94sRrIGoMJQzYGAYe1ZocI14B9HYWbgGngYJHR'+
				'/tUyssPDmCgCY5jq2kanthRjdmqhXKZarUoJ4OSZMzww5reV/kkfgIFTp+jv788PcnR0lKMzM0xf'+
				'vYqbm/srSNLTw7bxca5PTADg/a8/5A8HaWZIXLu9oQAAAABJRU5ErkJggg=="/img>';
			}else if (cellInfo.value == "notfinished"){
				return '<img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABIAAAATCAYAAACdkl3yAAAABHNCSVQICAgIfAhkiAAAAAlwSFlz'+
				'AAABFAAAARQB+zng/wAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAAHmSURB'+
				'VDiNrdQ/SFtBHMDx773EpyQxfwimaclQ4lAELYIiCBk7GIJuzo1jHgTUCoIgCB0Eu7ko8lYRUlwF'+
				'RcRBcDSbipChCR1MKK8laTTm5ToEQ9O82KT2pjt+v/vw+90dJ6SUD4Cd542qkFJKgEwmw+HhR1Q1'+
				'29HOUsmGorwjHk/gcrkQUkppGAZnZ2+Jxfq6KsMwamjaa/b2jlEAjo70rhGAcrnG4GCaXC5Xh6rV'+
				'ztp5HOn0PYuLeRRFEArZKBQK3R3y7a3J+vo3Li7uSaVeEgjYGjHlcXJ+fsfW1ndLwDRhc9NgfPwL'+
				'JyflFqQJmpzs4/T0J9PTX7m5eWgkHByUiESyzM/nMU3Y3Q22IPDH+1lb8zMxkSUSybKw4COdvmN/'+
				'v4hpgsMh0PUAw8OqZdXK74uhIZVEwkM+b7KyUiCVqiNCwMbGANGo0xJpgQCWl32EQs13EI+70TRP'+
				'W8QS8vttLC35GuueHkEy6X0SsYQANM3D2FgvALGYk9HR3n+D7HbB6qofIWBuzv1XpC0EMDPjZHbW'+
				'RTTq6ByqVF5hmq3B7e0X2O3iSeDqqoLb7a5DU1Pv2dn5Qa3WnOT1ti0YgOvrCpeXbwiHw/UHGQwG'+
				'GRn5TDL5CVXNddRKsajQ3x9B1z8AIP7XD/kL1lafSWp1M7YAAAAASUVORK5CYII="/img>';
			}else if (cellInfo.value == "finished" || cellInfo.value == "ok"){
				return '<img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABIAAAATCAYAAACdkl3yAAAABHNCSVQICAgIfAhkiAAAAAlwSFlz'+
				'AAABFQAAARUBKX4CGwAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAAGsSURB'+
				'VDiNrdS/S1tRFMDx733vPkFIsENAIqhIinSQIthKUURFEBdTu3RocUgLDqFTRmu3drGroJsogh2K'+
				'uoiDAX+gf0FHbRTNUgySIVDy+l5Ohxfyy8T4aO/07jvnfu65P7hKRP4Amn9rjhIRAUilUix8m+fC'+
				'/vmgkdq2mG6fIf4+TiAQQImIZLNZOpZCMOj6KkNy8GJjlIOtQwyAlZ1l3wiACsBp6zHpdNqDLnMX'+
				'/gQbnG2QX0BIyGQyHuSnuUeQ/wjGU1Dt5f+l0yr8ANUFqq0+UDgHZw0KZ9DyGYxIdbwEqU6wF0C/'+
				'BnOknCBZcDbB2fP6LZ/AeHx3ojL0CMwxsL+AOQFWDNwTcL6D3AAKrDiYz+tXXHUR9Utw98FNgnsK'+
				'/K6IzYCO1keAms3WoGeL3xWIOQjWXGPkLgSYw97Aygz9tl5mEwiKVVlF+BkYT+5HGkJGL+jJIjTV'+
				'HGkIAeg3YPSAOeQD6rK6oVAdUCGwPgDqfkCuIRgMelAs+g53V4HUzNLXBElD/+0AkUjEu0fhcJi9'+
				'V0m+ri9y5Vw+aClmXjMaHiexmvBW8L9eyL88XHXTHxi6lAAAAABJRU5ErkJggg=="/img>';
			}else if (cellInfo.value == "notstartet"){
				return '<img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABIAAAATCAYAAACdkl3yAAAABHNCSVQICAgIfAhkiAAAAAlwSFlz'+
				'AAABFQAAARUBKX4CGwAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAALwSURB'+
				'VDiNrZTPa1RXFMc/d959mfya1FpCRis6iywsLmotrS1dmC4c6EaqCGIKKUSlkj+gxJ20Lkv/g1IE'+
				'024bIYJgY8eNKZUmuFJDQZA0CRnivDfzZub9OPd0MRNjMEKh/W7u5d57PpzzPYdrVDUDPP6bfrVb'+
				'kOUnT5i7epVctQrGdK5Vt/ddGUCBprWMTkxw+uxZPM+zRlW1Wq1SOXGC944cwd+3D+P73SgDxmC6'+
				'68tQef6cvx4+5I9Tp/h6erpiAe5ev85HJ08ycPw4dngYTVMQ2bUG09+PttsgwjuHDjFXqcD0tLEA'+
				'plaj58ABekol+t4/hqYprT8XcUGwA+IfPozdvx8XBMSLi9jhYYpDQwBebqsETVNQRVXBWnqPvosC'+
				'EoZIGOIdPIhXLKLOge8jYYimKX4+D2ByAM45XLNJ+9EjsmoV1GF8S/8nH6OALZWwpRLqHOoc7aUl'+
				'XBgiQUDSagFgAVQEF8dIEBD+MsvQ6c/x9uzB5HsZKJdRazuZAK2FBdoLC2irRfz0KSuPHwNkFsCJ'+
				'IFGEF4aoKsFPP/PGF+OYwUFcLoeKgCrNe/cIZ2ZIV1fJ1teRWo2NTlP0BchFEdJogAguDKnf/Y2+'+
				'crmTrQgahqxfuUK2sYHGMZokqAhZby8A2x5FERIESL1ObmSE/NgYkiRkcYw6wX/rTYrff4fGMa7Z'+
				'RLOsM7BddUBZhosiXKNBbu9eBs6dw4mQJQmtBw/wJAWUvg8/4O0ff8D0+K/MVw5Au11TVQYvXECM'+
				'QbKM1v37rI2Ps/LlJJqkAAx8OsbIt9/sDtryyI6Oovk8ToT20hIbly6hcUxUqbDy1eWO6cDQmTOv'+
				'ATlHVqvRuH2bZHmZ5p07bExO4qLoxcPG3C3+vjxF+uwZtRs3XgFZACkWyTY3ydbWWCmXd5j4suqz'+
				'N6nP3txxtlkobGf02cWL3IoiXJK8FrKbfgeOTkwAYFQ7kfPz88xcuwarq/8K0sjnOXb+PFNTUxQK'+
				'hYr5v37IfwBur5uez+A9bwAAAABJRU5ErkJggg=="/img>';
			}
		},
		_getCellClass: function (cellInfo) {
			return "qooxdoo-table-cell";
		}
	}
});
