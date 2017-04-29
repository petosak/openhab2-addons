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
public class PLCLogoAnalogConfiguration extends PLCLogoBlockConfiguration {

    private Integer threshold = 0;

    public PLCLogoAnalogConfiguration() {
        super();
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

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isBlockValid() {
        boolean valid = false;
        final String name = getBlockName();
        if (name.length() >= 3) {
            valid = valid || name.startsWith("AI") || name.startsWith("NAI"); // Inputs
            valid = valid || name.startsWith("AQ") || name.startsWith("NAQ"); // Outputs
            valid = valid || name.startsWith("AM"); // Markers
            if (!valid && (name.startsWith("VW") || name.startsWith("VD"))) { // Memory block
                final String[] parts = name.split("\\.");
                valid = (parts.length == 1);
                if (valid && Character.isDigit(parts[0].charAt(2))) {
                    final int address = Integer.parseInt(parts[0].substring(2));
                    valid = (0 <= address) && (address <= (name.startsWith("VD") ? 847 : 849));
                }
            }
        }
        return valid;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInputBlock() {
        final String kind = getBlockKind();
        return kind.equalsIgnoreCase("AI") || kind.equalsIgnoreCase("NAI");
    }

}
