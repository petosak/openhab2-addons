/**
 * Copyright (c) 2014-2017 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.plclogo;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link PLCLogoBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Alexander Falkenstern - Initial contribution
 */
public class PLCLogoBindingConstants {

    public static final String BINDING_ID = "plclogo";

    // List of all Thing Type UIDs
    public final static ThingTypeUID THING_TYPE_DEVICE = new ThingTypeUID(BINDING_ID, "device");

    // Bridge config properties
    public static final String LOGO_HOST = "address";
    public static final String LOGO_FAMILY = "family";
    public static final String LOGO_LOCAL_TSAP = "localTSAP";
    public static final String LOGO_REMOTE_TSAP = "remoteTSAP";
    public static final String LOGO_REFRESH_INTERVAL = "refresh";

    // LOGO! family definitions
    @SuppressWarnings("serial")
    public static final Map<?, Integer> LOGO_FAMILIES = Collections.unmodifiableMap(new HashMap<String, Integer>() {
        {
            put("OBA7", 1024);
            put("OBA8", 1470);
        }
    });
}
