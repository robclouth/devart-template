package io.diffuse.makar;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.TextView;
import android.widget.Toast;

import com.metaio.sdk.jni.IGeometry;
import com.metaio.sdk.jni.IMetaioSDKCallback;
import com.metaio.sdk.jni.ImageStruct;
import com.metaio.sdk.jni.Rotation;
import com.metaio.sdk.jni.TrackingValues;
import com.metaio.sdk.jni.Vector3d;
import com.metaio.tools.io.AssetsManager;

public class CreateActivity extends ARViewActivity {
	enum State {
		TrackScene, TakePhotos, Finished, AdjustSketch
	};

	private State state;

	private MetaioSDKCallbackHandler mCallbackHandler;

	boolean mPreview = true;

	TextView trackingViewInfoText, photoViewInfoText, finishedViewInfoText;
	View startTrackingButton, trackingViewNextButton, takePhotoButton, trackingView, photoView, photoViewNextButton, adjustmentView, finishedView;
	View trackingViewInstructions, photoViewInstructions;

	String sketchName = "MakarTest_" + (int) (Math.random() * 100000000);
	String trackingFilesPath, currTrackingConfig, currMapFile;

	boolean isTakingPhoto = false;
	String photoCachePath;
	int minPhotos = 3;
	int maxPhotos = 10;
	TrackedPhoto currPhoto;
	ArrayList<TrackedPhoto> trackedPhotos;

	IGeometry cube;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		final Window win = getWindow();
		win.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		mCallbackHandler = new MetaioSDKCallbackHandler();

		trackedPhotos = new ArrayList<TrackedPhoto>();
		photoCachePath = getFilesDir() + "/photos";
		trackingFilesPath = Environment.getExternalStorageDirectory() + "/makar/tracking/";

		trackingViewInfoText = (TextView) mGUIView.findViewById(R.id.trackingViewInfoText);
		startTrackingButton = mGUIView.findViewById(R.id.startTrackingButton);
		trackingViewNextButton = mGUIView.findViewById(R.id.trackingViewNextButton);

		photoViewInfoText = (TextView) mGUIView.findViewById(R.id.photoViewInfoText);
		takePhotoButton = mGUIView.findViewById(R.id.takePhotoButton);
		photoViewNextButton = mGUIView.findViewById(R.id.photoViewNextButton);

		finishedViewInfoText = (TextView) mGUIView.findViewById(R.id.finishedViewInfoText);

		trackingView = mGUIView.findViewById(R.id.trackingView);
		photoView = mGUIView.findViewById(R.id.photoView);
		finishedView = mGUIView.findViewById(R.id.finishedView);

		trackingViewInstructions = mGUIView.findViewById(R.id.trackingViewInstructions);
		photoViewInstructions = mGUIView.findViewById(R.id.photoViewInstructions);

		setState(State.TrackScene);
	}

	@Override
	public void onBackPressed() {
		if (state == State.TrackScene) {
			super.onBackPressed();
		} else if (state == State.TakePhotos) {
			setState(State.TrackScene);
		} else if (state == State.Finished) {
			finish();
		} else if (state == State.AdjustSketch) {

		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mCallbackHandler.delete();
		mCallbackHandler = null;
	}

	@Override
	protected int getGUILayout() {
		return R.layout.create_view_layout;
	}

	@Override
	protected IMetaioSDKCallback getMetaioSDKCallbackHandler() {
		return mCallbackHandler;
	}

	public void onStartTrackingClick(View v) {
		metaioSDK.startInstantTracking("INSTANT_3D");
		trackingViewInfoText.setText("Tracking...");
		startTrackingButton.setVisibility(View.INVISIBLE);
	}

	public void onTakePhotoClick(View v) {
		if (!isTakingPhoto && trackedPhotos.size() < maxPhotos) {
			final TrackingValues trackingValues = metaioSDK.getTrackingValues(1);
			if (trackingValues.isTrackingState()) {
				isTakingPhoto = true;
				currPhoto = new TrackedPhoto();

				float[] modelViewMat = new float[16];
				metaioSDK.getTrackingValues(1, modelViewMat, true, true);
				currPhoto.modelViewMat = modelViewMat;

				float[] projectionMat = new float[16];
				metaioSDK.getProjectionMatrix(projectionMat, true);
				currPhoto.projectionMat = projectionMat;

				metaioSDK.requestCameraImage();

			} else {
				Toast.makeText(CreateActivity.this, "Tracking lost.", Toast.LENGTH_LONG).show();
			}
		}
	}

	public void onTrackingViewInstructionsOkClick(View v) {
		fadeOutView(trackingViewInstructions);
	}

	public void onPhotoViewInstructionsOkClick(View v) {
		fadeOutView(photoViewInstructions);
	}

	public void onTrackingViewNextClick(View v) {
		setState(State.TakePhotos);
	}

	public void onPhotoViewNextClick(View v) {
		showConfirmFinishDialog();
	}

	public void showConfirmFinishDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setPositiveButton("Save and finish", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				setState(State.Finished);
			}
		});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {

			}
		});
		builder.setTitle("Save and finish?");
		builder.create().show();
	}

	void fadeOutView(final View view) {
		AlphaAnimation anim = new AlphaAnimation(1.0f, 0.0f);
		anim.setDuration(200);
		anim.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationEnd(Animation animation) {
				view.setVisibility(View.INVISIBLE);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationStart(Animation animation) {
			}
		});
		view.startAnimation(anim);
	}

	final class MetaioSDKCallbackHandler extends IMetaioSDKCallback {
		@Override
		public void onSDKReady() {
			// show GUI
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mGUIView.setVisibility(View.VISIBLE);
				}
			});
		}

		@Override
		public void onNewCameraFrame(ImageStruct cameraFrame) {
			final Bitmap frameBitmap = cameraFrame.getBitmap();
			Thread thread = new Thread() {
				public void run() {
					try {
						File outFile = new File(photoCachePath + "/" + trackedPhotos.size() + ".jpg");
						outFile.getParentFile().mkdirs();
						outFile.getParentFile().exists();
						OutputStream output = new BufferedOutputStream(new FileOutputStream(outFile), 16 * 1024);

						Matrix matrix = new Matrix();
						matrix.postRotate(90);
						Bitmap rotatedBitmap = Bitmap.createBitmap(frameBitmap, 0, 0, frameBitmap.getWidth(), frameBitmap.getHeight(), matrix, true);
						rotatedBitmap.compress(CompressFormat.JPEG, 50, output);
						output.flush();
						output.close();

						isTakingPhoto = false;
						trackedPhotos.add(currPhoto);

						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								photoViewInfoText.setText(String.format("%s/%s", trackedPhotos.size(), maxPhotos));

								if (trackedPhotos.size() > minPhotos)
									photoViewNextButton.setVisibility(View.VISIBLE);

								if (trackedPhotos.size() == maxPhotos)
									takePhotoButton.setVisibility(View.INVISIBLE);
							}
						});
					} catch (FileNotFoundException e) {

					} catch (IOException e) {

					}
				}
			};
			thread.start();
		}

		@Override
		public void onInstantTrackingEvent(boolean success, final String file) {
			if (success) {
				// cube.setVisible(true);
				runOnUiThread(new Runnable() {
					public void run() {
						currTrackingConfig = file;
						currMapFile = new File(file).getParentFile() + "/map0.f3b";
						metaioSDK.setTrackingConfiguration(file);
						trackingViewInfoText.setText("Tracking success.");
						startTrackingButton.setVisibility(View.VISIBLE);
						trackingViewNextButton.setVisibility(View.VISIBLE);
						
						try {
							BufferedReader reader = new BufferedReader(new FileReader(file));
							String line = null;
							StringBuilder stringBuilder = new StringBuilder();
							String ls = System.getProperty("line.separator");

							while ((line = reader.readLine()) != null) {
								stringBuilder.append(line);
								stringBuilder.append(ls);
							}

							reader.close();
						} catch (FileNotFoundException e1) {

						} catch (IOException e) {

						}
					}
				});
			} else {
				runOnUiThread(new Runnable() {
					public void run() {
						trackingViewInfoText.setText("Tracking failed.");
						startTrackingButton.setVisibility(View.VISIBLE);
						Toast.makeText(CreateActivity.this, "Tracking unsuccessful. Try a more detailed scene.", Toast.LENGTH_LONG).show();
					}
				});
			}
		}
	}

	void setState(State newState) {
		state = newState;

		if (state == State.TrackScene) {
			trackingViewNextButton.setVisibility(View.INVISIBLE);
			photoView.setVisibility(View.INVISIBLE);
			finishedView.setVisibility(View.INVISIBLE);
			startTrackingButton.setVisibility(View.VISIBLE);
			trackingView.setVisibility(View.VISIBLE);
			trackingViewInstructions.setVisibility(View.VISIBLE);
			trackingViewInfoText.setText("");
		} else if (state == State.TakePhotos) {
			trackedPhotos.clear();
			trackingView.setVisibility(View.INVISIBLE);
			finishedView.setVisibility(View.INVISIBLE);
			photoView.setVisibility(View.VISIBLE);
			photoViewInstructions.setVisibility(View.VISIBLE);
			photoViewNextButton.setVisibility(View.INVISIBLE);
			photoViewInfoText.setText(String.format("%s/%s", trackedPhotos.size(), maxPhotos));
		} else if (state == State.Finished) {
			photoView.setVisibility(View.INVISIBLE);
			trackingView.setVisibility(View.INVISIBLE);
			finishedView.setVisibility(View.VISIBLE);
			finishedViewInfoText.setText("Saving...");
			new SaveTask().execute((Void) null);
		} else if (state == State.AdjustSketch) {

		}
	}

	enum SaveResult {
		Success, IOError, ServerError
	};

	public class SaveTask extends AsyncTask<Void, Void, SaveResult> {
		@Override
		protected SaveResult doInBackground(Void... no) {
			try {
				// CREATE ZIP FILE
				byte[] buf = new byte[1024];
				InputStream in;
				File trackingFile = new File(trackingFilesPath + "/" + sketchName + ".tracking");
				trackingFile.getParentFile().mkdirs();
				ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(trackingFile));
				int len;

				// add tracking config file
				in = new FileInputStream(currTrackingConfig);
				zip.putNextEntry(new ZipEntry("config"));
				while ((len = in.read(buf)) > 0)
					zip.write(buf, 0, len);
				zip.closeEntry();
				in.close();

				// add map data
				in = new FileInputStream(currMapFile);
				zip.putNextEntry(new ZipEntry("map"));
				while ((len = in.read(buf)) > 0)
					zip.write(buf, 0, len);
				zip.closeEntry();
				in.close();

				JSONArray trackedPhotosJsonArray = new JSONArray();
				// add tracked photos
				for (int i = 0; i < trackedPhotos.size(); i++) {
					File photoFile = new File(photoCachePath + "/" + i + ".jpg");
					in = new FileInputStream(photoFile);
					zip.putNextEntry(new ZipEntry(i + ".jpg"));
					while ((len = in.read(buf)) > 0)
						zip.write(buf, 0, len);
					zip.closeEntry();
					in.close();

					TrackedPhoto photo = trackedPhotos.get(i);
					JSONObject trackedPhotoJson = new JSONObject();
					JSONArray modelViewMatrix = new JSONArray();
					JSONArray projectionMatrix = new JSONArray();
					for (int j = 0; j < 16; j++) {
						modelViewMatrix.put(photo.modelViewMat[j]);
						projectionMatrix.put(photo.projectionMat[j]);
					}

					trackedPhotoJson.put("imageFilename", i + ".jpg");
					trackedPhotoJson.put("modelViewMatrix", modelViewMatrix);
					trackedPhotoJson.put("projectionMatrix", projectionMatrix);
					trackedPhotosJsonArray.put(trackedPhotoJson);
				}
				JSONObject trackedPhotosJson = new JSONObject();
				trackedPhotosJson.put("trackedPhotos", trackedPhotosJsonArray);

				// add tracked photos json
				in = new ByteArrayInputStream(trackedPhotosJson.toString(2).getBytes("UTF-8"));
				zip.putNextEntry(new ZipEntry("trackedPhotos"));
				while ((len = in.read(buf)) > 0)
					zip.write(buf, 0, len);
				zip.closeEntry();
				in.close();

				zip.close();

				for (int i = 0; i < trackedPhotos.size(); i++) {
					File photoFile = new File(photoCachePath + "/" + i + ".jpg");
					photoFile.delete();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			return SaveResult.Success;
		}

		@Override
		protected void onPostExecute(final SaveResult result) {
			if (result == SaveResult.Success) {
				finishedViewInfoText.setText("Saved.");

				final Handler handler = new Handler();
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						finish();
					}
				}, 2000);
			}
		}

		@Override
		protected void onCancelled() {

		}
	}

	@Override
	protected void loadContents() {
		String modelPath = AssetsManager.getAssetPath(getApplicationContext(), "box.obj");
		cube = metaioSDK.createGeometry(modelPath);
		cube.setScale(30f);
		cube.setRotation(new Rotation((float) -Math.PI / 2, 0, (float) Math.PI / 2));
		cube.setTranslation(new Vector3d(0, 15, 0));
		cube.setVisible(true);
	}

	@Override
	protected void onGeometryTouched(IGeometry geometry) {
		// TODO Auto-generated method stub

	}

	class TrackedPhoto {
		String imagePath;
		float[] modelViewMat, projectionMat;
	}
}