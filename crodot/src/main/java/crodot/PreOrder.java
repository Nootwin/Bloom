package crodot;

public enum PreOrder {
	DOT(3),
	IDENTIFIER(4, "VAR"),
	RIGHTBRACKET(5, "BRACKET"),
	FUN(4),
	ARR(4),
	LEFTBRACE(6),
	RIGHTBRACE(6),
	LEFTGENERIC(6),
	LEFTBRACKET(6),
	RIGHTGENERIC(6),
	ENDOFLINE(6);
	
	final int priority;
	final String newValue;
	
	PreOrder(int p, String n) {
		this.priority = p;
		this.newValue = n;
	}
	PreOrder(int p) {
		this.priority = p;
		this.newValue = this.toString();
	}
}
