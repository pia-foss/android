/*
 *  Copyright (c) 2020 Private Internet Access, Inc.
 *
 *  This file is part of the Private Internet Access Android Client.
 *
 *  The Private Internet Access Android Client is free software: you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as published by the Free
 *  Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  The Private Internet Access Android Client is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License along with the Private
 *  Internet Access Android Client.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.privateinternetaccess.android.wireguard.crypto;

/**
 * Represents a Curve25519 key pair as used by WireGuard.
 * <p>
 * Instances of this class are immutable.
 */
public class KeyPair {
    private final Key privateKey;
    private final Key publicKey;

    /**
     * Creates a key pair using a newly-generated private key.
     */
    public KeyPair() {
        this(Key.generatePrivateKey());
    }

    /**
     * Creates a key pair using an existing private key.
     *
     * @param privateKey a private key, used to derive the public key
     */
    public KeyPair(final Key privateKey) {
        this.privateKey = privateKey;
        publicKey = Key.generatePublicKey(privateKey);
    }

    /**
     * Returns the private key from the key pair.
     *
     * @return the private key
     */
    public Key getPrivateKey() {
        return privateKey;
    }

    /**
     * Returns the public key from the key pair.
     *
     * @return the public key
     */
    public Key getPublicKey() {
        return publicKey;
    }
}
