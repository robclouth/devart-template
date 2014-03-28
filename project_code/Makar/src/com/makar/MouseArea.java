package com.makar;

import processing.core.PApplet;
import processing.core.PVector;

public class MouseArea {
	float x, y, w, h;
	
	public MouseArea(float x, float y, float w, float h) {
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
	}

	public boolean isMouseOver() {
		PVector p1 = new PVector();
		Selector.unproject(Makar.pa.mouseX, Makar.pa.height - Makar.pa.mouseY, 0, p1);
		PVector p2 = new PVector();
		Selector.unproject(Makar.pa.mouseX, Makar.pa.height - Makar.pa.mouseY, 1, p2);

		return intersectRay(p1, p2);
	}

	public void draw() {
		Makar.pa.fill(255, 0, 0);
		Makar.pa.noStroke();
		Makar.pa.beginShape();
		Makar.pa.vertex(x, y);
		Makar.pa.vertex(x + w, y);
		Makar.pa.vertex(x + w, y + h);
		Makar.pa.vertex(x, y + h);
		Makar.pa.endShape(PApplet.CLOSE);
	}

	private boolean intersectRay(PVector rayStart, PVector rayEnd) {
		PVector S1 = Makar.getSceneMatrix().mult(new PVector(x, y), null);
		PVector S2 = Makar.getSceneMatrix().mult(new PVector(x + w, y), null);
		PVector S3 = Makar.getSceneMatrix().mult(new PVector(x, y + h), null);

		PVector dS21 = PVector.sub(S2, S1);
		PVector dS31 = PVector.sub(S3, S1);
		PVector n = dS21.cross(dS31);

		PVector dR = PVector.sub(rayStart, rayEnd);

		float ndotdR = n.dot(dR);

		if (Math.abs(ndotdR) < 1e-6f) { // Choose your tolerance
			return false;
		}

		float t = -n.dot(PVector.sub(rayStart, S1)) / ndotdR;
		PVector M = PVector.add(rayStart, PVector.mult(dR, t));

		PVector dMS1 = PVector.sub(M, S1);
		float u = dMS1.dot(dS21);
		float v = dMS1.dot(dS31);

		return (u >= 0.0f && u <= dS21.dot(dS21) && v >= 0.0f && v <= dS31.dot(dS31));
	}
}
