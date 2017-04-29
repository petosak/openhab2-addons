/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.plclogo.internal;

/**
 * The {@link PLCLogoDataType} describes data types supported.
 *
 * @author Alexander Falkenstern - Initial contribution
 */

public enum PLCLogoDataType {
    INVALID,
    BIT,
    WORD,
    DWORD;

    public static int getBytesCount(final PLCLogoDataType type) {
        int count = -1;
        switch (type) {
            case BIT: {
                count = 1;
                break;
            }
            case DWORD: {
                count = 4;
                break;
            }
            case WORD: {
                count = 2;
                break;
            }
            default:
            case INVALID: {
                break;
            }
        }
        return count;
    }
}
