package crodot;

import java.util.Objects;

public class FieldInfo {
	String name;
	String ownername;
	String type;
	String AccessModifiers;
	int AccessOpcode;
	ASTNode OwnerValue;
	
	FieldInfo(String name, String Ownername) {
		this.name = name;
		this.ownername = Ownername;
		this.type = null;
		this.OwnerValue = null;
	}
	FieldInfo(String name, String type, String Ownername) {
		this.name = name;
		this.type = type;
		this.ownername = Ownername;
		this.OwnerValue = null;
	}
	FieldInfo(String name, String type, ASTNode OwnerValue, String Ownername) {
		this.name = name;
		this.type = type;
		this.ownername = Ownername;
		this.OwnerValue = OwnerValue;
	}
	
	public boolean HasCroValue() {
		if (Objects.isNull(OwnerValue)) {
			return false;
		}
		return true;
	}
}
