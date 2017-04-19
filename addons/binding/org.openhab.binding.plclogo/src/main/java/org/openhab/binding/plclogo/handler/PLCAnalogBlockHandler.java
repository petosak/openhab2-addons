/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.plclogo.handler;

import static org.openhab.binding.plclogo.PLCLogoBindingConstants.*;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.openhab.binding.plclogo.internal.PLCLogoDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import Moka7.S7;

/**
 * The {@link PLCAnalogBlockHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Alexander Falkenstern - Initial contribution
 */
public class PLCAnalogBlockHandler extends PLCBlockHandler {

    private final Logger logger = LoggerFactory.getLogger(PLCAnalogBlockHandler.class);

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections.singleton(THING_TYPE_ANALOG);

    long threshold = 0;
    long oldValue = Long.MAX_VALUE;

    /**
     * {@inheritDoc}
     */
    public PLCAnalogBlockHandler(Thing thing) {
        super(thing);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
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
                final ChannelUID uid = new ChannelUID(getThing().getUID(), ANALOG_CHANNEL_ID);
                ChannelBuilder channel = ChannelBuilder.create(uid, "Number");
                channel = channel.withLabel(name);
                channel = channel.withDescription("Analog " + text);
                builder = builder.withChannel(channel.build());
            }

            oldValue = Long.MAX_VALUE;
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

        oldValue = Long.MAX_VALUE;
        threshold = 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setData(final byte[] data) {
        if ((data.length == 2) || (data.length == 4)) {
            final long value = data.length == 2 ? S7.GetShortAt(data, 0) : S7.GetDWordAt(data, 0);
            logger.debug("Block {} received {}.", getBlockName(), value);

            if ((Math.abs(oldValue - value) >= threshold) || isUpdateForcing()) {
                final Channel channel = thing.getChannel(ANALOG_CHANNEL_ID);

                final String type = channel.getAcceptedItemType();
                if (type.equalsIgnoreCase("Number")) {
                    updateState(channel.getUID(), new DecimalType(value));
                } else {
                    logger.warn("Channel {} will not accept {} items.", channel.getUID(), type);
                }
                logger.debug("Channel {} accepting {} was set to {}.", channel.getUID(), type, value);

                oldValue = value;
            }
        } else {
            logger.warn("Block {} received wrong data {}.", getBlockName(), data);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PLCLogoDataType getBlockDataType() {
        final String kind = getBlockKind();
        if ((kind != null) && isBlockValid(getBlockName())) {
            return kind.equalsIgnoreCase("VD") ? PLCLogoDataType.DWORD : PLCLogoDataType.WORD;
        }
        return PLCLogoDataType.INVALID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getAddress(final String name) {
        int address = -1;

        logger.debug("Get address of {} LOGO! for block {} .", getLogoFamily(), name);

        if (isBlockValid(name)) {
            final String block = name.trim().split("\\.")[0];
            if (Character.isDigit(block.charAt(2))) {
                address = Integer.parseInt(block.substring(2));
            } else if (Character.isDigit(block.charAt(3))) {
                address = Integer.parseInt(block.substring(3));
            }

            final int base = getBase(name);
            if (base != 0) { // Only VB/VD/VW memory ranges are 0 based
                address = base + (address - 1) * 2;
            }
        }
        return address;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getBit(final String name) {
        logger.debug("Get bit of {} LOGO! for block {} .", getLogoFamily(), name);

        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isBlockValid(final String name) {
        boolean valid = false;
        if (name.length() >= 3) {
            valid = valid || name.startsWith("AI") || name.startsWith("NAI"); // Inputs
            valid = valid || name.startsWith("AQ") || name.startsWith("NAQ"); // Outputs
            valid = valid || name.startsWith("AM"); // Markers
            if (!valid && (name.startsWith("VW") || name.startsWith("VD"))) { // Memory block
                String[] memparts = name.split("\\.");
                valid = (memparts.length == 1);
            }
        }
        return valid;
    }

    /**
     * Calculate address offset for given block name.
     *
     * @param name Name of the data block
     * @return Calculated address offset
     */
    private int getBase(final String name) {
        int base = 0;

        logger.debug("Get base address of {} LOGO! for block {} .", getLogoFamily(), name);

        final String block = name.trim().split("\\.")[0];
        final Map<?, Integer> family = LOGO_MEMORY_BLOCK.get(getLogoFamily());
        if (Character.isDigit(block.charAt(2))) {
            base = family.get(block.substring(0, 2));
        } else if (Character.isDigit(block.charAt(3))) {
            base = family.get(block.substring(0, 3));
        }
        return base;
    }

}
