package io.diffuse.makar;

import java.io.IOException;

import com.metaio.sdk.MetaioDebug;
import com.metaio.tools.io.AssetsManager;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

public final class MainActivity extends Activity {
public static MainActivity main;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		final Window win = getWindow();
		win.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		main = this;
		
		try {
			AssetsManager.extractAllAssets(getApplicationContext(), BuildConfig.DEBUG);
		} catch (IOException e) {
			MetaioDebug.log(Log.ERROR, "Error extracting application assets: " + e.getMessage());
		}
		
		setContentView(R.layout.main_layout);
//
//		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
//			Toast.makeText(MainActivity.this, "Reactag can't run while the device is connected via USB. Please disconnect and try again.", Toast.LENGTH_LONG)
//					.show();
//			finish();
//			return;
//		}
		
	}

	public void startViewActivity(View view) {
		Intent intent = new Intent(this, ViewActivity.class);
		startActivity(intent);
	}

	public void startCreateActivity(View view) {
		Intent intent = new Intent(this, CreateActivity.class);
		startActivity(intent);
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}
}
