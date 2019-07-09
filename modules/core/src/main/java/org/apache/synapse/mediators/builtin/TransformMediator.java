/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.mediators.builtin;

import org.apache.axis2.AxisFault;
import org.apache.synapse.MessageContext;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.mediators.jjparser.exceptions.ParserException;
import org.apache.synapse.mediators.jjparser.exceptions.ValidatorException;
import org.apache.synapse.mediators.jjparser.parser.JavaJsonParser;

public class TransformMediator extends AbstractMediator {
    private String schema = "{\n" +
            "  \"$schema\": \"http://json-schema.org/draft-04/schema#\",\n" +
            "  \"type\": \"object\",\n" +
            "  \"properties\": {\n" +
            "    \"fruit\": {\n" +
            "      \"type\": \"string\",\n" +
            "      \"minLength\": 4,\n" +
            "      \"maxLength\": 6,\n" +
            "      \"pattern\": \"^[0-9]{1,45}$\"\n" +
            "    },\n" +
            "    \"price\": {\n" +
            "      \"type\": \"number\",\n" +
            "      \"minimum\": 2,\n" +
            "      \"maximum\": 20,\n" +
            "      \"exclusiveMaximum\": 20,\n" +
            "      \"multipleOf\": 2.5\n" +
            "    },\n" +
            "    \"simpleObject\": {\n" +
            "      \"type\": \"object\",\n" +
            "      \"properties\": {\n" +
            "        \"age\": {\n" +
            "          \"type\": \"integer\"\n" +
            "        }\n" +
            "      },\n" +
            "      \"required\": [\n" +
            "        \"age\"\n" +
            "      ]\n" +
            "    },\n" +
            "    \"simpleArray\": {\n" +
            "      \"type\": \"array\",\n" +
            "      \"items\": [\n" +
            "        {\n" +
            "          \"type\": \"boolean\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"type\": \"boolean\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"type\": \"string\"\n" +
            "        }\n" +
            "      ],\n" +
            "      \"minItems\": 1,\n" +
            "      \"maxItems\": 3,\n" +
            "      \"uniqueItems\": false\n" +
            "    },\n" +
            "    \"objWithArray\": {\n" +
            "      \"type\": \"object\",\n" +
            "      \"properties\": {\n" +
            "        \"marks\": {\n" +
            "          \"type\": \"array\",\n" +
            "          \"items\": [\n" +
            "            {\n" +
            "              \"type\": \"integer\"\n" +
            "            },\n" +
            "            {\n" +
            "              \"type\": \"integer\"\n" +
            "            },\n" +
            "            {\n" +
            "              \"type\": \"integer\"\n" +
            "            },\n" +
            "            {\n" +
            "              \"type\": \"integer\"\n" +
            "            }\n" +
            "          ]\n" +
            "        }\n" +
            "      },\n" +
            "      \"required\": [\n" +
            "        \"marks\"\n" +
            "      ]\n" +
            "    },\n" +
            "    \"arrayOfObjects\": {\n" +
            "      \"type\": \"array\",\n" +
            "      \"items\": [\n" +
            "        {\n" +
            "          \"type\": \"object\",\n" +
            "          \"properties\": {\n" +
            "            \"maths\": {\n" +
            "              \"type\": \"integer\"\n" +
            "            }\n" +
            "          },\n" +
            "          \"required\": [\n" +
            "            \"maths\"\n" +
            "          ]\n" +
            "        },\n" +
            "        {\n" +
            "          \"type\": \"object\",\n" +
            "          \"properties\": {\n" +
            "            \"physics\": {\n" +
            "              \"type\": \"integer\"\n" +
            "            }\n" +
            "          },\n" +
            "          \"required\": [\n" +
            "            \"physics\"\n" +
            "          ]\n" +
            "        },\n" +
            "        {\n" +
            "          \"type\": \"object\",\n" +
            "          \"properties\": {\n" +
            "            \"chemistry\": {\n" +
            "              \"type\": \"integer\"\n" +
            "            }\n" +
            "          },\n" +
            "          \"required\": [\n" +
            "            \"chemistry\"\n" +
            "          ]\n" +
            "        }\n" +
            "      ]\n" +
            "    },\n" +
            "    \"singleObjArray\": {\n" +
            "      \"type\": \"array\",\n" +
            "      \"items\": [\n" +
            "        {\n" +
            "          \"type\": \"number\"\n" +
            "        }\n" +
            "      ]\n" +
            "    },\n" +
            "    \"nestedObject\": {\n" +
            "      \"type\": \"object\",\n" +
            "      \"properties\": {\n" +
            "        \"Lahiru\": {\n" +
            "          \"type\": \"object\",\n" +
            "          \"properties\": {\n" +
            "            \"age\": {\n" +
            "              \"type\": \"integer\"\n" +
            "            }\n" +
            "          },\n" +
            "          \"required\": [\n" +
            "            \"age\"\n" +
            "          ]\n" +
            "        },\n" +
            "        \"Nimal\": {\n" +
            "          \"type\": \"object\",\n" +
            "          \"properties\": {\n" +
            "            \"married\": {\n" +
            "              \"type\": \"boolean\"\n" +
            "            }\n" +
            "          },\n" +
            "          \"required\": [\n" +
            "            \"married\"\n" +
            "          ]\n" +
            "        },\n" +
            "        \"Kamal\": {\n" +
            "          \"type\": \"object\",\n" +
            "          \"properties\": {\n" +
            "            \"scores\": {\n" +
            "              \"type\": \"array\",\n" +
            "              \"items\": [\n" +
            "                {\n" +
            "                  \"type\": \"integer\"\n" +
            "                },\n" +
            "                {\n" +
            "                  \"type\": \"integer\"\n" +
            "                },\n" +
            "                {\n" +
            "                  \"type\": \"integer\"\n" +
            "                }\n" +
            "              ]\n" +
            "            }\n" +
            "          },\n" +
            "          \"required\": [\n" +
            "            \"scores\"\n" +
            "          ]\n" +
            "        }\n" +
            "      },\n" +
            "      \"required\": [\n" +
            "        \"Lahiru\",\n" +
            "        \"Nimal\",\n" +
            "        \"Kamal\"\n" +
            "      ]\n" +
            "    },\n" +
            "    \"nestedArray\": {\n" +
            "      \"type\": \"array\",\n" +
            "      \"items\": [\n" +
            "        {\n" +
            "          \"type\": \"array\",\n" +
            "          \"items\": [\n" +
            "            {\n" +
            "              \"type\": \"integer\"\n" +
            "            },\n" +
            "            {\n" +
            "              \"type\": \"integer\"\n" +
            "            },\n" +
            "            {\n" +
            "              \"type\": \"integer\"\n" +
            "            }\n" +
            "          ]\n" +
            "        },\n" +
            "        {\n" +
            "          \"type\": \"array\",\n" +
            "          \"items\": [\n" +
            "            {\n" +
            "              \"type\": \"boolean\"\n" +
            "            },\n" +
            "            {\n" +
            "              \"type\": \"boolean\"\n" +
            "            }\n" +
            "          ]\n" +
            "        },\n" +
            "        {\n" +
            "          \"type\": \"array\",\n" +
            "          \"items\": [\n" +
            "            {\n" +
            "              \"type\": \"string\"\n" +
            "            },\n" +
            "            {\n" +
            "              \"type\": \"string\"\n" +
            "            }\n" +
            "          ]\n" +
            "        }\n" +
            "      ]\n" +
            "    },\n" +
            "    \"allNumericArray\": {\n" +
            "      \"type\": \"array\",\n" +
            "      \"items\": {\n" +
            "        \"type\": \"integer\"\n" +
            "      },\n" +
            "      \"minItems\": 1,\n" +
            "      \"maxItems\": 3,\n" +
            "      \"uniqueItems\": true\n" +
            "    }\n" +
            "},\n" +
            "  \"required\": [\n" +
            "    \"fruit\",\n" +
            "    \"price\",\n" +
            "    \"simpleObject\",\n" +
            "    \"simpleArray\",\n" +
            "    \"objWithArray\",\n" +
            "    \"arrayOfObjects\",\n" +
            "    \"singleObjArray\",\n" +
            "    \"nestedObject\",\n" +
            "    \"nestedArray\"\n" +
            "  ],\n" +
            "  \"additionalProperties\": true,\n" +
            "  \"minProperties\": 10,\n" +
            "  \"maxProperties\": 20,\n" +
            "  \"patternProperties\": {\n" +
            "    \"_goals$\": {\n" +
            "      \"type\": \"integer\"\n" +
            "    }\n" +
            "  }\n" +
            "}";

    private String validatingInput = "{\n" +
            "  \"fruit\"           : \"12345\",\n" +
            "  \"price\"           : \"7.5\",\n" +
            "  \"simpleObject\"    : {\"age\":\"234\"},\n" +
            "  \"simpleArray\"     : [\"true\",\"false\",\"true\"],\n" +
            "  \"objWithArray\"    : {\"marks\":[\"34\",\"45\",\"56\",\"67\"]},\n" +
            "  \"arrayOfObjects\"  : [{\"maths\":\"90\"},{\"physics\":\"95\"},{\"chemistry\":\"65\"}],\n" +
            "  \"singleObjArray\"  : 1.618,\n" +
            "  \"nestedObject\"    : {\"Lahiru\" :{\"age\":\"27\"},\"Nimal\" :{\"married\" :\"true\"}, \"Kamal\" : {\"scores\": [\"24\",45,\"67\"]}},\n" +
            "  \"nestedArray\"     : [[12,\"23\",34],[\"true\",false],[\"Linking Park\",\"Coldplay\"]],\n" +
            "  \"allNumericArray\" : [\"3\",\"1\",\"4\"],\n" +
            "  \"Hello\"           : 890,\n" +
            "  \"league_goals\"    : \"10\"\n" +
            "}";

    private Value schemaKey = null;

    @Override
    public boolean mediate(MessageContext synCtx) {
        String jsonPayload = JsonUtil.jsonPayloadToString(((Axis2MessageContext)synCtx).getAxis2MessageContext());
        String result;
        try {
            result = JavaJsonParser.parseJson(jsonPayload, schema);
            if (result != null){
                JsonUtil.getNewJsonPayload(((Axis2MessageContext)synCtx).getAxis2MessageContext(), result, true, true);
            }
        } catch (ValidatorException e) {
            handleException("ValidatorException : ", synCtx);
        } catch (ParserException e) {
            handleException("ParserException : " , synCtx);
        } catch (AxisFault af) {
            handleException("Axisfault : ", synCtx);
        }
        System.out.println("This is a test inside transform mediator");
        return true;
    }

    public Value getSchemaKey() {
        return schemaKey;
    }

    public void setSchemaKey(Value schemaKey) {
        this.schemaKey = schemaKey;
    }
}
