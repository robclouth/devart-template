package io.diffuse.makar;

import io.diffuse.makar.ViewActivity.State;
import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.PowerManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.makar.Makar;

public class SketchHandler {
	public static SketchHandler instance;

	public Handler retryConnectHandler;
	public static boolean isSocketConnected = false;
	private int maxConnectionAttempts = 5;
	private int connectionAttempts = 0;

	private SocketIO socket;
	private static final String serverBaseAddress = "http://192.168.43.163:3000";
	private static final String socketSetVarMsg = "set variable";
	private static final String socketGetInitsMsg = "get inits";
	private static final String socketCallSyncedMethodMsg = "call method";

	public static final String sketchBaseUrl = String.format("%s/sketches", serverBaseAddress);
	public static final String sketchInfoUrl = sketchBaseUrl + "/%s/info";
	public static final String sketchDownloadUrl = sketchBaseUrl + "/%s/download";
	public static final String sketchDataFile = "sketch";

	public static String sketchesPath;

	private ViewActivity parent;
	private ProgressDialog progressDialog;

	public Sketch currSketch;

	Gson gson = new Gson();

	public SketchHandler(ViewActivity parent) {
		instance = this;

		this.parent = parent;
		Makar.pa = parent;

		sketchesPath = parent.getFilesDir().getAbsolutePath() + "/sketches/";

		connectionAttempts = 0;

		connect();
	}

	public void connect() {
		try {
			socket = new SocketIO(serverBaseAddress);
			socket.connect(new IOCallback() {
				@Override
				public void onMessage(JSONObject json, IOAcknowledge ack) {

				}

				@Override
				public void onMessage(String data, IOAcknowledge ack) {
				}

				@Override
				public void onError(final SocketIOException socketIOException) {
					parent.runOnUiThread(new Runnable() {
						public void run() {
							if (connectionAttempts < maxConnectionAttempts) {
								Toast.makeText(parent, "Server error. Retrying in 10 seconds.", Toast.LENGTH_LONG).show();
								retryConnectHandler = new Handler();
								retryConnectHandler.postDelayed(new Runnable() {
									@Override
									public void run() {
										connect();
									}
								}, 10000);

								connectionAttempts++;
							} else {
								Toast.makeText(parent, "Server unavailable. Try again later.", Toast.LENGTH_LONG).show();
							}
						}
					});
				}

				@Override
				public void onDisconnect() {
					isSocketConnected = false;
				}

				@Override
				public void onConnect() {
					isSocketConnected = true;
					connectionAttempts = 0;
				}

				@SuppressWarnings("unchecked")
				@Override
				public void on(String event, IOAcknowledge ack, Object... args) {
					if (event.equals(socketSetVarMsg)) {
						// set variable
						JSONObject json = (JSONObject) args[0];
						try {
							String varName = json.getString("name");
							String valueJsonString = json.getString("value");

							currSketch.setWatchedVariable(varName, valueJsonString);
						} catch (JSONException e) {
						}
					} else if (event.equals(socketGetInitsMsg)) {
						// get initial state
						JSONObject vars = (JSONObject) args[0];
						Iterator<String> iter = vars.keys();
						while (iter.hasNext()) {
							try {
								String varName = iter.next();
								String valueJsonString = vars.getString(varName);
								currSketch.setWatchedVariable(varName, valueJsonString);
							} catch (JSONException e) {
							}
						}
					} else if (event.equals(socketCallSyncedMethodMsg)) {
						currSketch.callSyncedMethod((JSONObject) args[0]);
					}

					// System.out.println("Server triggered event '" + event +
					// "'");
				}
			});
		} catch (MalformedURLException e) {
		}
	}

	public void initSketch() {
		if (!currSketch.init(parent)) {
			Toast.makeText(parent, "Error loading sketch", Toast.LENGTH_SHORT).show();
			parent.setState(State.Scanning);
		} else {
			socket.emit("room", currSketch.id);
			socket.emit("get inits", currSketch.id);
			currSketch.start();
			parent.setState(State.RunningSketch);
		}
	}

	public void getSketchInfo(String sketchId) {
		final GetInfoTask getInfoTask = new GetInfoTask();
		getInfoTask.execute(String.format(sketchInfoUrl, sketchId));
	}

	public void onConfirmDownload() {
		downloadSketch(currSketch.id);
	}

	public void onCancelDownload() {
		parent.setState(State.Scanning);
	}

	public void downloadSketch(String sketchId) {
		progressDialog = new ProgressDialog(parent);
		progressDialog.setMessage("Downloading...");
		progressDialog.setIndeterminate(true);
		progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progressDialog.setCancelable(true);

		final DownloadTask downloadTask = new DownloadTask(parent);
		downloadTask.execute(String.format(sketchDownloadUrl, sketchId));

		progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				downloadTask.cancel(true);

				Toast.makeText(parent, "Download canceled", Toast.LENGTH_SHORT).show();
			}
		});
	}

	public boolean isValidUrl(String urlString) {
		// if (urlString.startsWith(sketchBaseUrl)) {
		// String sketchId = urlString.replace(sketchBaseUrl, "");
		// try {
		// Integer.parseInt(sketchId);
		// } catch (NumberFormatException e) {
		// return false;
		// }
		// } else
		// return false;
		return true;
	}

	private class GetInfoTask extends AsyncTask<String, Integer, String> {
		@Override
		protected String doInBackground(String... sUrl) {
			String response = "";
			DefaultHttpClient client = new DefaultHttpClient();
			HttpParams params = client.getParams();
			HttpConnectionParams.setConnectionTimeout(params, 10000);
			HttpGet httpGet = new HttpGet(sUrl[0]);
			try {
				HttpResponse execute = client.execute(httpGet);
				InputStream content = execute.getEntity().getContent();

				BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
				String s = "";
				while ((s = buffer.readLine()) != null) {
					response += s;
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
			return response;
		}

		@Override
		protected void onPostExecute(String result) {
			try {
				JSONObject json = (JSONObject) new JSONTokener(result).nextValue();
				String sketchId = json.getString("id");
				String sketchTitle = json.getString("title");
				String sketchAuthor = json.getString("author");
				String sketchDescription = json.getString("description");
				String sketchCreated = "";// (String) json.get("createdAt");
				int sketchSize = json.getInt("size");
				String sketchClassName = "";// json.getString("classname");

				currSketch = new Sketch();
				currSketch.id = sketchId;
				currSketch.title = sketchTitle;
				currSketch.author = sketchAuthor;
				currSketch.description = sketchDescription;
				currSketch.size = sketchSize;
				currSketch.createdAt = sketchCreated;
				currSketch.className = sketchClassName;

				showConfirmDownloadDialog();
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

	public void showConfirmDownloadDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(parent);
		LayoutInflater inflater = parent.getLayoutInflater();
		View view = inflater.inflate(R.layout.confirm_download_dialog, null);

		((TextView) view.findViewById(R.id.sketchTitle)).setText(currSketch.title);
		((TextView) view.findViewById(R.id.sketchAuthor)).setText(currSketch.author);
		((TextView) view.findViewById(R.id.sketchDescription)).setText(currSketch.description);
		((TextView) view.findViewById(R.id.sketchSize)).setText("Size: " + currSketch.size + " bytes");

		builder.setView(view);
		builder.setPositiveButton("Download", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				onConfirmDownload();
			}
		});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				onCancelDownload();
			}
		});
		builder.setTitle("Confirm download");
		builder.create().show();
	}

	private class DownloadTask extends AsyncTask<String, Integer, String> {

		private Context context;
		private PowerManager.WakeLock wakeLock;

		public DownloadTask(Context context) {
			this.context = context;
		}

		@SuppressWarnings("resource")
		@Override
		protected String doInBackground(String... sUrl) {
			InputStream input = null;
			OutputStream output = null;
			HttpURLConnection connection = null;
			try {
				URL url = new URL(sUrl[0]);
				connection = (HttpURLConnection) url.openConnection();
				connection.connect();

				if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
					return "Server returned HTTP " + connection.getResponseCode() + " " + connection.getResponseMessage();
				}
				int fileLength = connection.getContentLength();
				input = connection.getInputStream();

				File outPath = new File(sketchesPath + currSketch.id);
				outPath.mkdirs();

				output = new FileOutputStream(outPath.getAbsolutePath() + "/" + sketchDataFile);

				byte data[] = new byte[4096];
				long total = 0;
				int count;
				while ((count = input.read(data)) != -1) {
					if (isCancelled()) {
						input.close();
						return null;
					}
					total += count;
					if (fileLength > 0)
						publishProgress((int) (total * 100 / fileLength));
					output.write(data, 0, count);
				}
			} catch (Exception e) {
				return e.toString();
			} finally {
				try {
					if (output != null)
						output.close();
					if (input != null)
						input.close();
				} catch (IOException ignored) {
				}

				if (connection != null)
					connection.disconnect();
			}
			return null;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			// take CPU lock to prevent CPU from going off if the user
			// presses the power button during download
			PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
			wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
			wakeLock.acquire();
			progressDialog.show();
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			super.onProgressUpdate(progress);
			// if we get here, length is known, now set indeterminate to false
			progressDialog.setIndeterminate(false);
			progressDialog.setMax(100);
			progressDialog.setProgress(progress[0]);
		}

		@Override
		protected void onPostExecute(String result) {
			wakeLock.release();
			progressDialog.dismiss();
			if (result != null) {
				Toast.makeText(context, "Download error: " + result, Toast.LENGTH_LONG).show();

			} else {
				initSketch();

			}
		}
	}

	public void sendVariable(String varJsonString, SharedVariable variable) {
		try {
			JSONObject jsonToSend = new JSONObject();
			jsonToSend.put("isPersistent", variable.isPersistent);
			jsonToSend.put("name", variable.field.getName());
			jsonToSend.put("value", varJsonString);
			socket.emit(socketSetVarMsg, jsonToSend);
		} catch (JSONException e) {
		}
	}

	public void callSyncedMethod(String methodName, Object[] params) {
		JSONObject jsonToSend = new JSONObject();
		try {
			jsonToSend.put("name", methodName);
			String paramJsonString = gson.toJson(params);
			JSONArray paramArray = (JSONArray) new JSONTokener(paramJsonString).nextValue();
			jsonToSend.put("params", paramArray);

			socket.emit(socketCallSyncedMethodMsg, jsonToSend);
		} catch (JSONException e) {
		}
	}

	public void mouseClicked() {
		if (currSketch != null && currSketch.isInitialised) {
			currSketch.updateInputVars();
			currSketch.child.mouseClicked();
		}
	}

	public void mousePressed() {
		if (currSketch != null && currSketch.isInitialised) {
			currSketch.updateInputVars();
			currSketch.child.mousePressed();
		}
	}

	public void mouseReleased() {
		if (currSketch != null && currSketch.isInitialised) {
			currSketch.updateInputVars();
			currSketch.child.mouseReleased();
		}
	}

	public void mouseDragged() {
		if (currSketch != null && currSketch.isInitialised) {
			currSketch.updateInputVars();
			currSketch.child.mouseDragged();
		}
	}

	public void mouseMoved() {
		if (currSketch != null && currSketch.isInitialised) {
			currSketch.updateInputVars();
			currSketch.child.mouseMoved();
		}
	}

	public void keyPressed() {
		if (currSketch != null && currSketch.isInitialised) {
			currSketch.updateInputVars();
			currSketch.child.keyPressed();
		}
	}

	public void keyReleased() {
		if (currSketch != null && currSketch.isInitialised) {
			currSketch.updateInputVars();
			currSketch.child.keyReleased();
		}
	}

	public void keyTyped() {
		if (currSketch != null && currSketch.isInitialised) {
			currSketch.updateInputVars();
			currSketch.child.keyTyped();
		}
	}
}
