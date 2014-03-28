/**
 * ##library.name##
 * ##library.sentence##
 * ##library.url##
 *
 * Copyright ##copyright## ##author##
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General
 * Public License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307  USA
 * 
 * @author      ##author##
 * @modified    ##date##
 * @version     ##library.prettyVersion## (##library.version##)
 */

package com.makar;

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;
import processing.core.PMatrix3D;
import processing.core.PShape;
import processing.core.PVector;
import processing.data.Table;
import processing.opengl.PGraphics3D;
import processing.opengl.PShader;

public class Makar {
	public static PApplet pa;
	private static boolean hasBeginDrawBeenCalled = false;
	private static PMatrix3D sceneMatrix = new PMatrix3D();

	public static void init(PApplet pApplet_) {

	}

	public static void beginDraw() {
		if (hasBeginDrawBeenCalled)
			pa.die("Makar: Makar.beginDraw() already called", null);
		hasBeginDrawBeenCalled = true;

		Selector.update((PGraphics3D) pa.g);

		pa.pushMatrix();
		pa.applyMatrix(sceneMatrix);
	}
	
	public static void endDraw() {
		if (!hasBeginDrawBeenCalled)
			pa.die("Makar: Makar.beginDraw() must be called before Reactag.endDraw()", null);
		hasBeginDrawBeenCalled = false;
		
		pa.popMatrix();
	}
	
	public static void resetScene() {
		sceneMatrix.reset();
	}

	public static  void translateScene(float tx, float ty, float tz) {
		sceneMatrix.translate(tx, ty, tz);
	}
	
	public static  void rotateScene(float rx,float ry, float rz) {
		sceneMatrix.rotateX(rx);
		sceneMatrix.rotateY(ry);
		sceneMatrix.rotateZ(rz);
	}

	public static  void rotateSceneX(float r) {
		sceneMatrix.rotateX(r);
	}

	public static  void rotateSceneY(float r) {
		sceneMatrix.rotateY(r);
	}

	public static  void rotateSceneZ(float r) {
		sceneMatrix.rotateZ(r);
	}

	public static  void scaleScene(float sx, float sy, float sz) {
		sceneMatrix.scale(sx, sy, sz);
	}
	
	public static  void scaleScene(float s) {
		sceneMatrix.scale(s);
	}
	
	public static PMatrix3D getSceneMatrix(){
		return sceneMatrix;
	}

	

	public static void setSceneMatrix(PMatrix3D mat) {
		sceneMatrix.set(mat);
	}

	protected static PMatrix3D glToPMatrix(float[] glMatrix) {
		PMatrix3D outMat = new PMatrix3D();
		outMat.m00 = glMatrix[0];
		outMat.m10 = glMatrix[1];
		outMat.m20 = glMatrix[2];
		outMat.m30 = glMatrix[3];
		outMat.m01 = glMatrix[4];
		outMat.m11 = glMatrix[5];
		outMat.m21 = glMatrix[6];
		outMat.m31 = glMatrix[7];
		outMat.m02 = glMatrix[8];
		outMat.m12 = glMatrix[9];
		outMat.m22 = glMatrix[10];
		outMat.m32 = glMatrix[11];
		outMat.m03 = glMatrix[12];
		outMat.m13 = glMatrix[13];
		outMat.m23 = glMatrix[14];
		outMat.m33 = glMatrix[15];
		return outMat;
	}

	public static PVector screenToWorld(float x, float y, float z) {
		PVector out = new PVector();
		com.makar.Selector.unproject(x, pa.height - y, z, out);
		return out;
	}

	public static PVector getCameraPosition() {
		PVector out = new PVector();
		Selector.unproject(0, 0, 0, out);
		return out;
	}

	public static PVector getCameraRotation() {
		PVector p1 = new PVector();
		Selector.unproject(0, 0, 0, p1);
		PVector p2 = new PVector();
		Selector.unproject(0, 0, 1, p2);
		PVector out = PVector.sub(p2, p1);
		out.normalize();
		return out;
	}

	public static void syncMethod(String methodName, Object... params) {

	}

	public static PImage loadImage(String filename) {
		return pa.loadImage(filename);
	}

	public static PShape loadShape(String filename) {
		return pa.loadShape(filename);
	}

	public static byte[] loadBytes(String filename) {
		return pa.loadBytes(filename);
	}

	public static PFont loadFont(String filename) {
		return pa.loadFont(filename);
	}

	public static String[] loadStrings(String filename) {
		return pa.loadStrings(filename);
	}

	public static Table loadTable(String filename) {
		return pa.loadTable(filename);
	}

	public static Table loadTable(String filename, String options) {
		return pa.loadTable(filename, options);
	}

	public static PShader loadShader(String fragFilename) {
		return pa.loadShader(fragFilename);
	}

	public static PShader loadShader(String fragFilename, String vertFilename) {
		return pa.loadShader(fragFilename, vertFilename);
	}
}
