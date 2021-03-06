/**
 * Copyright 2014 Telefonica Investigación y Desarrollo, S.A.U
 *
 * This file is part of fiware-connectors (FI-WARE project).
 *
 * fiware-connectors is free software: you can redistribute it and/or modify it under the terms of the GNU Affero
 * General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * fiware-connectors is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with fiware-connectors. If not, see
 * http://www.gnu.org/licenses/.
 *
 * For those usages not covered by the GNU Affero General Public License please contact with iot_support at tid dot es
 */

package es.tid.fiware.fiwareconnectors.cygnus.channelselectors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.channel.AbstractChannelSelector;
import org.apache.log4j.Logger;

/**
 *
 * @author frb
 */
public class RoundRobinChannelSelector extends AbstractChannelSelector {
    
    private Logger logger;
    private int numStorages;
    private LinkedHashMap<String, ArrayList<String>> channelsPerStorage;
    private LinkedHashMap<String, Integer> lastUsedChannelPerStorage;
    
    /**
     * Constructor.
     */
    public RoundRobinChannelSelector() {
        this.logger = Logger.getLogger(RoundRobinChannelSelector.class);
        this.channelsPerStorage = new LinkedHashMap<String, ArrayList<String>>();
        this.lastUsedChannelPerStorage = new LinkedHashMap<String, Integer>();
    } // RoundRobinChannelSelector
    
    @Override
    public void setChannels(List<Channel> channels) {
        super.setChannels(channels);
    } // setChannels
    
    @Override
    public void configure(Context context) {
        numStorages = context.getInteger("storages", 1);
        logger.debug("[" + this.getName() + "] Reading configuration (storages=" + numStorages + ")");
        
        for (int i = 0; i < numStorages; i++) {
            String channelsStr = context.getString("storages.storage" + (i + 1));
            logger.debug("[" + this.getName() + "] Reading configuration (storages.storage" + (i + 1) + "="
                    + channelsStr + ")");
            List<String> channelList = Arrays.asList(channelsStr.split(","));
            ArrayList<String> channelArrayList = new ArrayList<String>();
            channelArrayList.addAll(channelList);
            channelsPerStorage.put("storage" + (i +  1), channelArrayList);
            lastUsedChannelPerStorage.put("storage" + (i + 1), -1);
        } // for
    } // configure
    
    @Override
    public List<Channel> getOptionalChannels(Event event) {
        logger.debug("Returning empty optional channels");
        return new ArrayList<Channel>();
    } // getOptionalChannels
    
    @Override
    public List<Channel> getRequiredChannels(Event event) {
        // resulting list of required channels
        List<Channel> res = new ArrayList<Channel>();
        
        Iterator it = channelsPerStorage.keySet().iterator();
        
        while (it.hasNext()) {
            String key = (String) it.next();
            ArrayList<String> channelNames = channelsPerStorage.get(key);
            
            // get and update the last used channel
            int lastUsedChannel = (lastUsedChannelPerStorage.get(key) + 1) % channelNames.size();
            lastUsedChannelPerStorage.put(key, lastUsedChannel);
        
            // get the channel name and find its Channel object
            String channelName = channelNames.get(lastUsedChannel);
            List<Channel> allChannels = getAllChannels();
            Channel channel = null;
            
            for (int i = 0; i < allChannels.size(); i++) {
                channel = allChannels.get(i);
                
                if (channel.getName().equals(channelName)) {
                    break;
                } // if
            } // while
            
            // add the channel to the list
            if (channel != null) {
                res.add(channel);
            } // if
        } // while
                
        logger.debug("Returning " + res.toString() + " channels");
        return res;
    } // getRequiredChannels

} // RoundRobinChannelSelector
