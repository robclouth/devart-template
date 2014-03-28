package io.diffuse.makar;

import java.lang.reflect.Method;

import processing.core.PApplet;

import com.google.gson.Gson;

public class SyncedMethod {
	Gson gson = new Gson();
	PApplet parent;
	Method method;


	public SyncedMethod(PApplet parent, Method method) {
		this.parent = parent;
		this.method = method;
	}

	
}
