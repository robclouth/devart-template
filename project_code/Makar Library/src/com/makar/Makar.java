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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;
import processing.core.PMatrix3D;
import processing.core.PShape;
import processing.core.PVector;
import processing.data.JSONArray;
import processing.data.JSONObject;
import processing.data.Table;
import processing.event.KeyEvent;
import processing.event.MouseEvent;
import processing.opengl.PGraphics3D;
import processing.opengl.PGraphicsOpenGL;
import processing.opengl.PShader;

public class Makar {
	public final static String VERSION = "##library.prettyVersion##";

	public static PApplet pa;

	private static boolean hasBeginDrawBeenCalled = false;

	private static TrackedPhoto[] trackedPhotos;
	private static TrackedPhoto activePhoto = null;
	private static float scaleX = 1, scaleY = 1;

	private static boolean isCalibrating;

	private enum CalibrationState {
		NotCalibrating, RayOrigin, LocaliseOrigin, RayXAxis, LocaliseXAxis, RayYAxis, LocaliseYAxis, Finished
	};

	private static CalibrationState calibrationState = CalibrationState.NotCalibrating;
	private static Ray referenceRay;
	private static PVector originPoint, xAxisPoint, yAxisPoint;
	private static PMatrix3D sceneMatrix = new PMatrix3D();

	// private static int toolbarHeight = 30;

	public static void init(PApplet pApplet_) {
		pa = pApplet_;

		if (!(pa.g instanceof PGraphicsOpenGL)) {
			pa.die("Makar: Render mode must be P3D or OPENGL");
		}

		pa.registerMethod("keyEvent", new Makar().new InputListener());
		pa.registerMethod("mouseEvent", new Makar().new InputListener());

		loadTrackingFile();
	}

	public static void keyEvent(KeyEvent e) {
		if (e.getAction() == KeyEvent.PRESS) {
			if (e.getKey() == 'c') {
				isCalibrating = !isCalibrating;

				if (isCalibrating) {
					calibrationState = CalibrationState.RayOrigin;
					referenceRay = null;
					originPoint = null;
					xAxisPoint = null;
					yAxisPoint = null;
				}
			}
		}
	}

	public static void mouseEvent(MouseEvent e) {
		if (isCalibrating) {
			if (e.getAction() == MouseEvent.PRESS) {
				if (calibrationState == CalibrationState.RayOrigin)
					calibrationState = CalibrationState.LocaliseOrigin;
				else if (calibrationState == CalibrationState.LocaliseOrigin && originPoint != null) {
					calibrationState = CalibrationState.RayXAxis;
					referenceRay = null;
				} else if (calibrationState == CalibrationState.RayXAxis)
					calibrationState = CalibrationState.LocaliseXAxis;
				else if (calibrationState == CalibrationState.LocaliseXAxis && xAxisPoint != null) {
					calibrationState = CalibrationState.RayYAxis;
					referenceRay = null;
				} else if (calibrationState == CalibrationState.RayYAxis)
					calibrationState = CalibrationState.LocaliseYAxis;
				else if (calibrationState == CalibrationState.LocaliseYAxis && yAxisPoint != null) {
					referenceRay = null;
					calibrationState = CalibrationState.Finished;

					alignSceneMatrix(originPoint, xAxisPoint, yAxisPoint);
				}
			}
		}
	}

	private static void alignSceneMatrix(PVector origin, PVector xAxis, PVector yAxis) {

		PMatrix3D mat = new PMatrix3D();
		mat.translate(origin.x, origin.y, origin.z);

		// x-axis
		//PVector currXAxis = pa.getMatrix().mult(new PVector(1, 0, 0), null);
		PVector currXAxis = new PVector(1, 0, 0);

		currXAxis.normalize();
		PVector dOX = PVector.sub(xAxis, origin);
		dOX.normalize();
		PVector targetDir = dOX;
		float angle = (float) Math.acos(PVector.dot(targetDir, currXAxis));
		if (Math.abs(angle) > 0.0000001) {
			PVector rotAxis = new PVector();
			PVector.cross(targetDir, currXAxis, rotAxis);
			rotAxis.normalize();
			mat.rotateX(rotAxis.x);
			mat.rotateY(rotAxis.y);
			mat.rotateZ(rotAxis.z);
			
			//mat.rotateX(angle);
		}

		// y-axis
		//PVector currYAxis = pa.getMatrix().mult(new PVector(0, 1, 0), null);
		PVector currYAxis = new PVector(0, 1, 0);

		currYAxis.normalize();
		PVector dOY = PVector.sub(yAxis, origin);
		dOY.normalize();
		targetDir = dOY;
		angle = (float) Math.acos(PVector.dot(targetDir, currYAxis));
		if (Math.abs(angle) > 0.0000001) {
			PVector rotAxis = new PVector();
			PVector.cross(targetDir, currYAxis, rotAxis);
			rotAxis.normalize();
			mat.rotateX(rotAxis.x);
			mat.rotateY(rotAxis.y);
			mat.rotateZ(rotAxis.z);
			
			//mat.rotateY(angle);
		}

		setSceneMatrix(mat);
	}

	public static void beginDraw() {
		if (hasBeginDrawBeenCalled)
			pa.die("Makar: Makar.beginDraw() already called", null);
		hasBeginDrawBeenCalled = true;

		if (trackedPhotos != null) {
			drawBackground();
			PGraphicsOpenGL pgl = ((PGraphicsOpenGL) pa.g);
			pgl.pushMatrix();
			pgl.pushProjection();
			float[] modelMatrix = new float[16];
			System.arraycopy(activePhoto.modelViewMat, 0, modelMatrix, 0, 16);
			pgl.modelview.set(glToPMatrix(activePhoto.modelViewMat));

			float[] projMatrix = new float[16];
			System.arraycopy(activePhoto.projectionMat, 0, projMatrix, 0, 16);
			projMatrix[0] *= scaleX;
			projMatrix[5] *= scaleY;
			pgl.projection.set(glToPMatrix(projMatrix));

			

		} else
			pa.background(0);

		Selector.update((PGraphics3D) pa.g);

		pa.pushMatrix();
		pa.applyMatrix(sceneMatrix);
	}
	
	public static void endDraw() {
		if (!hasBeginDrawBeenCalled)
			pa.die("Makar: Makar.beginDraw() must be called before Reactag.endDraw()", null);
		hasBeginDrawBeenCalled = false;
		
		pa.popMatrix();

		if (trackedPhotos != null) {
			// CALIBRATION
			if (isCalibrating) {
				if (calibrationState == CalibrationState.RayOrigin || calibrationState == CalibrationState.RayXAxis
						|| calibrationState == CalibrationState.RayYAxis) {

					PVector start = Makar.screenToWorld(pa.mouseX, pa.mouseY, 0);
					PVector end = Makar.screenToWorld(pa.mouseX, pa.mouseY, 1);

					referenceRay = new Ray(start, end);

				} else if (calibrationState == CalibrationState.LocaliseOrigin || calibrationState == CalibrationState.LocaliseXAxis
						|| calibrationState == CalibrationState.LocaliseYAxis) {
					PVector start = Makar.screenToWorld(pa.mouseX, pa.mouseY, 0);
					PVector end = Makar.screenToWorld(pa.mouseX, pa.mouseY, 1);
					Ray ray2 = new Ray(start, end);

					PVector lineVec1 = PVector.sub(referenceRay.end, referenceRay.start);
					PVector lineVec2 = PVector.sub(ray2.end, ray2.start);
					PVector point = closestPointsOnTwoLines(referenceRay.start, lineVec1, ray2.start, lineVec2);

					if (calibrationState == CalibrationState.LocaliseOrigin)
						originPoint = point;
					else if (calibrationState == CalibrationState.LocaliseXAxis)
						xAxisPoint = point;
					else if (calibrationState == CalibrationState.LocaliseYAxis)
						yAxisPoint = point;
				}

				if (referenceRay != null) {
					if (calibrationState == CalibrationState.RayOrigin || calibrationState == CalibrationState.LocaliseOrigin)
						referenceRay.draw(pa.color(255, 0, 255));
					else if (calibrationState == CalibrationState.RayXAxis || calibrationState == CalibrationState.LocaliseXAxis)
						referenceRay.draw(pa.color(255, 0, 0));
					else if (calibrationState == CalibrationState.RayYAxis || calibrationState == CalibrationState.LocaliseYAxis)
						referenceRay.draw(pa.color(0, 255, 0));
				}

				if (originPoint != null) {
					pa.pushMatrix();
					pa.translate(originPoint.x, originPoint.y, originPoint.z);
					pa.fill(255, 0, 255, 100);
					pa.noStroke();
					pa.sphere(10);
					pa.popMatrix();
				}

				if (xAxisPoint != null) {
					pa.pushMatrix();
					pa.translate(xAxisPoint.x, xAxisPoint.y, xAxisPoint.z);
					pa.fill(255, 0, 0, 100);
					pa.noStroke();
					pa.sphere(10);
					pa.popMatrix();
					pa.stroke(255, 0, 0);
					pa.line(originPoint.x, originPoint.y, originPoint.z, xAxisPoint.x, xAxisPoint.y, xAxisPoint.z);
				}

				if (yAxisPoint != null) {
					pa.pushMatrix();
					pa.translate(yAxisPoint.x, yAxisPoint.y, yAxisPoint.z);
					pa.fill(0, 255, 0, 100);
					pa.noStroke();
					pa.sphere(10);
					pa.popMatrix();
					pa.stroke(0, 255, 0);
					pa.line(originPoint.x, originPoint.y, originPoint.z, yAxisPoint.x, yAxisPoint.y, yAxisPoint.z);
				}
			}

			if (pa.keyPressed) {
				if (pa.key == '1' && trackedPhotos.length > 0)
					setActivePhoto(trackedPhotos[0]);
				else if (pa.key == '2' && trackedPhotos.length > 1)
					setActivePhoto(trackedPhotos[1]);
				else if (pa.key == '3' && trackedPhotos.length > 2)
					setActivePhoto(trackedPhotos[2]);
				else if (pa.key == '4' && trackedPhotos.length > 3)
					setActivePhoto(trackedPhotos[3]);
				else if (pa.key == '5' && trackedPhotos.length > 4)
					setActivePhoto(trackedPhotos[4]);
				else if (pa.key == '6' && trackedPhotos.length > 5)
					setActivePhoto(trackedPhotos[5]);
				else if (pa.key == '7' && trackedPhotos.length > 6)
					setActivePhoto(trackedPhotos[6]);
				else if (pa.key == '8' && trackedPhotos.length > 7)
					setActivePhoto(trackedPhotos[7]);
				else if (pa.key == '9' && trackedPhotos.length > 8)
					setActivePhoto(trackedPhotos[8]);
				else if (pa.key == '0' && trackedPhotos.length > 9)
					setActivePhoto(trackedPhotos[9]);
			}
			
			PGraphicsOpenGL pgl = ((PGraphicsOpenGL) pa.g);
			pgl.popMatrix();
			pgl.popProjection();
			
			
		}
		// // draw gui
		// pApplet.pushStyle();
		// pApplet.textAlign(PApplet.CENTER, PApplet.CENTER);
		// pApplet.colorMode(PApplet.RGB);
		// pApplet.fill(255, 50);
		// pApplet.noStroke();
		// pApplet.rect(0, 0, pApplet.width, toolbarHeight);
		// float buttonWidth = pApplet.width / trackedPhotos.length;
		// for (int i = 0; i < trackedPhotos.length; i++) {
		// if (trackedPhotos[i] == activePhoto) {
		// pApplet.fill(50, 50);
		// pApplet.rect(i, 0, buttonWidth, toolbarHeight);
		// }
		//
		// pApplet.fill(0);
		// pApplet.text("Photo " + (i + 1), 0, buttonWidth, toolbarHeight);
		// }
		//
		// if (pApplet.mouseButton == PApplet.LEFT && pApplet.mouseY <
		// toolbarHeight) {
		// int trackedPhotoId = (int) PApplet.constrain((pApplet.mouseX /
		// (float) pApplet.width) * trackedPhotos.length, 0,
		// trackedPhotos.length - 1);
		// setActivePhoto(trackedPhotos[trackedPhotoId]);
		// }
		//
		// pApplet.popStyle();

		

		pa.pushStyle();
		pa.hint(PApplet.DISABLE_DEPTH_TEST);
		if (isCalibrating) {
			pa.fill(255, 0, 0);
			pa.textAlign(PApplet.LEFT, PApplet.TOP);
			pa.text("Calibrating...", 5, 5);

			if (referenceRay != null) {
				pa.fill(255, 0, 0);
				pa.noStroke();
				pa.ellipse(pa.screenX(referenceRay.start.x, referenceRay.start.y, referenceRay.start.z),
						pa.screenY(referenceRay.start.x, referenceRay.start.y, referenceRay.start.z), 5, 5);
			}
		}

		pa.popStyle();
		pa.hint(PApplet.ENABLE_DEPTH_TEST);
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

	private static void drawBackground() {
		float cameraAspect = (float) activePhoto.image.width / (float) activePhoto.image.height;
		float screenAspect = (float) pa.width / (float) pa.height;

		float offsetX, offsetY;
		if (cameraAspect > screenAspect) {
			float aspectRatio = screenAspect / cameraAspect;
			offsetX = 0.5f * (1 - aspectRatio);
			offsetY = 0;

			scaleX = cameraAspect / screenAspect;
			scaleY = 1;
		} else {
			float aspectRatio = cameraAspect / screenAspect;
			offsetY = 0.5f * (1 - aspectRatio);
			offsetX = 0;

			scaleX = 1;
			scaleY = screenAspect / cameraAspect;
		}

		// draw background
		pa.hint(PApplet.DISABLE_DEPTH_TEST);
		pa.noLights();
		pa.beginShape(PApplet.QUADS);
		pa.texture(activePhoto.image);
		pa.vertex(0, 0, offsetX * activePhoto.image.width, offsetY * activePhoto.image.height);
		pa.vertex(pa.width, 0, (1 - offsetX) * activePhoto.image.width, offsetY * activePhoto.image.height);
		pa.vertex(pa.width, pa.height, (1 - offsetX) * activePhoto.image.width, (1 - offsetY) * activePhoto.image.height);
		pa.vertex(0, pa.height, offsetX * activePhoto.image.width, (1 - offsetY) * activePhoto.image.height);
		pa.endShape();
		pa.hint(PApplet.ENABLE_DEPTH_TEST);
	}

	private static void setActivePhoto(TrackedPhoto photo) {
		activePhoto = photo;
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

	private static void cleanUp() {
		File trackingFolder = pa.dataFile("tracking");
		if (trackingFolder != null && trackingFolder.exists()) {
			for (File file : trackingFolder.listFiles())
				file.delete();
			trackingFolder.delete();
		}
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

	public static String version() {
		return VERSION;
	}

	public static class TrackedPhoto {
		public PImage image;
		public float[] modelViewMat, projectionMat;
	}

	public class InputListener {
		public void keyEvent(KeyEvent e) {
			Makar.keyEvent(e);
		}

		public void mouseEvent(MouseEvent e) {
			Makar.mouseEvent(e);
		}
	}

	public static PVector closestPointsOnTwoLines(PVector linePoint1, PVector lineVec1, PVector linePoint2, PVector lineVec2) {

		PVector closestPointLine1 = new PVector();

		float a = PVector.dot(lineVec1, lineVec1);
		float b = PVector.dot(lineVec1, lineVec2);
		float e = PVector.dot(lineVec2, lineVec2);

		float d = a * e - b * b;

		// lines are not parallel
		if (d != 0.0f) {
			PVector r = PVector.sub(linePoint1, linePoint2);
			float c = PVector.dot(lineVec1, r);
			float f = PVector.dot(lineVec2, r);

			float s = (b * f - c * e) / d;
			//float t = (a * f - c * b) / d;

			closestPointLine1 = PVector.add(linePoint1, PVector.mult(lineVec1, s));

			return closestPointLine1;
		}

		else {
			return null;
		}
	}

	private static void loadTrackingFile() {
		String sketchName = pa.getClass().getCanonicalName();
		File trackingFile = pa.dataFile(sketchName + ".tracking");

		if (trackingFile.exists()) {
			BufferedReader reader = null;
			try {
				unzipTrackingFile(trackingFile);

				// load matrices json
				File matricesFile = pa.dataFile("tracking/trackedPhotos");
				reader = new BufferedReader(new FileReader(matricesFile));
				String line = null;
				StringBuilder sb = new StringBuilder();
				while ((line = reader.readLine()) != null)
					sb.append(line);
				reader.close();
				matricesFile.delete();

				JSONObject json = JSONObject.parse(sb.toString());
				JSONArray trackedPhotosArray = json.getJSONArray("trackedPhotos");

				// load photos
				File[] photoFiles = pa.dataFile("tracking").listFiles(new FilenameFilter() {
					public boolean accept(File dir, String name) {
						return name.endsWith(".jpg");
					}
				});
				trackedPhotos = new TrackedPhoto[photoFiles.length];

				for (int i = 0; i < photoFiles.length; i++) {
					trackedPhotos[i] = new TrackedPhoto();
					trackedPhotos[i].image = pa.loadImage(photoFiles[i].getAbsolutePath());
					photoFiles[i].delete();

					JSONObject trackedPhotoObj = trackedPhotosArray.getJSONObject(i);

					JSONArray projectionMatrixJson = trackedPhotoObj.getJSONArray("projectionMatrix");
					trackedPhotos[i].projectionMat = projectionMatrixJson.getFloatArray();

					JSONArray modelViewMatrixJson = trackedPhotoObj.getJSONArray("modelViewMatrix");
					trackedPhotos[i].modelViewMat = modelViewMatrixJson.getFloatArray();
				}

				// delete tracking folder
				pa.dataFile("tracking").delete();

				setActivePhoto(trackedPhotos[0]);
			} catch (IOException e) {
				pa.die("Makar: Unable to open tracking file");
			} finally {
				try {
					reader.close();
				} catch (IOException e) {
				}

				cleanUp();
			}

		} else {
			System.err.println("Makar: no tracking file found");
		}
	}

	private static void unzipTrackingFile(File trackingFile) throws IOException {
		byte[] buffer = new byte[1024];

		try {

			ZipInputStream zis = new ZipInputStream(new FileInputStream(trackingFile));
			ZipEntry ze = zis.getNextEntry();

			while (ze != null) {
				String filename = ze.getName();
				if (filename.equals("config") || filename.equals("map")) {
					ze = zis.getNextEntry();
					continue;
				}

				File newFile = new File(pa.dataPath("tracking") + File.separator + filename);

				newFile.getParentFile().mkdirs();

				FileOutputStream fos = new FileOutputStream(newFile);

				int len;
				while ((len = zis.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}

				fos.close();
				ze = zis.getNextEntry();
			}

			zis.closeEntry();
			zis.close();
		} catch (IOException e) {
			throw e;
		}
	}
}
