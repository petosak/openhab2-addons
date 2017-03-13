/**
 * Copyright (c) 2014-2017 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.plclogo.handler;

import static org.openhab.binding.plclogo.PLCLogoBindingConstants.LOGO_MEMORY_BLOCK;

import java.util.Map;

import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link PLCAnalogBlockHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Alexander Falkenstern - Initial contribution
 */
public abstract class PLCAnalogBlockHandler extends PLCBlockHandler {

    private final Logger logger = LoggerFactory.getLogger(PLCAnalogBlockHandler.class);

    public PLCAnalogBlockHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        if (isBlockValid(getBlockName())) {
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
    public boolean isBlockValid(final String name) {
        boolean valid = false;
        if (name.length() >= 3) {
            valid = name.startsWith("AM");
            if (!valid && name.startsWith("VW")) {
                String[] memparts = name.split("\\.");
                valid = (memparts.length == 1);
            }
        }
        return valid;
    }
}
