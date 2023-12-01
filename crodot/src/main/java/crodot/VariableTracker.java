package crodot;

import java.util.LinkedHashMap;

public class VariableTracker {
	LinkedHashMap<String, VarInfo> vars;
	int length;
	VariableTracker() {
		vars = new LinkedHashMap<>();
		length = 0;
	}
	
	public void reset() {
		vars.clear();
		length = 0;
	}


}
