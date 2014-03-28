package com.makar;

import processing.core.PApplet;
import processing.core.PMatrix3D;
import processing.core.PVector;
import processing.opengl.PGraphics3D;

public class Selector {
	private static PMatrix3D mat = new PMatrix3D();
	private static int[] viewport = new int[4];

	public static void update(PGraphics3D g) {
		if (g != null) { 
			mat.set(g.projection);
			mat.apply(g.modelview);

			mat.invert();

			viewport[0] = 0;
			viewport[1] = 0;
			viewport[2] = g.width;
			viewport[3] = g.height;
		}
	}

	public static boolean unproject(float winx, float winy, float winz, PVector result) {

		float[] in = new float[4];
		float[] out = new float[4];

		// Transform to normalized screen coordinates (-1 to 1).
		in[0] = ((winx - (float) viewport[0]) / (float) viewport[2]) * 2.0f - 1.0f;
		in[1] = ((winy - (float) viewport[1]) / (float) viewport[3]) * 2.0f - 1.0f;
		in[2] = PApplet.constrain(winz, 0f, 1f) * 2.0f - 1.0f;
		in[3] = 1.0f;

		// Calculate homogeneous coordinates.
		out[0] = mat.m00 * in[0] + mat.m01 * in[1] + mat.m02 * in[2] + mat.m03 * in[3];
		out[1] = mat.m10 * in[0] + mat.m11 * in[1] + mat.m12 * in[2] + mat.m13 * in[3];
		out[2] = mat.m20 * in[0] + mat.m21 * in[1] + mat.m22 * in[2] + mat.m23 * in[3];
		out[3] = mat.m30 * in[0] + mat.m31 * in[1] + mat.m32 * in[2] + mat.m33 * in[3];

		if (out[3] == 0.0f) { // Check for an invalid result.
			result.x = 0.0f;
			result.y = 0.0f;
			result.z = 0.0f;
			return false;
		}

		// Scale to world coordinates.
		out[3] = 1.0f / out[3];
		result.x = out[0] * out[3];
		result.y = out[1] * out[3];
		result.z = out[2] * out[3];
		return true;
	}
}
