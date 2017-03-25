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

import org.eclipse.smarthome.config.core.Configuration;
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

    int threshold = 0;
    int oldValue = Integer.MAX_VALUE;

    public PLCAnalogBlockHandler(Thing thing) {
        super(thing);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        final String INPUT = "Input";
        final String OUTPUT = "Output";

        Configuration config = getConfig();
        if (config.containsKey("threshold")) {
            Object entry = config.get("threshold");
            if (entry instanceof String) {
                threshold = Integer.decode((String) entry).intValue();
            }
        }

        final String name = getBlockName();
        if (isBlockValid(name)) {
            final String kind = getBlockKind();
            String text = kind.equalsIgnoreCase("AI") || kind.equalsIgnoreCase("NAI") ? INPUT : OUTPUT;

            ThingBuilder builder = editThing();
            text = text.substring(0, 1).toUpperCase() + text.substring(1);
            builder = builder.withLabel(getBridge().getLabel() + ": " + text + " " + name);

            if (thing.getChannel(ANALOG_CHANNEL_ID) == null) {
                final ChannelUID UID = new ChannelUID(getThing().getUID(), ANALOG_CHANNEL_ID);
                ChannelBuilder channel = ChannelBuilder.create(UID, "Number");
                channel = channel.withLabel(name);
                channel = channel.withDescription("Analog " + text);
                builder = builder.withChannel(channel.build());
            }

            updateThing(builder.build());
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
        threshold = 0;
    }

    public void setData(final short data) {
        if ((Math.abs(oldValue - data) >= threshold) || isUpdateForcing()) {
            final Channel channel = thing.getChannel(ANALOG_CHANNEL_ID);

            final String type = channel.getAcceptedItemType();
            if (type.equalsIgnoreCase("Number")) {
                updateState(channel.getUID(), new DecimalType(data));
            } else {
                logger.warn("Channel {} will not accept {} items ", channel.getUID(), type);
            }
            logger.debug("Channel {} accepting {} was set to {}", channel.getUID(), type, data);

            oldValue = data;
        }
    }

    @Override
    protected int getAddress(final String name) {
        int address = -1;
        if (isBlockValid(name)) {
            final String block = name.split("\\.")[0];
            if (Character.isDigit(block.charAt(2))) {
                address = Integer.parseInt(block.substring(2));
            } else if (Character.isDigit(block.charAt(3))) {
                address = Integer.parseInt(block.substring(3));
            }

            final int base = getBase(name);
            if (base != 0) { // Only VB/VW memory ranges are 0 based
                address = base + (address - 1) * 2;
            }
        }
        return address;
    }

    @Override
    protected int getBit(final String name) {
        return 0;
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

    private int getBase(final String name) {
        int base = 0;
        final String block = name.split("\\.")[0];
        final Map<?, Integer> family = LOGO_MEMORY_BLOCK.get(getLogoFamily());
        if (Character.isDigit(block.charAt(2))) {
            base = family.get(block.substring(0, 2));
        } else if (Character.isDigit(block.charAt(3))) {
            base = family.get(block.substring(0, 3));
        }
        return base;
    }
}
