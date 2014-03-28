package io.diffuse.makar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import processing.core.PApplet;
import android.annotation.SuppressLint;

import com.google.gson.Gson;
import com.makar.Persistent;
import com.makar.Shared;
import com.makar.Synced;

import dalvik.system.DexClassLoader;

public class Sketch {
	private static final String trackingConfigString = "<?xml version=\"1.0\"?><TrackingData><Sensors><Sensor Type=\"FeatureBasedSensorSource\" Subtype=\"ML3D\"><SensorID>FeatureTracking1</SensorID><Parameters><FeatureOrientationAssignment>regular</FeatureOrientationAssignment></Parameters><SensorCOS><SensorCosID>Map3D</SensorCosID><Parameters><NumExtensibleFeatures>100000</NumExtensibleFeatures><MinTriangulationAngle>8</MinTriangulationAngle><Map>%map%</Map></Parameters></SensorCOS></Sensor></Sensors><Connections><COS><Name>COS0</Name><Fuser Type=\"SmoothingFuser\"><Parameters><AlphaRotation>0.5</AlphaRotation><AlphaTranslation>0.8</AlphaTranslation><GammaRotation>0.5</GammaRotation><GammaTranslation>0.8</GammaTranslation><KeepPoseForNumberOfFrames>1</KeepPoseForNumberOfFrames></Parameters></Fuser><SensorSource><SensorID>FeatureTracking1</SensorID><SensorCosID>Map3D</SensorCosID></SensorSource></COS></Connections></TrackingData>";

	Gson gson = new Gson();
	PApplet parent, child;
	public String id, title, author, description, createdAt, className;
	public int size;
	File configFile;

	HashMap<String, SharedVariable> sharedVariables;
	HashMap<String, SyncedMethod> syncedMethods;
	
	boolean isInitialised = false;

	@SuppressLint("NewApi")
	public boolean init(final PApplet parent) {
		this.parent = parent;

		try {
			final String sketchPath = SketchHandler.sketchesPath + id;

			unzipSketchFile(sketchPath);
			copyDataFiles(sketchPath);
			createTrackingConfig(sketchPath);
			
			String sketchName = (new File(sketchPath).list(new FilenameFilter() {
				public boolean accept(File path, String name) {
					return name.endsWith(".jar");
				}
			}))[0].replace(".jar", "");

			// make jar list
			String jarPathString = sketchPath + File.separator + sketchName + ".jar";
			File libsPath = new File(sketchPath + File.separator + "libs");

			if (libsPath.exists()) {
				jarPathString += File.pathSeparator;

				File[] libJars = libsPath.listFiles();
				for (File jarFile : libJars) {
					jarPathString += jarFile.getAbsolutePath() + File.pathSeparator;
				}
			}

			final File dexDir = parent.getDir(id + "_dex", 0);
			final DexClassLoader classloader = new DexClassLoader(jarPathString, dexDir.getAbsolutePath(), null, parent.getClassLoader());

			Class<?> sketchClazz = classloader.loadClass(sketchName);
			child = (PApplet) sketchClazz.newInstance();

			initSharedVariables();
			initSyncedMethods();
			
			setupInputListeners();

			isInitialised = true;
		} catch (Exception e) {
			return false;
		}

		return true;
	}
	
	private void setupInputListeners(){
		parent.registerMethod("mousePressed", this);
		//parent.registerMethod("mouseReleased", this);
		//parent.registerMethod("mouseDragged", this);
		//parent.registerMethod("mouseClicked", this);
		//parent.registerMethod("mouseMoved", this);
		//parent.registerMethod("keyPressed", this);
		//parent.registerMethod("keyReleased", this);
		//parent.registerMethod("keyTyped", this);

	}
	
	public void mousePressed(){
		child.mouseX = parent.mouseX;
		child.mouseY = parent.mouseY;
		child.mousePressed();
	}
	
	private void createTrackingConfig(String sketchPath){
		File mapFile = new File(sketchPath + File.separator + "map");
		String configString = trackingConfigString.replace("%map%", mapFile.getAbsolutePath());
		configFile = new File(sketchPath + File.separator + "config.xml");
		try {
			PrintWriter out = new PrintWriter(configFile);
			out.print(configString);
			out.close();
		} catch (FileNotFoundException e) {
		}
	}

	private void unzipSketchFile(String sketchPath) throws IOException {
		byte[] buffer = new byte[1024];

		try {

			ZipInputStream zis = new ZipInputStream(new FileInputStream(sketchPath + File.separator + SketchHandler.sketchDataFile));
			ZipEntry ze = zis.getNextEntry();

			while (ze != null) {

				String fileName = ze.getName();
				File newFile = new File(sketchPath + File.separator + fileName);

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

	private void copyDataFiles(String sketchPath) throws IOException {
		File childDataPath = new File(sketchPath + "/data");
		childDataPath.mkdirs();
		final ArrayList<File> dataFiles = new ArrayList<File>();
		search(childDataPath.getAbsolutePath(), dataFiles);

		for (File src : dataFiles) {
			File dst = new File(parent.sketchPath(src.getAbsolutePath().replace(childDataPath.getAbsolutePath() + "/", "")));
			copy(src, dst);
		}
	}

	private void copy(File src, File dst) throws IOException {
		dst.getParentFile().mkdirs();
		InputStream in = new FileInputStream(src);
		OutputStream out = new FileOutputStream(dst);

		// Transfer bytes from in to out
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
	}

	private void search(String path, ArrayList<File> foundFiles) {
		File root = new File(path);
		File[] list = root.listFiles();

		for (File f : list) {
			if (f.isDirectory())
				search(f.getAbsolutePath(), foundFiles);
			else
				foundFiles.add(f);
		}
	}

	public void initSharedVariables() {
		sharedVariables = new HashMap<String, SharedVariable>();

		Field[] fields = child.getClass().getDeclaredFields();
		for (Field field : fields) {
			boolean isShared = field.getAnnotation(Shared.class) != null;
			boolean isPersistent = field.getAnnotation(Persistent.class) != null;

			if (isShared) {
				field.setAccessible(true);
				SharedVariable var = new SharedVariable(child, field, isPersistent);
				sharedVariables.put(var.field.getName(), var);
			}
		}
	}

	public void updateSharedVariables() {
		if (SketchHandler.isSocketConnected) {
			for (SharedVariable var : sharedVariables.values()) {
				var.sendChanges();
			}
		}
	}
	
	public void setWatchedVariable(String varName, String valueJsonString) {
		SharedVariable var = sharedVariables.get(varName);
		var.setValueFromJson(valueJsonString);
	}
	
	public void initSyncedMethods() {
		syncedMethods = new HashMap<String, SyncedMethod>();

		Method[] methods = child.getClass().getDeclaredMethods();
		for (Method method : methods) {
			boolean isSynced = method.getAnnotation(Synced.class) != null;
		
			if (isSynced) {
				SyncedMethod syncedMethod = new SyncedMethod(child, method);
				syncedMethods.put(syncedMethod.method.getName(), syncedMethod);
			}
		}
	}
	
	public void callSyncedMethod(JSONObject jsonObject) {
		try {
			String methodName = jsonObject.getString("name");
			JSONArray paramsArray = jsonObject.getJSONArray("params");
			Object[] paramObjects = new Object[paramsArray.length()];
			for (int i = 0; i < paramsArray.length(); i++)
				paramObjects[i] = paramsArray.get(i);
			//syncedMethods.get
		} catch (JSONException e) {
		}

	}

	public void start() {
		parent.runOnUiThread(new Runnable() {
			public void run() {
				child.g = parent.g;
				child.noLoop();
				child.setup();
				child.start();
			}
		});
	}
	
	public void updateInputVars(){
		child.mousePressed = parent.mousePressed;
		child.keyPressed = parent.keyPressed;
		child.key = parent.key;
		child.keyCode = parent.keyCode;
		child.mouseX = parent.mouseX;
		child.mouseY = parent.mouseY;
		child.pmouseX = parent.pmouseX;
		child.pmouseY = parent.pmouseY;
	}

	public void update() {
		if (isInitialised) {
			child.frameCount++;
			child.frameRate = parent.frameRate;
			
			updateInputVars();
			
			updateSharedVariables();
			
			child.draw();
		}
	}


}
