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

import org.apache.log4j.Logger;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.SuccessResponse;
import com.cloud.event.EventTypes;
import com.cloud.network.SecurityGroupVO;
import com.cloud.server.ManagementServer;
import com.cloud.user.Account;

@Implementation(method="deleteSecurityGroup", manager=ManagementServer.class, description="Deletes a port forwarding service")
public class DeletePortForwardingServiceCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(DeletePortForwardingServiceCmd.class.getName());
    private static final String s_name = "deleteportforwardingserviceresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="id", type=CommandType.LONG, required=true, description="ID of the port forwarding service")
    private Long id;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }
    
    @Override
    public long getAccountId() {
        SecurityGroupVO sg = ApiDBUtils.findPortForwardingServiceById(getId());
        if (sg != null) {
            return sg.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_PORT_FORWARDING_SERVICE_DELETE;
    }

    @Override
    public String getEventDescription() {
        return  "deleting port forwarding service: " + getId();
    }

    @Override @SuppressWarnings("unchecked")
    public SuccessResponse getResponse() {
        SuccessResponse response = new SuccessResponse();
        Boolean responseObject = (Boolean)getResponseObject();
      
        if (responseObject != null) {
        	response.setSuccess(responseObject);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to delete port forwarding service");
        }

        response.setResponseName(getName());
        return response;
    }
}
