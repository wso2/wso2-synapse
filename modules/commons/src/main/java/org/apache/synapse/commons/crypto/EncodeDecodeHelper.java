/*
* Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* WSO2 Inc. licenses this file to you under the Apache License,
* Version 2.0 (the "License"); you may not use this file except
* in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.apache.synapse.commons.crypto;

import org.apache.axiom.util.base64.Base64Utils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.mail.util.Hex;

import java.math.BigInteger;

/**
 * This class is a helper class to do encoding and decoding
 */
public class EncodeDecodeHelper {
    private static Log log = LogFactory.getLog(EncodeDecodeHelper.class);

    /**
     * Encodes the provided byte array using the specified encoding type.
     *
     * @param input         The byte array to encode
     * @param encodingType The encoding to use
     * @return The encoded ByteArrayOutputStream as a String
     * @throws IllegalArgumentException if the specified decodingType is not supported
     */
    public static byte[] encode(byte[] input, EncodeDecodeTypes encodingType) {
        switch (encodingType) {
            case BASE64:
                if (log.isDebugEnabled()) {
                    log.debug("base64 encoding on output ");
                }
                return Base64Utils.encode(input).getBytes();
            case BIGINTEGER16:
                if (log.isDebugEnabled()) {
                    log.debug("BigInteger 16 encoding on output ");
                }
                return new BigInteger(input).toByteArray();
            case HEX:
                if (log.isDebugEnabled()) {
                    log.debug("Hex encoding on output ");
                }
                return Hex.encode(input);
            default:
                throw new IllegalArgumentException("Unsupported encoding type");
        }
    }

    /**
     * Decodes the provided byte array using the specified decoding type.
     *
     * @param input  The byte array to decode
     * @param decodingType The decoding type to use
     * @return The decoded byte array
     * @throws IllegalArgumentException if the specified decodingType is not supported
     */
    public static byte[] decode(byte[] input, EncodeDecodeTypes decodingType) {
        switch (decodingType) {
            case BASE64:
                if (log.isDebugEnabled()) {
                    log.debug("base64 decoding on input  ");
                }
                return Base64Utils.decode(new String(input));
            case BIGINTEGER16:
                if (log.isDebugEnabled()) {
                    log.debug("BigInteger 16 decoding on output ");
                }
                BigInteger n = new BigInteger(new String(input), 16);
                return n.toByteArray();
            case HEX:
                if (log.isDebugEnabled()) {
                    log.debug("Hex decoding on output ");
                }
                return Hex.decode(input);
            default:
                throw new IllegalArgumentException("Unsupported encoding type");
        }
    }
}
