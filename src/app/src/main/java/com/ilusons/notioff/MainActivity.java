package com.ilusons.notioff;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.anthonycr.grant.PermissionsManager;

import java.util.ArrayList;
import java.util.Collection;

import io.realm.Realm;

public class MainActivity extends AppCompatActivity
		implements NavigationView.OnNavigationItemSelectedListener {

	private static final String TAG = MainActivity.class.getSimpleName();

	static {
		AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
		AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
	}

	protected Realm realm;

	private Handler handler;

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// below line to be commented to prevent crash on nougat.
		// http://blog.sqisland.com/2016/09/transactiontoolargeexception-crashes-nougat.html
		//
		super.onSaveInstanceState(outState);
	}

	private RecyclerView recyclerView;
	private RecyclerViewAdapter adapter;

	private ImageButton all_status;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			public void uncaughtException(Thread paramThread, Throwable paramThrowable) {
				Log.e(TAG, "[app_crash]", paramThrowable);
			}
		});

		realm = RealmEx.get(this);

		handler = new Handler();

		// startService(new Intent(this, Service.class));

		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayShowTitleEnabled(false);

		FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				showCreateProfile();
			}
		});

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
				this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
		drawer.addDrawerListener(toggle);
		toggle.syncState();

		NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
		navigationView.setNavigationItemSelectedListener(this);

		checkRequirements();

		adapter = new RecyclerViewAdapter();

		recyclerView = findViewById(R.id.recyclerView);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));
		recyclerView.setAdapter(adapter);

		adapter.refresh();

		all_status = findViewById(R.id.all_status);
		updateGlobalStatus();
		all_status.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (Service.getTurnOffGloballyEnabled(MainActivity.this)) {
					Service.setTurnOffGloballyEnabled(MainActivity.this, false);

					info("Global blocking is OFF now.");
				} else {
					Service.setTurnOffGloballyEnabled(MainActivity.this, true);

					info("Global blocking is ON now.");
				}
				updateGlobalStatus();
			}
		});

	}

	public void setStatusBarColor(View statusBar, int color) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			Window w = getWindow();
			w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			//status bar height
			int actionBarHeight = getActionBarHeight();
			int statusBarHeight = getStatusBarHeight();
			//action bar height
			statusBar.getLayoutParams().height = actionBarHeight + statusBarHeight;
			statusBar.setBackgroundColor(color);
		}
	}

	public int getActionBarHeight() {
		int actionBarHeight = 0;
		TypedValue tv = new TypedValue();
		if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
			actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
		}
		return actionBarHeight;
	}

	public int getStatusBarHeight() {
		int result = 0;
		int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			result = getResources().getDimensionPixelSize(resourceId);
		}
		return result;
	}

	protected void setStatusBarTranslucent(boolean makeTranslucent) {
		View v = findViewById(R.id.toolbar);
		if (v != null) {
			int paddingTop = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ? getStatusBarHeight() : 0;
			TypedValue tv = new TypedValue();
			getTheme().resolveAttribute(android.support.v7.appcompat.R.attr.actionBarSize, tv, true);
			paddingTop += TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
			v.setPadding(0, makeTranslucent ? paddingTop : 0, 0, 0);
		}

		if (makeTranslucent) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
		} else {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		realm.close();
	}

	@Override
	public void onBackPressed() {
		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		if (drawer.isDrawerOpen(GravityCompat.START)) {
			drawer.closeDrawer(GravityCompat.START);
		} else {
			super.onBackPressed();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
//		if (id == R.id.action_settings) {
//			return true;
//		}

		return super.onOptionsItemSelected(item);
	}

	@SuppressWarnings("StatementWithEmptyBody")
	@Override
	public boolean onNavigationItemSelected(MenuItem item) {
		// Handle navigation view item clicks here.
		int id = item.getItemId();

		if (id == R.id.nav_create) {
			showCreateProfile();
		} else if (id == R.id.nav_help) {
			(new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AppTheme_Dialog))
					.setTitle("Help")
					.setMessage("NotiOff is simple tool to get rid of notifications."
							+ "\n"
							+ "\n- Turn off for selected apps"
							+ "\n1. Create a profile"
							+ "\n2. Add apps to profile"
							+ "\n3. Set it active"
							+ "\n"
							+ "\n- Or turn off notifications globally"
							+ "\n"
							+ "\nNote: Make sure app is enabled in Notification settings."
					)
					.setCancelable(true)
					.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							dialogInterface.dismiss();
						}
					}))
					.show();
		} else if (id == R.id.nav_exit) {
			android.os.Process.killProcess(android.os.Process.myPid());
		} else if (id == R.id.nav_contact) {
			final String email = "notioff@ilusons.com";
			try {
				Intent intent = new Intent(Intent.ACTION_SENDTO);
				intent.setType("text/plain");
				intent.putExtra(Intent.EXTRA_SUBJECT, "[#" + getString(R.string.app_name) + " #feedback #android #" + BuildConfig.VERSION_NAME + "]");
				intent.putExtra(Intent.EXTRA_TEXT, "");
				intent.setData(Uri.parse("mailto:"));
				intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
			} catch (Exception e) {
				e.printStackTrace();

				Toast.makeText(this, "Unable to open your email handler. Please send email to [" + email + "] instead.", Toast.LENGTH_LONG).show();
			}
		}

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawer.closeDrawer(GravityCompat.START);
		return true;
	}


	@Override
	protected void onStop() {
		super.onStop();

		hideProgressDialog();
	}

	public void info(String s) {
		Toast.makeText(this, s, Toast.LENGTH_LONG).show();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		PermissionsManager.getInstance().notifyPermissionsChange(permissions, grantResults);
	}

	@VisibleForTesting
	public ProgressDialog progressDialog;

	public ProgressDialog showProgressDialog() {
		if (progressDialog == null) {
			progressDialog = new ProgressDialog(this);
			progressDialog.setTitle("");
			progressDialog.setMessage("");
			progressDialog.getProgress().setIndeterminate(true);
			progressDialog.setCancelable(false);
			progressDialog.setCanceledOnTouchOutside(false);
			progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
		}

		progressDialog.show();

		return progressDialog;
	}

	public void hideProgressDialog() {
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
	}

	public ProgressDialog getProgressDialog() {
		if (progressDialog == null || !progressDialog.isShowing()) {
			showProgressDialog();
		}

		return progressDialog;
	}

	private void checkRequirements() {

		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (!Service.isRunning(MainActivity.this)) {
					Toast.makeText(MainActivity.this, "Please enable this app.", Toast.LENGTH_LONG).show();

					Service.toggleNotificationListenerService(MainActivity.this);

					startActivity(Service.getIntentNotificationListenerSettings());
				} else {
					Toast.makeText(MainActivity.this, "Service is up and working!", Toast.LENGTH_LONG).show();
				}
			}
		}, 5 * 1000);

	}

	private void updateGlobalStatus() {
		if (Service.getTurnOffGloballyEnabled(this)) {
			all_status.setImageDrawable(getDrawable(R.drawable.ic_notifications_off_black));
			all_status.setColorFilter(new PorterDuffColorFilter(getColor(android.R.color.holo_red_light), PorterDuff.Mode.SRC_ATOP));
			recyclerView.animate().alpha(0.4f).setDuration(777).start();
		} else {
			all_status.setImageDrawable(getDrawable(R.drawable.ic_notifications_active_black));
			all_status.setColorFilter(new PorterDuffColorFilter(getColor(android.R.color.holo_green_light), PorterDuff.Mode.SRC_ATOP));
			recyclerView.animate().alpha(1f).setDuration(777).start();
		}
	}

	private void showCreateProfile() {
		AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AppTheme_Dialog));
		builder.setTitle("Create new profile");

		final EditText input = new EditText(this);

		builder.setView(input);

		builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Profile.create(realm, input.getText().toString().trim());

				adapter.refresh();

				info("Profile created and added, set it active after adding apps!");
			}
		});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});

		builder.show();
	}

	public class RecyclerViewAdapter extends RecyclerView.Adapter<ViewHolder> {

		private ArrayList<Profile> data;
		private String dataDefault;

		public RecyclerViewAdapter() {
			data = new ArrayList<>();
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.profile, parent, false);

			return new ViewHolder(view);
		}

		@Override
		public void onBindViewHolder(final ViewHolder holder, int position) {
			final Profile d = data.get(position);
			final View v = holder.view;

			final ProgressBar progress = v.findViewById(R.id.progress);

			TextView title = v.findViewById(R.id.title);
			title.setText(d.getTitle());

			final ImageButton set_active = v.findViewById(R.id.set_active);
			if (!TextUtils.isEmpty(dataDefault)) {
				if (d.getTitle().equals(dataDefault))
					set_active.setImageDrawable(getDrawable(R.drawable.ic_pause_circle_outline_black));
				else
					set_active.setImageDrawable(getDrawable(R.drawable.ic_check_black));

				if (d.getTitle().equals(dataDefault))
					progress.setVisibility(View.VISIBLE);
				else
					progress.setVisibility(View.GONE);
			}
			set_active.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					String activeProfileKey = Profile.getActiveProfileKey(MainActivity.this);
					if (!TextUtils.isEmpty(activeProfileKey) && activeProfileKey.equals(d.getTitle())) {
						Profile.setActiveProfileKey(MainActivity.this, null);
						set_active.setImageDrawable(getDrawable(R.drawable.ic_check_black));
						progress.setVisibility(View.GONE);
					} else {
						Profile.setActiveProfileKey(MainActivity.this, d.getTitle());
						set_active.setImageDrawable(getDrawable(R.drawable.ic_pause_circle_outline_black));
						progress.setVisibility(View.VISIBLE);
					}
					refresh();

					info("Active profile updated!");
				}
			});

			ImageButton remove = v.findViewById(R.id.remove);
			remove.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					Profile.remove(realm, d);
					refresh();

					info("Profile removed!");
				}
			});

			ImageButton add = v.findViewById(R.id.add);
			add.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					(new PackagesDialog(new ContextThemeWrapper(MainActivity.this, R.style.AppTheme_Dialog), new PackagesDialog.Events() {
						@Override
						public void itemsSelected(final Collection<ProfileItem> items) {
							d.setItems(items);
							realm.executeTransaction(new Realm.Transaction() {
								@Override
								public void execute(@NonNull Realm realm) {
									realm.insertOrUpdate(items);
									realm.insertOrUpdate(d);
								}
							});
							refresh();

							info("Profile updated, you'll need to force restart for full update!");
						}
					}, d)).show();
				}
			});

			RecyclerView recyclerView = v.findViewById(R.id.recyclerView);
			recyclerView.setLayoutManager(new LinearLayoutManager(v.getContext()));
			ProfileItemsRecyclerViewAdapter adapter = new ProfileItemsRecyclerViewAdapter();
			recyclerView.setAdapter(adapter);

			adapter.reset(d.getItems());
		}

		@Override
		public int getItemCount() {
			return data.size();
		}

		public void refresh() {
			try {
				data.clear();
				data.addAll(realm.copyFromRealm(Profile.getAll(realm)));

				dataDefault = Profile.getActiveProfileKey(MainActivity.this);
			} catch (Exception e) {
				Log.w(TAG, e);
			}

			notifyDataSetChanged();
		}

	}

	public class ViewHolder extends RecyclerView.ViewHolder {
		public View view;

		public ViewHolder(View view) {
			super(view);

			this.view = view;
		}

	}

	public class ProfileItemsRecyclerViewAdapter extends RecyclerView.Adapter<ProfileItemsRecyclerViewAdapter.ViewHolder> {

		private ArrayList<ProfileItem> data;

		public ProfileItemsRecyclerViewAdapter() {
			data = new ArrayList<>();
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.profile_item, parent, false);

			return new ViewHolder(view);
		}

		@Override
		public void onBindViewHolder(final ViewHolder holder, int position) {
			final ProfileItem d = data.get(position);
			final View v = holder.view;

			TextView text1 = v.findViewById(R.id.text1);
			text1.setText(d.getTitle());

			TextView text2 = v.findViewById(R.id.text2);
			text2.setText(d.getPackage());

			ImageView image = v.findViewById(R.id.image);
			Drawable icon = d.getIcon(v.getContext());
			if (icon != null)
				image.setImageDrawable(icon);
			else
				image.setImageDrawable(v.getContext().getDrawable(R.mipmap.ic_launcher));

		}

		@Override
		public int getItemCount() {
			return data.size();
		}

		public void reset(Collection<ProfileItem> items) {
			try {
				data.clear();
				data.addAll(items);
			} catch (Exception e) {
				Log.w(TAG, e);
			}

			notifyDataSetChanged();
		}

		public class ViewHolder extends RecyclerView.ViewHolder {
			public View view;

			public ViewHolder(View view) {
				super(view);

				this.view = view;
			}

		}

	}

	public static class ProgressDialog extends Dialog {

		public ProgressDialog(Context context) {
			super(context, R.style.AppTheme_ProgressDialog);

			setContentView(R.layout.progress_dialog);
		}

		public void setMessage(String s) {
			((TextView) findViewById(R.id.text)).setText(s);
		}

		public CharSequence getMessage() {
			return ((TextView) findViewById(R.id.text)).getText();
		}

		public ProgressBar getProgress() {
			return ((ProgressBar) findViewById(R.id.progress));
		}

	}

}
