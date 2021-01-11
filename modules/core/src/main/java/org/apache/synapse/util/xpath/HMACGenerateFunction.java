/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.synapse.util.xpath;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jaxen.Context;
import org.jaxen.Function;
import org.jaxen.FunctionCallException;
import org.jaxen.function.StringFunction;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.List;

/*
Xpath function to generate HMAC signature
 */
public class HMACGenerateFunction implements Function {

    private static final Log log = LogFactory.getLog(HMACGenerateFunction.class);
    private static final String DEFAULT_HMAC_SIGNATURE = "HmacSHA1";

    @Override
    public Object call(Context context, List args) throws FunctionCallException {
        boolean debugOn = log.isDebugEnabled();
        if (args == null) {
            if (debugOn) {
                log.debug("Missing arguments in the function call");
            }
            return SynapseXPathConstants.NULL_STRING;
        }

        int size = args.size();
        if (size == 2) {
            String payload = StringFunction.evaluate(args.get(0), context.getNavigator());
            String secret = StringFunction.evaluate(args.get(1), context.getNavigator());
            return generateSignature(payload, secret, DEFAULT_HMAC_SIGNATURE);
        }
        if (size == 3) {
            String payload = StringFunction.evaluate(args.get(0), context.getNavigator());
            String secret = StringFunction.evaluate(args.get(1), context.getNavigator());
            String algorithm = StringFunction.evaluate(args.get(2), context.getNavigator());
            return generateSignature(payload, secret, algorithm);
        }
        if (debugOn) {
            log.debug("Missing arguments in the function call");
        }
        return SynapseXPathConstants.NULL_STRING;
    }

    /**
     * Generate the HMAC signature against the secret for given algorithm
     *
     * @param payload  The request body
     * @param secret   The secret
     * @param algorithm The algorithm to generate signature
     * @return The generated signature
     * @throws FunctionCallException On error during HMAC signature generation
     */
    private String generateSignature(String payload, String secret, String algorithm) throws FunctionCallException {

        try {
            Mac mac = Mac.getInstance(algorithm);
            final SecretKeySpec signingKey = new SecretKeySpec(secret.getBytes(), algorithm);
            mac.init(signingKey);
            return toHexString(mac.doFinal(payload.getBytes()));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            String msg = "Error while generating HMAC signature";
            log.error(msg, e);
            throw new FunctionCallException(msg, e);
        }
    }

    private static String toHexString(byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
}
