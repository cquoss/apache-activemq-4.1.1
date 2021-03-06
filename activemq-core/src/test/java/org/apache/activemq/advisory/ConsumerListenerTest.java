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
package org.apache.activemq.advisory;

import edu.emory.mathcs.backport.java.util.concurrent.ArrayBlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.BlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

import org.apache.activemq.EmbeddedBrokerTestSupport;
import org.apache.activemq.advisory.ConsumerEvent;
import org.apache.activemq.advisory.ConsumerEventSource;
import org.apache.activemq.advisory.ConsumerListener;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;

/**
 * 
 * @version $Revision: 426366 $
 */
public class ConsumerListenerTest extends EmbeddedBrokerTestSupport implements ConsumerListener {

    protected Session consumerSession1;
    protected Session consumerSession2;
    protected int consumerCounter;
    protected ConsumerEventSource consumerEventSource;
    protected BlockingQueue eventQueue = new ArrayBlockingQueue(1000);
    private Connection connection;

    public void testConsumerEvents() throws Exception {
        consumerEventSource.start();

        consumerSession1 = createConsumer();
        assertConsumerEvent(1, true);

        consumerSession2 = createConsumer();
        assertConsumerEvent(2, true);

        consumerSession1.close();
        consumerSession1 = null;
        assertConsumerEvent(1, false);

        consumerSession2.close();
        consumerSession2 = null;
        assertConsumerEvent(0, false);
    }

    public void testListenWhileAlreadyConsumersActive() throws Exception {
        consumerSession1 = createConsumer();
        consumerSession2 = createConsumer();

        consumerEventSource.start();
        assertConsumerEvent(2, true);
        assertConsumerEvent(2, true);

        consumerSession1.close();
        consumerSession1 = null;
        assertConsumerEvent(1, false);

        consumerSession2.close();
        consumerSession2 = null;
        assertConsumerEvent(0, false);
    }

    public void onConsumerEvent(ConsumerEvent event) {
        eventQueue.add(event);
    }

    protected void setUp() throws Exception {
        super.setUp();

        connection = createConnection();
        connection.start();
        consumerEventSource = new ConsumerEventSource(connection, destination);
        consumerEventSource.setConsumerListener(this);
    }

    protected void tearDown() throws Exception {
        if (consumerEventSource != null) {
            consumerEventSource.stop();
        }
        if (consumerSession2 != null) {
            consumerSession2.close();
        }
        if (consumerSession1 != null) {
            consumerSession1.close();
        }
        if (connection != null) {
            connection.close();
        }
        super.tearDown();
    }

    protected void assertConsumerEvent(int count, boolean started) throws InterruptedException {
        ConsumerEvent event = waitForConsumerEvent();
        assertEquals("Consumer count", count, event.getConsumerCount());
        assertEquals("started", started, event.isStarted());
    }

    protected Session createConsumer() throws JMSException {
        final String consumerText = "Consumer: " + (++consumerCounter);
        log.info("Creating consumer: " + consumerText + " on destination: " + destination);
        
        Session answer = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        MessageConsumer consumer = answer.createConsumer(destination);
        consumer.setMessageListener(new MessageListener() {
            public void onMessage(Message message) {
                log.info("Received message by: " + consumerText + " message: " + message);
            }
        });
        return answer;
    }

    protected ConsumerEvent waitForConsumerEvent() throws InterruptedException {
        ConsumerEvent answer = (ConsumerEvent) eventQueue.poll(100000, TimeUnit.MILLISECONDS);
        assertTrue("Should have received a consumer event!", answer != null);
        return answer;
    }

}
