package io.diffuse.makar;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PMatrix3D;
import processing.opengl.PGraphicsOpenGL;
import android.content.res.Configuration;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.metaio.sdk.MetaioDebug;
import com.metaio.sdk.jni.CameraVector;
import com.metaio.sdk.jni.ERENDER_SYSTEM;
import com.metaio.sdk.jni.ESCREEN_ROTATION;
import com.metaio.sdk.jni.IMetaioSDKAndroid;
import com.metaio.sdk.jni.IMetaioSDKCallback;
import com.metaio.sdk.jni.ImageStruct;
import com.metaio.sdk.jni.MetaioSDK;
import com.metaio.sdk.jni.TrackingValues;
import com.metaio.sdk.jni.TrackingValuesVector;
import com.metaio.sdk.jni.Vector2di;
import com.metaio.tools.Screen;

public final class ViewActivity extends PApplet {
	private CameraImageRenderer mCameraImageRenderer;
	PImage cameraImage;

	private static boolean nativeLibsLoaded;
	static {
		nativeLibsLoaded = IMetaioSDKAndroid.loadNativeLibs();
	}
	private IMetaioSDKAndroid metaioSDK;
	private MetaioSDKCallbackHandler mSDKCallback;

	private boolean isActivityPaused = false;
	private ESCREEN_ROTATION mScreenRotation;

	public String sketchRenderer() {
		return P3D;
	}

	enum State {
		Scanning, LoadingSketch, RunningSketch
	};

	private State state;

	SketchHandler sketchHandler;

	@Override
	public void setup() {
		runOnUiThread(new Runnable() {
			public void run() {
				getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			}
		});

		if (!nativeLibsLoaded) {
			MetaioDebug.log(Log.ERROR, "Unsupported platform, failed to load the native libs");
			finish();
		}

		metaioSDK = MetaioSDK.CreateMetaioSDKAndroid(this, getResources().getString(R.string.metaioSDKSignature));
		mScreenRotation = Screen.getRotation(this);
		metaioSDK.initializeRenderer(0, 0, mScreenRotation, ERENDER_SYSTEM.ERENDER_SYSTEM_NULL);
		mCameraImageRenderer = new CameraImageRenderer(this);

		mSDKCallback = new MetaioSDKCallbackHandler();
		metaioSDK.registerCallback(mSDKCallback);

		if (!isActivityPaused)
			startCamera();

		sketchHandler = new SketchHandler(this);

		metaioSDK.resizeRenderer(width, height);
	}

	public void draw() {
		metaioSDK.requestCameraImage();
		metaioSDK.render();

		mCameraImageRenderer.draw(mScreenRotation);

		final TrackingValues trackingValues = metaioSDK.getTrackingValues(1);

		if (trackingValues.isTrackingState()) {
			PGraphicsOpenGL pgl = ((PGraphicsOpenGL) g);

			pgl.pushMatrix();
			pgl.pushProjection();

			float[] modelMatrix = new float[16];
			metaioSDK.getTrackingValues(1, modelMatrix, true, true);
			pgl.modelview.set(glToPMatrix(modelMatrix));

			float[] projMatrix = new float[16];
			metaioSDK.getProjectionMatrix(projMatrix, true);
			projMatrix[0] *= mCameraImageRenderer.getScaleX();
			projMatrix[5] *= mCameraImageRenderer.getScaleY();
			pgl.projection.set(glToPMatrix(projMatrix));

			if (state == State.RunningSketch) {
				if (sketchHandler.currSketch != null)
					sketchHandler.currSketch.update();
			}

			pgl.popMatrix();
			pgl.popProjection();
		}

	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		mScreenRotation = Screen.getRotation(this);
		metaioSDK.setScreenRotation(mScreenRotation);
		super.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onPause() {
		super.onPause();

		isActivityPaused = true;
		if (metaioSDK != null)
			metaioSDK.pause();
	}

	@Override
	protected void onResume() {
		super.onResume();

		isActivityPaused = false;
		if (metaioSDK != null)
			metaioSDK.resume();
	}

	@Override
	protected void onStart() {
		super.onStart();

	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (metaioSDK != null) {
			metaioSDK.delete();
			metaioSDK = null;
		}

		if (mSDKCallback != null) {
			mSDKCallback.delete();
			mSDKCallback = null;
		}
		
		
		SketchHandler.instance.retryConnectHandler.removeCallbacksAndMessages (null); 
	}

	protected void startCamera() {
		runOnUiThread(new Runnable() {
			public void run() {
				final CameraVector cameras = metaioSDK.getCameraList();
				if (cameras.size() > 0) {
					com.metaio.sdk.jni.Camera camera = cameras.get(0);
					camera.setResolution(new Vector2di(CameraImageRenderer.camWidth, CameraImageRenderer.camHeight));
					camera.setDownsample(2);
					camera.setYuvPipeline(false);
					metaioSDK.startCamera(camera);
				}
			}
		});
	}

	void setState(State newState) {
		state = newState;

		if (state == State.Scanning) {
			metaioSDK.resumeTracking();
			if (!metaioSDK.setTrackingConfiguration("QRCODE"))
				MetaioDebug.log(Log.ERROR, "Failed to set tracking configuration");
		} else if (state == State.LoadingSketch) {
			metaioSDK.pauseTracking();
		} else if (state == State.RunningSketch) {
			loadTrackingConfig();
		}
	}

	private void loadTrackingConfig() {
		if (!metaioSDK.setTrackingConfiguration(SketchHandler.instance.currSketch.configFile.getAbsolutePath()))
			MetaioDebug.log(Log.ERROR, "Failed to set tracking configuration");
	}

	public void setProjectionMatrix(float[] glMatrix) {
		PMatrix3D mat = glToPMatrix(glMatrix);
		float far = mat.m23 / (mat.m22 + 1);
		float near = mat.m23 / (mat.m22 - 1);
		frustum((mat.m02 - 1) * near / mat.m00, (mat.m02 + 1) * near / mat.m00, (mat.m12 - 1) * near / mat.m11, (mat.m12 + 1) * near / mat.m11, near, far);
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

	final class MetaioSDKCallbackHandler extends IMetaioSDKCallback {

		@Override
		public void onNewCameraFrame(ImageStruct cameraFrame) {
			if (mCameraImageRenderer != null)
				mCameraImageRenderer.updateFrame(cameraFrame);
		}

		@Override
		public void onSDKReady() {
			setState(State.Scanning);
		}

		@Override
		public void onTrackingEvent(TrackingValuesVector trackingValues) {
			for (int i = 0; i < trackingValues.size(); i++) {
				final TrackingValues v = trackingValues.get(i);

				if (state == State.Scanning) {
					if (v.isTrackingState()) {
						final String[] tokens = v.getAdditionalValues().split("::");
						if (tokens.length > 1) {
							final String urlString = tokens[1];

							if (sketchHandler.isValidUrl(urlString)) {
								runOnUiThread(new Runnable() {
									public void run() {
										Toast.makeText(ViewActivity.this, "Getting info...", Toast.LENGTH_SHORT).show();
										String id = urlString.replace("/sketches/", "").replace("/info", "");

										sketchHandler.getSketchInfo(id);
									}
								});

								setState(State.LoadingSketch);
							} else {
								runOnUiThread(new Runnable() {
									public void run() {
										Toast.makeText(ViewActivity.this, "Not a valid code", Toast.LENGTH_SHORT).show();
									}
								});
							}
						}
					}
				} else if (state == State.RunningSketch) {
					if (v.isTrackingState()) {

					}
				}
			}
		}
	}

	// INPUT EVENTS
	@Override
	public void mouseClicked() {
		SketchHandler.instance.mouseClicked();
	}

	@Override
	public void mousePressed() {
		SketchHandler.instance.mousePressed();
	}

	@Override
	public void mouseReleased() {
		SketchHandler.instance.mouseReleased();
	}

	@Override
	public void mouseDragged() {
		SketchHandler.instance.mouseDragged();
	}

	@Override
	public void mouseMoved() {
		SketchHandler.instance.mouseMoved();
	}

	@Override
	public void keyPressed() {
		SketchHandler.instance.keyPressed();
	}

	@Override
	public void keyReleased() {
		SketchHandler.instance.keyReleased();
	}

	@Override
	public void keyTyped() {
		SketchHandler.instance.keyTyped();
	}
}
