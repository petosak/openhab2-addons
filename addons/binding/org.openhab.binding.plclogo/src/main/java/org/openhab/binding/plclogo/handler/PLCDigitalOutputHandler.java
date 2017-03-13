/**
 * Copyright (c) 2014-2017 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.plclogo.handler;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.Command;

/**
 * The {@link PLCDigitalOutputHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Alexander Falkenstern - Initial contribution
 */
public class PLCDigitalOutputHandler extends PLCDigitalBlockHandler {

    public PLCDigitalOutputHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // if(channelUID.getId().equals(CHANNEL_1)) {
        // TODO: handle command

        // Note: if communication with thing fails for some reason,
        // indicate that by setting the status with detail information
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
        // "Could not control device at IP address x.x.x.x");
        // }
    }

    @Override
    public boolean isBlockValid(final String name) {
        boolean valid = false;
        if (name.length() >= 2) {
            valid = name.startsWith("Q");
            valid = valid || name.startsWith("NQ");
        }
        return valid || super.isBlockValid(name);
    }
}
