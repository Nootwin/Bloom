package crodot;

public enum PostOrder {
	SEPERATOR(0, "SEP"),
	TRUEEQUALS(1),
	NOTEQUALS(1),
	TRUEGREATERTHAN(1),
	TRUELESSTHAN(1),
	LESSTHAN(1),
	GREATERTHAN(1),
	ADD(2),
	SUB(2),
	MUL(2),
	DIV(2),
	REM(2),
	EXP(2),
	DOT(3),
	IDENTIFIER(4, "VAR"),
	DECLARATION(4, "VAR"),
	NUMBER(4, "NUM"),
	STRING(4, "Ljava/lang/String;"),
	CHAR(4, "C"),
	BOOLEAN(4, "Z"),
	RIGHTBRACE(5, "BRACE"),
	RIGHTBRACKET(5, "BRACKET"),
	FUN(4),
	ARR(4),
	GENFUN(4),
	LEFTBRACE(6),
	LEFTBRACKET(6),
	RIGHTGENERIC(6);
	
	
	
	
	
	final int priority;
	final String newValue;
	
	PostOrder(int p, String n) {
		this.priority = p;
		this.newValue = n;
	}
	PostOrder(int p) {
		this.priority = p;
		this.newValue = this.toString();
	}
}
