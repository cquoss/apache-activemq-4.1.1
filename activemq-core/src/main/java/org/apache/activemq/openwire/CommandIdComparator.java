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
package org.apache.activemq.openwire;

import org.apache.activemq.command.Command;

import java.util.Comparator;

/**
 * A @{link Comparator} of commands using their {@link Command#getCommandId()}
 * 
 * @version $Revision: 426366 $
 */
public class CommandIdComparator implements Comparator {

    public int compare(Object o1, Object o2) {
        assert o1 instanceof Command;
        assert o2 instanceof Command;
        
        Command c1 = (Command) o1;
        Command c2 = (Command) o2;
        return c1.getCommandId() - c2.getCommandId();
    }

}
