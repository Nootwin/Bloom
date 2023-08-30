package crodot;

import java.util.LinkedList;

public class ArgsList<T> extends LinkedList<T> {
	private static final long serialVersionUID = 1L;
	
	
	public String toString() {
		return super.toString().substring(1, super.toString().length()-1).replace(", ", "");
	}
	public String toArgs() {
		StringBuilder b = new StringBuilder("(");
		b.append(this.toString());
		b.append(')');
		return b.toString();
	}
	
	
}
