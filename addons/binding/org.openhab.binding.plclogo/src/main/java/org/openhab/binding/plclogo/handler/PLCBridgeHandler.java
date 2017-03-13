/**
 * Copyright (c) 2014-2017 by the respective copyright holders.
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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import Moka7.S7;
import Moka7.S7Client;

/**
 * The {@link PLCBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Alexander Falkenstern - Initial contribution
 */
public class PLCBridgeHandler extends BaseBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(PLCBridgeHandler.class);

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections.singleton(THING_TYPE_DEVICE);

    /**
     * S7 client this bridge belongs to
     */
    private volatile S7Client client = null;

    /**
     * Buffer for read/write operations
     */
    private byte data[] = new byte[2048];

    private ScheduledFuture<?> job = null;
    private Runnable reader = new Runnable() {
        @Override
        public void run() {
            if (client.LastError > 0) {
                disconnect();
            }
            while (!connect()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException exception) {
                    exception.printStackTrace();
                }
            }

            final Map<?, Integer> block = LOGO_MEMORY_BLOCK.get(getLogoFamily());
            if (client.Connected && (block != null)) {
                final Integer size = block.get("SIZE");

                // read first portion directly to data, to avoid extra copy
                final int packet = Math.min(size.intValue(), 1024);
                int result = client.ReadArea(S7.S7AreaDB, 1, 0, packet, data);

                int offset = packet;
                while ((result == 0) && (offset < size.intValue())) {
                    byte buffer[] = new byte[Math.min(size.intValue() - offset, packet)];
                    result = client.ReadArea(S7.S7AreaDB, 1, offset, buffer.length, buffer);
                    System.arraycopy(buffer, 0, data, offset, buffer.length);
                    offset = offset + buffer.length;
                }
            }
        }
    };

    public PLCBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleRemoval() {
        super.handleRemoval();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.info("Handle command {} on channel {}", command, channelUID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        logger.debug("Initialize LOGO! bridge handler.");
        final Configuration config = getConfig();

        boolean configured = config.containsKey(LOGO_HOST);
        configured = configured && config.containsKey(LOGO_LOCAL_TSAP);
        configured = configured && config.containsKey(LOGO_REMOTE_TSAP);

        if (configured) {
            if (client == null) {
                client = new S7Client();
            }
            configured = connect();
        }

        if (configured) {
            super.initialize();
            String host = null;
            Object entry = config.get(LOGO_HOST);
            if (entry instanceof String) {
                host = (String) entry;
            }

            Integer interval = Integer.valueOf(100);
            entry = getConfigParameter(LOGO_REFRESH_INTERVAL);
            if (entry instanceof Integer) {
                interval = (Integer) entry;
            }

            logger.debug("Creating new reader job for {} with interval {} ms.", host, interval.toString());
            job = scheduler.scheduleAtFixedRate(reader, 1, interval, TimeUnit.MILLISECONDS);
        } else {
            final String message = "Can not initialize LOGO!. Please, check parameter.";
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, message);
            client = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        logger.debug("Dispose LOGO! bridge handler.");
        super.dispose();

        if (job != null) {
            job.cancel(false);
            while (!job.isDone()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException exception) {
                    exception.printStackTrace();
                    break;
                }
            }
            job = null;
        }
        if (disconnect()) {
            client = null;
        }
    }

    @Override
    public void childHandlerInitialized(ThingHandler childHandler, Thing childThing) {
        super.childHandlerInitialized(childHandler, childThing);
    }

    @Override
    public void childHandlerDisposed(ThingHandler childHandler, Thing childThing) {
        super.childHandlerDisposed(childHandler, childThing);
    }

    public synchronized int read(final Thing thing) {
        // return client.ReadArea(S7.S7AreaDB, 1, address, value.length, value);
        return 0;
    }

    public synchronized int write(final Thing thing) {
        // return client.WriteArea(S7.S7AreaDB, 1, address, value.length, value);
        return 0;
    }

    public String getLogoFamily() {
        Object family = getConfigParameter(LOGO_FAMILY);
        if (family instanceof String) {
            return (String) family;
        }
        return null;
    }

    private synchronized boolean connect() {
        Object entry = null;
        if (!client.Connected) {
            String host = null;
            entry = getConfigParameter(LOGO_HOST);
            if (entry instanceof String) {
                host = (String) entry;
            }

            Integer local = null;
            entry = getConfigParameter(LOGO_LOCAL_TSAP);
            if (entry instanceof String) {
                local = Integer.decode((String) entry);
            }

            Integer remote = null;
            entry = getConfigParameter(LOGO_REMOTE_TSAP);
            if (entry instanceof String) {
                remote = Integer.decode((String) entry);
            }

            if ((host != null) && (local != null) && (remote != null)) {
                client.SetConnectionParams(host, local.intValue(), remote.intValue());
                client.Connect();
            }
        }

        return client.Connected;
    }

    private synchronized boolean disconnect() {
        boolean result = false;
        if (client != null) {
            client.Disconnect();
            result = !client.Connected;
        }
        return result;
    }

    private Object getConfigParameter(final String name) {
        Object result = null;
        Configuration config = getConfig();
        if (config.containsKey(name)) {
            result = config.get(name);
        }
        return result;
    }
}
