package crodot;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Stack;

public class VariableManager {
	private VariableTracker main;
	private VariableTracker sub;
	private VariableTracker cur;
	private Stack<Integer> blastPoint;
	
	VariableManager() {
		main = new VariableTracker();
		sub = new VariableTracker();
		blastPoint = new Stack<>();
	}
	
	void setSubLength(int n) {
		sub.length = n;
	}
	void setMainLength(int n) {
		sub.length = n;
	}
	void setLength(int n) {
		sub.length = n;
	}
	
	int getLength() {
		return cur.length;
	}
	
	boolean contains(String n) {
		return cur.vars.containsKey(n);
	}
	
	void createBlackPoint() {
		blastPoint.add(cur.length);
	}
	
	void blast() {
		int point = blastPoint.pop();
		LinkedHashMap<String, VarInfo> c = cur.vars;
		Iterator<Entry<String, VarInfo>> i = c.entrySet().iterator();
		
		while (point < c.size()) {
			i.next();
			i.remove();
			
		}
	}
	
	void setMain() {
		cur = main;
	}
	
	void setSub() {
		cur = sub;
	}
	
	void resetSub() {
		sub.reset();
	}
	
	VarInfo add(String n, VarInfo v) {
		cur.length++;
		return cur.vars.put(n, v);
	}
	
	VarInfo addMain(String n, VarInfo v) {
		main.length++;
		return main.vars.put(n, v);
	}
	
	VarInfo addSub(String n, VarInfo v) {
		sub.length++;
		return sub.vars.put(n, v);
	}
	
	VarInfo get(String n) {
		return cur.vars.get(n);
	}
	
	
	
	void Inc() {
		cur.length++;
	}
	
	boolean isMain() {
		return cur == main;
	}
	
	boolean isSub() {
		return cur == sub;
	}
}
