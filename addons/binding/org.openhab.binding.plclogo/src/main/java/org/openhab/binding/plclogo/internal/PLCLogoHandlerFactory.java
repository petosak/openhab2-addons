/**
 * Copyright (c) 2014-2017 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.plclogo.internal;

import static org.openhab.binding.plclogo.PLCLogoBindingConstants.*;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.openhab.binding.plclogo.handler.PLCAnalogInputHandler;
import org.openhab.binding.plclogo.handler.PLCAnalogOutputHandler;
import org.openhab.binding.plclogo.handler.PLCBridgeHandler;
import org.openhab.binding.plclogo.handler.PLCDigitalInputHandler;
import org.openhab.binding.plclogo.handler.PLCDigitalOutputHandler;

/**
 * The {@link PLCLogoHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Alexander Falkenstern - Initial contribution
 */
public class PLCLogoHandlerFactory extends BaseThingHandlerFactory {
    private final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = new HashSet<ThingTypeUID>();

    public PLCLogoHandlerFactory() {
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_DEVICE);
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_DIGITAL_INPUT);
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_DIGITAL_OUTPUT);
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_ANALOG_INPUT);
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_ANALOG_OUTPUT);
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {
        if (THING_TYPE_DEVICE.equals(thing.getThingTypeUID()) && (thing instanceof Bridge)) {
            return new PLCBridgeHandler((Bridge) thing);
        } else if (THING_TYPE_DIGITAL_INPUT.equals(thing.getThingTypeUID())) {
            return new PLCDigitalInputHandler(thing);
        } else if (THING_TYPE_DIGITAL_OUTPUT.equals(thing.getThingTypeUID())) {
            return new PLCDigitalOutputHandler(thing);
        } else if (THING_TYPE_ANALOG_INPUT.equals(thing.getThingTypeUID())) {
            return new PLCAnalogInputHandler(thing);
        } else if (THING_TYPE_ANALOG_OUTPUT.equals(thing.getThingTypeUID())) {
            return new PLCAnalogOutputHandler(thing);
        }

        return null;
    }
}
