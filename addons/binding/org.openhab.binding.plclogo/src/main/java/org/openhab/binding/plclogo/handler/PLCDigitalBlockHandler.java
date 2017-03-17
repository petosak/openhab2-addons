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

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
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
 * The {@link PLCDigitalBlockHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Alexander Falkenstern - Initial contribution
 */
public class PLCDigitalBlockHandler extends PLCBlockHandler {

    private final Logger logger = LoggerFactory.getLogger(PLCDigitalBlockHandler.class);

    private int bit = -1;
    int oldValue = Integer.MAX_VALUE;

    public PLCDigitalBlockHandler(Thing thing) {
        super(thing);
        bit = -1;
    }

    @Override
    public void initialize() {
        final String INPUT = "input";
        final String OUTPUT = "output";

        final String name = getBlockName();
        if (isBlockValid(name)) {
            String text = name.startsWith("I") || name.startsWith("NI") ? INPUT : OUTPUT;

            final ChannelUID UID = new ChannelUID(getThing().getUID(), DIGITAL_CHANNEL_ID);
            ChannelBuilder channel = ChannelBuilder.create(UID, text.equals(INPUT) ? "Contact" : "Switch");
            channel = channel.withLabel(name);
            channel = channel.withDescription("Digital " + text);

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
        logger.debug("Dispose LOGO! digital handler.");
        super.dispose();

        oldValue = Integer.MAX_VALUE;
        bit = -1;
    }

    public void setData(final boolean data) {
        if (oldValue != (data ? 1 : 0)) {
            final Channel channel = thing.getChannel(DIGITAL_CHANNEL_ID);
            if (channel.getAcceptedItemType().equals("Contact")) {
                updateState(channel.getUID(), data ? OpenClosedType.OPEN : OpenClosedType.CLOSED);
            } else {
                updateState(channel.getUID(), data ? OnOffType.ON : OnOffType.OFF);
            }
            logger.debug("Thing: {}, channel {}: {}", thing.getUID(), channel.getUID(), data);
        }

        oldValue = data ? 1 : 0;
    }

    public int getBit() {
        final String name = getBlockName();
        if ((bit == -1) && isBlockValid(name)) {
            int base = 0;

            final String[] parts = name.split("\\.");
            final Map<?, Integer> family = LOGO_MEMORY_BLOCK.get(getLogoFamily());
            if (Character.isDigit(parts[0].charAt(1))) {
                base = family.get(parts[0].substring(0, 1));
                bit = Integer.parseInt(parts[0].substring(1));
            } else if (Character.isDigit(parts[0].charAt(2))) {
                base = family.get(parts[0].substring(0, 2));
                bit = Integer.parseInt(parts[0].substring(2));
            }

            if (base != 0) { // Only VB/VW memory ranges are 0 based
                bit = (bit - 1) % 8;
            } else {
                bit = Integer.parseInt(parts[1]);
            }
        }
        return bit;
    }

    @Override
    protected int getAddress(final String name) {
        int address = -1;
        if (isBlockValid(name)) {
            int base = 0;

            final String block = name.split("\\.")[0];
            final Map<?, Integer> family = LOGO_MEMORY_BLOCK.get(getLogoFamily());
            if (Character.isDigit(block.charAt(1))) {
                base = family.get(block.substring(0, 1));
                address = Integer.parseInt(block.substring(1));
            } else if (Character.isDigit(block.charAt(2))) {
                base = family.get(block.substring(0, 2));
                address = Integer.parseInt(block.substring(2));
            }

            if (base != 0) { // Only VB/VW memory ranges are 0 based
                address = base + (address - 1) / 8;
            }
        }
        return address;
    }

    @Override
    protected boolean isBlockValid(final String name) {
        boolean valid = false;
        if (name.length() >= 2) {
            valid = valid || name.startsWith("I") || name.startsWith("NI"); // Inputs
            valid = valid || name.startsWith("Q") || name.startsWith("NQ"); // Outputs
            valid = valid || name.startsWith("M"); // Markers
            if (!valid && name.startsWith("VB")) { // Memory block
                final String[] parts = name.split("\\.");
                if (parts.length == 2) {
                    final int bit = Integer.parseInt(parts[1]);
                    valid = (0 <= bit) && (bit <= 7);
                }
            }
        }
        return valid;
    }
}
