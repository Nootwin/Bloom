package crodot;

public class token {
	String type;
	String value;
	
	token() {
		this.type = null; this.value = null;
	}
	
	token(String a, String b) {
		this.type = a; this.value = b;
	}
}
