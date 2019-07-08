package com.ilusons.notioff;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.anthonycr.grant.PermissionsManager;
import com.anthonycr.grant.PermissionsResultAction;

public class SplashActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// Set splash theme

		// This way (theme defined in styles.xml, with no layout of activity) makes loading faster as styles pre-applied
		// Then we wait while main view is created, finally exiting splash
		setTheme(R.style.SplashTheme);

		super.onCreate(savedInstanceState);

		// Check if all permissions re granted
		PermissionsManager.getInstance().requestAllManifestPermissionsIfNecessary(this,
				new PermissionsResultAction() {
					@Override
					public void onGranted() {
						start();
					}

					@Override
					public void onDenied(String permission) {
						Toast.makeText(SplashActivity.this, "Please grant all the required permissions :)", Toast.LENGTH_LONG).show();
					}
				});
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		boolean should = true;
		for (int i = 0; i < permissions.length; i++) {
			String permission = permissions[i];
			int grantResult = grantResults[i];

			should &= grantResult == PackageManager.PERMISSION_GRANTED;
		}

		if (should)
			start();

	}

	private void start() {
		// Init realm
		RealmEx.get(this);

		// Start main
		Intent intent = new Intent(SplashActivity.this, MainActivity.class);
		startActivity(intent);
		finish();
	}

}
