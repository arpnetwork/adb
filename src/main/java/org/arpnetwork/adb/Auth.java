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

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
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
    private static final String PRIVATE_KEY = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIB\n" +
            "AQDZOSzTrqXBzg4IYzTXQFP9+NS/BMI1UXpiR1QJFbLggy1QuDMm7EmsZ10m3ABhR/vwSj8/EbzIvtJZD7\n" +
            "f3T2bvbSGSefXA1Vw50zhJtMrvHD3DnDzjrDiBT1t+K0bfcMhUPiZxgzESLfPpo7ST9FxOtltLfdLw6Ucp\n" +
            "d11X9stbExZgGS+HRlBKii0eWHWOhEU9Psn38TrN9uOt4wuFQH/puvRbLd0Df3uf6n9rOji0bi7I7P0MfQ\n" +
            "Exu3QDZPtDfLWoxXEbMGxpMfQJ5K/a+4JTwBYLy6K1s9ZQQ00R0TWimA0gQD0KAkpHNMDMu8af0hmCg6hA\n" +
            "o43YLByWUx8FIgwvAgMBAAECggEACOZ0CkxTfovo54ES3UrwO2Z+9XmNcGp9qf1q2Xzi5Ci4OH0DnGqMCq\n" +
            "tTwIigpngw6oq+d/1oZON6AHMQwj+l1X/9FO7Gi76rHJdsl4swtPh3z9ROo7Qe2zybRBNcwCTzZ3EerskT\n" +
            "2/gM7PD52wWeAGMt7xfL6nZfF4Bo3XaUpOR2wYIPaTCvmpcXHogvoYiTARyotJ4jqyFN7B2abGpnSOsnso\n" +
            "9Ub/w+BXLuT9DOWv0OmKvzOZhc0/O146zZqRDzLcBYpO+opIvz14hp4YHtRVdf/6dnXyl2ROyMdTaz1FrQ\n" +
            "vvdPXEY/i3KCl/h5/xnkYMW8GGadKJ8rY3+d9EXgqQKBgQD1iyBMTzWDkoFUuTjUB4B5tf3zU/lruBlENM\n" +
            "HcMDiSD5g0XKwnFPsHHAkA8NWifcfOfx84Muw4NheRm0uhMymAR+raYZ0y0AB6Yi/uwPd6I9aqh0+WnUD1\n" +
            "TdKNsbNCbY+NVAnEtdkAy3sAcpRwyofj3bk25+Djd172J6pYGkTGXQKBgQDieU7FvHJ5zXuQHOdINpAcbM\n" +
            "VtPEGtmLkdV0xwABeKNz+Omu4mqefTuOnqxdtDuXnZ47hm9yjDqBA5HZeAzLHCUy5np5dVNIu40Sc0GzW7\n" +
            "7JBSbj5MZ4UUM0UnJUIBHDoAR4JsFJ6hwyNZQg7+7SqbgXv7lR1yYeoD0y+Xwerb+wKBgDN3sxBrtfLbPa\n" +
            "KtpFzFKcfZPt7HJkvO7fTe/heSP/lVrXikSC109105IiYBVTZXGQ+Ok9Oq0NrDc9NAcuzaFYPfDzoxJcsl\n" +
            "0EPW2uc3qWf/pRpffG48jgYdBtpOeh2da26bQ+TonRDOlfy1B6pQuYUoz47Tsc7cEZqVG96Vuv69AoGBAK\n" +
            "t8wboW9PPoNW0tha/3qO3tKx2I6A6kO1/NT9LrLuf458Z66GQcea+nMHEWuu4wTuU/es10z8g/xXEKSEM+\n" +
            "PEfyJoxUqdHaBQbAURgylmCjQ7E3SzMdm/Zs0CtRGgavMfguLcLbZjcFBQo8bBB6062GwbQB+Jc5LzMQQd\n" +
            "R/APDrAoGAMRhAPypxoNG7uluKAq9Jo2FZz8OU7ZvuDLqTumNF1RFTIg9wwJfYbFQYAzBp0gn7djLaVPMi\n" +
            "F+XRPM3CgIjb31QyDiSf2gC4frwv2oKtlomV9DFE78DkDvfYCfcTRqDMDv8aR5zxUioiqbgSuBu1boGKvP\n" +
            "johjWGKb5WRJIe2pA=";

    // DER encoding T of SHA-1 DigestInfo value. See RFC3447#section-9.2
    private static final byte[] ID_SHA_1 = new byte[]{48, 33, 48, 9, 6, 5, 43, 14, 3, 2, 26, 5, 0, 4, 20};

    private static final int ANDROID_PUBKEY_MODULUS_SIZE = 2048 / 8;
    private static final int ANDROID_PUBKEY_ENCODED_SIZE = ANDROID_PUBKEY_MODULUS_SIZE * 2 + 12;

    private static RSAPrivateKey sPrivateKey;
    private static RSAPublicKey sPublicKey;

    // Signs `token` with the private key.
    public static byte[] sign(byte[] token) {
        byte[] signature = null;

        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, sPrivateKey);
            signature = cipher.doFinal(encodeSHA1(token));
        } catch (NoSuchAlgorithmException var5) {
        } catch (InvalidKeyException e) {
        } catch (NoSuchPaddingException e) {
        } catch (IllegalBlockSizeException e) {
        } catch (BadPaddingException e) {
        }

        return signature;
    }

    // Returns the public key.
    public static byte[] publicKey() {
        String key = encodePublicKey(sPublicKey) + " ARP\0";
        return key.getBytes();
    }

    // Encodes `key` in the Android RSA public key binary format.
    private static String encodePublicKey(RSAPublicKey key) {
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

        return Base64.encodeToString(bb.array(), Base64.NO_WRAP);
    }

    // EMSA-PKCS1-v1_5-ENCODE
    private static byte[] encodeSHA1(byte[] digest) {
        return ByteBuffer.allocate(ID_SHA_1.length + digest.length)
                .put(ID_SHA_1)
                .put(digest)
                .array();
    }

    private static void put(ByteBuffer bb, BigInteger value) {
        int length = ANDROID_PUBKEY_MODULUS_SIZE;
        byte[] bytes = value.toByteArray();
        for (int i = 0, j = bytes.length - 1; i < length; ++i, --j) {
            bb.put(bytes[j]);
        }
    }

    static {
        byte[] key = Base64.decode(PRIVATE_KEY, Base64.DEFAULT);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(key);

        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            sPrivateKey = (RSAPrivateKey) kf.generatePrivate(keySpec);
            RSAPublicKeySpec spec = new RSAPublicKeySpec(sPrivateKey.getModulus(), RSAKeyGenParameterSpec.F4);
            sPublicKey = (RSAPublicKey) kf.generatePublic(spec);
        } catch (InvalidKeySpecException e) {
        } catch (NoSuchAlgorithmException e) {
        }
    }
}
