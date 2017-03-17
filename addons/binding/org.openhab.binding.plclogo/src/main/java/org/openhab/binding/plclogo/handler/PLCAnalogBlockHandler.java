/**
 * Copyright (c) 2014-2017 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.plclogo.handler;

import static org.openhab.binding.plclogo.PLCLogoBindingConstants.*;

import java.util.Map;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link PLCAnalogBlockHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Alexander Falkenstern - Initial contribution
 */
public class PLCAnalogBlockHandler extends PLCBlockHandler {

    private final Logger logger = LoggerFactory.getLogger(PLCAnalogBlockHandler.class);

    int oldValue = Integer.MAX_VALUE;

    public PLCAnalogBlockHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        final String INPUT = "input";
        final String OUTPUT = "output";

        final String name = getBlockName();
        if (isBlockValid(name)) {
            String text = name.startsWith("AI") || name.startsWith("NAI") ? INPUT : OUTPUT;

            final ChannelUID UID = new ChannelUID(getThing().getUID(), ANALOG_CHANNEL_ID);
            ChannelBuilder channel = ChannelBuilder.create(UID, "Number");
            channel = channel.withLabel(name);
            channel = channel.withDescription("Analog " + text);

            ThingBuilder thing = editThing();
            text = text.substring(0, 1).toUpperCase() + text.substring(1);
            thing = thing.withLabel(getBridge().getLabel() + ": " + text + " " + name);
            thing = thing.withChannel(channel.build());
            updateThing(thing.build());
            super.initialize();
        } else {
            final String message = "Can not initialize LOGO! block. Please check blocks.";
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, message);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        logger.debug("Dispose LOGO! analog handler.");
        super.dispose();

        oldValue = Integer.MAX_VALUE;
    }

    public void setData(final short data) {
        if (oldValue != data) {
            final Channel channel = thing.getChannel(ANALOG_CHANNEL_ID);
            updateState(channel.getUID(), new DecimalType(data));
            logger.debug("Thing: {}, channel {}: {}", thing.getUID(), channel.getUID(), data);
        }

        oldValue = data;
    }

    @Override
    protected int getAddress(final String name) {
        int address = -1;
        if (isBlockValid(name)) {
            int base = 0;

            final String block = name.split("\\.")[0];
            final Map<?, Integer> family = LOGO_MEMORY_BLOCK.get(getLogoFamily());
            if (Character.isDigit(block.charAt(2))) {
                base = family.get(block.substring(0, 2));
                address = Integer.parseInt(block.substring(2));
            } else if (Character.isDigit(block.charAt(3))) {
                base = family.get(block.substring(0, 3));
                address = Integer.parseInt(block.substring(3));
            }

            if (base != 0) { // Only VB/VW memory ranges are 0 based
                address = base + (address - 1) * 2;
            }
        }
        return address;
    }

    @Override
    protected boolean isBlockValid(final String name) {
        boolean valid = false;
        if (name.length() >= 3) {
            valid = valid || name.startsWith("AI") || name.startsWith("NAI"); // Inputs
            valid = valid || name.startsWith("AQ") || name.startsWith("NAQ"); // Outputs
            valid = valid || name.startsWith("AM"); // Markers
            if (!valid && name.startsWith("VW")) { // Memory block
                String[] memparts = name.split("\\.");
                valid = (memparts.length == 1);
            }
        }
        return valid;
    }
}
