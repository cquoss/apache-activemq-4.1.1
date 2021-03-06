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
package org.apache.activemq.tool.reports.plugins;

import org.apache.activemq.tool.reports.PerformanceStatisticsUtil;

import java.util.Map;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Iterator;

public class ThroughputReportPlugin implements ReportPlugin {
    public static final String KEY_SYS_TOTAL_TP          = "SystemTotalTP";
    public static final String KEY_SYS_TOTAL_CLIENTS     = "SystemTotalClients";
    public static final String KEY_SYS_AVE_TP            = "SystemAveTP";
    public static final String KEY_SYS_AVE_EMM_TP        = "SystemAveEMMTP";
    public static final String KEY_SYS_AVE_CLIENT_TP     = "SystemAveClientTP";
    public static final String KEY_SYS_AVE_CLIENT_EMM_TP = "SystemAveClientEMMTP";
    public static final String KEY_MIN_CLIENT_TP = "MinClientTP";
    public static final String KEY_MAX_CLIENT_TP = "MaxClientTP";
    public static final String KEY_MIN_CLIENT_TOTAL_TP = "MinClientTotalTP";
    public static final String KEY_MAX_CLIENT_TOTAL_TP = "MaxClientTotalTP";
    public static final String KEY_MIN_CLIENT_AVE_TP = "MinClientAveTP";
    public static final String KEY_MAX_CLIENT_AVE_TP = "MaxClientAveTP";
    public static final String KEY_MIN_CLIENT_AVE_EMM_TP = "MinClientAveEMMTP";
    public static final String KEY_MAX_CLIENT_AVE_EMM_TP = "MaxClientAveEMMTP";

    protected Map clientThroughputs = new HashMap();

    public void handleCsvData(String csvData) {
        StringTokenizer tokenizer = new StringTokenizer(csvData, ",");
        String data, key, val, clientName = null;
        Long throughput = null;
        while (tokenizer.hasMoreTokens()) {
            data = tokenizer.nextToken();
            key  = data.substring(0, data.indexOf("="));
            val  = data.substring(data.indexOf("=") + 1);

            if (key.equalsIgnoreCase("clientName")) {
                clientName = val;
            } else if (key.equalsIgnoreCase("throughput")) {
                throughput = Long.valueOf(val);
            } else {
                // Ignore unknown token
            }
        }
        addToClientTPList(clientName, throughput);
    }

    public Map getSummary() {
        // Check if tp sampler wasn't used.
        if (clientThroughputs.size() == 0) {
            return new HashMap();
        }

        long   minClientTP = Long.MAX_VALUE, // TP = throughput
               maxClientTP = Long.MIN_VALUE,
               minClientTotalTP = Long.MAX_VALUE,
               maxClientTotalTP = Long.MIN_VALUE,
               systemTotalTP = 0;

        double minClientAveTP = Double.MAX_VALUE,
               maxClientAveTP = Double.MIN_VALUE,
               minClientAveEMMTP = Double.MAX_VALUE, // EMM = Excluding Min/Max
               maxClientAveEMMTP = Double.MIN_VALUE,
               systemAveTP = 0.0,
               systemAveEMMTP = 0.0;

        String nameMinClientTP = "",
               nameMaxClientTP = "",
               nameMinClientTotalTP = "",
               nameMaxClientTotalTP = "",
               nameMinClientAveTP = "",
               nameMaxClientAveTP = "",
               nameMinClientAveEMMTP = "",
               nameMaxClientAveEMMTP = "";

        Set clientNames = clientThroughputs.keySet();
        String clientName;
        List   clientTPList;
        long tempLong;
        double tempDouble;
        int clientCount = 0;
        for (Iterator i=clientNames.iterator(); i.hasNext();) {
            clientName = (String)i.next();
            clientTPList = (List)clientThroughputs.get(clientName);
            clientCount++;

            tempLong = PerformanceStatisticsUtil.getMin(clientTPList);
            if (tempLong < minClientTP) {
                minClientTP = tempLong;
                nameMinClientTP = clientName;
            }

            tempLong = PerformanceStatisticsUtil.getMax(clientTPList);
            if (tempLong > maxClientTP) {
                maxClientTP = tempLong;
                nameMaxClientTP = clientName;
            }

            tempLong = PerformanceStatisticsUtil.getSum(clientTPList);
            systemTotalTP += tempLong; // Accumulate total TP
            if (tempLong < minClientTotalTP) {
                minClientTotalTP = tempLong;
                nameMinClientTotalTP = clientName;
            }

            if (tempLong > maxClientTotalTP) {
                maxClientTotalTP = tempLong;
                nameMaxClientTotalTP = clientName;
            }

            tempDouble = PerformanceStatisticsUtil.getAve(clientTPList);
            systemAveTP += tempDouble; // Accumulate ave throughput
            if (tempDouble < minClientAveTP) {
                minClientAveTP = tempDouble;
                nameMinClientAveTP = clientName;
            }

            if (tempDouble > maxClientAveTP) {
                maxClientAveTP = tempDouble;
                nameMaxClientAveTP = clientName;
            }

            tempDouble = PerformanceStatisticsUtil.getAveEx(clientTPList);
            systemAveEMMTP += tempDouble; // Accumulate ave throughput excluding min/max
            if (tempDouble < minClientAveEMMTP) {
                minClientAveEMMTP = tempDouble;
                nameMinClientAveEMMTP = clientName;
            }

            if (tempDouble > maxClientAveEMMTP) {
                maxClientAveEMMTP = tempDouble;
                nameMaxClientAveEMMTP = clientName;
            }
        }

        Map summary = new HashMap();
        summary.put(KEY_SYS_TOTAL_TP, String.valueOf(systemTotalTP));
        summary.put(KEY_SYS_TOTAL_CLIENTS, String.valueOf(clientCount));
        summary.put(KEY_SYS_AVE_TP, String.valueOf(systemAveTP));
        summary.put(KEY_SYS_AVE_EMM_TP, String.valueOf(systemAveEMMTP));
        summary.put(KEY_SYS_AVE_CLIENT_TP, String.valueOf(systemAveTP / clientCount));
        summary.put(KEY_SYS_AVE_CLIENT_EMM_TP, String.valueOf(systemAveEMMTP / clientCount));
        summary.put(KEY_MIN_CLIENT_TP, nameMinClientTP + "=" + minClientTP);
        summary.put(KEY_MAX_CLIENT_TP, nameMaxClientTP + "=" + maxClientTP);
        summary.put(KEY_MIN_CLIENT_TOTAL_TP, nameMinClientTotalTP + "=" + minClientTotalTP);
        summary.put(KEY_MAX_CLIENT_TOTAL_TP, nameMaxClientTotalTP + "=" + maxClientTotalTP);
        summary.put(KEY_MIN_CLIENT_AVE_TP, nameMinClientAveTP + "=" + minClientAveTP);
        summary.put(KEY_MAX_CLIENT_AVE_TP, nameMaxClientAveTP + "=" + maxClientAveTP);
        summary.put(KEY_MIN_CLIENT_AVE_EMM_TP, nameMinClientAveEMMTP + "=" + minClientAveEMMTP);
        summary.put(KEY_MAX_CLIENT_AVE_EMM_TP, nameMaxClientAveEMMTP + "=" + maxClientAveEMMTP);

        return summary;
    }

    protected void addToClientTPList(String clientName, Long throughput) {
        // Write to client's throughput list
        if (clientName == null || throughput == null) {
            throw new IllegalArgumentException("Invalid Throughput CSV Data: clientName=" + clientName + ", throughput=" + throughput);
        }

        List clientTPList = (List)clientThroughputs.get(clientName);
        if (clientTPList == null) {
            clientTPList = new ArrayList();
            clientThroughputs.put(clientName, clientTPList);
        }
        clientTPList.add(throughput);
    }
}
