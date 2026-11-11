package jodd.csselly;

import java.util.ArrayList;
import java.nio.CharBuffer;
%%

// lexer config
%class CSSellyLexer
%final
%unicode
%byaccj
%char
%ignorecase

// for debugging, adds overhead
//%line
//%column

// code generation(%switch, %table, %pack)
%buffer 4096

// additional methods
%{
	// position methods
	public long position() { return yychar; }
	public int length()   { return yylength(); }
	public int line()     { return -1; /*yyline;*/ }   	// for debugging
	public int column()   { return -1; /*yycolumn;*/ } 	// for debugging

	// state methods
	public void stateReset() 		{ yybegin(YYINITIAL); }
	public void stateSelector() 	{ yybegin(SELECTOR); }
	public void stateAttr()			{ yybegin(ATTR); }
	public void stateCombinator()	{ yybegin(COMBINATOR); }
	public void statePseudoFn()		{ yybegin(PSEUDO_FN); }

	// fast methods
	public final CharSequence xxtext() {
		return CharBuffer.wrap(zzBuffer, zzStartRead, zzMarkedPos - zzStartRead);
	}

	public final String yytext(int startIndex) {
		startIndex += zzStartRead;
		return new String(zzBuffer, startIndex, zzMarkedPos - startIndex);
	}
	public final String yytext(int startIndex, int endIndexOffset) {
		startIndex += zzStartRead;
		return new String(zzBuffer, startIndex, zzMarkedPos - endIndexOffset - startIndex);
	}

	ArrayList<CssSelector> selectors = new ArrayList<CssSelector>();
	CssSelector cssSelector;
	String pseudoFnName;
	// pseudo function expression, may contain nested parentheses and strings
	final StringBuilder pseudoFnExpression = new StringBuilder();
	int pseudoFnDepth;

	private void startPseudoFn(String name) {
		pseudoFnName = name;
		pseudoFnExpression.setLength(0);
		pseudoFnDepth = 0;
		statePseudoFn();
	}

	private void startRelativeSelector() {
		if (!selectors.isEmpty()) {
			throw new CSSellyException("Invalid combinator <"+ yytext() +">.", yystate(), line(), column());
		}
		cssSelector = CssSelector.scope();
		selectors.add(cssSelector);
	}
%}

// macros

ident     =[-]?{nmstart}{nmchar}*
name      ={nmchar}+
nmstart   =[_a-zA-Z]|{nonascii}|{escape}
nonascii  =[^\0-\177]
unicode   =\\[0-9a-f]{1,6}(\r\n|[ \n\r\t\f])?
escape    ={unicode}|\\[^\n\r\f0-9a-f]
nmchar    =[_a-zA-Z0-9-]|{nonascii}|{escape}
num       =[0-9]+|[0-9]*\.[0-9]+
string    ={string1}|{string2}
string1   =\"([^\n\r\f\"]|\\{nl}|{nonascii}|{escape})*\"
string2   =\'([^\n\r\f\']|\\{nl}|{nonascii}|{escape})*\'
nl       =\n|\r\n|\r|\f
whitespace=[ \t\r\n\f]
w         ={whitespace}*
ww        ={whitespace}+
integer   =[0-9]+
dimension ={num}{ident}


// additional lexer states
%state SELECTOR, ATTR, COMBINATOR, PSEUDO_FN

%%

<YYINITIAL> {
	{ww}			{ /* ignore whitespaces */ }
	[>+~]			{ startRelativeSelector(); yypushback(1); stateSelector(); }
	{ident}		{ cssSelector = new CssSelector(yytext()); selectors.add(cssSelector); stateSelector(); }
	"*"				{ cssSelector = new CssSelector(); selectors.add(cssSelector); stateSelector(); }
	.				{ cssSelector = new CssSelector(); selectors.add(cssSelector); yypushback(1); stateSelector(); }
}

<SELECTOR> {
	"["				{ stateAttr(); }
	"#"{name}		{ cssSelector.addIdSelector(yytext(1)); }
	"."{ident}		{ cssSelector.addClassSelector(yytext(1)); }
	":"(":")?{ident}"("		{ startPseudoFn(yytext(yycharat(1) == ':' ? 2 : 1,1)); }
	":"(":")?{ident}		{ cssSelector.addPseudoClassSelector(yytext( yycharat(1) == ':' ? 2 : 1 )); }
	// any character, including newlines, which are combinators like other whitespaces
	[^]				{ yypushback(1); stateCombinator(); }
}

<ATTR> {
    {w}{ident}{w}
	(
		("=" | "~=" | "|=" | "^=" | "$=" | "*=" | "!=" )
		{w}
		({ident} {ww} [is] | {string} {w} [is] | {ident} | {string})
		{w}
	)?				{ cssSelector.addAttributeSelector(yytext()); }
	"]"				{ stateSelector(); }
}

<COMBINATOR> {
	{w}">"{w}		{ cssSelector.setCombinator(Combinator.CHILD); stateReset(); }
	{w}"+"{w}		{ cssSelector.setCombinator(Combinator.ADJACENT_SIBLING); stateReset(); }
	{w}"~"{w}		{ cssSelector.setCombinator(Combinator.GENERAL_SIBLING); stateReset(); }
	{w}				{ cssSelector.setCombinator(Combinator.DESCENDANT); stateReset(); }
	.				{ throw new CSSellyException("Invalid combinator <"+ yytext() +">.", yystate(), line(), column()); }
}

<PSEUDO_FN> {
	{string}		{ pseudoFnExpression.append(xxtext()); }
	"("				{ pseudoFnDepth++; pseudoFnExpression.append('('); }
	")"				{
						if (pseudoFnDepth == 0) {
							cssSelector.addPseudoFunctionSelector(pseudoFnName, pseudoFnExpression.toString());
							stateSelector();
						} else {
							pseudoFnDepth--;
							pseudoFnExpression.append(')');
						}
					}
	[^\(\)\"\'\\]+	{ pseudoFnExpression.append(xxtext()); }
	{escape}		{ pseudoFnExpression.append(xxtext()); }
	// unclosed quote, eg: :contains(don't), or backslash that is not an escape
	[\"\'\\]		{ pseudoFnExpression.append(xxtext()); }
	<<EOF>>			{ throw new CSSellyException("Unclosed pseudo function <" + pseudoFnName + ">.", yystate(), line(), column()); }
}


// fallback rule, when nothing else matches
[^]|\n				{ throw new CSSellyException("Illegal character <"+ yytext() +">.", yystate(), line(), column()); }
