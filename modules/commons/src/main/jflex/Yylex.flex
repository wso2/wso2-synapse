/*
 * Copyright 2011, 2012 Odysseus Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. ackage org.apache.synapse.commons.staxon.core.json.stream.impl;
 */
package org.apache.synapse.commons.staxon.core.json.stream.impl;

@SuppressWarnings("unused")
%%
%final
%implements JsonStreamSourceImpl.Scanner
%apiprivate

%char
%line
%column

%unicode

%type Symbol
%{
	private StringBuilder builder = new StringBuilder();
	private String text = null;

	@Override
	public String getText() { return text; }
	@Override
	public Symbol nextSymbol() throws java.io.IOException { return yylex(); }
	@Override
	public void close() throws java.io.IOException { yyclose(); }
	@Override
	public int getCharOffset() { return yychar; }
	@Override
	public int getLineNumber() { return yyline; }
	@Override
	public int getColumnNumber() { return yycolumn; }
%}

%state STRING

WHITESPACE = [\n\r\ \t\b\012]
NUMBER = -? (0 | [1-9] [0-9]*) (\.[0-9]+)? ([eE] [+-]? [0-9]+)?
UNICODE = \\u[0-9A-Fa-f]{4}

%%

<YYINITIAL> {
	","				{ text = null; return Symbol.COMMA; }
	":"				{ text = null; return Symbol.COLON; }
	"["				{ text = null; return Symbol.START_ARRAY; }
	"]"				{ text = null; return Symbol.END_ARRAY; }
	"{"				{ text = null; return Symbol.START_OBJECT; }
	"}"				{ text = null; return Symbol.END_OBJECT; }
	"null"			{ text = "null"; return Symbol.NULL; }
	"true"			{ text = "true"; return Symbol.TRUE; }
	"false"			{ text = "false"; return Symbol.FALSE; }
	{NUMBER}		{ text = yytext(); return Symbol.NUMBER; } 
	{WHITESPACE}	{ /* ignore whitespace */ }
	\"				{ builder.setLength(0); yybegin(STRING); }
}

<STRING> {
	\"				{ text = builder.toString(); yybegin(YYINITIAL); return Symbol.STRING; }
	[^\n\r\"\\]+	{ builder.append(yytext()); }
	\\\"			{ builder.append('\"'); }
	\\\\			{ builder.append('\\'); }
	\\\/			{ builder.append('/'); }
	\\b				{ builder.append('\b'); }
	\\f				{ builder.append('\f'); }
	\\n				{ builder.append('\n'); }
	\\r				{ builder.append('\r'); }
	\\t				{ builder.append('\t'); }
	{UNICODE}		{ builder.append(Character.toChars(Integer.parseInt(yytext().substring(2), 16))); }
}

<<EOF>>				{ text = null; return Symbol.EOF; }

.					{ throw new java.io.IOException("Illegal character: <" + yytext() + ">"); }
