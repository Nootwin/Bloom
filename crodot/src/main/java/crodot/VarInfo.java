package crodot;

public class VarInfo {
	String name;
	String type;
	String simpleType;
	int pos;
	
	public VarInfo() {
		this.name = null;
		this.type = null;
		this.pos = -1;
	}
	
	public VarInfo(String name, String type, int pos) {
		this.name = name;
		this.type = type;
		this.simpleType = type;
		this.pos = pos;
	}
	
	public VarInfo(String name, String type, String simpleType, int pos) {
		this.name = name;
		this.type = type;
		this.simpleType = simpleType;
		this.pos = pos;
	}
	
	public String toString() {
		return type;
	}
	
}
