lexer grammar ExpressionLexer;

JSONPATH_PARAMS:  'in' | 'nin' | 'subsetof' | 'anyof' | 'noneof' | 'size' | 'empty' | '=~';

JSONPATH_FUNCTIONS: 'length()' | 'size()' | 'min()' | 'max()' | 'avg()' | 'sum()' | 'stddev()' | 'keys()' | 'first()' | 'last()';

// Tokens for identifiers, operators, and keywords
VAR: 'vars';
PAYLOAD: 'payload' | '$';
HEADERS: 'headers';
CONFIG: 'configs';
PARAMS: 'params';
PROPERTY: 'props' | 'properties';
AND: 'and' | '&&';
OR: 'or' | '||';
NOT: '!';

DOUBLE_DOT : '..';
ASTERISK : '*';

// Operators
PLUS: '+';
MINUS: '-';
DIV: '/';
MODULO: '%';
EQ: '==';
NEQ: '!=';
GT: '>';
LT: '<';
GTE: '>=';
LTE: '<=';

// Delimiters
LPAREN: '(';
RPAREN: ')';
LBRACKET: '[';
RBRACKET: ']';
DOT: '.';
COMMA: ',';
COLON: ':';
QUOTE: '"' | '\'';

// Literals
BOOLEAN_LITERAL: 'true' | 'false';
NUMBER: '-'? [0-9]+ ('.' [0-9]+)?;

PARAM_ACCESS: 'queryParams' | 'pathParams' | 'functionParams';
PROPERTY_ACCESS: 'axis2' | 'synapse';
FUNCTIONS: 'length' | 'toUpper' | 'toLower' | 'subString' | 'startsWith' | 'endsWith' | 'contains' | 'trim' | 'replace'
    | 'split' | 'indexOf' | 'now' | 'formatDateTime' | 'charAt' | 'abs' | 'ceil' | 'floor' | 'sqrt' | 'log' | 'pow'
    | 'base64encode' | 'base64decode' | 'urlEncode' | 'urlDecode' | 'isString' | 'isNumber' | 'isArray' | 'isObject'
    | 'string' | 'float' | 'boolean' | 'integer' | 'round' | 'exists' | 'xpath' | 'wso2-vault' | 'hashicorp-vault'
    | 'not' | 'registry' | 'object' | 'array' ;

SECONDARY_FUNCTIONS: 'property';


STRING_LITERAL : ('"' (ESC | ~["\\])* '"' | '\'' (ESC | ~['\\])* '\'');


fragment ESC
    :   '\\' [btnfr"'\\/]  // Basic escape sequences
    |   UNICODE_ESC
    |   OCTAL_ESC
    ;

fragment UNICODE_ESC
    :   '\\' 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
    ;

fragment OCTAL_ESC
    :   '\\' [0-3]? [0-7] [0-7]
    ;

fragment HEX_DIGIT
    :   [0-9a-fA-F]
    ;

NULL_LITERAL
    : 'null'           // Define null as a recognized keyword
    ;

// Identifiers
ID: [a-zA-Z_][a-zA-Z_0-9-]*;

// Special symbols for JSONPath filter expressions
QUESTION: '?';
AT: '@';

// Whitespace
WS: [ \t\n\r]+ -> skip;
