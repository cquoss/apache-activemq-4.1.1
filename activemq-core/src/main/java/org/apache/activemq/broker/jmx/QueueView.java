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
package org.apache.activemq.broker.jmx;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.OpenDataException;

import org.apache.activemq.broker.ConnectionContext;
import org.apache.activemq.broker.region.Queue;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.command.Message;

/**
 * Provides a JMX Management view of a Queue.
 */
public class QueueView extends DestinationView implements QueueViewMBean{
    public QueueView(ManagedRegionBroker broker, Queue destination){
        super(broker, destination);
    }

    public CompositeData getMessage(String messageId) throws OpenDataException{
        Message rc=((Queue) destination).getMessage(messageId);
        if(rc==null)
            return null;
        return OpenTypeSupport.convert(rc);
    }

    public void purge(){
        ((Queue) destination).purge();
    }

    public boolean removeMessage(String messageId) throws Exception{
        return ((Queue) destination).removeMessage(messageId);
    }

    public int removeMatchingMessages(String selector) throws Exception {
        return ((Queue) destination).removeMatchingMessages(selector);
    }
    
    public int removeMatchingMessages(String selector, int maximumMessages) throws Exception {
        return ((Queue) destination).removeMatchingMessages(selector, maximumMessages);
    }
    
    public boolean copyMessageTo(String messageId, String destinationName) throws Exception {
        ConnectionContext context = BrokerView.getConnectionContext(broker.getContextBroker());
        ActiveMQDestination toDestination = ActiveMQDestination.createDestination(destinationName, ActiveMQDestination.QUEUE_TYPE);
        return ((Queue) destination).copyMessageTo(context, messageId, toDestination);
    }

    public int copyMatchingMessagesTo(String selector, String destinationName) throws Exception {
        ConnectionContext context = BrokerView.getConnectionContext(broker.getContextBroker());
        ActiveMQDestination toDestination = ActiveMQDestination.createDestination(destinationName, ActiveMQDestination.QUEUE_TYPE);
        return ((Queue) destination).copyMatchingMessagesTo(context, selector, toDestination);
    }
    
    public int copyMatchingMessagesTo(String selector, String destinationName, int maximumMessages) throws Exception {
        ConnectionContext context = BrokerView.getConnectionContext(broker.getContextBroker());
        ActiveMQDestination toDestination = ActiveMQDestination.createDestination(destinationName, ActiveMQDestination.QUEUE_TYPE);
        return ((Queue) destination).copyMatchingMessagesTo(context, selector, toDestination, maximumMessages);
    }
    
    public boolean moveMessageTo(String messageId, String destinationName) throws Exception {
        ConnectionContext context = BrokerView.getConnectionContext(broker.getContextBroker());
        ActiveMQDestination toDestination = ActiveMQDestination.createDestination(destinationName, ActiveMQDestination.QUEUE_TYPE);
        return ((Queue) destination).moveMessageTo(context, messageId, toDestination);
    }
    
    public int moveMatchingMessagesTo(String selector, String destinationName) throws Exception {
        ConnectionContext context = BrokerView.getConnectionContext(broker.getContextBroker());
        ActiveMQDestination toDestination = ActiveMQDestination.createDestination(destinationName, ActiveMQDestination.QUEUE_TYPE);
        return ((Queue) destination).moveMatchingMessagesTo(context, selector, toDestination);
    }
    
    public int moveMatchingMessagesTo(String selector, String destinationName, int maximumMessages) throws Exception {
        ConnectionContext context = BrokerView.getConnectionContext(broker.getContextBroker());
        ActiveMQDestination toDestination = ActiveMQDestination.createDestination(destinationName, ActiveMQDestination.QUEUE_TYPE);
        return ((Queue) destination).moveMatchingMessagesTo(context, selector, toDestination, maximumMessages);
    }

}
