/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.security;

import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.jaas.GroupPrincipal;

import java.util.*;
import java.util.Set;

import junit.framework.TestCase;

/**
 * 
 * @version $Revision: 426366 $
 */
public class AuthorizationMapTest extends TestCase {
    static final GroupPrincipal guests = new GroupPrincipal("guests");
    static final GroupPrincipal users = new GroupPrincipal("users");
    static final GroupPrincipal admins = new GroupPrincipal("admins");

    public void testAuthorizationMap() {
        AuthorizationMap map = createAuthorizationMap();

        Set readACLs = map.getReadACLs(new ActiveMQQueue("USERS.FOO.BAR"));
        assertEquals("set size: " + readACLs, 2, readACLs.size());
        assertTrue("Contains users group", readACLs.contains(admins));
        assertTrue("Contains users group", readACLs.contains(users));
    }

    protected AuthorizationMap createAuthorizationMap() {
        DefaultAuthorizationMap answer = new DefaultAuthorizationMap();

        List entries = new ArrayList();

        AuthorizationEntry entry = new AuthorizationEntry();
        entry.setQueue(">");
        entry.setRead("admins");
        entries.add(entry);

        entry = new AuthorizationEntry();
        entry.setQueue("USERS.>");
        entry.setRead("users");
        entries.add(entry);

        answer.setAuthorizationEntries(entries);

        return answer;
    }

}
