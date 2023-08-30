package crodot;

import java.util.Objects;

public class FieldInfo {
	String name;
	String type;
	String AccessModifiers;
	ASTNode OwnerValue;
	
	FieldInfo(String name) {
		this.name = name;
		this.type = null;
		this.OwnerValue = null;
	}
	FieldInfo(String name, String type) {
		this.name = name;
		this.type = type;
		this.OwnerValue = null;
	}
	FieldInfo(String name, String type, ASTNode OwnerValue) {
		this.name = name;
		this.type = type;
		this.OwnerValue = OwnerValue;
	}
	
	public boolean HasCroValue() {
		if (Objects.isNull(OwnerValue)) {
			return false;
		}
		return true;
	}
}
