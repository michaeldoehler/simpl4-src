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
qx.Class.define("ms123.codemirror.helper.DocumentMarkdown", {
	extend: qx.ui.container.Composite,


	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */
	construct: function (context) {
		this.base(arguments);
		this.__storeDesc = context.facade.storeDesc;
		this.setLayout(new qx.ui.layout.Dock());


		var tabView = new qx.ui.tabview.TabView().set({
				contentPadding: 1
		});
		this._context = context;
		context.helperTree=["sw.filter","sw.form"];
		context.createContextMenu = this._createContextMenu.bind(this);
		var rh = new ms123.graphicaleditor.plugins.propertyedit.ResourceDetailTree(context, context.facade);
		rh.addListener("nodeSelected", function (e) {
			//this.handleNodeSelected(e);
			console.log("nodeSelected");
		}, this);
		this.add(tabView, {
			edge: "center"
		});
		var resDetailTab = new qx.ui.tabview.Page("", "icon/16/actions/edit-select-all.png");
		resDetailTab.setLayout(new qx.ui.layout.Grow());
    resDetailTab.add(rh);
    tabView.add(resDetailTab);

		var helpTab = new qx.ui.tabview.Page(this.tr("Hilfe"), "icon/16/actions/help-faq.png");
		helpTab.setLayout(new qx.ui.layout.Grow());
    helpTab.add(this._createHelpWidget());
    tabView.add(helpTab);

	},

	/******************************************************************************
	 STATICS
	 ******************************************************************************/
	statics: {
	},

	events: {},


	members: {
		 _createContextMenu:function(item, id){
			console.log("model:"+qx.util.Serializer.toJson(item.getModel()));
			console.log("target:"+item);
			var type = item.getModel().getType();
			var model = item.getModel();
			item.setUserData("model", item.getModel());
			item.setUserData("code", item.getModel().getValue());
			item.setDraggable(true);
			item.addListener("dragstart", function (e) {
				e.addAction("move");
			},this);
			
			var childs = model.getChildren();
			console.log("Lenght:"+childs.getLength()+"/"+type+"/"+model.getValue());
			if( type == "sw.field" && childs.getLength() > 0){	
				var cm = this._context.cmWrapper.getEditor();
				var menu = new qx.ui.menu.Menu;
				var button = new qx.ui.menu.Button(this.tr("documentmarkdown.insert_table2"),"icon/16/actions/insert-text.png");
				button.setUserData("model",item.getModel());
				button.addListener("execute", function (e) {
					var b = e.getTarget();
					var model = b.getUserData("model");
					var fields = model.getChildren();
					var t = '<% l = '+model.getParent()+'.'+model.getValue()+' %>\n';
					t += '(% frame="none" font="big" %)\n';
					for(var i=0;i< fields.getLength();i++){
						var citem = fields.getItem(i);
						var title=null;
						if( citem.getDisplay && citem.getDisplay()){
							title = citem.getDisplay();
						}else{
							title = citem.getValue();
						}
						var cr = i < (fields.getLength()-1) ? '\n' : '';
						t+='|='+title+cr;
					}
					t+='<% l.each{rec -> %>\n';
					for(var i=0;i< fields.getLength();i++){
						var citem = fields.getItem(i);
						var fname = citem.getValue();
						var le = i < (fields.getLength()-1) ? '' : '<%}%>';
						t+='|$rec.'+fname+le+'\n';
					}

					if (cm.somethingSelected()) {
						cm.replaceSelection(t);
					} else {
						cm.replaceRange(t, cm.getCursor());
					}
				}, this);
				menu.add(button);
				return menu;
			}
			if( type == "sw.filter"){	
				var cm = this._context.cmWrapper.getEditor();
				var menu = new qx.ui.menu.Menu;
				var button = new qx.ui.menu.Button(this.tr("documentmarkdown.insert_table1"),"icon/16/actions/insert-text.png");
				button.setUserData("model",item.getModel());
				button.addListener("execute", function (e) {
					var b = e.getTarget();
					var model = b.getUserData("model");
					var fields = this._getFilterField(model.getValue());
					var t = '<% k = executeNamedFilter("'+model.getValue()+'")[0] %>\n';
					t += '(% cs1=",,n,n" frame="none" font="big" %)\n';
					for(var i=0;i< fields.length;i++){
						var fname = fields[i];
						t+='|(%font="smallbold"%) $k.'+fname+'\n';
					}
					if (cm.somethingSelected()) {
						cm.replaceSelection(t);
					} else {
						cm.replaceRange(t, cm.getCursor());
					}
				}, this);
				menu.add(button);
				var button = new qx.ui.menu.Button(this.tr("documentmarkdown.insert_table2"),"icon/16/actions/insert-text.png");
				button.setUserData("model",item.getModel());
				button.addListener("execute", function (e) {
					var b = e.getTarget();
					var model = b.getUserData("model");

					var fields = this._getFilterField(model.getValue());
					var t = '<% l = executeNamedFilter("'+model.getValue()+'") %>\n';
					t += '(% frame="none" font="big" %)\n';
					var b ='';
					for(var i=0;i< fields.length;i++){
						var fname = fields[i];
						var cr = i < (fields.length-1) ? '\n' : '';
						t+=b+'|='+fname+cr;
						b=' ';
					}
					t+='<% l.each{rec -> %>\n';
					var b ='';
					for(var i=0;i< fields.length;i++){
						var fname = fields[i];
						var le = i < (fields.length-1) ? '' : '<%}%>';
						t+=b+'|$rec.'+fname+le+'\n';
						b=' ';
					}

					if (cm.somethingSelected()) {
						cm.replaceSelection(t);
					} else {
						cm.replaceRange(t, cm.getCursor());
					}
				}, this);
				menu.add(button);
				return menu;
			}
			return null;
		},
		_getFilterField:function(filter){
			console.log("_getFilterField:"+filter);
			try {
				filter = ms123.util.Remote.rpcSync("git:searchContent", {
					reponame: this.__storeDesc.getNamespace(),
					name: filter,
					type: "sw.filter"
				});
				filter = filter.evalJSON();
			} catch (e) {
				ms123.form.Dialog.alert("DocumentMarkdown._getFilterField:" + e);
				return;
			}
			var fieldList=[];
			for( var i=0; i < filter.fields.length;i++){
				var fieldName = filter.fields[i].id;
				var path = filter.fields[i].path;
				if( path.indexOf("$") != -1) continue;
				fieldList.push(fieldName);
			}
			return fieldList;
		},
		_createHelpWidget:function(){
      var htmlEmbed = new qx.ui.embed.Html(this._getHelp()).set({
					allowGrowX: true,
					overflowY: "auto",
					overflowX: "auto"
			});
			return htmlEmbed;
		},
		_getHelp:function(){
var s0 =  '<body id="body" style="width:100%;"\n'+
'  <div>\n'+
'    <div>\n'+
'      <div>\n'+
'        <div>\n'+
'          <div class="leftsidecolumns">\n'+
'            <div id="contentcolumn">\n'+
'              <div class="main layoutsubsection">\n'+
'                <div id="mainContentArea">\n'+
'                  <div id="document-title">\n'+
'                    <h1>Markdown Syntax</h1>\n'+
'                  </div>\n'+
'                  <div id="document-info">\n'+
'                    <div class="clearfloats"></div>\n'+
'                  </div>\n'+
'                  <div>\n'+
'                    <div id="top" class="box floatinginfobox">\n'+
'                      <strong>Inhaltsverzeichnis</strong>\n'+
'                      <ul>\n'+
'                        <li><span class="wikilink"><a href="#HGrundlegendeBemerkungen">Grundlegende Bemerkungen</a></span></li>\n'+
'                        <li><span class="wikilink"><a href="#HAbsatz">Absatz</a></span></li>\n'+
'                        <li><span class="wikilink"><a href="#HDCberschriften">Überschriften</a></span></li>\n'+
'                        <li><span class="wikilink"><a href="#HTextformatierung">Textformatierung</a></span></li>\n'+
'                        <li><span class="wikilink"><a href="#HHorizontaleLinie">Horizontale Linie</a></span></li>\n'+
'                        <li><span class="wikilink"><a href="#HListen">Listen</a></span></li>\n'+
'                        <li><span class="wikilink"><a href="#HDefinitionslisten">Definitionslisten</a></span></li>\n'+
'                        <li><span class="wikilink"><a href="#HNeueZeile2FZeilenumbruch">Neue Zeile/Zeilenumbruch</a></span></li>\n'+
'                        <li><span class="wikilink"><a href="#HLinks">Links</a></span></li>\n'+
'                        <li><span class="wikilink"><a href="#HTabellen">Tabellen</a></span></li>\n'+
'                        <li><span class="wikilink"><a href="#HBilder">Bilder</a></span></li>\n'+
'                        <li><span class="wikilink"><a href="#HWortgetreu">Wortgetreu</a></span></li>\n'+
'                        <li><span class="wikilink"><a href="#HZitate">Zitate</a></span></li>\n'+
'                        <li><span class="wikilink"><a href="#HGruppen">Gruppen</a></span></li>\n'+
'                        <li><span class="wikilink"><a href="#HEntziehungszeichen">Entziehungszeichen</a></span></li>\n'+
'                        <li><span class="wikilink"><a href="#HMakros">Makros</a></span></li>\n'+
'                        <li><span class="wikilink"><a href="#HHTML">HTML</a></span></li>\n'+
'                        <li><span class="wikilink"><a href="#HVelocity2FGroovyScripte">Groovy Scripte</a></span></li>\n'+
'                        <li><span class="wikilink"><a href="#HParameter">Parameter</a></span></li>\n'+
'                      </ul>\n'+
'                    </div>\n'+
'                    <h1 id="HAbsatz"><span>Absatz</span><a style="font-size:11px" href="#top">Nach oben</a></h1>\n'+
'                    <p>Absätze sind Textelemente, die durch zwei oder mehr neue Zeilen getrennt sind.</p>\n'+
'                    <table border="1" cellpadding="2" cellspacing="0" style="width:100%">\n'+
'                      <tbody>\n'+
'                        <tr>\n'+
'                          <th scope="col">Funktion</th>\n'+
'                          <th scope="col">Syntax</th>\n'+
'                          <th scope="col">Ergebnis</th>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Einfacher Absatz</td>\n'+
'                          <td>Dies ist ein Absatz</td>\n'+
'                          <td>Dies ist ein Absatz</td>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Absatz mit mehreren Zeilen</td>\n'+
'                          <td>Absatz auf<br />\n'+
'                          mehreren Zeilen</td>\n'+
'                          <td>Absatz auf<br />\n'+
'                          mehreren Zeilen</td>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Zwei Absätze</td>\n'+
'                          <td>Absatz eins<br />\n'+
'                          <br />\n'+
'                          Absatz zwei</td>\n'+
'                          <td>Absatz eins<br />\n'+
'                          <br />\n'+
'                          Absatz zwei</td>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Parametrisierter Absatz</td>\n'+
'                          <td><tt class="wikimodel-verbatim">(% style="text-align:center;color:blue" %)</tt><br />\n'+
'                          Zentrierter und blauer Absatz</td>\n'+
'                          <td style="text-align:center;color:blue"><br />\n'+
'                          Zentrierter und blauer Absatz</td>\n'+
'                        </tr>\n'+
'                      </tbody>\n'+
'                    </table>\n'+
'                    <h1 id="HDCberschriften"><span>Überschriften</span><a style="font-size:11px" href="#top">Nach oben</a></h1>\n'+
'                    <table border="1" cellpadding="2" cellspacing="0" style="width:100%">\n'+
'                      <tbody>\n'+
'                        <tr>\n'+
'                          <th scope="col">Funktion</th>\n'+
'                          <th scope="col">Syntax</th>\n'+
'                          <th scope="col">Ergebnis</th>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Standardüberschriften</td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <pre>\n'+
'= Ebene 1 = \n'+
'== Ebene 2 ==\n'+
'=== Ebene 3 ===\n'+
'==== Ebene 4 ====\n'+
'===== Ebene 5 =====\n'+
'====== Ebene 6 ======\n'+
'</pre>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <h1 id="Hlevel1"><span>Ebene 1</span></h1>\n'+
'                              <p>&#160;</p>\n'+
'                              <h2 id="Hlevel2"><span>Ebene 2</span></h2>\n'+
'                              <h3 id="Hlevel3"><span>Ebene 3</span></h3>\n'+
'                              <h4 id="Hlevel4"><span>Ebene 4</span></h4>\n'+
'                              <h5 id="Hlevel5"><span>Ebene 5</span></h5>\n'+
'                              <h6 id="Hlevel6"><span>Ebene 6</span></h6>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Parametrisierte Überschrift</td>\n'+
'                          <td><tt class="wikimodel-verbatim">(% style="color:blue" %)</tt><br />\n'+
'                          = Überschrift =</td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <h1 id="Hheading" style="color:blue"><span>Überschrift</span></h1>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Überschrift</td>\n'+
'                          <td><tt class="wikimodel-verbatim">=== Überschrift mit **fett** ===</tt></td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <h3 id="HDCberschriftmitfett"><span>Überschrift mit <strong>fett</strong></span></h3>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                        </tr>\n'+
'                      </tbody>\n'+
'                    </table>\n'+
'                    <h1 id="HTextformatierung"><span>Textformatierung</span><a style="font-size:11px" href="#top">Nach oben</a></h1>\n'+
'                    <table border="1" cellpadding="2" cellspacing="0" style="width:100%">\n'+
'                      <tbody>\n'+
'                        <tr>\n'+
'                          <th scope="col">Funktion</th>\n'+
'                          <th scope="col">Syntax</th>\n'+
'                          <th scope="col">Ergebnis</th>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Fett</td>\n'+
'                          <td><tt class="wikimodel-verbatim">**fett**</tt></td>\n'+
'                          <td><strong>fett</strong></td>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Unterstrichen</td>\n'+
'                          <td><tt class="wikimodel-verbatim">__unterstrichen__</tt></td>\n'+
'                          <td>\n'+
'                            <ins>unterstrichen</ins>\n'+
'                          </td>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Kursiv</td>\n'+
'                          <td><tt class="wikimodel-verbatim">//kursiv//</tt></td>\n'+
'                          <td><em>kursiv</em></td>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Durchgestrichen</td>\n'+
'                          <td><tt class="wikimodel-verbatim">--durchgestrichen--</tt></td>\n'+
'                          <td>\n'+
'                            <del>durchgestrichen</del>\n'+
'                          </td>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Nichtproportional</td>\n'+
'                          <td><tt class="wikimodel-verbatim">##nichtproportional##</tt></td>\n'+
'                          <td><tt>nichtproportional</tt></td>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Hochgestellt</td>\n'+
'                          <td><tt class="wikimodel-verbatim">etwas ^^hochgestellt^^</tt></td>\n'+
'                          <td>etwas <sup>hochgestellt</sup></td>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Tiefgestellt</td>\n'+
'                          <td><tt class="wikimodel-verbatim">etwas ,,tiefgestellt,,</tt></td>\n'+
'                          <td>etwas <sub>tiefgestellt</sub></td>\n'+
'                        </tr>\n'+
'                      </tbody>\n'+
'                    </table>\n'+
'                    <h1 id="HHorizontaleLinie"><span>Horizontale Linie</span><a style="font-size:11px" href="#top">Nach oben</a></h1>\n'+
'                    <div class="box infomessage">\n'+
'                      Es müssen vier oder mehr Striche sein.\n'+
'                    </div>\n'+
'                    <table border="1" cellpadding="2" cellspacing="0" style="width:100%">\n'+
'                      <tbody>\n'+
'                        <tr>\n'+
'                          <th scope="col">Funktion</th>\n'+
'                          <th scope="col">Syntax</th>\n'+
'                          <th scope="col">Ergebnis</th>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Einfache horizontale Linie</td>\n'+
'                          <td><tt class="wikimodel-verbatim">----</tt></td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <hr />\n'+
'                            </div>\n'+
'                          </td>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Parametrisierte horizontale linie</td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <pre>\n'+
'(% style="color:blue" %)\n'+
'----\n'+
'</pre>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <hr style="color:blue" />\n'+
'                            </div>\n'+
'                          </td>\n'+
'                        </tr>\n'+
'                      </tbody>\n'+
'                    </table>\n'+
'                    <h1 id="HListen"><span>Listen</span><a style="font-size:11px" href="#top">Nach oben</a></h1>\n'+
'                    <table border="1" cellpadding="2" cellspacing="0" style="width:100%">\n'+
'                      <tbody>\n'+
'                        <tr>\n'+
'                          <th scope="col">Funktion</th>\n'+
'                          <th scope="col">Syntax</th>\n'+
'                          <th scope="col">Ergebnis</th>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Aufzählung</td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <pre>\n'+
'* Element 1\n'+
'** Element 2\n'+
'*** Element 3\n'+
'* Element 4\n'+
'</pre>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <ul>\n'+
'                                <li>Element 1\n'+
'                                  <ul>\n'+
'                                    <li>Element 2\n'+
'                                      <ul>\n'+
'                                        <li>Element 3</li>\n'+
'                                      </ul>\n'+
'                                    </li>\n'+
'                                  </ul>\n'+
'                                </li>\n'+
'                                <li>Element 4</li>\n'+
'                              </ul>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Nummerierung</td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <pre>\n'+
'1. Element 1\n'+
'11. Element 2\n'+
'111. Element 3\n'+
'1. Element 4\n'+
'</pre>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <ol>\n'+
'                                <li>Element 1\n'+
'                                  <ol>\n'+
'                                    <li>Element 2\n'+
'                                      <ol>\n'+
'                                        <li>Element 3</li>\n'+
'                                      </ol>\n'+
'                                    </li>\n'+
'                                  </ol>\n'+
'                                </li>\n'+
'                                <li>Element 4</li>\n'+
'                              </ol>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Gemischte Liste</td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <pre>\n'+
'1. Element 1\n'+
'1*. Element 2\n'+
'1*. Element 3\n'+
'1. Element 4\n'+
'</pre>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <ol>\n'+
'                                <li>Element 1\n'+
'                                  <ul>\n'+
'                                    <li>Element 2</li>\n'+
'                                    <li>Element 3</li>\n'+
'                                  </ul>\n'+
'                                </li>\n'+
'                                <li>Element 4</li>\n'+
'                              </ol>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Aufzählung (Quadrat)</td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <pre>\n'+
'(% style="list-style-type: square" %)\n'+
'* Element 1\n'+
'* Element 2\n'+
'</pre>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <ul style="list-style-type: square">\n'+
'                                <li>Element 1</li>\n'+
'                                <li>Element 2</li>\n'+
'                              </ul>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Aufzählung (Kreis)</td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <pre>\n'+
'(% style="list-style-type: disc" %)\n'+
'* Element 1\n'+
'* Element 2\n'+
'</pre>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <ul style="list-style-type: disc">\n'+
'                                <li>Element 1</li>\n'+
'                                <li>Element 2</li>\n'+
'                              </ul>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Nummerierung (Kleinbuchstaben)</td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <pre>\n'+
'(% style="list-style-type: lower-alpha" %)\n'+
'* Element 1\n'+
'* Element 2\n'+
'</pre>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <ul style="list-style-type: lower-alpha">\n'+
'                                <li>Element 1</li>\n'+
'                                <li>Element 2</li>\n'+
'                              </ul>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Nummerierung (Großbuchstaben)</td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <pre>\n'+
'(% style="list-style-type: upper-alpha" %)\n'+
'* Element 1\n'+
'* Element 2\n'+
'</pre>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <ul style="list-style-type: upper-alpha">\n'+
'                                <li>Element 1</li>\n'+
'                                <li>Element 2</li>\n'+
'                              </ul>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Nummerierung (kleine römische Zahlen)</td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <pre>\n'+
'(% style="list-style-type: lower-roman" %)\n'+
'* Element 1\n'+
'* Element 2\n'+
'</pre>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <ul style="list-style-type: lower-roman">\n'+
'                                <li>Element 1</li>\n'+
'                                <li>Element 2</li>\n'+
'                              </ul>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Nummerierung (große römische Zahlen)</td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <pre>\n'+
'(% style="list-style-type: upper-roman" %)\n'+
'* Element 1\n'+
'* Element 2\n'+
'</pre>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <ul style="list-style-type: upper-roman">\n'+
'                                <li>Element 1</li>\n'+
'                                <li>Element 2</li>\n'+
'                              </ul>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Nummerierung (griechische Kleinbuchstaben)</td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <pre>\n'+
'(% style="list-style-type: lower-greek" %)\n'+
'* Element 1\n'+
'* Element 2\n'+
'</pre>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <ul style="list-style-type: lower-greek">\n'+
'                                <li>Element 1</li>\n'+
'                                <li>Element 2</li>\n'+
'                              </ul>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                        </tr>\n'+
'                      </tbody>\n'+
'                    </table>\n'+
'                    <h1 id="HDefinitionslisten"><span>Definitionslisten</span><a style="font-size:11px" href="#top">Nach oben</a></h1>\n'+
'                    <table border="1" cellpadding="2" cellspacing="0" style="width:100%">\n'+
'                      <tbody>\n'+
'                        <tr>\n'+
'                          <th scope="col">Funktion</th>\n'+
'                          <th scope="col">Syntax</th>\n'+
'                          <th scope="col">Ergebnis</th>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Standarddefinition</td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <pre>\n'+
'; Begriff\n'+
': Definition\n'+
'</pre>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <dl>\n'+
'                                <dt>Begriff</dt>\n'+
'                                <dd>Definition</dd>\n'+
'                              </dl>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Verschachtelte Definitionen</td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <pre>\n'+
'; Begriff 1\n'+
': Definition 1\n'+
':; Begriff 2\n'+
':: Definition 2\n'+
'</pre>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <dl>\n'+
'                                <dt>Begriff 1</dt>\n'+
'                                <dd>\n'+
'                                  Definition 1\n'+
'                                  <dl>\n'+
'                                    <dt>Begriff 2</dt>\n'+
'                                    <dd>Definition 2</dd>\n'+
'                                  </dl>\n'+
'                                </dd>\n'+
'                              </dl>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Parametrisierte Definition</td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <pre>\n'+
'(% style="color:blue" %)\n'+
'; Begriff\n'+
': Definition\n'+
'</pre>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <dl style="color:blue">\n'+
'                                <dt>Begriff</dt>\n'+
'                                <dd>Definition</dd>\n'+
'                              </dl>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                        </tr>\n'+
'                      </tbody>\n'+
'                    </table>\n'+
'                    <h1 id="HNeueZeile2FZeilenumbruch"><span>Neue Zeile/Zeilenumbruch</span><a style="font-size:11px" href="#top">Nach oben</a></h1>\n'+
'                    <p>Eine neue Zeile ist ein Absatzende. Ein Zeilenumbruch ist eine erzwungene neue Zeile, welche an jeder Stelle im Text auftreten kann.</p>\n'+
'                    <table border="1" cellpadding="2" cellspacing="0" style="width:100%">\n'+
'                      <tbody>\n'+
'                        <tr>\n'+
'                          <th scope="col">Funktion</th>\n'+
'                          <th scope="col">Syntax</th>\n'+
'                          <th scope="col">Ergebnis Syntax</th>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Zeilenumbruch</td>\n'+
'                          <td><tt class="wikimodel-verbatim">Zeile\\Neue Zeile</tt></td>\n'+
'                          <td>Zeile<br />\n'+
'                          Neue Zeile</td>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Neue Zeile</td>\n'+
'                          <td>Zeile<br />\n'+
'                          Neue Zeile</td>\n'+
'                          <td>Zeile<br />\n'+
'                          Neue Zeile</td>\n'+
'                        </tr>\n'+
'                      </tbody>\n'+
'                    </table>\n'+
'                    <h1 id="HLinks"><span>Links</span><a style="font-size:11px" href="#top">Nach oben</a></h1>\n'+
'                    <table border="1" cellpadding="2" cellspacing="0" style="width:100%;overflow:hidden;word-wrap:break-word">\n'+
'                      <tbody>\n'+
'                        <tr>\n'+
'                          <th style="width:10%" scope="col">Funktion</th>\n'+
'                          <th scope="col">Syntax</th>\n'+
'                          <th style="width:15%" scope="col">Ergebnis</th>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Link zu Seite im aktuellen Space</td>\n'+
'                          <td><tt class="wikimodel-verbatim">[[WebHome]]</tt></td>\n'+
'                          <td><span class="wikilink"><a href="http://doku.afterbuy.de/bin/view/XWiki/WebHome"><span class="wikigeneratedlinkcontent">WebHome</span></a></span></td>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Link mit Linktext</td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <p><tt class="wikimodel-verbatim">[[Linktext&gt;&gt;wf:workflow1]]</tt><br />\n'+
'                              <span class="box infomessage">Syntax innerhalb des Linktextes wird unterstützt.</span></p>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                          <td><span class="wikilink"><a href="#">Linktext</a></span></td>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Link mit Syntax im Linktext</td>\n'+
'                          <td><tt class="wikimodel-verbatim">[[**fetter Linktext**&gt;&gt;wf:workflow1]]</tt></td>\n'+
'                          <td><span class="wikilink"><a href="#"><strong>fetter Linktext</strong></a></span></td>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Link zu Seite im angegebenen Space</td>\n'+
'                          <td><tt class="wikimodel-verbatim">[[Main.WebHome]]</tt></td>\n'+
'                          <td><span class="wikilink"><a href="http://doku.afterbuy.de/bin/view/Main/WebHome"><span class="wikigeneratedlinkcontent">WebHome</span></a></span></td>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Link der in neuem Fenster öffnet</td>\n'+
'                          <td><tt class="wikimodel-verbatim">[[label&gt;&gt;WebHome||rel="__blank"]]</tt></td>\n'+
'                          <td><span class="wikilink"><a target="_blank" rel="__blank" href="http://doku.afterbuy.de/bin/view/XWiki/WebHome">label</a></span></td>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Link zu einer URL direkt im Text</td>\n'+
'                          <td><tt class="wikimodel-verbatim">Dies ist eine URL: http://xwiki.org</tt></td>\n'+
'                          <td>Dies ist eine URL: <span class="wikiexternallink"><a class="wikimodel-freestanding" href="http://xwiki.org/"><span class="wikigeneratedlinkcontent">http://xwiki.org</span></a></span></td>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Link zu einer URL</td>\n'+
'                          <td><tt class="wikimodel-verbatim">[[http://xwiki.org]]</tt></td>\n'+
'                          <td><span class="wikiexternallink"><a href="http://xwiki.org/"><span class="wikigeneratedlinkcontent">http://xwiki.org</span></a></span></td>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Link zu einer URL mit Linktext</td>\n'+
'                          <td><tt class="wikimodel-verbatim">[[XWiki&gt;&gt;http://xwiki.org]]</tt></td>\n'+
'                          <td><span class="wikiexternallink"><a href="http://xwiki.org/">XWiki</a></span></td>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Link zu einer E-Mail-Adresse</td>\n'+
'                          <td><tt class="wikimodel-verbatim">[[john@smith.net&gt;&gt;mailto:john@smith.net]]</tt></td>\n'+
'                          <td><span class="wikiexternallink"><a href="mailto:john@smith.net">john@smith.net</a></span></td>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Bild-Link</td>\n'+
'                          <td><tt class="wikimodel-verbatim">[[image:Space2.Seite2@Bild.png&gt;&gt;Space1.Seite1]]</tt></td>\n'+
'                          <td><span class="wikilink"><a href="http://doku.afterbuy.de/bin/view/Main/WebHome"><img src="aa-Dateien/img_002.png" class="wikimodel-freestanding" alt="img.png" /></a></span></td>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Bild-Link mit Bildparametern</td>\n'+
'                          <td><tt class="wikimodel-verbatim">[[[[image:Space2.Seite2@Bild.png||width="26" height="26"]]&gt;&gt;Space1.Seite1]]</tt></td>\n'+
'                          <td><span class="wikilink"><a href="http://doku.afterbuy.de/bin/view/Main/WebHome"><img src="aa-Dateien/img.png" alt="img.png" height="26" width="26" /></a></span></td>\n'+
'                        </tr>\n'+
'                      </tbody>\n'+
'                    </table>\n'+
'                    <h1 id="HTabellen"><span>Tabellen</span><a style="font-size:11px" href="#top">Nach oben</a></h1>\n'+
'                    <p>Erlaubt es auf einfache Weise Inhalte im Tabellenformat zu erstellen. Parameter für Tabelle, Reihe oder Zelle sind ebenfalls unterstützt.</p>\n'+
'                    <table border="1" cellpadding="2" cellspacing="0" style="width:100%">\n'+
'                      <tbody>\n'+
'                        <tr>\n'+
'                          <th scope="col">Funktion</th>\n'+
'                          <th scope="col">Syntax</th>\n'+
'                          <th scope="col">Ergebnis</th>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Standardtabelle</td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <pre>\n'+
'|=Titel 1|=Titel 2\n'+
'|Wort 1|Wort 2\n'+
'</pre>\n'+
'                            </div>oder\n'+
'                            <div>\n'+
'                              <pre>\n'+
'!=Titel 1!=Titel 2\n'+
'!!Wort 1!!Wort 2\n'+
'</pre>';
var s1 = '                            </div>\n'+
'                          </td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <table>\n'+
'                                <tbody>\n'+
'                                  <tr>\n'+
'                                    <th scope="col">Titel 1</th>\n'+
'                                    <th scope="col">Titel 2</th>\n'+
'                                  </tr>\n'+
'                                  <tr>\n'+
'                                    <td>Wort 1</td>\n'+
'                                    <td>Wort 2</td>\n'+
'                                  </tr>\n'+
'                                </tbody>\n'+
'                              </table>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Parametrisierte Tabelle</td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <pre>\n'+
'(% style="background-color:red;align=center" %)\n'+
'|=Titel 1|=(% style="background-color:yellow" %)Titel 2\n'+
'|Wort 1|Wort 2\n'+
'</pre>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <table style="background-color:red;align=center">\n'+
'                                <tbody>\n'+
'                                  <tr>\n'+
'                                    <th scope="col">Titel 1</th>\n'+
'                                    <th style="background-color:yellow" scope="col">Titel 2</th>\n'+
'                                  </tr>\n'+
'                                  <tr>\n'+
'                                    <td>Wort 1</td>\n'+
'                                    <td>Wort 2</td>\n'+
'                                  </tr>\n'+
'                                </tbody>\n'+
'                              </table>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                        </tr>\n'+
'                      </tbody>\n'+
'                    </table>\n'+
'                    <h1 id="HBilder"><span>Bilder</span><a style="font-size:11px" href="#top">Nach oben</a></h1>\n'+
'                    <table border="1" cellpadding="2" cellspacing="0" style="width:100%">\n'+
'                      <tbody>\n'+
'                        <tr>\n'+
'                          <th scope="col">Funktion</th>\n'+
'                          <th scope="col">Syntax</th>\n'+
'                          <th scope="col">Ergebnis</th>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Bild von Anhang auf aktueller Seite</td>\n'+
'                          <td><tt class="wikimodel-verbatim">image:img.png</tt></td>\n'+
'                          <td><img src="aa-Dateien/img_002.png" class="wikimodel-freestanding" alt="img.png" /></td>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Bild von Anhang auf anderer Seite</td>\n'+
'                          <td><tt class="wikimodel-verbatim">image:Space.Seite@img.png</tt></td>\n'+
'                          <td><img src="aa-Dateien/img_002.png" class="wikimodel-freestanding" alt="img.png" /></td>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Bild mit Parametern</td>\n'+
'                          <td><tt class="wikimodel-verbatim">[[image:img.png||width="25" height="25"]]</tt></td>\n'+
'                          <td><img src="aa-Dateien/img_003.png" alt="img.png" height="25" width="25" /></td>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Bild befindlich an URL</td>\n'+
'                          <td><tt class="wikimodel-verbatim">image:http://eine/url/img.png</tt></td>\n'+
'                          <td><img src="aa-Dateien/img_002.png" class="wikimodel-freestanding" alt="img.png" /></td>\n'+
'                        </tr>\n'+
'                      </tbody>\n'+
'                    </table>\n'+
'                    <h1 id="HWortgetreu"><span>Wortgetreu</span><a style="font-size:11px" href="#top">Nach oben</a></h1>\n'+
'                    <p>Erlaubt es Inhalte einzugeben die nicht formatiert werden (in anderen Worten es wird die Syntax nicht beachtet).</p>\n'+
'                    <table border="1" cellpadding="2" cellspacing="0" style="width:100%">\n'+
'                      <tbody>\n'+
'                        <tr>\n'+
'                          <th scope="col">Funktion</th>\n'+
'                          <th scope="col">Syntax</th>\n'+
'                          <th scope="col">Ergebnis</th>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Wortgetreu in Zeile</td>\n'+
'                          <td><tt class="wikimodel-verbatim">Etwas wortgetreuer {{{**[[nicht gerenderter]]**}}} Inhalt</tt></td>\n'+
'                          <td>Etwas wortgetreuer <tt class="wikimodel-verbatim">**[[nicht gerenderter]]**</tt> Inhalt</td>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Wortgetreuer Block</td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <pre>\n'+
'{{{\n'+
'mehrzeiliger\n'+
'**wortgetreuer**\n'+
'Inhalt\n'+
'}}}\n'+
'</pre>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <pre>\n'+
'mehrzeiliger\n'+
'**wortgetreuer**\n'+
'Inhalt\n'+
'</pre>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                        </tr>\n'+
'                      </tbody>\n'+
'                    </table>\n'+
'                    <h1 id="HZitate"><span>Zitate</span><a style="font-size:11px" href="#top">Nach oben</a></h1>\n'+
'                    <p>Erlaubt es einen Text zu zitieren.</p>\n'+
'                    <table border="1" cellpadding="2" cellspacing="0" style="width:100%">\n'+
'                      <tbody>\n'+
'                        <tr>\n'+
'                          <th scope="col">Funktion</th>\n'+
'                          <th scope="col">Syntax</th>\n'+
'                          <th scope="col">Ergebnis</th>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Einfaches Zitat</td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <pre>\n'+
'&gt; Max hat das gesagt\n'+
'Ich habe OK gesagt\n'+
'</pre>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <blockquote>\n'+
'                                <p>&#160;Max hat das gesagt</p>\n'+
'                              </blockquote>\n'+
'                              <p>Ich habe OK gesagt</p>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Nested quotes</td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <pre>\n'+
'&gt; Max hat das gesagt\n'+
'&gt;&gt; Marie hat das geantwortet\n'+
'Ich hab OK gesagt\n'+
'</pre>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <blockquote>\n'+
'                                <p>&#160;Max hat das gesagt</p>\n'+
'                                <blockquote>\n'+
'                                  <p>&#160;Marie hat das geantwortet</p>\n'+
'                                </blockquote>\n'+
'                              </blockquote>\n'+
'                              <p>Ich hab OK gesagt</p>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                        </tr>\n'+
'                      </tbody>\n'+
'                    </table>\n'+
'                    <h1 id="HGruppen"><span>Gruppen</span><a style="font-size:11px" href="#top">Nach oben</a></h1>\n'+
'                    <p>Gruppen können benutzt werden um ein Dokument "inline" direkt in ein anderes Dokument einzufügen. Dies ermöglicht es beispielsweise komplexe Elemente in einem Listenelement oder in einer Tabellenzelle einzufügen. Gruppen sind durch die folgenden Syntaxelemente begrenzt: <tt><tt class="wikimodel-verbatim">(((...)))</tt></tt>. Eine Gruppe kann eine andere Gruppe beinhalten und es gibt keine Grenzen bei der Verschachtelung.</p>\n'+
'                    <table border="1" cellpadding="2" cellspacing="0" style="width:100%">\n'+
'                      <tbody>\n'+
'                        <tr>\n'+
'                          <th scope="col">Syntax</th>\n'+
'                          <th scope="col">Ergebnis</th>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <pre>\n'+
'|=Kopfzeile 1|=Kopfzeile 2|=Kopfzeile 3\n'+
'|Zelle Eins|(((\n'+
'= Eingebundenes Dokument =\n'+
'Ein eingebetteter Absatz.\n'+
'* Listenelement eins\n'+
'* Listenelement zwei\n'+
'  ** Unterelement 1\n'+
'  ** Unterelement 2\n'+
'))) | Zelle Drei\n'+
'Nächster Absatz im Dokument der obersten Ebene\n'+
'</pre>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <table>\n'+
'                                <tbody>\n'+
'                                  <tr>\n'+
'                                    <th scope="col">Kopfzeile 1</th>\n'+
'                                    <th scope="col">Kopfzeile 2</th>\n'+
'                                    <th scope="col">Kopfzeile 3</th>\n'+
'                                  </tr>\n'+
'                                  <tr>\n'+
'                                    <td>Zelle Eins</td>\n'+
'                                    <td>\n'+
'                                      <div>\n'+
'                                        <h1 id="HEmbeddeddocument"><span>Eingebettetes Dokument</span></h1>\n'+
'                                        <p>Ein eingebetteter Absatz.</p>\n'+
'                                        <ul>\n'+
'                                          <li>Listenelement eins</li>\n'+
'                                          <li>Listenelement zwei\n'+
'                                            <ul>\n'+
'                                              <li>Unterelement 1</li>\n'+
'                                              <li>Unterelement 2</li>\n'+
'                                            </ul>\n'+
'                                          </li>\n'+
'                                        </ul>\n'+
'                                      </div>\n'+
'                                    </td>\n'+
'                                    <td>&#160;Zelle Drei</td>\n'+
'                                  </tr>\n'+
'                                </tbody>\n'+
'                              </table>\n'+
'                              <p>Nächster Absatz im Dokument der obersten Ebene</p>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                        </tr>\n'+
'                      </tbody>\n'+
'                    </table>\n'+
'                    <h1 id="HEntziehungszeichen"><span>Entziehungszeichen</span><a style="font-size:11px" href="#top">Nach oben</a></h1>\n'+
'                    <p>Erlaubt es der Syntax zu entgehen.</p>\n'+
'                    <table border="1" cellpadding="2" cellspacing="0" style="width:100%">\n'+
'                      <tbody>\n'+
'                        <tr>\n'+
'                          <th scope="col">Funktion</th>\n'+
'                          <th scope="col">Syntax</th>\n'+
'                          <th scope="col">Ergebnis</th>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Entziehungszeichen für ein Zeichen</td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <p><tt class="wikimodel-verbatim">Dies ist kein ~[~[Link~]~]</tt><br />\n'+
'                              <span class="box infomessage">Um ein ~ Zeichen einzufügen ist dieses doppelt einzugeben: ~~</span></p>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                          <td><tt class="wikimodel-verbatim">Dies ist kein [[Link]]</tt></td>\n'+
'                        </tr>\n'+
'                      </tbody>\n'+
'                    </table>\n'+
'                    <h1 id="HMakros"><span>Makros</span></h1>\n'+
'                    <ul>\n'+
'                      <li>Makros (aufgerufen mittels der <tt><tt class="wikimodel-verbatim">{{macroname param1="value1" ... paramN="valueN"</tt>}}</tt> Syntax)</li>\n'+
'                    </ul>\n'+
'                    <table border="1" cellpadding="2" cellspacing="0" style="width:100%">\n'+
'                      <tbody>\n'+
'                        <tr>\n'+
'                          <th scope="col">Funktion</th>\n'+
'                          <th scope="col">Syntax</th>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Makro</td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <pre>\n'+
'{{code language="java"}}\n'+
'Java Inhalt\n'+
'{{/code}}\n'+
'</pre>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                        </tr>\n'+
'                      </tbody>\n'+
'                    </table>\n'+
'                    <h1 id="HHTML"><span>HTML</span><a style="font-size:11px" href="#top">Nach oben</a></h1>\n'+
'                    <table border="1" cellpadding="2" cellspacing="0" style="width:100%">\n'+
'                      <tbody>\n'+
'                        <tr>\n'+
'                          <th scope="col">Syntax</th>\n'+
'                          <th scope="col">Ergebnis</th>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>&lt;b&gt;fett&lt;/b&gt;</td>\n'+
'                          <td><strong>fett</strong></td>\n'+
'                        </tr>\n'+
'                      </tbody>\n'+
'                    </table>\n'+
'                    <h1 id="HVelocity2FGroovyScripte"><span>Groovy Scripte</span><a style="font-size:11px" href="#top">Nach oben</a></h1>\n'+
'                    <table border="1" cellpadding="2" cellspacing="0" style="width:100%">\n'+
'                      <tbody>\n'+
'                        <tr>\n'+
'                          <th scope="col">Funktion</th>\n'+
'                          <th scope="col">Syntax</th>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>Groovy Skript</td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <pre>\n'+
'{{groovy}}\n'+
'def var = "whatever"\n'+
'{{/groovy}}\n'+
'</pre>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                        </tr>\n'+
'                      </tbody>\n'+
'                    </table>\n'+
'                    <h1 id="HParameter"><span>Parameter</span><a style="font-size:11px" href="#top">Nach oben</a></h1>\n'+
'                    <p>Es ist möglich, Parameter an die verschiedenen Syntaxelemente und auch an Textblöcke weiterzugeben. Dies wird beispielsweise benutzt um diese zu gestalten. Sie können jegliche Parameterschlüssel/Wertepaare verwenden, die Sie möchten. Der XHTML Renderer wird diese Parameter als XHTML Attribute an die zugrundeliegende XHTML Representation der verschiedenen Syntaxelemente weitergeben.</p>\n'+
'                    <table border="1" cellpadding="2" cellspacing="0" style="width:200px;max-width:200px !important;">\n'+
'                      <tbody>\n'+
'                        <tr>\n'+
'                          <th scope="col">Syntax</th>\n'+
'                          <th scope="col">Generiertes XHTML</th>\n'+
'                        </tr>\n'+
'                        <tr>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <pre>\n'+
'(% class="meineKlasse" style="meinStil" id="meineID" %)\n'+
'= Überschrift =\n'+
'</pre>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                          <td>\n'+
'                            <div>\n'+
'                              <pre>\n'+
'&lt;h1 class="meineKlasse" style="meinStil" id="meineID"&gt;Überschrift&lt;/h1&gt;\n'+
'</pre>\n'+
'                            </div>\n'+
'                          </td>\n'+
'                        </tr>\n'+
'                      </tbody>\n'+
'                    </table>\n'+
'                  </div>\n'+
'                </div>\n'+
'              </div>\n'+
'            </div>\n'+
'          </div>\n'+
'          <div class="clearfloats"></div>\n'+
'        </div>\n'+
'      </div>\n'+
'    </div>\n'+
'  </div>\n'+
'</body>';
return s0 + s1;
		}
	},



	/**
	 *****************************************************************************
	 DESTRUCTOR
	 *****************************************************************************
	 */

	destruct: function () {}
});
