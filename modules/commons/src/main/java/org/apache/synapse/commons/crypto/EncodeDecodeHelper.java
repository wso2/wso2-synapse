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
