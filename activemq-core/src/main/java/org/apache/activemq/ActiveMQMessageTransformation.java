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

import java.util.Enumeration;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.MessageEOFException;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.StreamMessage;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.TextMessage;
import javax.jms.Topic;


import org.apache.activemq.command.ActiveMQBytesMessage;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.activemq.command.ActiveMQMessage;
import org.apache.activemq.command.ActiveMQObjectMessage;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQStreamMessage;
import org.apache.activemq.command.ActiveMQTempQueue;
import org.apache.activemq.command.ActiveMQTempTopic;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.activemq.command.ActiveMQTopic;

/**
 * A helper class for converting normal JMS interfaces into ActiveMQ specific ones.
 * 
 * @version $Revision: 1.1 $
 */
public class ActiveMQMessageTransformation {

	/**
     * Creates a an available JMS message from another provider.
	 * 
	 * @param destination -  Destination to be converted into ActiveMQ's implementation.
	 * @return ActiveMQDestination - ActiveMQ's implementation of the destination.
	 * @throws JMSException if an error occurs
	 */
	public static ActiveMQDestination transformDestination(Destination destination) throws JMSException {
        ActiveMQDestination activeMQDestination = null;
        
        if (destination != null) {
            if (destination instanceof ActiveMQDestination) {
            	return (ActiveMQDestination) destination;
            	
            }
            else {
                if (destination instanceof TemporaryQueue) {
                	activeMQDestination = new ActiveMQTempQueue(((Queue) destination).getQueueName());
                }
                else if (destination instanceof TemporaryTopic) {
                	activeMQDestination = new ActiveMQTempTopic(((Topic) destination).getTopicName());
                }
                else if (destination instanceof Queue) {
                	activeMQDestination = new ActiveMQQueue(((Queue) destination).getQueueName());
                }
                else if (destination instanceof Topic) {
                	activeMQDestination = new ActiveMQTopic(((Topic) destination).getTopicName());
                }
            }
        }

        return activeMQDestination;		
	}
	
	
    /**
     * Creates a fast shallow copy of the current ActiveMQMessage or creates a whole new
     * message instance from an available JMS message from another provider.
     *
     * @param message - Message to be converted into ActiveMQ's implementation.
     * @param connection 
     * @return ActiveMQMessage -  ActiveMQ's implementation object of the message.
     * @throws JMSException if an error occurs
     */
	public static final ActiveMQMessage transformMessage(Message message, ActiveMQConnection connection) throws JMSException {
        if (message instanceof ActiveMQMessage) {
            return (ActiveMQMessage) message;

        } else {
            ActiveMQMessage activeMessage = null;

            if (message instanceof BytesMessage) {
                BytesMessage bytesMsg = (BytesMessage) message;
                bytesMsg.reset();
                ActiveMQBytesMessage msg = new ActiveMQBytesMessage();
                msg.setConnection(connection);
                try {
                    for (;;) {
                        // Reads a byte from the message stream until the stream
                        // is empty
                        msg.writeByte(bytesMsg.readByte());
                    }
                } catch (MessageEOFException e) {
                    // if an end of message stream as expected
                } catch (JMSException e) {
                }

                activeMessage = msg;
            } else if (message instanceof MapMessage) {
                MapMessage mapMsg = (MapMessage) message;
                ActiveMQMapMessage msg = new ActiveMQMapMessage();
                msg.setConnection(connection);
                Enumeration iter = mapMsg.getMapNames();

                while (iter.hasMoreElements()) {
                    String name = iter.nextElement().toString();
                    msg.setObject(name, mapMsg.getObject(name));
                }

                activeMessage = msg;
            } else if (message instanceof ObjectMessage) {
                ObjectMessage objMsg = (ObjectMessage) message;
                ActiveMQObjectMessage msg = new ActiveMQObjectMessage();
                msg.setConnection(connection);
                msg.setObject(objMsg.getObject());
                msg.storeContent();
                activeMessage = msg;
            } else if (message instanceof StreamMessage) {
                StreamMessage streamMessage = (StreamMessage) message;
                streamMessage.reset();
                ActiveMQStreamMessage msg = new ActiveMQStreamMessage();
                msg.setConnection(connection);
                Object obj = null;

                try {
                    while ((obj = streamMessage.readObject()) != null) {
                        msg.writeObject(obj);
                    }
                } catch (MessageEOFException e) {
                    // if an end of message stream as expected
                } catch (JMSException e) {
                }

                activeMessage = msg;
            } else if (message instanceof TextMessage) {
                TextMessage textMsg = (TextMessage) message;
                ActiveMQTextMessage msg = new ActiveMQTextMessage();
                msg.setConnection(connection);
                msg.setText(textMsg.getText());
                activeMessage = msg;
            } else {
                activeMessage = new ActiveMQMessage();
                activeMessage.setConnection(connection);
            }

            activeMessage.setJMSMessageID(message.getJMSMessageID());
            activeMessage.setJMSCorrelationID(message.getJMSCorrelationID());
            activeMessage.setJMSReplyTo(transformDestination(message.getJMSReplyTo()));
            activeMessage.setJMSDestination(transformDestination(message.getJMSDestination()));
            activeMessage.setJMSDeliveryMode(message.getJMSDeliveryMode());
            activeMessage.setJMSRedelivered(message.getJMSRedelivered());
            activeMessage.setJMSType(message.getJMSType());
            activeMessage.setJMSExpiration(message.getJMSExpiration());
            activeMessage.setJMSPriority(message.getJMSPriority());
            activeMessage.setJMSTimestamp(message.getJMSTimestamp());

            Enumeration propertyNames = message.getPropertyNames();

            while (propertyNames.hasMoreElements()) {
                String name = propertyNames.nextElement().toString();
                Object obj = message.getObjectProperty(name);
                activeMessage.setObjectProperty(name, obj);
            }

            return activeMessage;
        }
    }
}
