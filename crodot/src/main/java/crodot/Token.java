package crodot;

public class Token {
	byte type;
	String value;
	
	Token() {
		this.type = -2; this.value = null;
	}
	
	Token(byte a, String b) {
		this.type = a; this.value = b;
	}
}
