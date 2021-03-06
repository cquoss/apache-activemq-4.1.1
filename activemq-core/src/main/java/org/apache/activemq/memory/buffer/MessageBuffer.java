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

package org.apache.activemq.memory.buffer;

/**
 * Represents a collection of MessageQueue instances which are all bound by the
 * same memory buffer to fix the amount of RAM used to some uppper bound.
 * 
 * @version $Revision: 1.1 $
 */
public interface MessageBuffer {

    public int getSize();

    /**
     * Creates a new message queue instance
     */
    public MessageQueue createMessageQueue();

    /**
     * After a message queue has changed we may need to perform some evictions
     * 
     * @param delta
     * @param queueSize
     */
    public void onSizeChanged(MessageQueue queue, int delta, int queueSize);

    public void clear();

}
