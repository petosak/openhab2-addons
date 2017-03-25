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

    public int getAddress() {
        final String name = getBlockName();
        if ((address == -1) && isBlockValid(name)) {
            address = getAddress(name);
        }
        return address;
    }

    public int getBit() {
        final String name = getBlockName();
        if ((bit == -1) && isBlockValid(name)) {
            bit = getBit(name);
        }
        return bit;
    }

    public String getBlockName() {
        Object entry = getConfigParameter(LOGO_BLOCK);
        if (entry instanceof String) {
            final String name = (String) entry;
            return name.toUpperCase();
        }
        return null;
    }

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

    public String getLogoFamily() {
        Bridge bridge = getBridge();
        if (bridge != null) {
            PLCBridgeHandler handler = (PLCBridgeHandler) bridge.getHandler();
            return handler.getLogoFamily();
        }
        return null;
    }

    public boolean isUpdateForcing() {
        boolean result = false;
        Object entry = getConfigParameter(FORCE_UPDATE);
        if (entry instanceof String) {
            final String value = (String) entry;
            result = Boolean.parseBoolean(value.toLowerCase());
        }
        return result;
    }

    abstract protected int getAddress(final String name);

    abstract protected int getBit(final String name);

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
