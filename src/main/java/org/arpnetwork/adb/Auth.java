/*
 * Copyright 2018 ARP Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.arpnetwork.adb;

import android.util.Base64;
import android.util.Log;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.RSAPublicKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class Auth {
    // DER encoding T of SHA-1 DigestInfo value. See RFC3447#section-9.2
    private static final byte[] ID_SHA_1 = new byte[]{48, 33, 48, 9, 6, 5, 43, 14, 3, 2, 26, 5, 0, 4, 20};

    private static final int ANDROID_PUBKEY_MODULUS_SIZE = 2048 / 8;
    private static final int ANDROID_PUBKEY_ENCODED_SIZE = ANDROID_PUBKEY_MODULUS_SIZE * 2 + 12;

    private RSAPrivateKey mPrivateKey;
    private byte[] mPublicKey;

    public Auth() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            KeyPair keyPair = kpg.generateKeyPair();
            mPrivateKey = (RSAPrivateKey) keyPair.getPrivate();
            mPublicKey = encodePublicKey((RSAPublicKey) keyPair.getPublic());
        } catch (NoSuchAlgorithmException e) {
        }
    }

    public Auth(String key) throws InvalidKeySpecException {
        byte[] encodedKey = Base64.decode(key, Base64.DEFAULT);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encodedKey);

        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            mPrivateKey = (RSAPrivateKey) kf.generatePrivate(keySpec);
            RSAPublicKeySpec spec = new RSAPublicKeySpec(mPrivateKey.getModulus(), RSAKeyGenParameterSpec.F4);
            mPublicKey = encodePublicKey((RSAPublicKey) kf.generatePublic(spec));
        } catch (NoSuchAlgorithmException e) {
        }
    }

    // Signs `token` with the private key.
    public byte[] sign(byte[] token) {
        byte[] signature = null;

        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, mPrivateKey);
            signature = cipher.doFinal(encodeSHA1(token));
        } catch (NoSuchAlgorithmException var5) {
        } catch (InvalidKeyException e) {
        } catch (NoSuchPaddingException e) {
        } catch (IllegalBlockSizeException e) {
        } catch (BadPaddingException e) {
        }

        return signature;
    }

    // Returns the private key.
    public String getPrivateKey() {
        return Base64.encodeToString(mPrivateKey.getEncoded(), Base64.NO_WRAP);
    }

    // Returns the public key.
    public String getPublicKey() {
        return Base64.encodeToString(mPublicKey, Base64.NO_WRAP);
    }

    // Returns the public key digest.
    public String getPublicKeyDigest() {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] digest = md5.digest(mPublicKey);
            return toHexString(digest);
        } catch (NoSuchAlgorithmException e) {
        }

        return null;
    }

    // Encodes `key` in the Android RSA public key binary format.
    private byte[] encodePublicKey(RSAPublicKey key) {
        ByteBuffer bb = ByteBuffer.allocate(ANDROID_PUBKEY_ENCODED_SIZE);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        BigInteger n = key.getModulus();
        BigInteger e = key.getPublicExponent();

        // Store the modulus size.
        bb.putInt(ANDROID_PUBKEY_MODULUS_SIZE / 4);

        // Compute and store n0inv = -1 / N[0] mod 2^32.
        BigInteger r32 = BigInteger.valueOf(0x100000000L);
        BigInteger n0inv = n.mod(r32);
        n0inv = n0inv.modInverse(r32);
        n0inv = r32.subtract(n0inv);
        bb.putInt(n0inv.intValue());

        // Store the modulus.
        put(bb, n);

        // Compute and store rr = (2^(rsa_size)) ^ 2 mod N.
        BigInteger rr = BigInteger.ZERO;
        rr = rr.setBit(ANDROID_PUBKEY_MODULUS_SIZE * 8);
        rr = rr.modPow(BigInteger.valueOf(2L), n);
        put(bb, rr);

        // Store the exponent.
        bb.putInt(e.intValue());

        return bb.array();
    }

    // EMSA-PKCS1-v1_5-ENCODE
    private byte[] encodeSHA1(byte[] digest) {
        return ByteBuffer.allocate(ID_SHA_1.length + digest.length)
                .put(ID_SHA_1)
                .put(digest)
                .array();
    }

    private void put(ByteBuffer bb, BigInteger value) {
        int length = ANDROID_PUBKEY_MODULUS_SIZE;
        byte[] bytes = value.toByteArray();
        for (int i = 0, j = bytes.length - 1; i < length; ++i, --j) {
            bb.put(bytes[j]);
        }
    }

    private String toHexString(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            if (sb.length() > 0) {
                sb.append(":");
            }
            sb.append(String.format("%02X", b & 0xFF));
        }
        return sb.toString();
    }
}
