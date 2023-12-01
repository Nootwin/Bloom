package crodot;

import java.util.LinkedList;

public class ASTNode {
	LinkedList<ASTNode> next = new LinkedList<>();
	ASTNode prev;
	byte type;
	String value;
	int line;
	boolean lastCheck = true;
	
	ASTNode(int line) {
		this.prev = null;
		this.line = line;
	}
	ASTNode(ASTNode p, int line) {
		this.prev = p;
		this.line = line;
	}
	ASTNode(ASTNode p, byte a, int line) {
		this.prev = p;
		this.type = a;
		this.line = line;
	}
	ASTNode(ASTNode p, byte a, String b, int line) {
		this.prev = p;
		this.type = a;
		this.value = b;
		this.line = line;
	}
	ASTNode(byte a, int line) {
		this.type = a;
		this.line = line;
	}
	ASTNode(byte a, String b, int line) {
		this.type = a;
		this.value = b;
		this.line = line;
	}
		
	void SetNode(ASTNode a) {
		if (lastCheck) {
			next.add(a);
		}
		else {
			next.add(next.size()-1, a);
		}
	}
	void SetLast(ASTNode a) {
		next.add(a);
		lastCheck = false;
		
	}
	void PlaceNode(ASTNode a, int index) {
		next.add(index, a);
	}
	void SetNodeInd(ASTNode a, int index) {
		next.set(index, a);
	}
	ASTNode GetNode(int n) {
		return next.get(n);
	}
	ASTNode GetFirstNode() {
		return next.getFirst();
	}
	ASTNode GetLastNode() {
		return next.getLast();
	}
	int GetNodeSize() {
		return next.size();
	}
	ASTNode Grab(byte Type) {
		for (int i = 0; i < next.size(); i++) {
			if (next.get(i).type == Type) {
				return next.get(i);
			}
		}
		return null;
	}
	ASTNode GetLastRealNode() {
		if (lastCheck) {
			return next.getLast();
		}
		else {
			return next.get(next.size()-2);
		}
	}
	
	void debugCheck() {
		System.out.println("                " + "DEBUGCHECK");
		System.out.println("                " + value);
		for (ASTNode a: this.next) {
			
			System.out.println("                " + caller.TokenStateToString(a.type));
			
			
			
		}
		System.out.println("                " + lastCheck);
		System.out.println("                " + "DEBUGCHECKOVER");
	}
	
	public void ForceSet(ASTNode node, int i ) {
		next.set(i, node);
	}
	public void remove(ASTNode tree) {
		next.remove(tree);
		
	}

}

