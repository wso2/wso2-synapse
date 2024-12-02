parser grammar ExpressionParser;

options {
    tokenVocab = ExpressionLexer;
}

expression
    : comparisonExpression
    | conditionalExpression
    | EOF
    ;

conditionalExpression
    : comparisonExpression (QUESTION expression COLON expression)?
    ;

comparisonExpression
    : logicalExpression ( (GT | LT | GTE | LTE | EQ | NEQ) logicalExpression )*
    | logicalExpression (EQ | NEQ) NULL_LITERAL
    ;

logicalExpression
    : arithmeticExpression (AND logicalExpression | OR logicalExpression)?
    ;

arithmeticExpression
    : term ( (PLUS | MINUS) term )*
    ;

term
    : factor ( (ASTERISK | DIV | MODULO) factor )*
    ;

factor
    : literal
    | functionCall
    | variableAccess
    | payloadAccess
    | headerAccess
    | configAccess
    | parameterAccess
    | propertyAccess
    | LPAREN expression RPAREN
    ;

configAccess
    : CONFIG propertyName
    ;

headerAccess
    : HEADERS propertyName
    ;

parameterAccess
    : PARAMS (DOT ID  propertyName)
    ;

propertyAccess
    : PROPERTY (DOT ID  propertyName)
    ;

propertyName
    : DOT ID
    | (DOT)? LBRACKET STRING_LITERAL RBRACKET
    ;

literal
    : arrayLiteral
    | BOOLEAN_LITERAL
    | NUMBER
    | STRING_LITERAL
    | NULL_LITERAL
    ;

jsonPathExpression
    :( DOT JSONPATH_FUNCTIONS
     |DOUBLE_DOT ASTERISK
     | DOUBLE_DOT ID
     | DOT ID
     | (DOT)? LBRACKET arrayIndex RBRACKET
     | DOT ASTERISK
     | DOUBLE_DOT ID (LBRACKET arrayIndex RBRACKET)?)+
     ;


variableAccess
    : VAR ( DOT ID
          | DOT STRING_LITERAL
          | (DOT)? LBRACKET STRING_LITERAL RBRACKET  // Bracket notation: var["variableName"]
          )
      ( jsonPathExpression )?
    ;

arrayLiteral
    : LBRACKET (expression (COMMA expression)*)? RBRACKET // Array with zero or more literals, separated by commas
    ;

payloadAccess
    : PAYLOAD ( jsonPathExpression)?
    ;

arrayIndex
    : NUMBER
    | STRING_LITERAL
    | expression
    | multipleArrayIndices
    | sliceArrayIndex
    | expression ( (PLUS | MINUS | ASTERISK | DIV ) expression)*
    | ASTERISK
    | QUESTION? filterExpression
    ;

multipleArrayIndices
    : expression (COMMA expression)+
    ;

sliceArrayIndex
    : signedExpressions? COLON signedExpressions? (COLON signedExpressions?)?
    ;

signedExpressions
    : MINUS? expression
    ;

filterExpression
    : (filterComponent)+
    ;

filterComponent
    : variableAccess
    | payloadAccess
    | headerAccess
    | configAccess
    | parameterAccess
    | propertyAccess
    | functionCall
    | stringOrOperator
    ;

stringOrOperator
    : QUESTION | AT | JSONPATH_PARAMS | STRING_LITERAL |NUMBER | BOOLEAN_LITERAL | ID | GT | LT | GTE | LTE | EQ | NEQ
    | PLUS | MINUS | DIV | LPAREN | RPAREN | DOT | COMMA | COLON | WS | AND | OR | NOT | ASTERISK
    ;


functionCall
    : ID LPAREN (expression (COMMA expression)*)? RPAREN functionCallSuffix?
    ;

functionCallSuffix
    : DOT ID LPAREN (expression (COMMA expression)*)? RPAREN   // Method chaining
    | jsonPathExpression
    ;
