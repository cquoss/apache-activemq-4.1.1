package org.apache.activemq;
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

import org.apache.activemq.command.ActiveMQQueue;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import javax.jms.Destination;
import javax.jms.Session;

import junit.framework.Assert;
import junit.framework.TestCase;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;

/**
 * @author rnewson
 */
public final class LargeStreamletTest extends TestCase {

    private static final Log log = LogFactory.getLog(LargeStreamletTest.class);
    
    private static final String BROKER_URL = "vm://localhost?broker.persistent=false";

    private static final int BUFFER_SIZE = 1 * 1024;

    private static final int MESSAGE_COUNT = 1024*1024;
    
    private AtomicInteger totalRead = new AtomicInteger();

    private AtomicInteger totalWritten = new AtomicInteger();

    private AtomicBoolean stopThreads = new AtomicBoolean(false);

    protected Exception writerException;

    protected Exception readerException;

    public void testStreamlets() throws Exception {
        final ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(
                BROKER_URL);

        final ActiveMQConnection connection = (ActiveMQConnection) factory.createConnection();
        connection.start();
        final ActiveMQConnection connection2 = (ActiveMQConnection) factory.createConnection();
        connection2.start();
        
        try {
            final Destination destination = new ActiveMQQueue("streamtest");
            final Thread readerThread = new Thread(new Runnable() {

                public void run() {
                    totalRead.set(0);
                    try {
                        final InputStream inputStream = connection
                                .createInputStream(destination);
                        try {
                            int read;
                            final byte[] buf = new byte[BUFFER_SIZE];
                            while (!stopThreads.get()
                                    && (read = inputStream.read(buf)) != -1) {
                                totalRead.addAndGet(read);
                            }
                        } finally {
                            inputStream.close();
                        }
                    } catch (Exception e) {
                        readerException  = e;
                        e.printStackTrace();
                    } finally {
                        log.info(totalRead + " total bytes read.");
                    }
                }
            });

            final Thread writerThread = new Thread(new Runnable() {

                public void run() {
                    totalWritten.set(0);
                    int count = MESSAGE_COUNT;
                    try {
                        final OutputStream outputStream = connection2
                                .createOutputStream(destination);
                        try {
                            final byte[] buf = new byte[BUFFER_SIZE];
                            new Random().nextBytes(buf);
                            while (count > 0 && !stopThreads.get()) {
                                outputStream.write(buf);
                                totalWritten.addAndGet(buf.length);
                                count--;
                            }
                        } finally {
                            outputStream.close();
                        }
                    } catch (Exception e) {
                        writerException = e;
                        e.printStackTrace();
                    } finally {
                        log.info(totalWritten
                                + " total bytes written.");
                    }
                }
            });

            readerThread.start();
            writerThread.start();

            
            // Wait till reader is has finished receiving all the messages or he has stopped
            // receiving messages.
            Thread.sleep(1000);
            int lastRead = totalRead.get();
            while( readerThread.isAlive() ) {
                readerThread.join(1000);
                // No progress?? then stop waiting..
                if( lastRead == totalRead.get() ) {
                    break;
                }
                lastRead = totalRead.get();
            }
            
            stopThreads.set(true);

            assertTrue("Should not have received a reader exception", readerException == null);
            assertTrue("Should not have received a writer exception", writerException == null);
            
            Assert.assertEquals("Not all messages accounted for", 
                    totalWritten.get(), totalRead.get());
                
        } finally {
            connection.close();
            connection2.close();
        }
    }

}
