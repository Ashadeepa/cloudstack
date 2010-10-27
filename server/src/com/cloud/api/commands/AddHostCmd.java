/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.api.commands;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.HostResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.dc.ClusterVO;
import com.cloud.host.Host;
import com.cloud.host.HostStats;
import com.cloud.host.HostVO;
import com.cloud.host.Status.Event;
import com.cloud.offering.ServiceOffering;
import com.cloud.storage.GuestOSCategoryVO;
import com.cloud.vm.UserVmVO;

@Implementation(method="discoverHosts", manager=AgentManager.class, description="Adds a new host.")
public class AddHostCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(AddHostCmd.class.getName());

    private static final String s_name = "addhostresponse";
    
    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="clusterid", type=CommandType.LONG, description="the cluster ID for the host")
    private Long clusterId;

    @Parameter(name="clustername", type=CommandType.STRING, description="the cluster name for the host")
    private String clusterName;

    @Parameter(name="password", type=CommandType.STRING, required=true, description="the password for the host")
    private String password;

    @Parameter(name="podid", type=CommandType.LONG, description="the Pod ID for the host")
    private Long podId;

    @Parameter(name="url", type=CommandType.STRING, required=true, description="the host URL")
    private String url;

    @Parameter(name="username", type=CommandType.STRING, required=true, description="the username for the host")
    private String username;

    @Parameter(name="zoneid", type=CommandType.LONG, required=true, description="the Zone ID for the host")
    private Long zoneId;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getClusterId() {
        return clusterId;
    }

    public String getClusterName() {
        return clusterName;
    }

    public String getPassword() {
        return password;
    }

    public Long getPodId() {
        return podId;
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public Long getZoneId() {
        return zoneId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
    	return s_name;
    }

    @Override @SuppressWarnings("unchecked")
    public ListResponse<HostResponse> getResponse() {
    	List<HostVO> hosts = (List<HostVO>)getResponseObject();

    	ListResponse<HostResponse> response = new ListResponse<HostResponse>();
        List<HostResponse> hostResponses = new ArrayList<HostResponse>();
        if (hosts != null) {
            for (HostVO host : hosts) {
	        	HostResponse hostResponse = new HostResponse();
	        	hostResponse.setId(host.getId());
	            hostResponse.setCapabilities(host.getCapabilities());
	            hostResponse.setClusterId(host.getClusterId());
	            hostResponse.setCpuNumber(host.getCpus());
	            hostResponse.setZoneId(host.getDataCenterId());
	            hostResponse.setDisconnectedOn(host.getDisconnectedOn());
	            hostResponse.setHypervisor(host.getHypervisorType());
	            hostResponse.setHostType(host.getType());
	            hostResponse.setLastPinged(new Date(host.getLastPinged()));
	            hostResponse.setManagementServerId(host.getManagementServerId());
	            hostResponse.setName(host.getName());
	            hostResponse.setPodId(host.getPodId());
	            hostResponse.setCreated(host.getCreated());
	            hostResponse.setRemoved(host.getRemoved());
	            hostResponse.setCpuSpeed(host.getSpeed());
	            hostResponse.setState(host.getStatus());
	            hostResponse.setIpAddress(host.getPrivateIpAddress());
	            hostResponse.setVersion(host.getVersion());
	
	            GuestOSCategoryVO guestOSCategory = ApiDBUtils.getHostGuestOSCategory(host.getId());
	            if (guestOSCategory != null) {
	                hostResponse.setOsCategoryId(guestOSCategory.getId());
	                hostResponse.setOsCategoryName(guestOSCategory.getName());
	            }
	            hostResponse.setZoneName(ApiDBUtils.findZoneById(host.getDataCenterId()).getName());
	            hostResponse.setPodName(ApiDBUtils.findPodById(host.getPodId()).getName());
	
	            // calculate cpu allocated by vm
	            int cpu = 0;
	            String cpuAlloc = null;
	            DecimalFormat decimalFormat = new DecimalFormat("#.##");
	            List<UserVmVO> instances = ApiDBUtils.listUserVMsByHostId(host.getId());
	            for (UserVmVO vm : instances) {
	                ServiceOffering so = ApiDBUtils.findServiceOfferingById(vm.getServiceOfferingId());
	                cpu += so.getCpu() * so.getSpeed();
	            }
	            cpuAlloc = decimalFormat.format(((float) cpu / (float) (host.getCpus() * host.getSpeed())) * 100f) + "%";
	            hostResponse.setCpuAllocated(cpuAlloc);
	
	            // calculate cpu utilized
	            String cpuUsed = null;
	            HostStats hostStats = ApiDBUtils.getHostStatistics(host.getId());
	            if (hostStats != null) {
	                float cpuUtil = (float) hostStats.getCpuUtilization();
	                cpuUsed = decimalFormat.format(cpuUtil) + "%";
	                hostResponse.setCpuUsed(cpuUsed);
	                hostResponse.setAverageLoad((long)hostStats.getAverageLoad());
	                hostResponse.setNetworkKbsRead((long)hostStats.getNetworkReadKBs());
	                hostResponse.setNetworkKbsWrite((long)hostStats.getNetworkWriteKBs());
	            }
	
	            if (host.getType() == Host.Type.Routing) {
	                hostResponse.setMemoryTotal(host.getTotalMemory());
	                
	                // calculate memory allocated by systemVM and userVm
	                long mem = ApiDBUtils.getMemoryUsagebyHost(host.getId());
	                hostResponse.setMemoryAllocated(mem);
	                hostResponse.setMemoryUsed(mem);
	            } else if (host.getType().toString().equals("Storage")) {
	                hostResponse.setDiskSizeTotal(host.getTotalSize());
	                hostResponse.setDiskSizeAllocated(0L);
	            }
	
	            if (host.getClusterId() != null) {
	                ClusterVO cluster = ApiDBUtils.findClusterById(host.getClusterId());
	                hostResponse.setClusterName(cluster.getName());
	            }
	
	            hostResponse.setLocalStorageActive(ApiDBUtils.isLocalStorageActiveOnHost(host));
	
	            Set<Event> possibleEvents = host.getStatus().getPossibleEvents();
	            if ((possibleEvents != null) && !possibleEvents.isEmpty()) {
	                String events = "";
	                Iterator<Event> iter = possibleEvents.iterator();
	                while (iter.hasNext()) {
	                    Event event = iter.next();
	                    events += event.toString();
	                    if (iter.hasNext()) {
	                        events += "; ";
	                    }
	                }
	                hostResponse.setEvents(events);
	            }
	            hostResponse.setResponseName("host");
	            hostResponses.add(hostResponse);
            }
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to add host");
        }

        response.setResponses(hostResponses);
        response.setResponseName(getName());
        return response;
    }
}
