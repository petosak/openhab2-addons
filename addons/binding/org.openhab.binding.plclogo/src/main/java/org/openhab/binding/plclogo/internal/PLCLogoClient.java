/**
 * Copyright (c) 2014-2017 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.plclogo.internal;

import java.util.concurrent.locks.ReentrantLock;

import Moka7.S7Client;

/**
 * The {@link PLCLogoClient} is thread safe LOGO! client.
 *
 * @author Alexander Falkenstern - Initial contribution
 */
public class PLCLogoClient extends S7Client {

    private final ReentrantLock lock = new ReentrantLock();

    public PLCLogoClient() {
        super();
    }

    public int Connect(String Address, int LocalTSAP, int RemoteTSAP) {
        SetConnectionParams(Address, LocalTSAP, RemoteTSAP);
        return Connect();
    }

    @Override
    public int ReadArea(int Area, int DBNumber, int Start, int Amount, int WordLength, byte[] Data) {
        if (LastError != 0) {
            Disconnect();
        }
        if (!Connected) {
            Connect();
        }

        final int packet = Math.min(Amount, 1024);
        int offset = packet;

        lock.lock();
        int result = -1;
        do {
            // read first portion directly to data
            result = super.ReadArea(Area, DBNumber, Start, packet, WordLength, Data);
            while ((result == 0) && (offset < Amount)) {
                byte buffer[] = new byte[Math.min(Amount - offset, packet)];
                result = super.ReadArea(Area, DBNumber, offset, buffer.length, WordLength, buffer);
                System.arraycopy(buffer, 0, Data, offset, buffer.length);
                offset = offset + buffer.length;
            }
            if (result != 0) {
                Disconnect();
                Connect();
            }
        } while (result != 0);
        lock.unlock();

        return result;
    }

    @Override
    public int WriteArea(int Area, int DBNumber, int Start, int Amount, int WordLength, byte[] Data) {
        if (LastError != 0) {
            Disconnect();
        }
        if (!Connected) {
            Connect();
        }

        lock.lock();
        int result = -1;
        do {
            result = super.WriteArea(Area, DBNumber, Start, Amount, WordLength, Data);
            if (result != 0) {
                Disconnect();
                Connect();
            }
        } while (result != 0);
        lock.unlock();
        return result;
    }

}