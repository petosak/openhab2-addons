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
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.openhab.binding.plclogo.config.PLCLogoDigitalConfiguration;
import org.openhab.binding.plclogo.internal.PLCLogoDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import Moka7.S7;

/**
 * The {@link PLCDigitalBlockHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Alexander Falkenstern - Initial contribution
 */
public class PLCDigitalBlockHandler extends PLCBlockHandler {

    private final Logger logger = LoggerFactory.getLogger(PLCDigitalBlockHandler.class);

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections.singleton(THING_TYPE_DIGITAL);

    PLCLogoDigitalConfiguration config = getConfigAs(PLCLogoDigitalConfiguration.class);
    int oldValue = Integer.MAX_VALUE;

    public PLCDigitalBlockHandler(Thing thing) {
        super(thing);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        final String name = getBlockName();
        if (isBlockValid(name)) {
            final String kind = getBlockKind();
            String text = kind.equalsIgnoreCase("I") || kind.equalsIgnoreCase("NI") ? INPUT : OUTPUT;

            ThingBuilder builder = editThing();
            text = text.substring(0, 1).toUpperCase() + text.substring(1);
            builder = builder.withLabel(getBridge().getLabel() + ": " + text + " " + name);

            if (thing.getChannel(DIGITAL_CHANNEL_ID) == null) {
                final ChannelUID uid = new ChannelUID(getThing().getUID(), DIGITAL_CHANNEL_ID);
                final String type = INPUT.equalsIgnoreCase(text) ? "Contact" : "Switch";
                ChannelBuilder channel = ChannelBuilder.create(uid, type);
                channel = channel.withLabel(name);
                channel = channel.withDescription("Digital " + text);
                builder = builder.withChannel(channel.build());
            }

            oldValue = Integer.MAX_VALUE;
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
        logger.debug("Dispose LOGO! digital handler.");
        super.dispose();

        oldValue = Integer.MAX_VALUE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setData(final byte[] data) {
        if (data.length == 1) {
            final boolean value = S7.GetBitAt(data, 0, getBit());
            logger.debug("Block {} received {}.", getBlockName(), value);

            if ((oldValue != (value ? 1 : 0)) || config.isUpdateForced()) {
                final Channel channel = thing.getChannel(DIGITAL_CHANNEL_ID);

                final String type = channel.getAcceptedItemType();
                if (type.equalsIgnoreCase("Contact")) {
                    updateState(channel.getUID(), value ? OpenClosedType.CLOSED : OpenClosedType.OPEN);
                } else if (type.equalsIgnoreCase("Switch")) {
                    updateState(channel.getUID(), value ? OnOffType.ON : OnOffType.OFF);
                } else {
                    logger.warn("Channel {} will not accept {} items.", channel.getUID(), type);
                }
                logger.debug("Channel {} accepting {} was set to {}.", channel.getUID(), type, value);

                oldValue = value ? 1 : 0;
            }
        } else {
            logger.warn("Block {} received wrong data {}.", getBlockName(), data);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getBlockName() {
        return config.getBlockName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PLCLogoDataType getBlockDataType() {
        return isBlockValid(getBlockName()) ? PLCLogoDataType.BIT : PLCLogoDataType.INVALID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateConfiguration(Configuration configuration) {
        super.updateConfiguration(configuration);
        config = getConfigAs(PLCLogoDigitalConfiguration.class);
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
            if (Character.isDigit(block.charAt(1))) {
                address = Integer.parseInt(block.substring(1));
            } else if (Character.isDigit(block.charAt(2))) {
                address = Integer.parseInt(block.substring(2));
            }

            final int base = getBase(name);
            if (base != 0) { // Only VB/VD/VW memory ranges are 0 based
                address = base + (address - 1) / 8;
            }
        }
        return address;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getBit(final String name) {
        int bit = -1;

        logger.debug("Get bit of {} LOGO! for block {} .", getLogoFamily(), name);

        if (isBlockValid(name)) {
            final String[] parts = name.trim().split("\\.");
            if (Character.isDigit(parts[0].charAt(1))) {
                bit = Integer.parseInt(parts[0].substring(1));
            } else if (Character.isDigit(parts[0].charAt(2))) {
                bit = Integer.parseInt(parts[0].substring(2));
            }

            if (getBase(name) != 0) { // Only VB/VD/VW memory ranges are 0 based
                bit = (bit - 1) % 8;
            } else {
                bit = Integer.parseInt(parts[1]);
            }
        }
        return bit;
    }

    /**
     * {@inheritDoc}
     */
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
        if (Character.isDigit(block.charAt(1))) {
            base = family.get(block.substring(0, 1));
        } else if (Character.isDigit(block.charAt(2))) {
            base = family.get(block.substring(0, 2));
        }
        return base;
    }

}
