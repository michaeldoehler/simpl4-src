/**
 * Copyright (c) 2006
 * Martin Czuchra, Nicolas Peters, Daniel Polak, Willi Tscheschner
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 **/

/**
	* @ignore(Hash)
*/
qx.Class.define("this.tr("ge."), {
	/******************************************************************************
	 STATICS
	 ******************************************************************************/
	statics: {
		Language: "de_DE",

		AddDocker: {
			group: "Docker",
			add: "Docker Hinzufügen",
			addDesc: "Fügen Sie einer Kante einen Docker hinzu, indem Sie auf die Kante klicken",
			del: "Docker Löschen",
			delDesc: "Löscht einen Docker durch Klicken auf den zu löschenden Docker"
		},

		Arrangement: {
			acDesc: qx.locale.Manager.tr("Vertikal ausrichten"),
			ac: qx.locale.Manager.tr("Vertikal ausrichten"),
			amDesc: "Horizontal ausrichten",
			am: "Horizontal ausrichten",
			asDesc: "Größenangleichung",
			as: "Größenangleichung",
			groupA: "Alignment"
		},

		Edit: {
			group: "Edit",
			cut: "Ausschneiden",
			cutDesc: "Ausschneiden der selektierten Elemente",
			copy: "Kopieren",
			copyDesc: "Kopieren der selektierten Elemente",
			paste: "Einfügen",
			pasteDesc: "Einfügen von kopierten/ausgeschnittenen Elementen",
			del: "Löschen",
			delDesc: "Löschen der selektierten Elemente"
		},

		Save: {
			group: "File",
			saveAs: "Speichern als...",
			saveDesc: "Speichern",
			saved: "Gespeichert",
			save: "Speichern",
			saving: "Speichern",
			showJson: "json anzeigen",
			showJsonDesc: "json anzeigen",
			renewResourceIds: "Ids erneueren",
			renewResourceIdsDesc: "Ids erneuern",
			deploy: "Deploy",
			undeploy: "Undeploy"
		},

		Grouping: {
			grouping: "Grouping",
			group: "Gruppieren",
			groupDesc: "Gruppierung der selektierten Elemente",
			ungroup: "Gruppierung aufheben",
			ungroupDesc: "Aufheben aller Gruppierungen der selektierten Elemente"
		},

		ShapeMenuPlugin: {
			clickDrag: "Klicken oder ziehen",
			morphMsg: "Shape morphen",
			drag: "Ziehen"
		},


		Undo: {
			group: "Undo",
			undo: "Rückgängig",
			undoDesc: "Rückgängig",
			redo: "Wiederherstellen",
			redoDesc: "Wiederherstellen"
		},

		View: {
			group: "Zoom",
			zoomIn: "Vergrößern",
			zoomInDesc: "Vergrößern",
			zoomOut: "Verkleinern",
			zoomOutDesc: "Verkleinern",
			zoomStandard: "Originalgröße",
			zoomStandardDesc: "Originalgröße",
			zoomFitToModel: "Modelgröße",
			zoomFitToModelDesc: "Modelgröße"
		},

		JSONImport: {
			title: "JSON Import"
		},

		Edge: "Kante",
		Node: "Knoten",

		PropertyWindow: {
			oftenUsed: "Hauptattribute",
			title: "Eigenschaften",
			dateFormat: "d/m/y",
			moreProps: "Mehr Attribute"
		},

		SyntaxChecker: {
			desc: "Überprüfung der Verbindungen",
			group: "Verification",
			name: "Verbindungs-Checker",
			noErrors: "Es wurden keine Fehler gefunden.",
			notice: "Fehler bei roten Kreuz(en)."
		}
	}
});
