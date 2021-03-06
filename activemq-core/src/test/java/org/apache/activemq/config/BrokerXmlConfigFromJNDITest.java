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
package org.apache.activemq.config;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.test.JmsTopicSendReceiveWithTwoConnectionsTest;

import javax.naming.InitialContext;
import javax.naming.Context;

import java.io.File;
import java.util.Hashtable;

/**
 * @version $Revision: 467693 $
 */
public class BrokerXmlConfigFromJNDITest extends JmsTopicSendReceiveWithTwoConnectionsTest {

    protected ActiveMQConnectionFactory createConnectionFactory() throws Exception {

        final Hashtable<String, String> properties = new Hashtable<>();
        properties.put("java.naming.factory.initial", "org.apache.activemq.jndi.ActiveMQInitialContextFactory");

        // configure the embedded broker using an XML config file
        // which is either a URL or a resource on the classpath
        
        final String providerUrl = "vm://localhost?brokerConfig=xbean:classpath:activemq.xml";
        properties.put(Context.PROVIDER_URL, providerUrl);

        final InitialContext context = new InitialContext(properties);
        return (ActiveMQConnectionFactory) context.lookup("ConnectionFactory");

    }

}
