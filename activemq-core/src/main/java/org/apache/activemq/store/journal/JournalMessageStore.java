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
package org.apache.activemq.store.journal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.apache.activeio.journal.RecordLocation;
import org.apache.activemq.broker.ConnectionContext;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.command.JournalQueueAck;
import org.apache.activemq.command.Message;
import org.apache.activemq.command.MessageAck;
import org.apache.activemq.command.MessageId;
import org.apache.activemq.memory.UsageManager;
import org.apache.activemq.store.MessageRecoveryListener;
import org.apache.activemq.store.MessageStore;
import org.apache.activemq.store.PersistenceAdapter;
import org.apache.activemq.transaction.Synchronization;
import org.apache.activemq.util.Callback;
import org.apache.activemq.util.TransactionTemplate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A MessageStore that uses a Journal to store it's messages.
 * 
 * @version $Revision: 1.14 $
 */
public class JournalMessageStore implements MessageStore {

    private static final Log log = LogFactory.getLog(JournalMessageStore.class);

    protected final JournalPersistenceAdapter peristenceAdapter;
    protected final JournalTransactionStore transactionStore;
    protected final MessageStore longTermStore;
    protected final ActiveMQDestination destination;
    protected final TransactionTemplate transactionTemplate;

    private LinkedHashMap messages = new LinkedHashMap();
    private ArrayList messageAcks = new ArrayList();

    /** A MessageStore that we can use to retrieve messages quickly. */
    private LinkedHashMap cpAddedMessageIds;
    
    protected RecordLocation lastLocation;
    protected HashSet inFlightTxLocations = new HashSet();

    private UsageManager usageManager;
    
    public JournalMessageStore(JournalPersistenceAdapter adapter, MessageStore checkpointStore, ActiveMQDestination destination) {
        this.peristenceAdapter = adapter;
        this.transactionStore = adapter.getTransactionStore();
        this.longTermStore = checkpointStore;
        this.destination = destination;
        this.transactionTemplate = new TransactionTemplate(adapter, new ConnectionContext());
    }
    
    public void setUsageManager(UsageManager usageManager) {
        this.usageManager = usageManager;
        longTermStore.setUsageManager(usageManager);
    }


    /**
     * Not synchronized since the Journal has better throughput if you increase
     * the number of concurrent writes that it is doing.
     */
    public void addMessage(ConnectionContext context, final Message message) throws IOException {
        
        final MessageId id = message.getMessageId();
        
        final boolean debug = log.isDebugEnabled();
        message.incrementReferenceCount();
        
        final RecordLocation location = peristenceAdapter.writeCommand(message, message.isResponseRequired());
        if( !context.isInTransaction() ) {
            if( debug )
                log.debug("Journalled message add for: "+id+", at: "+location);
            addMessage(message, location);
        } else {
            if( debug )
                log.debug("Journalled transacted message add for: "+id+", at: "+location);
            synchronized( this ) {
                inFlightTxLocations.add(location);
            }
            transactionStore.addMessage(this, message, location);
            context.getTransaction().addSynchronization(new Synchronization(){
                public void afterCommit() throws Exception {                    
                    if( debug )
                        log.debug("Transacted message add commit for: "+id+", at: "+location);
                    synchronized( JournalMessageStore.this ) {
                        inFlightTxLocations.remove(location);
                        addMessage(message, location);
                    }
                }
                public void afterRollback() throws Exception {                    
                    if( debug )
                        log.debug("Transacted message add rollback for: "+id+", at: "+location);
                    synchronized( JournalMessageStore.this ) {
                        inFlightTxLocations.remove(location);
                    }
                    message.decrementReferenceCount();
                }
            });
        }
    }

    private void addMessage(final Message message, final RecordLocation location) {
        synchronized (this) {
            lastLocation = location;
            MessageId id = message.getMessageId();
            messages.put(id, message);
        }
    }
    
    public void replayAddMessage(ConnectionContext context, Message message) {
        try {
            // Only add the message if it has not already been added.
            Message t = longTermStore.getMessage(message.getMessageId());
            if( t==null ) {
                longTermStore.addMessage(context, message);
            }
        }
        catch (Throwable e) {
            log.warn("Could not replay add for message '" + message.getMessageId() + "'.  Message may have already been added. reason: " + e);
        }
    }

    /**
     */
    public void removeMessage(ConnectionContext context, final MessageAck ack) throws IOException {
        final boolean debug = log.isDebugEnabled();
        JournalQueueAck remove = new JournalQueueAck();
        remove.setDestination(destination);
        remove.setMessageAck(ack);
        
        final RecordLocation location = peristenceAdapter.writeCommand(remove, ack.isResponseRequired());
        if( !context.isInTransaction() ) {
            if( debug )
                log.debug("Journalled message remove for: "+ack.getLastMessageId()+", at: "+location);
            removeMessage(ack, location);
        } else {
            if( debug )
                log.debug("Journalled transacted message remove for: "+ack.getLastMessageId()+", at: "+location);
            synchronized( this ) {
                inFlightTxLocations.add(location);
            }
            transactionStore.removeMessage(this, ack, location);
            context.getTransaction().addSynchronization(new Synchronization(){
                public void afterCommit() throws Exception {                    
                    if( debug )
                        log.debug("Transacted message remove commit for: "+ack.getLastMessageId()+", at: "+location);
                    synchronized( JournalMessageStore.this ) {
                        inFlightTxLocations.remove(location);
                        removeMessage(ack, location);
                    }
                }
                public void afterRollback() throws Exception {                    
                    if( debug )
                        log.debug("Transacted message remove rollback for: "+ack.getLastMessageId()+", at: "+location);
                    synchronized( JournalMessageStore.this ) {
                        inFlightTxLocations.remove(location);
                    }
                }
            });

        }
    }
    
    private void removeMessage(final MessageAck ack, final RecordLocation location) {
        synchronized (this) {
            lastLocation = location;
            MessageId id = ack.getLastMessageId();
            Message message = (Message) messages.remove(id);
            if (message == null) {
                messageAcks.add(ack);
            } else {
                message.decrementReferenceCount();
            }
        }
    }
    
    public void replayRemoveMessage(ConnectionContext context, MessageAck messageAck) {
        try {
            // Only remove the message if it has not already been removed.
            Message t = longTermStore.getMessage(messageAck.getLastMessageId());
            if( t!=null ) {
                longTermStore.removeMessage(context, messageAck);
            }
        }
        catch (Throwable e) {
            log.warn("Could not replay acknowledge for message '" + messageAck.getLastMessageId() + "'.  Message may have already been acknowledged. reason: " + e);
        }
    }

    /**
     * @return
     * @throws IOException
     */
    public RecordLocation checkpoint() throws IOException {
        return checkpoint(null);
    }
    
    /**
     * @return
     * @throws IOException
     */
    public RecordLocation checkpoint(final Callback postCheckpointTest) throws IOException {

        
        RecordLocation rc;
        final ArrayList cpRemovedMessageLocations;
        final ArrayList cpActiveJournalLocations;
        final int maxCheckpointMessageAddSize = peristenceAdapter.getMaxCheckpointMessageAddSize();

        // swap out the message hash maps..
        synchronized (this) {
            cpAddedMessageIds = this.messages;
            cpRemovedMessageLocations = this.messageAcks;

            cpActiveJournalLocations=new ArrayList(inFlightTxLocations);
            
            this.messages = new LinkedHashMap();
            this.messageAcks = new ArrayList();            
        }

        transactionTemplate.run(new Callback() {
            public void execute() throws Exception {

                int size = 0;
                
                PersistenceAdapter persitanceAdapter = transactionTemplate.getPersistenceAdapter();
                ConnectionContext context = transactionTemplate.getContext();
                
                // Checkpoint the added messages.
                Iterator iterator = cpAddedMessageIds.values().iterator();
                while (iterator.hasNext()) {
                    Message message = (Message) iterator.next();
                    try {
                        longTermStore.addMessage(context, message);
                    } catch (Throwable e) {
                        log.warn("Message could not be added to long term store: " + e.getMessage(), e);
                    }
                    
                    size += message.getSize();
                    
                    message.decrementReferenceCount();
                    
                    // Commit the batch if it's getting too big
                    if( size >= maxCheckpointMessageAddSize ) {
                        persitanceAdapter.commitTransaction(context);
                        persitanceAdapter.beginTransaction(context);
                        size=0;
                    }
                    
                }

                persitanceAdapter.commitTransaction(context);
                persitanceAdapter.beginTransaction(context);

                // Checkpoint the removed messages.
                iterator = cpRemovedMessageLocations.iterator();
                while (iterator.hasNext()) {
                    try {
                        MessageAck ack = (MessageAck) iterator.next();
                        longTermStore.removeMessage(transactionTemplate.getContext(), ack);
                    } catch (Throwable e) {
                        log.debug("Message could not be removed from long term store: " + e.getMessage(), e);
                    }
                }
                
                if( postCheckpointTest!= null ) {
                    postCheckpointTest.execute();
                }
            }

        });

        synchronized (this) {
            cpAddedMessageIds = null;
        }
        
        if( cpActiveJournalLocations.size() > 0 ) {
            Collections.sort(cpActiveJournalLocations);
            return (RecordLocation) cpActiveJournalLocations.get(0);
        } else {
            return lastLocation;
        }
    }

    /**
     * 
     */
    public Message getMessage(MessageId identity) throws IOException {
        Message answer = null;

        synchronized (this) {
            // Do we have a still have it in the journal?
            answer = (Message) messages.get(identity);
            if( answer==null && cpAddedMessageIds!=null )
                answer = (Message) cpAddedMessageIds.get(identity);
        }
        
        if (answer != null ) {
            return answer;
        }
        
        // If all else fails try the long term message store.
        return longTermStore.getMessage(identity);
    }

    /**
     * Replays the checkpointStore first as those messages are the oldest ones,
     * then messages are replayed from the transaction log and then the cache is
     * updated.
     * 
     * @param listener
     * @throws Exception 
     */
    public void recover(final MessageRecoveryListener listener) throws Exception {
        peristenceAdapter.checkpoint(true, true);
        longTermStore.recover(listener);
    }

    public void start() throws Exception {
        if( this.usageManager != null )
            this.usageManager.addUsageListener(peristenceAdapter);
        longTermStore.start();
    }

    public void stop() throws Exception {
        longTermStore.stop();
        if( this.usageManager != null )
            this.usageManager.removeUsageListener(peristenceAdapter);
    }

    /**
     * @return Returns the longTermStore.
     */
    public MessageStore getLongTermMessageStore() {
        return longTermStore;
    }

    /**
     * @see org.apache.activemq.store.MessageStore#removeAllMessages(ConnectionContext)
     */
    public void removeAllMessages(ConnectionContext context) throws IOException {
        peristenceAdapter.checkpoint(true, true);
        longTermStore.removeAllMessages(context);
    }
    
    public ActiveMQDestination getDestination() {
        return destination;
    }

    public void addMessageReference(ConnectionContext context, MessageId messageId, long expirationTime, String messageRef) throws IOException {
        throw new IOException("The journal does not support message references.");
    }

    public String getMessageReference(MessageId identity) throws IOException {
        throw new IOException("The journal does not support message references.");
    }

}
