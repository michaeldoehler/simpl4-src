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
class JcrRestTest extends RestTest{

		String host = "http://admin:admin@localhost:8075";
		String traitName;

		def userData = [
			req: "${host}/testapp/xconfig/user?what=user", 
			data:null,
		 	meth:GET 
		];
		def traitData = [
			req: "${host}/testapp/meta/traits/%__trait?what=asRow", 
			data:null,
		 	meth:GET 
		];
		def createTrait = [
			req: "${host}/testapp/meta/traits/", 
			data:'traitid=merkmal1&user_manage=admin',
		 	meth:POST 
		];
		def deleteTrait = [
			req: "${host}/testapp/meta/traits", 
		 	meth:DELETE, 
			data:null
		];
		def fieldsAsMapList = [
			req: "${host}/testapp/xconfig/configfields/enumdata/main-form", 
			data:null,
		 	meth:GET 
		];
		def fieldsAsList = [
			req: "${host}/testapp/xconfig/configfields/fieldset/main-form", 
			data:null,
		 	meth:GET 
		];
		def createUser = [
			req: "${host}/testapp/security/authorizables/user/testuser1", 
			data:'__data={"userid":"testuser1","email":"","givenname":"","surname":"","password":"","read":true,"manage":true,"admin":true,"trait_manage":"","groups":["testgroup1"]}',
		 	meth:PUT 
		];
		def createUserEmpty = [
			req: "${host}/testapp/security/authorizables/user", 
			data:'__data={"userid":""}',
		 	meth:POST 
		];
		def createUserWrong = [
			req: "${host}/testapp/security/authorizables/user", 
			data:'__data={"userid":"Xxx"}',
		 	meth:POST 
		];
		def createGroup = [
			req: "${host}/testapp/security/authorizables/group/testgroup1", 
			data:'__data={"groupid":"testgroup1","description":"","config":"","read":"","manage":true,"admin":true,"trait_manage":true}',
		 	meth:PUT 
		];
		def deleteUser = [
			req: "${host}/testapp/security/authorizables/user/testuser1", 
		 	meth:DELETE, 
			data:null
		];
		def deleteGroup = [
			req: "${host}/testapp/security/authorizables/group/testgroup1", 
		 	meth:DELETE, 
			data:null
		];
		def queryUsers = [
			req: "${host}/testapp/security/authorizables/user?query=true", 
			data:'',
		 	meth:POST 
		];
		def checkActivitiUser = [
			req: "${host}/testapp/workflow/user/testuser1", 
			data:'',
		 	meth:GET 
		];
		def queryEnumData = [
			req: "${host}/testapp/meta/enums/anreden/data?nosql=true&module=enumdata&query=true", 
			data:'filters=&rows=100&page=1',
		 	meth:POST 
		];
		def createEnumField = [
			req: "${host}/testapp/meta/enums/anreden/fields/fieldneu?module=enumfield", 
			data:'__data={"fieldname":"fieldneu","description":"ddddd"}',
		 	meth:PUT 
		];
		def deleteEnumField = [
			req: "${host}/testapp/meta/enums/anreden/fields/fieldneu", 
		 	meth:DELETE, 
			data:null
		];
		def createEnumData = [
			req: "${host}/testapp/meta/enums/robinson/data/key1?module=enumdata", 
			data:'__data={"key":"key1","bedeutung":"desc1","sort":"0","invalid":"false","invalid_from":1308607200000}',
		 	meth:PUT 
		];
		def deleteEnumData = [
			req: "${host}/testapp/meta/enums/robinson/data/key1", 
		 	meth:DELETE, 
			data:null
		];

    @Test
    void testAll() {
			getUserData();
			createTrait();
			getTraitData();
			deleteTrait();
			getFieldsAsMapList();
			getFieldsAsList();
			getEnumData();
			createEnumField();
			deleteEnumField();
			createEnumData();
			deleteEnumData();
			createGroup();
			createUser();
			createUserEmpty();
			createUserWrong();
			checkActivitiUser();
			queryUsers();
			deleteUser();
			checkActivitiUser2();
			deleteGroup();
		}
    //@Test
    void getFieldsAsMapList() {
			def ret = callTest(fieldsAsMapList.req, fieldsAsMapList.meth, fieldsAsMapList.data );
			assert ret instanceof List
			assert ret  != null
    }
    //@Test
    void getFieldsAsList() {
			def ret = callTest(fieldsAsList.req, fieldsAsList.meth, fieldsAsList.data );
			assert ret instanceof List
    }
    //@Test
    void createEnumData() {
			def ret = callTest(createEnumData.req, createEnumData.meth, createEnumData.data );
			assert ret instanceof Map
			assert ret.id == "key1"
    }
    //@Test
    void deleteEnumData() {
			def ret = callTest(deleteEnumData.req, deleteEnumData.meth, deleteEnumData.data );
			assert ret instanceof Map
    }
    //@Test
    void createEnumField() {
			def ret = callTest(createEnumField.req, createEnumField.meth, createEnumField.data );
			assert ret instanceof Map
			assert ret.id == "fieldneu"
    }
    //@Test
    void deleteEnumField() {
			def ret = callTest(deleteEnumField.req, deleteEnumField.meth, deleteEnumField.data );
			assert ret instanceof Map
    }
    //@Test
    void getEnumData() {
			def ret = callTest(queryEnumData.req, queryEnumData.meth, queryEnumData.data );
			assert ret instanceof Map
			assert ret.rows != null
			assert ret.rows instanceof List
			assert ret.total > 0
    }
    //@Test
    void createTrait() {
			def ret = callTest(createTrait.req, createTrait.meth, createTrait.data );
			assert ret instanceof Map
			assert ret.list != null
			traitName = ret.list.get(0).name;
    }
    //@Test
    void deleteTrait() {
			def ret = callTest(deleteTrait.req+"/"+traitName, deleteTrait.meth, deleteTrait.data );
			assert ret instanceof Map
    }
    //@Test
    void getTraitData() {
			def req = traitData.req.replace("%__trait", traitName);
			def ret = callTest(req, traitData.meth, traitData.data );
			assert ret instanceof Map
			assert ret.nodename == traitName
    }
    //@Test
    void getUserData() {
			def ret = callTest(userData.req, userData.meth, userData.data );
			assert ret instanceof Map
			assert ret.userid != null
    }
    //@Test
    void createUser() {
			def ret = callTest(createUser.req, createUser.meth, createUser.data );
			assert ret instanceof Map
			assert ret.id instanceof String
			assert ret.id == "testuser1"
    }
    //@Test
    void createUserEmpty() {
			def ret = callTest(createUserEmpty.req, createUserEmpty.meth, createUserEmpty.data );
			assert ret instanceof Map
			assert ret.constraintViolations != null
    }
    //@Test
    void createUserWrong() {
			def ret = callTest(createUserWrong.req, createUserWrong.meth, createUserWrong.data );
			assert ret instanceof Map
			assert ret.constraintViolations != null
    }
    //@Test
    void createGroup() {
			def ret = callTest(createGroup.req, createGroup.meth, createGroup.data );
			assert ret instanceof Map
			assert ret.id instanceof String
			assert ret.id == "testgroup1"
    }
    //@Test
    void deleteUser() {
			def ret = callTest(deleteUser.req, deleteUser.meth, deleteUser.data );
			assert ret instanceof Map
    }
    //@Test
    void deleteGroup() {
			def ret = callTest(deleteGroup.req, deleteGroup.meth, deleteGroup.data );
			assert ret instanceof Map
    }
    //@Test
    void queryUsers() {
			def ret = callTest(queryUsers.req, queryUsers.meth, queryUsers.data );
			assert ret instanceof Map
			assert ret.rows instanceof List
			assert ret.rows.size() > 0
    }
    //@Test
    void checkActivitiUser() {
			def ret = callTest(checkActivitiUser.req, checkActivitiUser.meth, checkActivitiUser.data );
			assert ret instanceof Map
			assert ret.id instanceof String
			assert ret.id == "testuser1"
    }
    //@Test
    void checkActivitiUser2() {
			def ret = callTest(checkActivitiUser.req, checkActivitiUser.meth, checkActivitiUser.data );
			assert ret instanceof Map
			assert ret.size() ==  0
			assert ret.id ==  null
    }
}
