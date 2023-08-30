package crodot;

public class StackInfo {
	String type;
	int posInQueue;
	public StackInfo(String type, int posInQueue) {
		super();
		this.type = type;
		this.posInQueue = posInQueue;
	}
	public StackInfo(String type) {
		super();
		this.type = type;
		this.posInQueue = -1;
	}
	
	
}
