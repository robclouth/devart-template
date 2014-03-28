package com.makar;

import processing.core.PVector;

class Ray {
	PVector start, end;

	Ray(PVector start, PVector end) {
		this.start = new PVector();
		this.start.set(start);
		this.end = new PVector();
		this.end.set(end);
	}

	void draw(int c) {
		Makar.pa.stroke(c);
		Makar.pa.line(start.x, start.y, start.z, end.x, end.y, end.z);
	}
}
