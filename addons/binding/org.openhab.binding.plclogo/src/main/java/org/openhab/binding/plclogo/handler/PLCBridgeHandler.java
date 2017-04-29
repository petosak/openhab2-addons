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

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.plclogo.config.PLCLogoBridgeConfiguration;
import org.openhab.binding.plclogo.internal.PLCLogoClient;
import org.openhab.binding.plclogo.internal.PLCLogoDataType;
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

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections.singleton(THING_TYPE_DEVICE);

    /**
     * S7 client this bridge belongs to
     */
    private volatile PLCLogoClient client = null;
    private Set<PLCBlockHandler> handlers = new HashSet<PLCBlockHandler>();
    private PLCLogoBridgeConfiguration config = getConfigAs(PLCLogoBridgeConfiguration.class);

    private ScheduledFuture<?> job = null;
    private final Runnable reader = new Runnable() {
        // Buffer for block data read operation
        private final byte[] buffer = new byte[2048];

        // Buffer for diagnostic data
        private final byte[] data = { 0, 0, 0, 0, 0, 0, 0 };

        @Override
        public void run() {
            try {
                final Map<?, Integer> memory = LOGO_MEMORY_BLOCK.get(getLogoFamily());
                if ((memory != null) && (client != null)) {
                    int result = client.ReadDBArea(1, LOGO_STATE.intValue(), data.length, S7Client.S7WLByte, data);
                    if (result == 0) {
                        Calendar calendar = Calendar.getInstance();
                        final int year = calendar.get(Calendar.YEAR) / 100;
                        calendar.set(year * 100 + data[1], data[2] - 1, data[3], data[4], data[5], data[6]);
                        final Channel channel = thing.getChannel(RTC_CHANNEL_ID);
                        updateState(channel.getUID(), new DateTimeType(calendar));
                    } else {
                        logger.error("Can not read diagnostics from LOGO!: {}.", S7Client.ErrorText(result));
                    }

                    final Integer size = memory.get("SIZE");
                    result = client.ReadDBArea(1, 0, size.intValue(), S7Client.S7WLByte, buffer);
                    if (result == 0) {
                        synchronized (handlers) {
                            for (PLCBlockHandler handler : handlers) {
                                if (handler == null) {
                                    logger.warn("Skip processing of invalid handler.");
                                    continue;
                                }

                                final int offset = PLCLogoDataType.getBytesCount(handler.getBlockDataType());
                                if (offset > 0) {
                                    final int address = handler.getAddress();
                                    handler.setData(Arrays.copyOfRange(buffer, address, address + offset));
                                } else {
                                    logger.error("Invalid handler {} found.", handler.getClass().getSimpleName());
                                }
                            }
                        }
                    } else {
                        logger.error("Can not read data from LOGO!: {}.", S7Client.ErrorText(result));
                    }
                } else {
                    logger.error("Either memory block {} or LOGO! client {} is invalid.", memory, client);
                }
            } catch (Exception exception) {
                logger.error("Reader thread got exception: {}.", exception.getMessage());
            } catch (Error error) {
                logger.error("Reader thread got error: {}.", error.getMessage());
                throw error;
            }
        }
    };

    /**
     * {@inheritDoc}
     */
    public PLCBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Handle command {} on channel {}", command, channelUID);

        final String channelId = channelUID.getId();
        final ThingUID thingUID = channelUID.getThingUID();
        if ((thingUID == null) || (thingRegistry == null) || (client == null)) {
            logger.error("Can not refresh channel {}: {}, {}, {} .", channelUID, thingUID, thingRegistry, client);
            return;
        }

        final ThingHandler handler = thingRegistry.get(thingUID).getHandler();
        if (handler instanceof PLCBridgeHandler && command instanceof RefreshType) {
            if (RTC_CHANNEL_ID.equals(channelId)) {
                byte[] buffer = { 0, 0, 0, 0, 0, 0, 0 };
                int result = client.ReadDBArea(1, LOGO_STATE.intValue(), buffer.length, S7Client.S7WLByte, buffer);
                if (result == 0) {
                    Calendar calendar = Calendar.getInstance();
                    final int year = calendar.get(Calendar.YEAR) / 100;
                    calendar.set(year * 100 + buffer[1], buffer[2] - 1, buffer[3], buffer[4], buffer[5], buffer[6]);
                    updateState(channelUID, new DateTimeType(calendar));
                } else {
                    logger.error("Can not read data from LOGO!: {}.", S7Client.ErrorText(result));
                }
            } else {
                logger.warn("Can not refresh not supported channel: {}.", channelUID);
            }
        } else if (handler instanceof PLCBlockHandler && command instanceof RefreshType) {
            final PLCBlockHandler bHandler = (PLCBlockHandler) handler;
            final int offset = PLCLogoDataType.getBytesCount(bHandler.getBlockDataType());
            if ((offset > 0) && (ANALOG_CHANNEL_ID.equals(channelId) || DIGITAL_CHANNEL_ID.equals(channelId))) {
                final byte[] buffer = new byte[offset];
                int result = client.ReadDBArea(1, bHandler.getAddress(), buffer.length, S7Client.S7WLByte, buffer);
                if (result == 0) {
                    bHandler.setData(Arrays.copyOfRange(buffer, 0, offset));
                } else {
                    logger.error("Can not read data from LOGO!: {}.", S7Client.ErrorText(result));
                }
            } else {
                logger.warn("Can not refresh not supported channel: {}.", channelUID);
            }
        } else if (handler instanceof PLCDigitalBlockHandler
                && (command instanceof OnOffType || command instanceof OpenClosedType)) {
            final PLCDigitalBlockHandler bHandler = (PLCDigitalBlockHandler) handler;
            final int offset = PLCLogoDataType.getBytesCount(bHandler.getBlockDataType());
            if ((offset > 0) && DIGITAL_CHANNEL_ID.equals(channelId)) {
                final byte[] buffer = new byte[offset];
                if (command instanceof OnOffType) {
                    final OnOffType state = (OnOffType) command;
                    S7.SetBitAt(buffer, 0, 0, state == OnOffType.ON);
                } else if (command instanceof OpenClosedType) {
                    final OpenClosedType state = (OpenClosedType) command;
                    S7.SetBitAt(buffer, 0, 0, state == OpenClosedType.CLOSED);
                }

                final int address = 8 * bHandler.getAddress() + bHandler.getBit();
                int result = client.WriteDBArea(1, address, buffer.length, S7Client.S7WLBit, buffer);
                if (result != 0) {
                    logger.error("Can not write data to LOGO!: {}.", S7Client.ErrorText(result));
                }
            } else {
                logger.warn("Can not update not supported channel: {}.", channelUID);
            }
        } else if (handler instanceof PLCAnalogBlockHandler && command instanceof DecimalType) {
            final PLCAnalogBlockHandler bHandler = (PLCAnalogBlockHandler) handler;
            final int offset = PLCLogoDataType.getBytesCount(bHandler.getBlockDataType());
            if (((offset == 2) || (offset == 4)) && ANALOG_CHANNEL_ID.equals(channelId)) {
                final byte[] buffer = new byte[offset];
                if (offset == 2) {
                    final DecimalType state = (DecimalType) command;
                    S7.SetShortAt(buffer, 0, state.intValue());
                } else {
                    final DecimalType state = (DecimalType) command;
                    S7.SetDWordAt(buffer, 0, state.longValue());
                }
                int result = client.WriteDBArea(1, bHandler.getAddress(), buffer.length, S7Client.S7WLByte, buffer);
                if (result != 0) {
                    logger.error("Can not write data to LOGO!: {}.", S7Client.ErrorText(result));
                }
            } else {
                logger.warn("Can not update not supported channel: {}.", channelUID);
            }
        } else {
            logger.error("Invalid handler {} found.", handler.getClass().getSimpleName());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void initialize() {
        logger.debug("Initialize LOGO! bridge handler.");

        boolean configured = (config.getAddress() != null);
        configured = configured && (config.getLocalTSAP() != null);
        configured = configured && (config.getRemoteTSAP() != null);

        if (configured) {
            if (client == null) {
                client = new PLCLogoClient();
            }
            configured = connect();
        }

        if (configured) {
            if (job == null) {
                final String host = config.getAddress();
                final Integer interval = config.getRefreshRate();
                logger.info("Creating new reader job for {} with interval {} ms.", host, interval.toString());
                job = scheduler.scheduleWithFixedDelay(reader, 100, interval, TimeUnit.MILLISECONDS);
            }
            super.initialize();
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
    public synchronized void dispose() {
        logger.debug("Dispose LOGO! bridge handler.");
        super.dispose();

        if (job != null) {
            job.cancel(false);
            while (!job.isDone()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException exception) {
                    logger.error("Dispose LOGO! bridge handler throw an error: {}.", exception.getMessage());
                    break;
                }
            }
            job = null;
            logger.info("Destroy reader job for {}.", config.getAddress());
        }

        if (disconnect()) {
            client = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void childHandlerInitialized(ThingHandler childHandler, Thing childThing) {
        super.childHandlerInitialized(childHandler, childThing);
        if (childHandler instanceof PLCBlockHandler) {
            final PLCBlockHandler handler = (PLCBlockHandler) childHandler;
            synchronized (handlers) {
                final String name = handler.getBlockName();
                if (!handlers.contains(handler)) {
                    handlers.add(handler);
                    logger.debug("Insert handler for block {}.", name);
                } else {
                    logger.info("Handler {} for block {} already registered.", childThing.getUID(), name);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void childHandlerDisposed(ThingHandler childHandler, Thing childThing) {
        if (childHandler instanceof PLCBlockHandler) {
            final PLCBlockHandler handler = (PLCBlockHandler) childHandler;
            synchronized (handlers) {
                final String name = handler.getBlockName();
                if (handlers.contains(handler)) {
                    handlers.remove(handler);
                    logger.debug("Remove handler for block {}.", name);
                } else {
                    logger.info("Handler {} for block {} already disposed.", childThing.getUID(), name);
                }
            }
        }
        super.childHandlerDisposed(childHandler, childThing);
    }

    /**
     * Returns configured Siemens LOGO! family: 0BA7 or 0BA8.
     *
     * @return Configured Siemens LOGO! family
     */
    public String getLogoFamily() {
        return config.getFamily();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateConfiguration(Configuration configuration) {
        super.updateConfiguration(configuration);
        config = getConfigAs(PLCLogoBridgeConfiguration.class);
    }

    /**
     * Read connection parameter and connect to Siemens LOGO!
     *
     * @return True, if connected and false otherwise
     */
    private synchronized boolean connect() {
        if (!client.Connected) {
            final String host = config.getAddress();
            final Integer local = config.getLocalTSAP();
            final Integer remote = config.getRemoteTSAP();

            if ((host != null) && (local != null) && (remote != null)) {
                client.Connect(host, local.intValue(), remote.intValue());
            }
        }

        return client.Connected;
    }

    /**
     * Disconnect from Siemens LOGO!
     *
     * @return True, if disconnected and false otherwise
     */
    private synchronized boolean disconnect() {
        boolean result = false;
        if (client != null) {
            client.Disconnect();
            result = !client.Connected;
        }
        return result;
    }

}
