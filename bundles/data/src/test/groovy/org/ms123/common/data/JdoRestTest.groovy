/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ms123.common.data;

import org.junit.After
import org.junit.Before
import org.junit.Test
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.POST
import static groovyx.net.http.Method.PUT
import static groovyx.net.http.Method.DELETE



/**
 */
class JdoRestTest extends RestTest{

		static Long contactId;
		static Long activityId;
		String host = "http://admin:admin@localhost:8075";


		def deleteContact = [
			req: "${host}/testapp/data/contacts", 
			data:null,
		 	meth:DELETE 
		];

		def createContact = [
			req: "${host}/testapp/data/contacts", 
			data:'__data={"origin":"","company3":"12345", "company2":"12345", "shortname_company":"_company1","shortname_person":"_person1","_team_list":[{"teamid":"100000.100001"}]}',
		 	meth:POST, 
			resp:'' 
		];
		def createActivity = [
			req: "${host}/testapp/data/contacts/%contactId/activities", 
			data:'__data={"date":1307311200000,"done":false,"process":"v1","contact_type":"k1","address":"a1","note":"note1","_team_list":[{"teamid":"100000.100001"}]}',
		 	meth:POST, 
			resp:'' 
		];
		def createActivities = [ //Multi-Create
			req: "${host}/testapp/data/contacts/__idMaster__/activities", 
			data:'__data={"date":1307397600000,"done":true,"contact_type":"ct","process":"vx"}&__filter={"connector":"and","children":[{"connector":null,"field":"data.contacts.fieldset.set1","op":"cn","data":"_company3","children":[]}]}',
		 	meth:POST, 
			resp:'' 
		];

		def createCommunication = [
			req: "${host}/testapp/data/contacts/%contactId/communication", 
			data:'__data={"faxd1":"123456","mail1":"ms@tpso.com","www1":"http://wwww.tpso.com"}',
		 	meth:POST, 
			resp:'' 
		];
		def updateContact = [
			req: "${host}/testapp/data/contacts", 
			data:'__data={"origin":"","company3":"12345", "company2":"1234","shortname_company":"_company2","shortname_person":"_person2","_team_list":[{"teamid":"100000.100001"}]}',
		 	meth:PUT, 
			resp:'' 
		];
		def updateContacts = [ //Multi-Update
			req: "${host}/testapp/data/contacts", 
			data:'__data={"shortname_company":"_company3"}&__filter={"label":"1","connector":"and","children":[{"connector":null,"field":"data.contacts.fieldset.set1","op":"cn","data":"_person2","children":[]}]}',
		 	meth:PUT, 
			resp:'' 
		];
		def updateActivity1 = [
			req: "${host}/testapp/data/contacts/%contactId/activities", 
			data:'__data={"done":true,"process":"v2","contact_type":"k2","address":"a2","note":"note2","_team_list":[{teamid:"100000.100001"}]}',
		 	meth:PUT, 
			resp:'' 
		];
		def updateActivity2 = [
			req: "${host}/testapp/data/activities", 
			data:'__data={"done":true,"process":"v2","contact_type":"k3","address":"a3","note":"note3","_team_list":[{teamid:"100000.100001"}]}',
		 	meth:PUT, 
			resp:'' 
		];
		def updateCommunication = [
			req: "${host}/testapp/data/contacts/%contactId/communication", 
			data:'__data={"faxd1":"123457","mail1":"ms@xpso.com","www1":"http://wwww.xpso.com"}',
		 	meth:PUT, 
			resp:'' 
		];
		def getCommunication = [
			req: "${host}/testapp/data/contacts/%contactId/communication?what=asRow", 
			data:'',
		 	meth:GET, 
			resp:'' 
		];
		def queryContacts = [
			req: "${host}/testapp/data/contacts?query=true", 
			data:'filters={"label":"1","connector":"and","children":[{"connector":null,"field":"data.contacts.fieldset.set1","op":"cn","data":"_company3","children":[]},{"connector":null,"field":"communication.mail1","op":"cn","data":"ms@xpso","children":[]},{"connector":null,"field":"activities.note","op":"cn","data":"note3","children":[]}]}&fields=["shortname_company","shortname_person","company1","givenname"]&rows=100&page=1',
		  meth:POST, 
			resp:'' 
		];
		def queryActivities = [
			req: "${host}/testapp/data/contacts/%contactId/activities?orderby=date&query=true", 
			data:'filters=&rows=100&page=1',
		  meth:POST, 
			resp:'' 
		];
		def queryActivitiesWithFields = [
			req: "${host}/testapp/data/contacts/%contactId/activities?orderby=date&query=true", 
			data:'filters=&rows=100&page=1&fields=["note","process"]',
		  meth:POST, 
			resp:'' 
		];
		def queryActivitiesWithFieldsFQ = [
			req: "${host}"+'/testapp/data/contacts/%contactId/activities?orderby=contacts$activities.date&query=true', 
			data:'filters=&rows=100&page=1&fields=["contacts$activities.note","contacts$activities.process"]',
		  meth:POST, 
			resp:'' 
		];

    @Before
    void setUp() {
    }

    @After
    void tearDown() {
    }
    @Test
    void testAll() {
			createContact();
			updateContact();
			updateContacts();
			createActivity();
			createActivities();
			updateActivity1();
			updateActivity2();
			createCommunication();
			updateCommunication();
			getCommunication();
			queryContacts();
			queryActivities();
		//	queryActivitiesWithFields();
			queryActivitiesWithFieldsFQ();
			deleteContacts();
		}

    //@Test
    void createContact() {
			def ret = callTest(createContact.req, createContact.meth, createContact.data );
			assert ret instanceof Map
			assert ret.id instanceof Number
			contactId = ret.id;
    }

    //@Test
    void updateContact() {
			def ret = callTest(updateContact.req+"/"+contactId, updateContact.meth, updateContact.data );
			assert ret instanceof Map
    }
    //@Test
    void updateContacts() {
			def ret = callTest(updateContacts.req, updateContacts.meth, updateContacts.data );
			assert ret instanceof Map
    }

    //@Test
    void createActivity() {
			def req = createActivity.req.replace("%contactId", contactId+"");
			System.out.println("req:"+req);
			def ret = callTest(req, createActivity.meth, createActivity.data );
			assert ret instanceof Map
			assert ret.id instanceof Number
			activityId = ret.id;
    }
    //@Test
    void createActivities() {
			def ret = callTest(createActivities.req, createActivities.meth, createActivities.data );
			assert ret instanceof Map
			assert ret.ids instanceof List
    }
    //@Test
    void updateActivity1() {
			def req = updateActivity1.req.replace("%contactId", contactId+"");
			System.out.println("req:"+req);
			def ret = callTest(req+"/"+activityId, updateActivity1.meth, updateActivity1.data );
			assert ret instanceof Map
    }

    //@Test
    void updateActivity2() {
			def ret = callTest(updateActivity2.req+"/"+activityId, updateActivity2.meth, updateActivity2.data );
			assert ret instanceof Map
    }

    //@Test
    void createCommunication() {
			def req = createCommunication.req.replace("%contactId", contactId+"");
			def ret = callTest(req, createCommunication.meth, createCommunication.data );
			assert ret instanceof Map
			assert ret.id instanceof Number
    }
    //@Test
    void updateCommunication() {
			def req = updateCommunication.req.replace("%contactId", contactId+"");
			def ret = callTest(req, updateCommunication.meth, updateCommunication.data );
			assert ret instanceof Map
    }
    //@Test
    void getCommunication() {
			def req = getCommunication.req.replace("%contactId", contactId+"");
			def ret = callTest(req, getCommunication.meth, getCommunication.data );
			assert ret instanceof Map
			assert ret["faxd1"] == "123457"
    }
    //@Test
    void queryContacts() {
			def ret = callTest(queryContacts.req, queryContacts.meth, queryContacts.data );
			assert ret instanceof Map
			assert ret.rows instanceof List
			assert ret.rows.size() > 0
			assert ret.rows[0].shortname_person == "_person2"
    }
    //@Test
    void queryActivities() {
			def req = queryActivities.req.replace("%contactId", contactId+"");
			def ret = callTest(req, queryActivities.meth, queryActivities.data );
			assert ret instanceof Map
			assert ret.rows instanceof List
			assert ret.rows.size() > 0
			assert ret.rows[0].process == "v2"
    }
    //@Test
    void queryActivitiesWithFields() {
			def req = queryActivitiesWithFields.req.replace("%contactId", contactId+"");
			def ret = callTest(req, queryActivitiesWithFields.meth, queryActivitiesWithFields.data );
			assert ret instanceof Map
			assert ret.rows instanceof List
			assert ret.rows.size() > 0
			assert ret.rows[0].done == null
			assert ret.rows[0].process == "v2"
    }
    //@Test
    void queryActivitiesWithFieldsFQ() {
			def req = queryActivitiesWithFieldsFQ.req.replace("%contactId", contactId+"");
			def ret = callTest(req, queryActivitiesWithFieldsFQ.meth, queryActivitiesWithFieldsFQ.data );
			assert ret instanceof Map
			assert ret.rows instanceof List
			assert ret.rows.size() > 0
			assert ret.rows[0].done == null
			assert ret.rows[0]['contacts$activities.process'] == "v2"
    }
    //@Test
    void deleteContacts() {
				def ret = callTest(deleteContact.req+"/"+contactId, deleteContact.meth, deleteContact.data );
				assert ret instanceof Map
    }
}
