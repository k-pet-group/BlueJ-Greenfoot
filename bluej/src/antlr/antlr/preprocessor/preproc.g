header {
package antlr.preprocessor;
}

{
import antlr.collections.impl.IndexedVector;
import java.util.Hashtable;
import antlr.preprocessor.Grammar;
}

class Preprocessor extends Parser;
options {
	k=1;
	interactive=true;
}

grammarFile[Hierarchy hier, String file]
{
	Grammar gr;
	IndexedVector opt=null;
}
	:	( hdr:HEADER_ACTION )?
		( opt=optionSpec[null] )?
		(	gr=class_def[hier]
			{
			// System.out.println(gr);
			if ( hdr!=null ) {
				hier.getFile(file).setHeaderAction(hdr.getText());
			}
			if ( opt!=null ) {
				hier.getFile(file).setOptions(opt);
			}
			if ( gr!=null ) {
				gr.setFileName(file);
				hier.addGrammar(gr);
			}
			}
		)*
		EOF
	;

class_def[Hierarchy hier] returns [Grammar gr]
{
	gr=null;
	IndexedVector rules = new IndexedVector(100);
	IndexedVector classOptions = null;
}
	:	( preamble:ACTION )?
		"class" sub:ID "extends" sup:ID SEMI
		{
			gr = (Grammar)hier.getGrammar(sub.getText());
			if ( gr!=null ) {
				antlr.Tool.toolError("redefinition of grammar "+gr.getName()+" ignored");
				gr=null;
			}
			else {
				gr = new Grammar(sub.getText(), sup.getText(), rules);
				if ( preamble!=null ) {
					gr.setPreambleAction(preamble.getText());
				}
			}
		}
		( classOptions = optionSpec[gr] )?
		{
		if ( gr!=null ) {
			gr.setOptions(classOptions);
		}
		}
		( tokensSpec )?
		( memberA:ACTION {gr.setMemberAction(memberA.getText());} )?
		( rule[gr] )+
	;

tokensSpec
	:	TOKENS_START
			TOKEN_REF ( ASSIGN STRING_LITERAL )?
			(	COMMA TOKEN_REF ( ASSIGN STRING_LITERAL )?
			)*
		RCURLY
	;

dummy: "tokens" ;	// not used, just defines LITERAL_tokens

optionSpec[Grammar gr] returns [IndexedVector options]
{
	options = new IndexedVector();
}
	:	OPTIONS_START
			(	op:ID rhs:ASSIGN_RHS
				{
				Option newOp = new Option(op.getText(),rhs.getText(),gr);
				options.appendElement(newOp.getName(),newOp);
				if ( rhs.getText().equals("tokdef") ) {
					gr.specifiedVocabulary = true;
				}
				}
			)*
		RCURLY
	;

rule[Grammar gr]
{
	IndexedVector o = null;	// options for rule
	String vis = null;
	boolean bang=false;
	String eg=null;
}
	:	(	"protected"	{vis="protected";}
		|	"private"	{vis="private";}
		|	"public"	{vis="public";}
		)?
		r:ID
		( BANG {bang=true;} )?
		( arg:ARG_ACTION )?
		( "returns" ret:ARG_ACTION )?
		( o = optionSpec[null] )?
		( init:ACTION )?
		blk:RULE_BLOCK
		eg=exceptionGroup
		{
		String rtext = blk.getText()+eg;
		Rule ppr = new Rule(r.getText(),rtext,o,gr);
		if ( arg!=null ) {
			ppr.setArgs(arg.getText());
		}
		if ( ret!=null ) {
			ppr.setReturnValue(ret.getText());
		}
		if ( init!=null ) {
			ppr.setInitAction(init.getText());
		}
		if ( bang ) {
			ppr.setBang();
		}
		ppr.setVisibility(vis);
		if ( gr!=null ) {
			gr.addRule(ppr);
		}
		}
	;

exceptionGroup returns [String g]
{String e=null; g="";}
	:	( e=exceptionSpec {g += e;} )*
	;

exceptionSpec returns [String es]
{ String h=null;
  es = System.getProperty("line.separator")+"exception ";
}
	:	"exception"
		( aa:ARG_ACTION {es += aa.getText();} )?
		( h=exceptionHandler {es += h;} )*
	;

exceptionHandler returns [String h]
{h=null;}
	:	"catch" a1:ARG_ACTION a2:ACTION
		{h = System.getProperty("line.separator")+
			 "catch "+a1.getText()+" "+a2.getText();}
	;

class PreprocessorLexer extends Lexer;
options {
	k=2;
	charVocabulary = '\3'..'\176';	// common ASCII
	interactive=true;
}

RULE_BLOCK
options {
	ignore=WS;
}
    :   ':' ALT ( '|' ALT )* ';'
    ;

SUBRULE_BLOCK
	:	'(' (WS)? ALT ( (WS)? '|' (WS)? ALT )* (WS)? ')'
		(	'*'
		|	'+'
		|	'?'
		|	"=>"
		)?
	;

protected
ALT	:	( ELEMENT )*
	;

protected
ELEMENT
	:	COMMENT
	|	ACTION	
	|	STRING_LITERAL
	|	CHAR_LITERAL
	|	SUBRULE_BLOCK
	|	'\r' ('\n')?	{newline();}
	|	'\n'		{newline();}
	|	( ~('\n' | '\r' | '(' | ')' | '/' | '{' | '"' | '\'' | ';') )+
	;

BANG:	'!'
	;

SEMI:	';'
	;

RCURLY
	:	'}'
	;

/** This rule picks off keywords in the lexer that need to be
 *  handled specially.  For example, "header" is the start
 *  of the header action (used to distinguish between options
 *  block and an action).  We do not want "header" to go back
 *  to the parser as a simple keyword...it must pick off
 *  the action afterwards.
 */
ID_OR_KEYWORD
	:	id:ID	{$setType(id.getType());}
		(	{id.getText().equals("header")}? (WS)? ACTION
			{$setType(HEADER_ACTION);}
		|	{id.getText().equals("tokens")}? (WS)? '{'
			{$setType(TOKENS_START);}
		|	{id.getText().equals("options")}? (WS)? '{'
			{$setType(OPTIONS_START);}
		)?
	;

protected
ID
options {
	testLiterals=true;
}
	:	('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'_'|'0'..'9')*
	;

ASSIGN_RHS
	:	'='! (WS!)?
		(	STRING_LITERAL 
		|	CHAR_LITERAL 
		|	( ~('"' | '\'' | ';') )+
		)*
		';'
	;

WS	:	(	' '
		|	'\t'
		|	'\r' ('\n')?	{newline();}
		|	'\n'		{newline();}
		)+
		{$setType(Token.SKIP);}
	;

COMMENT
	:	( SL_COMMENT | ML_COMMENT )
		{$setType(Token.SKIP);}
	;

protected
SL_COMMENT :
	"//"
	(~('\n'|'\r'))* ('\n'|'\r'('\n')?)
	{ newline(); }
	;

protected
ML_COMMENT
	:	"/*"
		(	{ LA(2)!='/' }? '*'
		|	'\r' ('\n')?	{newline();}
		|	'\n'		{newline();}
		|	~('*'|'\n'|'\r')
		)*
		"*/"
	;

CHAR_LITERAL
	:	'\'' (ESC|~'\'') '\''
	;

STRING_LITERAL
	:	'"' (ESC|~'"')* '"'
	;

protected
ESC	:	'\\'
		(	'n'
		|	'r'
		|	't'
		|	'b'
		|	'f'
		|	'"'
		|	'\''
		|	'\\'
		|	('0'..'3') ( DIGIT (DIGIT)? )?
		|	('4'..'7') (DIGIT)?
		|	'u' XDIGIT XDIGIT XDIGIT XDIGIT
		)
	;

protected
DIGIT
	:	'0'..'9'
	;

protected
XDIGIT
	:	'0' .. '9'
	|	'a' .. 'f'
	|	'A' .. 'F'
	;

ARG_ACTION
	:	'['
		(	ARG_ACTION
		|	'\r' ('\n')?	{newline();}
		|	'\n'		{newline();}
		|	CHAR_LITERAL 
		|	STRING_LITERAL 
		|	~']'
		)* 
		']'
	;

ACTION
	:	'{'
		(	'\r' ('\n')?	{newline();}
		|	'\n'		{newline();}
		|	ACTION
		|	CHAR_LITERAL
		|	{LA(2)=='/'||LA(2)=='*'}? COMMENT
		|	STRING_LITERAL
		|	{true}? ~'}'
		|	{true}? '/'
		)*
		'}'
   ;
