/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.plclogo.config;

import java.util.Objects;

/**
 * The {@link PLCLogoAnalogConfiguration} holds configuration of Siemens LOGO! PLC
 * analog input/outputs blocks.
 *
 * @author Alexander Falkenstern - Initial contribution
 */
public class PLCLogoAnalogConfiguration {

    private String block = null;
    private Boolean force = false;
    private Integer threshold = 0;

    public PLCLogoAnalogConfiguration() {
    }

    /**
     * Get configured Siemens LOGO! device block name.
     *
     * @return Configured Siemens LOGO! block name
     */
    public String getBlockName() {
        return block;
    }

    /**
     * Set Siemens LOGO! device block name.
     *
     * @param name Siemens LOGO! block name
     */
    public void setBlockName(final String name) {
        Objects.requireNonNull(name, "Block name may not be null");
        this.block = name.trim();
    }

    /**
     * Returns if Siemens LOGO! device block channel update must be forced.
     *
     * @return True, if channel update to be forced and false otherwise
     */
    public Boolean isUpdateForced() {
        return force;
    }

    /**
     * Set Siemens LOGO! device blocks update must be forced.
     *
     * @param force Force update of Siemens LOGO! device blocks
     */
    public void setForceUpdate(final Boolean force) {
        Objects.requireNonNull(force, "Force may not be null");
        this.force = force;
    }

    /**
     * Get Siemens LOGO! device blocks update threshold.
     *
     * @return Configured Siemens LOGO! update threshold
     */
    public Integer getThreshold() {
        return threshold;
    }

    /**
     * Set Siemens LOGO! device blocks update threshold.
     *
     * @param force Force update of Siemens LOGO! device blocks
     */
    public void setThreshold(final Integer threshold) {
        Objects.requireNonNull(threshold, "Threshold may not be null");
        this.threshold = threshold;
    }
}
