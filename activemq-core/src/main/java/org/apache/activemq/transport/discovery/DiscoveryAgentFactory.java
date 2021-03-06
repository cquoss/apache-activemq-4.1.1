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
package org.apache.activemq.transport.discovery;

import java.io.IOException;
import java.net.URI;

import org.apache.activemq.util.FactoryFinder;
import org.apache.activemq.util.IOExceptionSupport;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;

public abstract class DiscoveryAgentFactory {

    static final private FactoryFinder discoveryAgentFinder = new FactoryFinder("META-INF/services/org/apache/activemq/transport/discoveryagent/");    
    static final private ConcurrentHashMap discoveryAgentFactorys = new ConcurrentHashMap();

    /**
     * @param uri
     * @return
     * @throws IOException
     */
    private static DiscoveryAgentFactory findDiscoveryAgentFactory(URI uri) throws IOException {
        String scheme = uri.getScheme();
        if( scheme == null )
            throw new IOException("DiscoveryAgent scheme not specified: [" + uri + "]");
        DiscoveryAgentFactory daf = (DiscoveryAgentFactory) discoveryAgentFactorys.get(scheme);
        if (daf == null) {
            // Try to load if from a META-INF property.
            try {
                daf = (DiscoveryAgentFactory) discoveryAgentFinder.newInstance(scheme);
                discoveryAgentFactorys.put(scheme, daf);
            }
            catch (Throwable e) {
                throw IOExceptionSupport.create("DiscoveryAgent scheme NOT recognized: [" + scheme + "]", e);
            }
        }
        return daf;
    }
    
    public static DiscoveryAgent createDiscoveryAgent(URI uri) throws IOException {
        DiscoveryAgentFactory tf = findDiscoveryAgentFactory(uri);
        return tf.doCreateDiscoveryAgent(uri);

    }

    abstract protected DiscoveryAgent doCreateDiscoveryAgent(URI uri) throws IOException;
//    {
//        try {
//            String type = ( uri.getScheme() == null ) ? uri.getPath() : uri.getScheme();
//            DiscoveryAgent rc = (DiscoveryAgent) discoveryAgentFinder.newInstance(type);
//            Map options = URISupport.parseParamters(uri);
//            IntrospectionSupport.setProperties(rc, options);
//            if( rc.getClass() == SimpleDiscoveryAgent.class ) {
//                CompositeData data = URISupport.parseComposite(uri);
//                ((SimpleDiscoveryAgent)rc).setServices(data.getComponents());
//            }
//            return rc;
//        } catch (Throwable e) {
//            throw IOExceptionSupport.create("Could not create discovery agent: "+uri, e);
//        }
//    }   
}
