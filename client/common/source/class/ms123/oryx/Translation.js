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
qx.Class.define("ms123.oryx.Translation", {
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
			BPMN2_DATA_INPUT_WITH_INCOMING_DATA_ASSOCIATION: "Ein Dateninput darf keine ausgehenden Datenassoziationen haben.",
			BPMN2_DATA_OUTPUT_WITH_OUTGOING_DATA_ASSOCIATION: "Ein Datenoutput darf keine eingehenden Datenassoziationen haben.",
			BPMN2_EVENT_BASED_EVENT_TARGET_CONTRADICTION: "Wenn Nachrichten-Zwischenereignisse im Diagramm verwendet werden, dann dürfen Receive Tasks nicht verwendet werden und umgekehrt.",
			BPMN2_EVENT_BASED_NOT_INSTANTIATING: "Das Gateway erfüllt nicht die Voraussetzungen um den Prozess zu instantiieren. Bitte verwenden Sie ein Start-Ereignis oder setzen Sie die Instanziierungs-Attribute korrekt.",
			BPMN2_EVENT_BASED_TARGET_WITH_TOO_MANY_INCOMING_SEQUENCE_FLOWS: "Ziele von Ereignis-basierten Gateways dürfen nicht mehr als einen eingehenden Sequenzfluss haben.",
			BPMN2_EVENT_BASED_WITH_TOO_LESS_INCOMING_SEQUENCE_FLOWS: "Ein Ereignis-basiertes Gateway, dass nicht instanziierend ist, muss mindestens einen eingehenden Kontrollfluss besitzen.",
			BPMN2_EVENT_BASED_WITH_TOO_LESS_OUTGOING_SEQUENCE_FLOWS: "Ein Ereignis-basiertes Gateway muss 2 oder mehr ausgehende Sequenzflüsse besitzen.",
			BPMN2_EVENT_BASED_WRONG_CONDITION_EXPRESSION: "Die ausgehenden Sequenzflüsse eines Ereignis-Gateways dürfen keinen Bedingungsausdruck besitzen.",
			BPMN2_EVENT_BASED_WRONG_TRIGGER: "Nur die folgenden Zwischen-Ereignis-Auslöser sind hier zulässig: Nachricht, Signal, Timer, Bedingungs und Mehrfach.",
			BPMN2_EVENT_SUBPROCESS_BAD_CONNECTION: "Ein Ereignis-Unterprozess darf keinen eingehenden oder ausgehenden Sequenzfluss besitzen.",
			BPMN2_GATEWAYDIRECTION_CONVERGING_FAILURE: "Das Gateway muss mehrere eingehende aber darf keine mehrfache ausgehende Sequenzflüsse besitzen.",
			BPMN2_GATEWAYDIRECTION_DIVERGING_FAILURE: "Das Gateway darf keine mehrfachen eingehenden aber muss mehrfache ausgehende Sequenzflüsse besitzen.",
			BPMN2_GATEWAYDIRECTION_MIXED_FAILURE: "Das Gateway muss mehrere eingehende und ausgehende Sequenzflüsse besitzen.",
			BPMN2_GATEWAY_WITH_NO_OUTGOING_SEQUENCE_FLOW: "Ein Gateway muss mindestens einen ausgehenden Sequenzfluss besitzen.",
			BPMN2_RECEIVE_TASK_WITH_ATTACHED_EVENT: "Empfangende Tasks, die in Ereignis-Gateway-Konfigurationen benutzt werden, dürfen keine angehefteten Zwischen-Ereignisse besitzen.",
			BPMN2_TOO_FEW_INITIATING_PARTICIPANTS: "Eine Choreographie-Aktivität musst genau einen initiierenden Teilnehmer (weiß) besitzen.",
			BPMN2_TOO_MANY_INITIATING_MESSAGES: "Eine Choreographie-Aktivität darf nur eine initiierende Nachricht besitzen.",
			BPMN2_TOO_MANY_INITIATING_PARTICIPANTS: "Eine Choreographie-Aktivität darf nicht mehr als einen initiierenden Teilnehmer (weiß) besitzen.",
			BPMN_ATTACHEDINTERMEDIATEEVENT_WITH_INCOMING_CONTROL_FLOW: "Angeheftete Zwischen-Ereignisse dürfen keinen eingehenden Sequenzfluss haben.",
			BPMN_ATTACHEDINTERMEDIATEEVENT_WITHOUT_OUTGOING_CONTROL_FLOW: "Angeheftete Zwischen-Ereignisse müssen genau einen ausgehenden Sequenzfluss haben.",
			BPMN_DIFFERENT_PROCESS: "Ursprungs- und Zielknoten müssen im gleichen Prozess sein.",
			BPMN_ENDEVENT_WITH_OUTGOING_CONTROL_FLOW: "End-Ereignisse dürfen keinen ausgehenden Sequenzfluss haben.",
			BPMN_ENDEVENT_WITHOUT_INCOMING_CONTROL_FLOW: "Ein End-Ereignis muss einen eingehenden Sequenzfluss haben.",
			BPMN_EVENTBASEDGATEWAY_BADCONTINUATION: "Auf Ereignis-basierte Gateways dürfen weder Gateways noch Subprozesse folgen.",
			BPMN_FLOWOBJECT_NOT_CONTAINED_IN_PROCESS: "Ein Kontrollflussobjekt muss sich in einem Prozess befinden.",
			BPMN_MESSAGE_FLOW_NOT_ALLOWED: "Ein Nachrichtenfluss ist an dieser Stelle nicht erlaubt.",
			BPMN_MESSAGE_FLOW_NOT_CONNECTED: "Mindestens ein Ende des Nachrichtenflusses muss mit einem anderen Objekt verbunden sein.",
			BPMN_NODE_NOT_ALLOWED: "Knotentyp ist nicht erlaubt.",
			BPMN_NO_SOURCE: "Eine Kante muss einen Ursprung haben.",
			BPMN_NO_TARGET: "Eine Kante muss ein Ziel haben.",
			BPMN_SAME_PROCESS: "Ursprungs- und Zielknoten müssen in verschiedenen Pools enthalten sein.",
			BPMN_STARTEVENT_WITH_INCOMING_CONTROL_FLOW: "Start-Ereignisse dürfen keinen eingehenden Sequenzfluss haben.",
			BPMN_STARTEVENT_WITHOUT_OUTGOING_CONTROL_FLOW: "Ein Start-Ereignis muss einen ausgehenden Sequenzfluss haben.",
			checkingMessage: "Überprüfung wird durchgeführt ...",
			COMMUNICATION_AT_LEAST_TWO_PARTICIPANTS: "Die Kommunikation oder Sub-Konversation muss mit mindestens zwei Teilnehmern verbunden sein.",
			CONV_LINK_CANNOT_CONNECT_CONV_NODES: "Der Konversationslink muss eine Kommunikation oder Sub-Konversation mit einem Teilnehmer verbinden.",
			desc: "Überprüfung der Verbindungen",
			group: "Verification",
			IBPMN_NO_INCOMING_SEQFLOW: "Dieser Knoten muss eingehenden Sequenzfluss besitzen.",
			IBPMN_NO_OUTGOING_SEQFLOW: "Dieser Knoten muss ausgehenden Sequenzfluss besitzen.",
			IBPMN_NO_ROLE_SET: "Für Interaktionen muss ein Sender und ein Empfänger definiert sein.",
			InteractionNet_MESSAGETYPE_NOT_SET: "Nachrichtentyp ist nicht definiert.",
			InteractionNet_RECEIVER_NOT_SET: "Empfänger ist nicht definiert",
			InteractionNet_ROLE_NOT_SET: "Rolle ist nicht definiert.",
			InteractionNet_SENDER_NOT_SET: "Sender ist nicht definiert",
			invalid: "Ungültige Antwort vom Server.",
			MESSAGEFLOW_END_MUST_BE_PARTICIPANT: "Das Nachrichtenflussziel muss ein Teilnehmer sein.",
			MESSAGEFLOW_START_MUST_BE_PARTICIPANT: "Die Nachrichtenflussquelle muss ein Teilnehmer sein.",
			MULT_ERRORS: "Mehrere Fehler",
			name: "Verbindungs-Checker",
			noErrors: "Es wurden keine Fehler gefunden.",
			notice: "Fehler bei roten Kreuz(en)."
		},

		RESIZE: {
			tipGrow: "Zeichenfläche vergrößern:",
			tipShrink: "Zeichenfläche verkleinern:",
			N: "Nach oben",
			W: "Nach links",
			S : "Nach unten",
			E : "Nach rechts"
		}
	}
});
