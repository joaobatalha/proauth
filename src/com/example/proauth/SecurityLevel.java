package com.example.proauth;

public enum SecurityLevel {
	PRIVATE("Private"),
	HIGH("High"),
	MEDIUM("Medium"),
	LOW("Low"),
	PUBLIC("Public");
	
	private String text;
	
	private SecurityLevel(String text){
		this.text = text;
	}
	
	@Override
	public String toString(){
		return this.text;
	}
}