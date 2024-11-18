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
package org.apache.synapse.util.synapse.expression.visitor;

import org.apache.synapse.SynapseConstants;
import org.apache.synapse.util.synapse.expression.ast.*;
import org.apache.synapse.util.synapse_expression.ExpressionParser;
import org.apache.synapse.util.synapse_expression.ExpressionParserBaseVisitor;
import org.apache.synapse.util.synapse_expression.ExpressionParserVisitor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a visitor that traverses the parse tree and constructs the abstract syntax tree (AST)
 * of the Synapse Expression.
 */
public class ExpressionVisitor extends ExpressionParserBaseVisitor<ExpressionNode>
        implements ExpressionParserVisitor<ExpressionNode> {

    @Override
    public ExpressionNode visitExpression(ExpressionParser.ExpressionContext ctx) {
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
        if (ctx.ID() != null) {
            String functionName = ctx.ID().getText();
            switch (functionName) {
                case SynapseConstants.LENGTH:
                    return new PredefinedFunctionNode(parameterList, SynapseConstants.LENGTH);
                case SynapseConstants.TO_UPPER:
                    return new PredefinedFunctionNode(parameterList, SynapseConstants.TO_UPPER);
                case SynapseConstants.TO_LOWER:
                    return new PredefinedFunctionNode(parameterList, SynapseConstants.TO_LOWER);
                case SynapseConstants.SUBSTRING:
                    return new PredefinedFunctionNode(parameterList, SynapseConstants.SUBSTRING);
                case SynapseConstants.STARTS_WITH:
                    return new PredefinedFunctionNode(parameterList, SynapseConstants.STARTS_WITH);
                case SynapseConstants.ENDS_WITH:
                    return new PredefinedFunctionNode(parameterList, SynapseConstants.ENDS_WITH);
                case SynapseConstants.CONTAINS:
                    return new PredefinedFunctionNode(parameterList, SynapseConstants.CONTAINS);
                case SynapseConstants.TRIM:
                    return new PredefinedFunctionNode(parameterList, SynapseConstants.TRIM);
                case SynapseConstants.REPLACE:
                    return new PredefinedFunctionNode(parameterList, SynapseConstants.REPLACE);
                case SynapseConstants.SPLIT:
                    return new PredefinedFunctionNode(parameterList, SynapseConstants.SPLIT);
                case SynapseConstants.INDEX_OF:
                    return new PredefinedFunctionNode(parameterList, SynapseConstants.INDEX_OF);
                case SynapseConstants.NOW:
                    return new PredefinedFunctionNode(parameterList, SynapseConstants.NOW);
                case SynapseConstants.FORMAT_DATE_TIME:
                    return new PredefinedFunctionNode(parameterList, SynapseConstants.FORMAT_DATE_TIME);
                case SynapseConstants.CHAR_AT:
                    return new PredefinedFunctionNode(parameterList, SynapseConstants.CHAR_AT);
                case SynapseConstants.ABS:
                    return new PredefinedFunctionNode(parameterList, SynapseConstants.ABS);
                case SynapseConstants.CEIL:
                    return new PredefinedFunctionNode(parameterList, SynapseConstants.CEIL);
                case SynapseConstants.FLOOR:
                    return new PredefinedFunctionNode(parameterList, SynapseConstants.FLOOR);
                case SynapseConstants.SQRT:
                    return new PredefinedFunctionNode(parameterList, SynapseConstants.SQRT);
                case SynapseConstants.LOG:
                    return new PredefinedFunctionNode(parameterList, SynapseConstants.LOG);
                case SynapseConstants.POW:
                    return new PredefinedFunctionNode(parameterList, SynapseConstants.POW);
                case SynapseConstants.B64ENCODE:
                    return new PredefinedFunctionNode(parameterList, SynapseConstants.B64ENCODE);
                case SynapseConstants.B64DECODE:
                    return new PredefinedFunctionNode(parameterList, SynapseConstants.B64DECODE);
                case SynapseConstants.URL_ENCODE:
                    return new PredefinedFunctionNode(parameterList, SynapseConstants.URL_ENCODE);
                case SynapseConstants.URL_DECODE:
                    return new PredefinedFunctionNode(parameterList, SynapseConstants.URL_DECODE);
                case SynapseConstants.IS_STRING:
                    return new PredefinedFunctionNode(parameterList, SynapseConstants.IS_STRING);
                case SynapseConstants.IS_NUMBER:
                    return new PredefinedFunctionNode(parameterList, SynapseConstants.IS_NUMBER);
                case SynapseConstants.IS_ARRAY:
                    return new PredefinedFunctionNode(parameterList, SynapseConstants.IS_ARRAY);
                case SynapseConstants.IS_OBJECT:
                    return new PredefinedFunctionNode(parameterList, SynapseConstants.IS_OBJECT);
                case SynapseConstants.STRING:
                    return new PredefinedFunctionNode(parameterList, SynapseConstants.STRING);
                case SynapseConstants.FLOAT:
                    return new PredefinedFunctionNode(parameterList, SynapseConstants.FLOAT);
                case SynapseConstants.BOOLEAN:
                    return new PredefinedFunctionNode(parameterList, SynapseConstants.BOOLEAN);
                case SynapseConstants.INTEGER:
                    return new PredefinedFunctionNode(parameterList, SynapseConstants.INTEGER);
                case SynapseConstants.ROUND:
                    return new PredefinedFunctionNode(parameterList, SynapseConstants.ROUND);
                case SynapseConstants.EXISTS:
                    return new PredefinedFunctionNode(parameterList, SynapseConstants.EXISTS);
                case SynapseConstants.XPATH:
                    return new PredefinedFunctionNode(parameterList, SynapseConstants.XPATH);
                case SynapseConstants.SECRET:
                    return new PredefinedFunctionNode(parameterList, SynapseConstants.SECRET);
                case SynapseConstants.NOT:
                    return new PredefinedFunctionNode(parameterList, SynapseConstants.NOT);
                case SynapseConstants.REGISTRY:
                    if (ctx.functionCallSuffix() != null) {
                        if (ctx.functionCallSuffix().jsonPathExpression() != null) {
                            PredefinedFunctionNode node = new PredefinedFunctionNode(parameterList,
                                    SynapseConstants.REGISTRY);
                            return visitJsonPathAfterPayload(ctx.functionCallSuffix().jsonPathExpression(),
                                    node, PayloadAccessNode.Type.REGISTRY);
                        }
                    } else {
                        return new PredefinedFunctionNode(parameterList, SynapseConstants.REGISTRY);
                    }
                case SynapseConstants.OBJECT:
                    if (ctx.functionCallSuffix() != null) {
                        if (ctx.functionCallSuffix().jsonPathExpression() != null) {
                            PredefinedFunctionNode node = new PredefinedFunctionNode(parameterList,
                                    SynapseConstants.OBJECT);
                            return visitJsonPathAfterPayload(ctx.functionCallSuffix().jsonPathExpression(),
                                    node, PayloadAccessNode.Type.OBJECT);
                        }
                    } else {
                        return new PredefinedFunctionNode(parameterList, SynapseConstants.OBJECT);
                    }
                case SynapseConstants.ARRAY:
                    if (ctx.functionCallSuffix() != null) {
                        if (ctx.functionCallSuffix().jsonPathExpression() != null) {
                            PredefinedFunctionNode node = new PredefinedFunctionNode(parameterList,
                                    SynapseConstants.ARRAY);
                            return visitJsonPathAfterPayload(ctx.functionCallSuffix().jsonPathExpression(),
                                    node, PayloadAccessNode.Type.ARRAY);
                        }
                    } else {
                        return new PredefinedFunctionNode(parameterList, SynapseConstants.ARRAY);
                    }
            }
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
        if (ctx.ID() != null) {
            return new LiteralNode(ctx.ID().getText(), LiteralNode.Type.STRING);
        } else if (ctx.STRING_LITERAL() != null) {
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
            if (ctx.ID() != null) {
                String scope = ctx.ID().getText();
                switch (scope) {
                    case SynapseConstants.AXIS2:
                        return new HeadersAndPropertiesAccessNode(visit(ctx.propertyName()), SynapseConstants.AXIS2);
                    case SynapseConstants.SYNAPSE:
                        return new HeadersAndPropertiesAccessNode(visit(ctx.propertyName()), SynapseConstants.SYNAPSE);
                    case SynapseConstants.QUERY_PARAM:
                        return new HeadersAndPropertiesAccessNode(visit(ctx.propertyName()),
                                SynapseConstants.QUERY_PARAM);
                    case SynapseConstants.URI_PARAM:
                        return new HeadersAndPropertiesAccessNode(visit(ctx.propertyName()), SynapseConstants.URI_PARAM);
                }
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
