package com.example.proauth;

public enum SecurityLevel {
	PRIVATE(4),
	HIGH(3),
	MEDIUM(2),
	LOW(1),
	PUBLIC(0);
	
	public int value;
	
	private SecurityLevel(int value){
		this.value = value;
	}
}