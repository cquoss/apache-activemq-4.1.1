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

import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerFilter;
import org.apache.activemq.broker.ConnectionContext;
import org.apache.activemq.broker.region.Destination;
import org.apache.activemq.broker.region.Subscription;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTempDestination;
import org.apache.activemq.command.ActiveMQTopic;
import org.apache.activemq.command.ConsumerInfo;
import org.apache.activemq.command.Message;
import org.apache.activemq.command.ProducerInfo;
import org.apache.activemq.filter.BooleanExpression;
import org.apache.activemq.filter.MessageEvaluationContext;

import javax.jms.JMSException;

import java.util.Set;


/**
 * Verifies if a authenticated user can do an operation against the broker using an authorization map.
 * 
 * @version $Revision: 426366 $
 */
public class AuthorizationBroker extends BrokerFilter implements SecurityAdminMBean {
    
    private final AuthorizationMap authorizationMap;

    public AuthorizationBroker(Broker next, AuthorizationMap authorizationMap) {
        super(next);
        this.authorizationMap = authorizationMap;
    }
    
    public Destination addDestination(ConnectionContext context, ActiveMQDestination destination) throws Exception {
        final SecurityContext securityContext = (SecurityContext) context.getSecurityContext();
        if( securityContext == null )
            throw new SecurityException("User is not authenticated.");

        // You don't need to be an admin to create temp destinations.
        if( !destination.isTemporary() 
            || !((ActiveMQTempDestination)destination).getConnectionId().equals(context.getConnectionId().getValue()) ) {
            
            Set allowedACLs = authorizationMap.getAdminACLs(destination);
            if(allowedACLs!=null && !securityContext.isInOneOf(allowedACLs))
                throw new SecurityException("User "+securityContext.getUserName()+" is not authorized to create: "+destination);
        }
        
        return super.addDestination(context, destination);
    }
    
    public void removeDestination(ConnectionContext context, ActiveMQDestination destination, long timeout) throws Exception {
        
        final SecurityContext securityContext = (SecurityContext) context.getSecurityContext();
        if( securityContext == null )
            throw new SecurityException("User is not authenticated.");

        // You don't need to be an admin to remove temp destinations.
        if( !destination.isTemporary() 
            || !((ActiveMQTempDestination)destination).getConnectionId().equals(context.getConnectionId().getValue()) ) {
            
            Set allowedACLs = authorizationMap.getAdminACLs(destination);
            if(allowedACLs!=null && !securityContext.isInOneOf(allowedACLs))
                throw new SecurityException("User "+securityContext.getUserName()+" is not authorized to remove: "+destination);
        }

        super.removeDestination(context, destination, timeout);
    }
    
    public Subscription addConsumer(ConnectionContext context, ConsumerInfo info) throws Exception {
        
        final SecurityContext subject = (SecurityContext) context.getSecurityContext();
        if( subject == null )
            throw new SecurityException("User is not authenticated.");
        
        Set allowedACLs = authorizationMap.getReadACLs(info.getDestination());
        if(allowedACLs!=null && !subject.isInOneOf(allowedACLs))
            throw new SecurityException("User "+subject.getUserName()+" is not authorized to read from: "+info.getDestination());
        subject.getAuthorizedReadDests().put(info.getDestination(), info.getDestination());
        
        /* 
         * Need to think about this a little more.  We could do per message security checking
         * to implement finer grained security checking. For example a user can only see messages
         * with price>1000 .  Perhaps this should just be another additional broker filter that installs 
         * this type of feature.
         * 
         * If we did want to do that, then we would install a predicate.  We should be careful since
         * there may be an existing predicate already assigned and the consumer info may be sent to a remote 
         * broker, so it also needs to support being marshaled.
         * 
            info.setAdditionalPredicate(new BooleanExpression() {
                public boolean matches(MessageEvaluationContext message) throws JMSException {
                    if( !subject.getAuthorizedReadDests().contains(message.getDestination()) ) {
                        Set allowedACLs = authorizationMap.getReadACLs(message.getDestination());
                        if(allowedACLs!=null && !subject.isInOneOf(allowedACLs))
                            return false;
                        subject.getAuthorizedReadDests().put(message.getDestination(), message.getDestination());
                    }
                    return true;
                }
                public Object evaluate(MessageEvaluationContext message) throws JMSException {
                    return matches(message) ? Boolean.TRUE : Boolean.FALSE;
                }
            });
        */
        
        return super.addConsumer(context, info);
    }
    
    public void addProducer(ConnectionContext context, ProducerInfo info) throws Exception {
        
        SecurityContext subject = (SecurityContext) context.getSecurityContext();
        if( subject == null )
            throw new SecurityException("User is not authenticated.");
        
        if( info.getDestination()!=null ) {
            Set allowedACLs = authorizationMap.getWriteACLs(info.getDestination());
            if(allowedACLs!=null && !subject.isInOneOf(allowedACLs))
                throw new SecurityException("User "+subject.getUserName()+" is not authorized to write to: "+info.getDestination());
            subject.getAuthorizedWriteDests().put(info.getDestination(), info.getDestination());
        }
        
        super.addProducer(context, info);
    }
        
    public void send(ConnectionContext context, Message messageSend) throws Exception {
        SecurityContext subject = (SecurityContext) context.getSecurityContext();
        if( subject == null )
            throw new SecurityException("User is not authenticated.");
        
        if( !subject.getAuthorizedWriteDests().contains(messageSend.getDestination()) ) {
            Set allowedACLs = authorizationMap.getWriteACLs(messageSend.getDestination());            
            if(allowedACLs!=null && !subject.isInOneOf(allowedACLs))
                throw new SecurityException("User "+subject.getUserName()+" is not authorized to write to: "+messageSend.getDestination());
            subject.getAuthorizedWriteDests().put(messageSend.getDestination(), messageSend.getDestination());
        }

        super.send(context, messageSend);
    }
    
    // SecurityAdminMBean interface
    // -------------------------------------------------------------------------

    public void addQueueRole(String queue, String operation, String role) {
        addDestinationRole(new ActiveMQQueue(queue), operation, role);
    }

    public void addTopicRole(String topic, String operation, String role) {
        addDestinationRole(new ActiveMQTopic(topic), operation, role);
    }

    public void removeQueueRole(String queue, String operation, String role) {
        removeDestinationRole(new ActiveMQQueue(queue), operation, role);
    }

    public void removeTopicRole(String topic, String operation, String role) {
        removeDestinationRole(new ActiveMQTopic(topic), operation, role);
    }
    
    public void addDestinationRole(javax.jms.Destination destination, String operation, String role) {
    }
    
    public void removeDestinationRole(javax.jms.Destination destination, String operation, String role) {
    }


    public void addRole(String role) {
    }

    public void addUserRole(String user, String role) {
    }

    public void removeRole(String role) {
    }

    public void removeUserRole(String user, String role) {
    }

}
