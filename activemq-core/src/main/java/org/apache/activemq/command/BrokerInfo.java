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
package org.apache.activemq.command;

import org.apache.activemq.state.CommandVisitor;
/**
 * When a client connects to a broker, the broker send the client a BrokerInfo so that the client knows which broker
 * node he's talking to and also any peers that the node has in his cluster. This is the broker helping the client out
 * in discovering other nodes in the cluster.
 * 
 * @openwire:marshaller code="2"
 * @version $Revision: 1.7 $
 */
public class BrokerInfo extends BaseCommand{
    public static final byte DATA_STRUCTURE_TYPE=CommandTypes.BROKER_INFO;
    BrokerId brokerId;
    String brokerURL;
    boolean slaveBroker;
    boolean masterBroker;
    boolean faultTolerantConfiguration;
    boolean networkConnection;
    boolean duplexConnection;
    BrokerInfo peerBrokerInfos[];
    String brokerName;
    long connectionId;

    public boolean isBrokerInfo(){
        return true;
    }

    public byte getDataStructureType(){
        return DATA_STRUCTURE_TYPE;
    }

    /**
     * @openwire:property version=1 cache=true
     */
    public BrokerId getBrokerId(){
        return brokerId;
    }

    public void setBrokerId(BrokerId brokerId){
        this.brokerId=brokerId;
    }

    /**
     * @openwire:property version=1
     */
    public String getBrokerURL(){
        return brokerURL;
    }

    public void setBrokerURL(String brokerURL){
        this.brokerURL=brokerURL;
    }

    /**
     * @openwire:property version=1 testSize=0
     */
    public BrokerInfo[] getPeerBrokerInfos(){
        return peerBrokerInfos;
    }

    public void setPeerBrokerInfos(BrokerInfo[] peerBrokerInfos){
        this.peerBrokerInfos=peerBrokerInfos;
    }

    /**
     * @openwire:property version=1
     */
    public String getBrokerName(){
        return brokerName;
    }

    public void setBrokerName(String brokerName){
        this.brokerName=brokerName;
    }

    public Response visit(CommandVisitor visitor) throws Exception{
        return visitor.processBrokerInfo(this);
    }

    /**
     * @openwire:property version=1
     */
    public boolean isSlaveBroker(){
        return slaveBroker;
    }

    public void setSlaveBroker(boolean slaveBroker){
        this.slaveBroker=slaveBroker;
    }

    /**
     * @openwire:property version=1
     */
    public boolean isMasterBroker(){
        return masterBroker;
    }

    /**
     * @param masterBroker
     *            The masterBroker to set.
     */
    public void setMasterBroker(boolean masterBroker){
        this.masterBroker=masterBroker;
    }

    /**
     * @openwire:property version=1
     * @return Returns the faultTolerantConfiguration.
     */
    public boolean isFaultTolerantConfiguration(){
        return faultTolerantConfiguration;
    }

    /**
     * @param faultTolerantConfiguration
     *            The faultTolerantConfiguration to set.
     */
    public void setFaultTolerantConfiguration(boolean faultTolerantConfiguration){
        this.faultTolerantConfiguration=faultTolerantConfiguration;
    }

    
    /**
     * @openwire:property version=2
     * @return the duplexConnection
     */
    public boolean isDuplexConnection(){
        return this.duplexConnection;
    }

    
    /**
     * @param duplexConnection the duplexConnection to set
     */
    public void setDuplexConnection(boolean duplexConnection){
        this.duplexConnection=duplexConnection;
    }

    
    /**
     * @openwire:property version=2
     * @return the networkConnection
     */
    public boolean isNetworkConnection(){
        return this.networkConnection;
    }

    
    /**
     * @param networkConnection the networkConnection to set
     */
    public void setNetworkConnection(boolean networkConnection){
        this.networkConnection=networkConnection;
    }

    /**
     * The broker assigns a each connection it accepts a connection id.
     * 
     * @openwire:property version=2
     */
	public long getConnectionId() {
		return connectionId;
	}

	public void setConnectionId(long connectionId) {
		this.connectionId = connectionId;
	}

  
}
