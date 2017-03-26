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
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.plclogo.PLCLogoBindingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link PLCBlockHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Alexander Falkenstern - Initial contribution
 */
public abstract class PLCBlockHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(PLCBlockHandler.class);

    private int address = -1;
    private int bit = -1;

    /**
     * {@inheritDoc}
     */
    public PLCBlockHandler(Thing thing) {
        super(thing);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        getBridge().getHandler().handleCommand(channelUID, command);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        logger.debug("Initialize LOGO! common block handler.");

        String message = "";
        boolean success = false;
        if ((getBridge() != null) && (getBlockName() != null)) {
            final Map<?, Integer> block = LOGO_MEMORY_BLOCK.get(getLogoFamily());
            if ((0 <= getAddress()) && (getAddress() <= block.get("SIZE"))) {
                success = true;
                super.initialize();
            } else {
                message = "Can not initialize LOGO! block. Please check blocks.";
            }
        } else {
            message = "Can not initialize LOGO! block. Please check bridge/blocks.";
        }

        if (!success) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, message);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        logger.debug("Dispose LOGO! common block handler.");
        super.dispose();
        address = -1;
        bit = -1;
    }

    /**
     * Calculate memory address for configured block.
     *
     * @return Calculated address
     */
    public int getAddress() {
        final String name = getBlockName();
        if ((address == -1) && isBlockValid(name)) {
            address = getAddress(name);
        }
        return address;
    }

    /**
     * Calculate bit within memory address for configured block.
     *
     * @return Calculated bit
     */
    public int getBit() {
        final String name = getBlockName();
        if ((bit == -1) && isBlockValid(name)) {
            bit = getBit(name);
        }
        return bit;
    }

    /**
     * Returns configured block name.
     *
     * @return Name of configured LOGO! block
     */
    public String getBlockName() {
        Object entry = getConfigParameter(LOGO_BLOCK);
        if (entry instanceof String) {
            final String name = (String) entry;
            return name.toUpperCase();
        }
        return null;
    }

    /**
     * Returns configured LOGO! block kind.
     * Can be VB, VW, I, Q, M, AI, AQ, AM, NI, NAI, NQ or NAQ
     *
     * @see PLCLogoBindingConstants#LOGO_MEMORY_0BA7
     * @see PLCLogoBindingConstants#LOGO_MEMORY_0BA8
     * @return Kind of configured block
     */
    public String getBlockKind() {
        final String name = getBlockName();
        if (Character.isDigit(name.charAt(1))) {
            return name.substring(0, 1);
        } else if (Character.isDigit(name.charAt(2))) {
            return name.substring(0, 2);
        } else if (Character.isDigit(name.charAt(3))) {
            return name.substring(0, 3);
        }
        return null;
    }

    /**
     * Returns configured LOGO! family.
     *
     * @see PLCLogoBindingConstants#LOGO_0BA7
     * @see PLCLogoBindingConstants#LOGO_0BA8
     * @return Configured LOGO! family
     */
    public String getLogoFamily() {
        Bridge bridge = getBridge();
        if (bridge != null) {
            PLCBridgeHandler handler = (PLCBridgeHandler) bridge.getHandler();
            return handler.getLogoFamily();
        }
        return null;
    }

    /**
     * Returns if channel update must be forced.
     *
     * @return True, if channel update to be forced and false otherwise
     */
    public boolean isUpdateForcing() {
        boolean result = false;
        Object entry = getConfigParameter(FORCE_UPDATE);
        if (entry instanceof String) {
            final String value = (String) entry;
            result = Boolean.parseBoolean(value.toLowerCase());
        }
        return result;
    }

    /**
     * Calculate address for the block with given name.
     *
     * @param name Name of the LOGO! block
     * @return Calculated address offset
     */
    abstract protected int getAddress(final String name);

    /**
     * Calculate bit within address for block with given name.
     *
     * @param name Name of the LOGO! block
     * @return Calculated bit
     */
    abstract protected int getBit(final String name);

    /**
     * Checks, if given name for the block is valid.
     *
     * @param name Name of the LOGO! block
     * @return True, if the name is valid and false otherwise
     */
    abstract protected boolean isBlockValid(final String name);

    private Object getConfigParameter(final String name) {
        Object result = null;
        Configuration config = getConfig();
        if (config.containsKey(name)) {
            result = config.get(name);
        }
        return result;
    }

}
