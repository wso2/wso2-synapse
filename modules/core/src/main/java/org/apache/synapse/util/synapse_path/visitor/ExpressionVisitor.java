/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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
package org.apache.synapse.util.synapse_path.visitor;

import org.apache.synapse.SynapseConstants;
import org.apache.synapse.util.synapse_path.ExpressionParser;
import org.apache.synapse.util.synapse_path.ExpressionParserBaseVisitor;
import org.apache.synapse.util.synapse_path.ExpressionParserVisitor;
import org.apache.synapse.util.synapse_path.ast.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a visitor that traverses the parse tree and constructs the abstract syntax tree (AST) of the SIEL
 */
public class ExpressionVisitor extends ExpressionParserBaseVisitor<ExpressionNode>
        implements ExpressionParserVisitor<ExpressionNode> {

    @Override
    public ExpressionNode visitExpression(ExpressionParser.ExpressionContext ctx) {
        System.out.println("visitExpression " + ctx.getText());
        if (ctx.comparisonExpression() != null) {
            return visitComparisonExpression(ctx.comparisonExpression());
        } else if (ctx.conditionalExpression() != null) {
            return visitConditionalExpression(ctx.conditionalExpression());
        }
        return null;
    }

    @Override
    public ExpressionNode visitComparisonExpression(ExpressionParser.ComparisonExpressionContext ctx) {
        if (ctx.logicalExpression() != null) {
            if (ctx.logicalExpression().size() == 1) {
                return visitLogicalExpression(ctx.logicalExpression().get(0));
            } else {
                ExpressionNode left = visit(ctx.logicalExpression().get(0));
                for (int i = 1; i < ctx.logicalExpression().size(); i++) {
                    ExpressionNode right = visit(ctx.logicalExpression(i));
                    left = new BinaryOperationNode(left, ctx.getChild(2 * i - 1).getText(), right);
                }
                return left;
            }
        }
        return null;
    }

    @Override
    public ExpressionNode visitLogicalExpression(ExpressionParser.LogicalExpressionContext ctx) {
        if (ctx.arithmeticExpression() != null) {
            ExpressionNode left = visit(ctx.arithmeticExpression());
            if (ctx.logicalExpression() != null && ctx.getChild(1) != null) {
                ExpressionNode right = visit(ctx.logicalExpression());
                left = new BinaryOperationNode(left, ctx.getChild(1).getText(), right);
            }
            return left;
        }
        return null;
    }

    @Override
    public ExpressionNode visitArithmeticExpression(ExpressionParser.ArithmeticExpressionContext ctx) {
        if (ctx.term() != null) {
            if (ctx.term().size() == 1) {
                return visit(ctx.term().get(0));
            } else {
                ExpressionNode left = visit(ctx.term().get(0));
                for (int i = 1; i < ctx.term().size(); i++) {
                    ExpressionNode right = visit(ctx.term(i));
                    left = new BinaryOperationNode(left, ctx.getChild(2 * i - 1).getText(), right);
                }
                return left;
            }
        }
        return null;
    }

    @Override
    public ExpressionNode visitTerm(ExpressionParser.TermContext ctx) {
        if (ctx.factor() != null) {
            if (ctx.factor().size() == 1) {
                return visit(ctx.factor().get(0));
            } else {
                ExpressionNode left = visit(ctx.factor().get(0));
                for (int i = 1; i < ctx.factor().size(); i++) {
                    ExpressionNode right = visit(ctx.factor(i));
                    left = new BinaryOperationNode(left, ctx.getChild(2 * i - 1).getText(), right);
                }
                return left;
            }
        }
        return null;
    }

    @Override
    public ExpressionNode visitFactor(ExpressionParser.FactorContext ctx) {
        if (ctx.literal() != null) {
            return visit(ctx.literal());
        } else if (ctx.functionCall() != null) {
            return visit(ctx.functionCall());
        } else if (ctx.expression() != null) {
            return visit(ctx.expression());
        } else if (ctx.payloadAccess() != null) {
            return visit(ctx.payloadAccess());
        } else if (ctx.variableAccess() != null) {
            return visit(ctx.variableAccess());
        } else if (ctx.headerAccess() != null) {
            return visit(ctx.headerAccess());
        } else if (ctx.configAccess() != null) {
            return visit(ctx.configAccess());
        } else if (ctx.attributeAccess() != null) {
            return visit(ctx.attributeAccess());
        }
        return null;
    }

    @Override
    public ExpressionNode visitFunctionCall(ExpressionParser.FunctionCallContext ctx) {
        ArgumentListNode parameterList = new ArgumentListNode();
        if (ctx.expression() != null) {
            for (ExpressionParser.ExpressionContext expressionContext : ctx.expression()) {
                parameterList.addArgument(visit(expressionContext));
            }
        }
        if (ctx.LENGTH() != null) {
            return new PredefinedFunctionNode(parameterList, SynapseConstants.LENGTH);
        } else if (ctx.TOUPPER() != null) {
            return new PredefinedFunctionNode(parameterList, SynapseConstants.TO_UPPER);
        } else if (ctx.TOLOWER() != null) {
            return new PredefinedFunctionNode(parameterList, SynapseConstants.TO_LOWER);
        } else if (ctx.SUBSTRING() != null) {
            return new PredefinedFunctionNode(parameterList, SynapseConstants.SUBSTRING);
        } else if (ctx.STARTSWITH() != null) {
            return new PredefinedFunctionNode(parameterList, SynapseConstants.STARTS_WITH);
        } else if (ctx.ENDSWITH() != null) {
            return new PredefinedFunctionNode(parameterList, SynapseConstants.ENDS_WITH);
        } else if (ctx.CONTAINS() != null) {
            return new PredefinedFunctionNode(parameterList, SynapseConstants.CONTAINS);
        } else if (ctx.TRIM() != null) {
            return new PredefinedFunctionNode(parameterList, SynapseConstants.TRIM);
        } else if (ctx.REPLACE() != null) {
            return new PredefinedFunctionNode(parameterList, SynapseConstants.REPLACE);
        } else if (ctx.SPLIT() != null) {
            return new PredefinedFunctionNode(parameterList, SynapseConstants.SPLIT);
        } else if (ctx.ABS() != null) {
            return new PredefinedFunctionNode(parameterList, SynapseConstants.ABS);
        } else if (ctx.CEIL() != null) {
            return new PredefinedFunctionNode(parameterList, SynapseConstants.CEIL);
        } else if (ctx.FLOOR() != null) {
            return new PredefinedFunctionNode(parameterList, SynapseConstants.FLOOR);
        } else if (ctx.SQRT() != null) {
            return new PredefinedFunctionNode(parameterList, SynapseConstants.SQRT);
        } else if (ctx.LOG() != null) {
            return new PredefinedFunctionNode(parameterList, SynapseConstants.LOG);
        } else if (ctx.POW() != null) {
            return new PredefinedFunctionNode(parameterList, SynapseConstants.POW);
        } else if (ctx.BASE64ENCODE() != null) {
            return new PredefinedFunctionNode(parameterList, SynapseConstants.B64ENCODE);
        } else if (ctx.BASE64DECODE() != null) {
            return new PredefinedFunctionNode(parameterList, SynapseConstants.B64DECODE);
        } else if (ctx.URLENCODE() != null) {
            return new PredefinedFunctionNode(parameterList, SynapseConstants.URL_ENCODE);
        } else if (ctx.URLDECODE() != null) {
            return new PredefinedFunctionNode(parameterList, SynapseConstants.URL_DECODE);
        } else if (ctx.ISSTRING() != null) {
            return new PredefinedFunctionNode(parameterList, SynapseConstants.IS_STRING);
        } else if (ctx.ISNUMBER() != null) {
            return new PredefinedFunctionNode(parameterList, SynapseConstants.IS_NUMBER);
        } else if (ctx.ISARRAY() != null) {
            return new PredefinedFunctionNode(parameterList, SynapseConstants.IS_ARRAY);
        } else if (ctx.ISOBJECT() != null) {
            return new PredefinedFunctionNode(parameterList, SynapseConstants.IS_OBJECT);
        } else if (ctx.STRING() != null) {
            return new PredefinedFunctionNode(parameterList, SynapseConstants.STRING);
        } else if (ctx.FLOAT() != null) {
            return new PredefinedFunctionNode(parameterList, SynapseConstants.FLOAT);
        } else if (ctx.BOOLEAN() != null) {
            return new PredefinedFunctionNode(parameterList, SynapseConstants.BOOLEAN);
        } else if (ctx.INTEGER() != null) {
            return new PredefinedFunctionNode(parameterList, SynapseConstants.INTEGER);
        } else if (ctx.ROUND() != null) {
            return new PredefinedFunctionNode(parameterList, SynapseConstants.ROUND);
        } else if (ctx.REGISTRY() != null) {
            if (ctx.jsonPathExpression() != null && ctx.jsonPathExpression().getChildCount() > 0) {
                PredefinedFunctionNode node = new PredefinedFunctionNode(parameterList, SynapseConstants.REGISTRY);
                return visitJsonPathAfterPayload(ctx.jsonPathExpression(), node, PayloadAccessNode.Type.REGISTRY);
            }
            return new PredefinedFunctionNode(parameterList, SynapseConstants.REGISTRY);
        } else if (ctx.EXISTS() != null) {
            return new PredefinedFunctionNode(parameterList, SynapseConstants.EXISTS);
        } else if (ctx.OBJECT() != null) {
            if (ctx.jsonPathExpression() != null && ctx.jsonPathExpression().getChildCount() > 0) {
                PredefinedFunctionNode node = new PredefinedFunctionNode(parameterList, SynapseConstants.OBJECT);
                return visitJsonPathAfterPayload(ctx.jsonPathExpression(), node, PayloadAccessNode.Type.OBJECT);
            }
            return new PredefinedFunctionNode(parameterList, SynapseConstants.OBJECT);
        } else if (ctx.ARRAY() != null) {
            if (ctx.arrayIndex() != null) {
                PredefinedFunctionNode node = new PredefinedFunctionNode(parameterList, SynapseConstants.ARRAY);
                Map<String, ExpressionNode> expressionNodeMap = new HashMap<>();
                expressionNodeMap.put(SynapseConstants.ARRAY, visit(ctx.arrayIndex()));
                return new PayloadAccessNode("", expressionNodeMap, PayloadAccessNode.Type.ARRAY, node);
            }
            return new PredefinedFunctionNode(parameterList, SynapseConstants.ARRAY);
        } else if (ctx.XPATH() != null) {
            return new PredefinedFunctionNode(parameterList, SynapseConstants.XPATH);
        } else if (ctx.SECRET() != null) {
            return new PredefinedFunctionNode(parameterList, SynapseConstants.SECRET);
        } else if (ctx.NOT() != null) {
            return new PredefinedFunctionNode(parameterList, SynapseConstants.NOT);
        }
        return null;
    }

    @Override
    public ExpressionNode visitLiteral(ExpressionParser.LiteralContext ctx) {
        if (ctx.NUMBER() != null) {
            return new LiteralNode(ctx.NUMBER().getText(), LiteralNode.Type.NUMBER);
        } else if (ctx.BOOLEAN_LITERAL() != null) {
            return new LiteralNode(ctx.BOOLEAN_LITERAL().getText(), LiteralNode.Type.BOOLEAN);
        } else if (ctx.STRING_LITERAL() != null) {
            return new LiteralNode(ctx.STRING_LITERAL().getText(), LiteralNode.Type.STRING);
        } else if (ctx.arrayLiteral() != null) {
            return visit(ctx.arrayLiteral());
        } else if (ctx.NULL_LITERAL() != null) {
            return new LiteralNode(ctx.NULL_LITERAL().getText(), LiteralNode.Type.NULL);
        }
        return null;
    }

    @Override
    public ExpressionNode visitArrayLiteral(ExpressionParser.ArrayLiteralContext ctx) {
        if (ctx.expression() != null) {
            ArgumentListNode parameterList = new ArgumentListNode();
            for (ExpressionParser.ExpressionContext expressionContext : ctx.expression()) {
                parameterList.addArgument(visit(expressionContext));
            }
            return new LiteralNode(parameterList, LiteralNode.Type.ARRAY);
        }
        return null;
    }

    @Override
    public ExpressionNode visitVariableAccess(ExpressionParser.VariableAccessContext ctx) {
        Map<String, ExpressionNode> expressionNodeMap = new HashMap<>();
        if (ctx.jsonPathExpression() != null) {
            expressionNodeMap = visitJsonPath(ctx.jsonPathExpression());
        }
        return new PayloadAccessNode(ctx.getText(), expressionNodeMap, PayloadAccessNode.Type.VARIABLE, null);
    }

    @Override
    public ExpressionNode visitPayloadAccess(ExpressionParser.PayloadAccessContext ctx) {
        Map<String, ExpressionNode> expressionNodeMap = new HashMap<>();
        if (ctx.jsonPathExpression() != null) {
            expressionNodeMap = visitJsonPath(ctx.jsonPathExpression());
        }
        return new PayloadAccessNode(ctx.getText(), expressionNodeMap, PayloadAccessNode.Type.PAYLOAD, null);
    }

    public Map<String, ExpressionNode> visitJsonPath(ExpressionParser.JsonPathExpressionContext ctx) {
        Map<String, ExpressionNode> expressionNodeMap = new HashMap<>();
        if (ctx.arrayIndex() != null) {
            for (ExpressionParser.ArrayIndexContext expressionContext : ctx.arrayIndex()) {
                expressionNodeMap.put(expressionContext.getText(), visit(expressionContext));
            }
        }
        return expressionNodeMap;
    }

    public ExpressionNode visitJsonPathAfterPayload(ExpressionParser.JsonPathExpressionContext ctx,
                                              PredefinedFunctionNode functionNode, PayloadAccessNode.Type type) {
        Map<String, ExpressionNode> expressionNodeMap = new HashMap<>();
        if (ctx.arrayIndex() != null) {
            for (ExpressionParser.ArrayIndexContext expressionContext : ctx.arrayIndex()) {
                expressionNodeMap.put(expressionContext.getText(), visit(expressionContext));
            }
        }
        return new PayloadAccessNode(ctx.getText(), expressionNodeMap, type, functionNode);
    }

    @Override
    public ExpressionNode visitArrayIndex(ExpressionParser.ArrayIndexContext ctx) {
        if (ctx.NUMBER() != null) {
            return new LiteralNode(ctx.NUMBER().getText(), LiteralNode.Type.NUMBER);
        } else if (ctx.STRING_LITERAL() != null) {
            return new LiteralNode(ctx.STRING_LITERAL().getText(), LiteralNode.Type.STRING);
        } else if (ctx.expression() != null) {
            if (ctx.expression().size() == 1) {
                return visit(ctx.expression(0));
            }
        } else if (ctx.ASTERISK() != null) {
            return null;
        } else if (ctx.multipleArrayIndices() != null) {
            return visit(ctx.multipleArrayIndices());
        } else if (ctx.sliceArrayIndex() != null) {
            return visit(ctx.sliceArrayIndex());
        } else if (ctx.filterExpression() != null) {
            return visit(ctx.filterExpression());
        }
        return visitChildren(ctx);
    }

    @Override
    public ExpressionNode visitMultipleArrayIndices(ExpressionParser.MultipleArrayIndicesContext ctx) {
        if (ctx.expression() != null) {
            ArgumentListNode expressionNodes = new ArgumentListNode();
            for (ExpressionParser.ExpressionContext expressionContext : ctx.expression()) {
                expressionNodes.addArgument(visit(expressionContext));
            }
            return new ArrayIndexNode(expressionNodes, ',');
        }
        return null;
    }

    @Override
    public ExpressionNode visitSliceArrayIndex(ExpressionParser.SliceArrayIndexContext ctx) {
        if (ctx.signedExpressions() != null) {
            ArgumentListNode expressionNodes = new ArgumentListNode();
            if (ctx.getChildCount() == 2 && ctx.getChild(0).getText().equals(":")) {
                expressionNodes.addArgument(null);
            }
            for (ExpressionParser.SignedExpressionsContext expressionContext : ctx.signedExpressions()) {
                expressionNodes.addArgument(visit(expressionContext));
            }
            if (ctx.getChildCount() == 2 && ctx.getChild(1).getText().equals(":")) {
                expressionNodes.addArgument(null);
            }
            return new ArrayIndexNode(expressionNodes, ':');
        }
        return null;
    }

    @Override
    public ExpressionNode visitSignedExpressions(ExpressionParser.SignedExpressionsContext ctx) {
        if (ctx.expression() != null) {
            if (ctx.MINUS() != null) {
                return new SignedExpressionNode(visit(ctx.expression()), true);
            } else {
                return visit(ctx.expression());
            }
        }
        return null;
    }

    @Override
    public ExpressionNode visitFilterExpression(ExpressionParser.FilterExpressionContext ctx) {
        Map<String, ExpressionNode> expressionNodeMap = new HashMap<>();
        if (ctx.filterComponent() != null) {
            for (ExpressionParser.FilterComponentContext filterExpressionContext : ctx.filterComponent()) {
                expressionNodeMap.put(filterExpressionContext.getText(), visit(filterExpressionContext));
            }
        }
        return new FilterExpressionNode(ctx.getText(), expressionNodeMap);
    }

    @Override
    public ExpressionNode visitFilterComponent(ExpressionParser.FilterComponentContext ctx) {
        if (ctx.payloadAccess() != null) {
            return visit(ctx.payloadAccess());
        } else if (ctx.stringOrOperator() != null) {
            return null;
        } else if (ctx.variableAccess() != null) {
            return visit(ctx.variableAccess());
        } else if (ctx.functionCall() != null) {
            return visit(ctx.functionCall());
        } else if (ctx.headerAccess() != null) {
            return visit(ctx.headerAccess());
        } else if (ctx.configAccess() != null) {
            return visit(ctx.configAccess());
        } else if (ctx.attributeAccess() != null) {
            return visit(ctx.attributeAccess());
        }
        return null;
    }

    @Override
    public ExpressionNode visitHeaderAccess(ExpressionParser.HeaderAccessContext ctx) {
        if (ctx.propertyName() != null) {
            return new HeadersAndPropertiesAccessNode(visit(ctx.propertyName()),
                    HeadersAndPropertiesAccessNode.Type.HEADER);
        }
        return null;
    }

    @Override
    public ExpressionNode visitPropertyName(ExpressionParser.PropertyNameContext ctx) {
        if(ctx.ID() != null){
            return new LiteralNode(ctx.ID().getText(), LiteralNode.Type.STRING);
        } else if(ctx.STRING_LITERAL() != null){
            return new LiteralNode(ctx.STRING_LITERAL().getText(), LiteralNode.Type.STRING);
        }
        return null;
    }

    @Override
    public ExpressionNode visitConfigAccess(ExpressionParser.ConfigAccessContext ctx) {
        if (ctx.propertyName() != null) {
            return new HeadersAndPropertiesAccessNode(visit(ctx.propertyName()),
                    HeadersAndPropertiesAccessNode.Type.CONFIG);
        }
        return null;
    }

    @Override
    public ExpressionNode visitAttributeAccess(ExpressionParser.AttributeAccessContext ctx) {
        if (ctx.propertyName() != null) {
            if (ctx.AXIS2() != null) {
                return new HeadersAndPropertiesAccessNode(visit(ctx.propertyName()), SynapseConstants.AXIS2);
            } else if (ctx.SYNAPSE() != null) {
                return new HeadersAndPropertiesAccessNode(visit(ctx.propertyName()),
                        SynapseConstants.SYNAPSE);
            } else if (ctx.QUERY_PARAM() != null) {
                return new HeadersAndPropertiesAccessNode(visit(ctx.propertyName()),
                        SynapseConstants.QUERY_PARAM);
            } else if (ctx.URI_PARAM() != null) {
                return new HeadersAndPropertiesAccessNode(visit(ctx.propertyName()),
                        SynapseConstants.URI_PARAM);
            }
        }
        return null;
    }

    @Override
    public ExpressionNode visitConditionalExpression(ExpressionParser.ConditionalExpressionContext ctx) {
        ExpressionNode condition = null;
        if (ctx.comparisonExpression() != null) {
            condition = visit(ctx.comparisonExpression());
        }
        List<ExpressionParser.ExpressionContext> expList = ctx.expression();
        if (condition != null && expList.size() == 2) {
            return new ConditionalExpressionNode(condition, visit(expList.get(0)), visit(expList.get(1)));
        }
        return null;
    }


    @Override
    public ExpressionNode visitChildren(org.antlr.v4.runtime.tree.RuleNode node) {
        return super.visitChildren(node);
    }

    @Override
    public ExpressionNode visitTerminal(org.antlr.v4.runtime.tree.TerminalNode node) {
        return super.visitTerminal(node);
    }

    @Override
    public ExpressionNode visitErrorNode(org.antlr.v4.runtime.tree.ErrorNode node) {
        return super.visitErrorNode(node);
    }

}
