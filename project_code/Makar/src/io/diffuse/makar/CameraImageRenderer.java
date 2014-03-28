// Copyright 2007-2014 metaio GmbH. All rights reserved.
package io.diffuse.makar;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import processing.core.PApplet;
import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.metaio.sdk.jni.ESCREEN_ROTATION;
import com.metaio.sdk.jni.ImageStruct;

public final class CameraImageRenderer {
	private static final String TAG = "CameraImageRenderer";

	public static int camWidth = 640;
	public static int camHeight = 480;

	/**
	 * Camera frame aspect ratio (does not change with screen rotation, e.g.
	 * 640/480 = 1.333)
	 */
	private PApplet parent;

	private float mCameraAspect;
	private int mCameraImageHeight;
	private int mCameraImageWidth;
	private boolean mInitialized = false;
	private boolean mMustUpdateTexture = false;

	/**
	 * Value by which the X axis must be scaled in the overall projection matrix
	 * in order to make up for a aspect-corrected (by cropping) camera image.
	 * Set on each draw() call.
	 */
	private float mScaleX;
	private float mScaleY;
	private int mTexture = -1;
	private ByteBuffer mTextureBuffer;
	private boolean mTextureInitialized = false;
	private int mTextureHeight;
	private int mTextureWidth;
	private FloatBuffer mTexCoordsBuffer;
	private FloatBuffer mVertexBuffer;

	CameraShader shader;

	public CameraImageRenderer(PApplet parent) {
		this.parent = parent;

		shader = new CameraShader();

		final float[] vertices = { -1, -1, 0, 1, -1, 0, -1, 1, 0, 1, 1, 0 };

		ByteBuffer buffer = ByteBuffer.allocateDirect(vertices.length * 4);
		buffer.order(ByteOrder.nativeOrder());
		mVertexBuffer = buffer.asFloatBuffer();
		mVertexBuffer.put(vertices);
		mVertexBuffer.rewind();

		// Create texture coordinates buffer but don't fill it yet
		buffer = ByteBuffer.allocateDirect(vertices.length / 3 * 8);
		buffer.order(ByteOrder.nativeOrder());
		mTexCoordsBuffer = buffer.asFloatBuffer();

		// Generate texture
		int[] tmp = new int[1];
		GLES20.glGenTextures(1, tmp, 0);
		mTexture = tmp[0];
	}

	public void draw(ESCREEN_ROTATION screenRotation) {
		if (!mInitialized)
			return;

		GLES20.glDisable(GLES20.GL_DEPTH_TEST);
		GLES20.glBindTexture(GL10.GL_TEXTURE_2D, mTexture);

		if (mMustUpdateTexture) {
			if (!mTextureInitialized) {
				// Allocate camera image texture once with 2^n dimensions

				GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mTextureWidth, mTextureHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

				mTextureInitialized = true;
			}

			// ...but only overwrite the camera image-sized region
			GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, mCameraImageWidth, mCameraImageHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
					mTextureBuffer);

			final float xRatio = (float) mCameraImageWidth / mTextureWidth;
			final float yRatio = (float) mCameraImageHeight / mTextureHeight;

			final boolean cameraIsRotated = screenRotation == ESCREEN_ROTATION.ESCREEN_ROTATION_90 || screenRotation == ESCREEN_ROTATION.ESCREEN_ROTATION_270;
			final float cameraAspect = cameraIsRotated ? 1.0f / mCameraAspect : mCameraAspect;

			Display display = ((WindowManager) parent.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
			DisplayMetrics displayMetrics = new DisplayMetrics();
			display.getMetrics(displayMetrics);

			// DisplayMetrics.widthPixels/heightPixels are the width/height in
			// the current
			// orientation (i.e. values get swapped when you rotate the device)
			float screenAspect = (float) displayMetrics.widthPixels / displayMetrics.heightPixels;

			float offsetX, offsetY;

			if (cameraAspect > screenAspect) {
				// Camera image is wider (e.g. 480x640 camera image vs. a
				// 480x800 device, example
				// in portrait mode), so crop the width of the camera image
				float aspectRatio = screenAspect / cameraAspect;
				offsetX = 0.5f * (1 - aspectRatio);
				offsetY = 0;

				mScaleX = cameraAspect / screenAspect;
				mScaleY = 1;
			} else {
				// Screen is wider, so crop the height of the camera image
				float aspectRatio = cameraAspect / screenAspect;
				offsetY = 0.5f * (1 - aspectRatio);
				offsetX = 0;

				mScaleX = 1;
				mScaleY = screenAspect / cameraAspect;
			}

			if (cameraIsRotated) {
				// Camera image will be rendered with +-90° rotation, so switch
				// UV coordinates
				float tmp = offsetX;
				offsetX = offsetY;
				offsetY = tmp;
			}

			float x1 = offsetX * xRatio;
			float y1 = (1 - offsetY) * yRatio;
			float x2 = (1 - offsetX) * xRatio;
			float y2 = (1 - offsetY) * yRatio;
			float x3 = offsetX * xRatio;
			float y3 = offsetY * yRatio;
			float x4 = (1 - offsetX) * xRatio;
			float y4 = offsetY * yRatio;

			mTexCoordsBuffer.put(new float[] { x1, y1, x2, y2, x3, y3, x4, y4 });
			//mTexCoordsBuffer.put(new float[] { y1, x1, y2, x2, y3, x3, y4, x4 });
			mTexCoordsBuffer.rewind();

			mMustUpdateTexture = false;
		}
		
		float[] mvpMatrix = new float[16];
		Matrix.setIdentityM(mvpMatrix, 0);

		switch (screenRotation)
		{
			// Portrait
			case ESCREEN_ROTATION_270:
				Matrix.rotateM(mvpMatrix, 0, -90, 0, 0, 1);
				break;

			// Reverse portrait (upside down)
			case ESCREEN_ROTATION_90:
				Matrix.rotateM(mvpMatrix, 0, 90, 0, 0, 1);
				break;

			// Landscape (right side of tall device facing up)
			case ESCREEN_ROTATION_0:
				break;

			// Reverse landscape (left side of tall device facing up)
			case ESCREEN_ROTATION_180:
				Matrix.rotateM(mvpMatrix, 0, 180, 0, 0, 1);
				break;

			default:
				Log.e(TAG, "Unknown screen rotation");
		}

		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

		GLES20.glUseProgram(shader.shaderHandle);
		shader.setUniform4fv("u_mvpMatrix", mvpMatrix);
		shader.setUniform1i("u_texture", 0);
		shader.setVerticies(mVertexBuffer);
		shader.setTextureCoords(mTexCoordsBuffer);

		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

		GLES20.glDisable(GLES20.GL_TEXTURE_2D);
		GLES20.glUseProgram(0);
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
	}

	private static int getNextPowerOf2(int value) {
		for (int i = 0; i < 12; ++i) {
			if ((1 << i) >= value)
				return 1 << i;
		}

		throw new RuntimeException("Value too large");
	}

	public float getScaleX() {
		return mScaleX;
	}

	public float getScaleY() {
		return mScaleY;
	}

	private void init(int cameraImageWidth, int cameraImageHeight) {
		mTextureWidth = getNextPowerOf2(cameraImageWidth);
		mTextureHeight = getNextPowerOf2(cameraImageHeight);

		mTextureBuffer = ByteBuffer.allocateDirect(cameraImageWidth * cameraImageHeight * 4);

		mInitialized = true;
	}

	public void updateFrame(ImageStruct frame) {
		final int frameWidth = frame.getWidth();
		final int frameHeight = frame.getHeight();

		mCameraAspect = (float) frameWidth / frameHeight;

		switch (frame.getColorFormat()) {
		case ECF_A8R8G8B8:
			if (!mInitialized)
				init(frameWidth, frameHeight);

			if (!frame.getOriginIsUpperLeft()) {
				Log.e(TAG, "Unimplemented: ARGB upside-down");
				return;
			}

			mTextureBuffer.rewind();
			frame.copyBufferToNioBuffer(mTextureBuffer);
			mTextureBuffer.rewind();

			break;

		default:
			Log.e(TAG, "Unimplemented color format " + frame.getColorFormat());
			return;
		}

		mMustUpdateTexture = true;

		mCameraImageWidth = frameWidth;
		mCameraImageHeight = frameHeight;
	}
	
	public class CameraShader {
		public static final String vertexShaderCode = "attribute vec4 a_position;attribute vec2 a_texCoord;varying vec2 v_texCoord;uniform mat4 u_mvpMatrix; void main(){v_texCoord = a_texCoord;gl_Position = u_mvpMatrix*a_position;}";
		public static final String fragmentShaderCode = "precision mediump float;uniform sampler2D u_texture;varying vec2 v_texCoord;void main(){gl_FragColor = texture2D(u_texture, v_texCoord);}";

		public int shaderHandle;

		public CameraShader() {
			final int vertexShaderHandle = compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
			final int fragmentShaderHandle = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
			shaderHandle = createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle, new String[] { "a_position", "a_texCoord" });
		}

		public void setVerticies(FloatBuffer buffer) {
			int handle = GLES20.glGetAttribLocation(shaderHandle, "a_position");
			buffer.position(0);
			GLES20.glVertexAttribPointer(handle, 3, GLES20.GL_FLOAT, false, 0, buffer);
			GLES20.glEnableVertexAttribArray(handle);
		}

		public void setTextureCoords(FloatBuffer buffer) {
			int handle = GLES20.glGetAttribLocation(shaderHandle, "a_texCoord");
			buffer.position(0);
			GLES20.glVertexAttribPointer(handle, 2, GLES20.GL_FLOAT, false, 0, buffer);
			GLES20.glEnableVertexAttribArray(handle);
		}

		public void setUniform1i(String name, int value) {
			int uniformHandle = GLES20.glGetUniformLocation(shaderHandle, name);
			GLES20.glUniform1i(uniformHandle, value);
		}

		public void setUniform1f(String name, float value) {
			int uniformHandle = GLES20.glGetUniformLocation(shaderHandle, name);
			GLES20.glUniform1f(uniformHandle, value);
		}
		
		public void setUniform4fv(String name, float[] mat) {
			int uniformHandle = GLES20.glGetUniformLocation(shaderHandle, name);
			GLES20.glUniformMatrix4fv(uniformHandle, 1, false, mat, 0);
		}

		private int compileShader(final int shaderType, final String shaderSource) {
			int shaderHandle = GLES20.glCreateShader(shaderType);

			if (shaderHandle != 0) {
				// Pass in the shader source.
				GLES20.glShaderSource(shaderHandle, shaderSource);

				// Compile the shader.
				GLES20.glCompileShader(shaderHandle);

				// Get the compilation status.
				final int[] compileStatus = new int[1];
				GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

				// If the compilation failed, delete the shader.
				if (compileStatus[0] == 0) {
					Log.e("Shader", "Error compiling shader: " + GLES20.glGetShaderInfoLog(shaderHandle));
					GLES20.glDeleteShader(shaderHandle);
					shaderHandle = 0;
				}
			}

			if (shaderHandle == 0) {
				throw new RuntimeException("Error creating shader.");
			}

			return shaderHandle;
		}

		private int createAndLinkProgram(final int vertexShaderHandle, final int fragmentShaderHandle, final String[] attributes) {
			int programHandle = GLES20.glCreateProgram();

			if (programHandle != 0) {
				// Bind the vertex shader to the program.
				GLES20.glAttachShader(programHandle, vertexShaderHandle);

				// Bind the fragment shader to the program.
				GLES20.glAttachShader(programHandle, fragmentShaderHandle);

				// Bind attributes
				if (attributes != null) {
					final int size = attributes.length;
					for (int i = 0; i < size; i++) {
						GLES20.glBindAttribLocation(programHandle, i, attributes[i]);
					}
				}

				// Link the two shaders together into a program.
				GLES20.glLinkProgram(programHandle);

				// Get the link status.
				final int[] linkStatus = new int[1];
				GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);

				// If the link failed, delete the program.
				if (linkStatus[0] == 0) {
					Log.e("Shader", "Error compiling program: " + GLES20.glGetProgramInfoLog(programHandle));
					GLES20.glDeleteProgram(programHandle);
					programHandle = 0;
				}
			}

			if (programHandle == 0) {
				throw new RuntimeException("Error creating program.");
			}

			return programHandle;
		}

		

	}
}
