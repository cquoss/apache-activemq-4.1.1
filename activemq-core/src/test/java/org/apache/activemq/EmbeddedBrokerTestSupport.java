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
package org.apache.activemq;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;

import junit.framework.TestCase;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jms.core.JmsTemplate;

/**
 * A useful base class which creates and closes an embedded broker
 * 
 * @version $Revision: 1.1 $
 */
public abstract class EmbeddedBrokerTestSupport extends TestCase {

    protected static final Log log = LogFactory.getLog(EmbeddedBrokerTestSupport.class);
    
    protected BrokerService broker;
    //protected String bindAddress = "tcp://localhost:61616";
    protected String bindAddress = "vm://localhost";
    protected ConnectionFactory connectionFactory;
    protected boolean useTopic = false;
    protected Destination destination;
    protected JmsTemplate template;
    private boolean usePooledConnectionWithTemplate = true;

    protected void setUp() throws Exception {
        if (broker == null) {
            broker = createBroker();
        }
        startBroker();

        connectionFactory = createConnectionFactory();

        destination = createDestination();

        template = createJmsTemplate();
        template.setDefaultDestination(destination);
        template.setPubSubDomain(useTopic);
        template.afterPropertiesSet();
    }

    protected void tearDown() throws Exception {
        if (broker != null) {
            broker.stop();
        }
    }

    /**
     * Factory method to create a new {@link JmsTemplate}
     * 
     * @return a newly created JmsTemplate
     */
    protected JmsTemplate createJmsTemplate() {
        if (usePooledConnectionWithTemplate) {
            // lets use a pool to avoid creating and closing producers
            return new JmsTemplate(new PooledConnectionFactory(bindAddress));
        }
        else {
            return new JmsTemplate(connectionFactory);
        }
    }

    /**
     * Factory method to create a new {@link Destination}
     * 
     * @return newly created Destinaiton
     */
    protected Destination createDestination() {
        return createDestination(getDestinationString());
    }

    /**
     * Factory method to create the destination in either the queue or topic
     * space based on the value of the {@link #useTopic} field
     */
    protected Destination createDestination(String subject) {
        if (useTopic) {
            return new ActiveMQTopic(subject);
        }
        else {
            return new ActiveMQQueue(subject);
        }
    }

    /**
     * Returns the name of the destination used in this test case
     */
    protected String getDestinationString() {
        return getClass().getName() + "." + getName();
    }

    /**
     * Factory method to create a new {@link ConnectionFactory} instance
     * 
     * @return a newly created connection factory
     */
    protected ConnectionFactory createConnectionFactory() throws Exception {
        return new ActiveMQConnectionFactory(bindAddress);
    }

    /**
     * Factory method to create a new broker
     * 
     * @throws Exception
     */
    protected BrokerService createBroker() throws Exception {
        BrokerService answer = new BrokerService();
        answer.setPersistent(isPersistent());
        answer.addConnector(bindAddress);
        return answer;
    }

    protected void startBroker() throws Exception {
        broker.start();
    }

    /**
     * @return whether or not persistence should be used
     */
    protected boolean isPersistent() {
        return false;
    }

    /**
     * Factory method to create a new connection
     */
    protected Connection createConnection() throws Exception {
        return connectionFactory.createConnection();
    }
}
