package crodot;

public class Token {
	byte type;
	String value;
	int line;
	
	Token() {
		this.type = -2; this.value = null;
	}
	
	Token(byte a, String b, int line) {
		this.type = a; this.value = b; this.line = line;
	}
}
