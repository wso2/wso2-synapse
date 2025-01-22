/*
 *  Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com) All Rights Reserved.
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.mediators.v2;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.util.JavaUtils;
import org.apache.commons.logging.Log;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.config.SynapseConfigUtils;
import org.apache.synapse.config.xml.SynapsePath;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.core.axis2.Axis2MessageContext;

import java.io.ByteArrayInputStream;
import javax.xml.namespace.QName;

public class Utils {

    /**
     * Check whether the message is a scatter message or not
     *
     * @param synCtx MessageContext
     * @return true if the message is a scatter message
     */
    public static boolean isScatterMessage(MessageContext synCtx) {

        Boolean isScatterMessage = (Boolean) synCtx.getProperty(SynapseConstants.SCATTER_MESSAGES);
        return isScatterMessage != null && isScatterMessage;
    }

    /**
     * Check whether the message is a foreach message or not
     *
     * @param synCtx MessageContext
     * @return true if the message is a foreach message
     */
    public static boolean isContinuationTriggeredFromMediatorWorker(MessageContext synCtx) {

        Boolean isContinuationTriggeredMediatorWorker =
                (Boolean) synCtx.getProperty(SynapseConstants.CONTINUE_FLOW_TRIGGERED_FROM_MEDIATOR_WORKER);
        return isContinuationTriggeredMediatorWorker != null && isContinuationTriggeredMediatorWorker;
    }

    public static Object convertValue(String value, String type, Log log) {

        if (type == null) {
            return value;
        }
        try {
            XMLConfigConstants.VARIABLE_DATA_TYPES dataType = XMLConfigConstants.VARIABLE_DATA_TYPES.valueOf(type);
            switch (dataType) {
                case BOOLEAN:
                    return JavaUtils.isTrueExplicitly(value);
                case DOUBLE:
                    return Double.parseDouble(value);
                case INTEGER:
                    return Integer.parseInt(value);
                case LONG:
                    return Long.parseLong(value);
                case XML:
                    return buildOMElement(value);
                case JSON:
                    return buildJSONElement(value, log);
                default:
                    return value;
            }
        } catch (IllegalArgumentException e) {
            String msg = "Unknown type : " + type + " for the variable mediator or the " +
                    "variable value cannot be converted into the specified type.";
            log.error(msg, e);
            throw new SynapseException(msg, e);
        }
    }

    public static OMElement buildOMElement(String xml) {

        if (xml == null) {
            return null;
        }
        OMElement result = SynapseConfigUtils.stringToOM(xml);
        result.buildWithAttachments();
        return result;
    }

    private static JsonElement buildJSONElement(String jsonPayload, Log log) {

        JsonParser jsonParser = new JsonParser();
        try {
            return jsonParser.parse(jsonPayload);
        } catch (JsonSyntaxException ex) {
            // Enclosing using quotes due to the following issue
            // https://github.com/google/gson/issues/1286
            String enclosed = "\"" + jsonPayload + "\"";
            try {
                return jsonParser.parse(enclosed);
            } catch (JsonSyntaxException e) {
                // log the original exception and discard the new exception
                log.error("Malformed JSON payload : " + jsonPayload, ex);
                return null;
            }
        }
    }

    private static boolean isXMLType(String type) {

        return type != null && XMLConfigConstants.VARIABLE_DATA_TYPES.XML.equals(XMLConfigConstants.VARIABLE_DATA_TYPES.valueOf(type));
    }

    private static boolean isStringType(String type) {

        return type != null && XMLConfigConstants.VARIABLE_DATA_TYPES.STRING.equals(XMLConfigConstants.VARIABLE_DATA_TYPES.valueOf(type));
    }

    public static Object getResolvedValue(MessageContext synCtx, SynapsePath expression, Object value, String type, Log log) {

        if (value != null) {
            return value;
        } else {
            if (expression != null) {
                if (isXMLType(type)) {
                    return Utils.buildOMElement(expression.stringValueOf(synCtx));
                } else if (isStringType(type)) {
                    return expression.stringValueOf(synCtx);
                }
                return convertExpressionResult(expression.objectValueOf(synCtx), type, expression.getExpression(), log);
            }
        }
        return null;
    }

    /**
     * Convert the evaluated value to the expected data type.
     *
     * @param evaluatedValue Evaluated value to be converted
     * @param type           Expected data type
     * @return Converted value
     */
    private static Object convertExpressionResult(Object evaluatedValue, String type, String expression, Log log) {

        if (type == null) {
            return evaluatedValue;
        }

        if (evaluatedValue instanceof JsonPrimitive) {
            return convertJsonPrimitive((JsonPrimitive) evaluatedValue, type, expression, log);
        }

        XMLConfigConstants.VARIABLE_DATA_TYPES dataType = XMLConfigConstants.VARIABLE_DATA_TYPES.valueOf(type);
        switch (dataType) {
            case BOOLEAN:
                if (!(evaluatedValue instanceof Boolean)) {
                    handleDataTypeException("BOOLEAN", expression, log);
                }
                break;
            case DOUBLE:
                if (!(evaluatedValue instanceof Double)) {
                    handleDataTypeException("DOUBLE", expression, log);
                }
                break;
            case INTEGER:
                if (!(evaluatedValue instanceof Integer)) {
                    handleDataTypeException("INTEGER", expression, log);
                }
                break;
            case LONG:
                if (!(evaluatedValue instanceof Long)) {
                    handleDataTypeException("LONG", expression, log);
                }
                break;
            case XML:
                if (!(evaluatedValue instanceof OMElement)) {
                    handleDataTypeException("XML", expression, log);
                }
                break;
            case JSON:
                if (!(evaluatedValue instanceof JsonElement)) {
                    handleDataTypeException("JSON", expression, log);
                }
                break;
            default:
        }
        return evaluatedValue;
    }

    /**
     * Convert the JSON primitive to the expected data type.
     *
     * @param jsonPrimitive JSON primitive to be converted
     * @param type          Expected data type
     * @return Converted JSON primitive
     */
    private static Object convertJsonPrimitive(JsonPrimitive jsonPrimitive, String type, String expression, Log log) {

        XMLConfigConstants.VARIABLE_DATA_TYPES dataType = XMLConfigConstants.VARIABLE_DATA_TYPES.valueOf(type);
        switch (dataType) {
            case BOOLEAN:
                if (jsonPrimitive.isBoolean()) {
                    return jsonPrimitive.getAsBoolean();
                } else {
                    handleDataTypeException("BOOLEAN", expression, log);
                }
            case DOUBLE:
                if (jsonPrimitive.isNumber()) {
                    return jsonPrimitive.getAsDouble();
                } else {
                    handleDataTypeException("DOUBLE", expression, log);
                }
            case INTEGER:
                if (jsonPrimitive.isNumber()) {
                    return jsonPrimitive.getAsInt();
                } else {
                    handleDataTypeException("INTEGER", expression, log);
                }
            case LONG:
                if (jsonPrimitive.isNumber()) {
                    return jsonPrimitive.getAsLong();
                } else {
                    handleDataTypeException("LONG", expression, log);
                }
            default:
                return jsonPrimitive.getAsString();
        }
    }

    /**
     * This method will throw a SynapseException with a message indicating that the expression result does not match
     * the expected data type.
     *
     * @param dataType Expected data type
     */
    private static void handleDataTypeException(String dataType, String expression, Log log) {

        String msg = "Expression '${" + expression + "}' result does not match the expected data type '" + dataType + "'";
        log.error(msg);
        throw new SynapseException(msg);
    }

    public static SOAPEnvelope createNewSoapEnvelope(SOAPEnvelope envelope) {

        SOAPFactory fac;
        if (SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI.equals(envelope.getBody().getNamespace().getNamespaceURI())) {
            fac = OMAbstractFactory.getSOAP11Factory();
        } else {
            fac = OMAbstractFactory.getSOAP12Factory();
        }
        return fac.getDefaultEnvelope();
    }

    /**
     * This method is used to check whether the return object is a valid variable type
     *
     * @param returnObject return object
     * @return true if the return object is a valid type
     */
    public static boolean isValidReturnObjectType(Object returnObject) {

        return returnObject instanceof String ||
                returnObject instanceof Boolean ||
                returnObject instanceof Integer ||
                returnObject instanceof Long ||
                returnObject instanceof Double ||
                returnObject instanceof OMElement ||
                returnObject instanceof JsonElement;
    }

    public static boolean isTargetBody(String resultTarget) {

        return "body".equalsIgnoreCase(resultTarget);
    }

    public static boolean isTargetNone(String resultTarget) {

        return "none".equalsIgnoreCase(resultTarget);
    }

    /**
     * This method is used to set the result to the target
     *
     * @param synCtx       MessageContext
     * @param resultTarget Target to set the result
     * @param result       Result object to set
     * @return true if the result is set successfully
     * @throws AxisFault
     * @throws SynapseException
     */
    public static boolean setResultTarget(MessageContext synCtx, String resultTarget, Object result) throws AxisFault,
            SynapseException {

        if (Utils.isTargetNone(resultTarget)) {
            return true;
        }
        if (result != null) {
            if (Utils.isTargetBody(resultTarget)) {
                // set result to body
                if (result instanceof JsonElement) {
                    JsonUtil.getNewJsonPayload(((Axis2MessageContext) synCtx).getAxis2MessageContext(), new
                            ByteArrayInputStream(result.toString().getBytes()), true, true);
                } else {
                    OMElement rootElement = OMAbstractFactory.getOMFactory().createOMElement(new QName(
                            "result"));
                    rootElement.setText(result.toString());
                    SOAPEnvelope newEnvelope = Utils.createNewSoapEnvelope(synCtx.getEnvelope());
                    newEnvelope.getBody().addChild(rootElement);
                    synCtx.setEnvelope(newEnvelope);
                }
            } else {
                // set result to variable
                if (Utils.isValidReturnObjectType(result)) {
                    synCtx.setVariable(resultTarget, result);
                } else {
                    throw new SynapseException("Return object type is not supported. Supported types are " +
                            "String, Boolean, Integer, Long, Double, JsonElement, OMElement");
                }
            }
            return true;
        } else {
            throw new SynapseException("Return object is null. Cannot set null object to the target : " +
                    resultTarget);
        }
    }
}
