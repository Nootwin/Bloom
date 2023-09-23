package crodot;

import java.util.LinkedList;

public class ASTNode {
	LinkedList<ASTNode> next = new LinkedList<>();
	ASTNode prev;
	byte type;
	String value;
	boolean lastCheck = true;
	
	ASTNode() {
		this.prev = null;
	}
	ASTNode(ASTNode p) {
		this.prev = p;
	}
	ASTNode(ASTNode p, byte a) {
		this.prev = p;
		this.type = a;
	}
	ASTNode(ASTNode p, byte a, String b) {
		this.prev = p;
		this.type = a;
		this.value = b;
	}
	ASTNode(byte a) {
		this.type = a;
	}
	ASTNode(byte a, String b) {
		this.type = a;
		this.value = b;
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

}

