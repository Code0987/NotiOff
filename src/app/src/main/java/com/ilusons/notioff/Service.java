package com.ilusons.notioff;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class Service extends NotificationListenerService {

	private static String TAG = Service.class.getSimpleName();

	@Override
	public void onCreate() {
		super.onCreate();

		try {
			if (getTurnOffGloballyEnabled(this)) {
				notify("Started, active for all");
			} else {
				Profile profile = Profile.getActiveProfile(this);
				if (profile == null)
					throw new Exception();

				notify("Started, active [" + profile.getTitle() + "]");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void onDestroy() {
		try {
			NotificationManager notificationManager =
					(NotificationManager) getSystemService(NOTIFICATION_SERVICE);

			notificationManager.cancelAll();
		} catch (Exception e) {
			e.printStackTrace();
		}

		super.onDestroy();
	}

	@Override
	public void onNotificationPosted(StatusBarNotification sbn) {
		Log.d(TAG, "onNotificationPosted\n" + sbn);

		try {
			if (!(!sbn.isOngoing() && sbn.isClearable()) || sbn.getPackageName().equals(Service.class.getPackage().getName()))
				return;

			boolean should = false;

			if (getTurnOffGloballyEnabled(this)) {
				should = true;
			} else {
				Profile profile = Profile.getActiveProfile(this);
				try {
					if (profile == null)
						throw new Exception();

					notify("Active [" + profile.getTitle() + "]");
				} catch (Exception e) {
					e.printStackTrace();
				}
				for (ProfileItem profileItem : profile.getItems()) {
					if (profileItem.getPackage().equalsIgnoreCase(sbn.getPackageName())) {
						should = true;
						break;
					}
				}
			}

			if (should) {
				if (sbn.getNotification().priority >= Notification.PRIORITY_HIGH) {
					notifyBlank();
					cancelBlank();
				}

				cancelNotification(sbn.getKey());

				notify("Silenced " + sbn.getPackageName() + " ...");

				Log.d(TAG, "canceled\n" + sbn);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void onNotificationRemoved(StatusBarNotification sbn) {
		Log.d(TAG, "onNotificationRemoved\n" + sbn);

	}

	private void notify(String msg) {
		try {
			Intent intent = new Intent(this, Service.class);
			PendingIntent pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);

			Notification n = new Notification.Builder(this)
					.setContentTitle(getString(R.string.app_name))
					.setContentText(msg)
					.setSmallIcon(R.mipmap.ic_launcher)
					.setContentIntent(pIntent)
					.setAutoCancel(false)
					.setPriority(Notification.PRIORITY_MIN)
					.build();

			NotificationManager notificationManager =
					(NotificationManager) getSystemService(NOTIFICATION_SERVICE);

			notificationManager.notify(0, n);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private final int ID_BLANK = 896;

	private void notifyBlank() {
		try {
			Intent intent = new Intent(this, Service.class);
			PendingIntent pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);

			Notification n = new Notification.Builder(this)
					.setContentTitle("").setContentText("")
					.setSmallIcon(R.drawable.transparent)
					.setPriority(Notification.PRIORITY_DEFAULT)
					.setFullScreenIntent(pIntent, true)
					.setAutoCancel(true)
					.build();

			NotificationManager notificationManager =
					(NotificationManager) getSystemService(NOTIFICATION_SERVICE);

			notificationManager.notify(ID_BLANK, n);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void cancelBlank() {
		try {
			NotificationManager notificationManager =
					(NotificationManager) getSystemService(NOTIFICATION_SERVICE);

			notificationManager.cancel(ID_BLANK);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static boolean isRunning(Context context) {
		ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

		if (manager != null) {
			for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
				if (Service.class.getName().equals(service.service.getClassName())) {
					return true;
				}
			}
		}

		return false;
	}

	@SuppressLint("InlinedApi")
	@TargetApi(19)
	public static Intent getIntentNotificationListenerSettings() {
		final String ACTION_NOTIFICATION_LISTENER_SETTINGS;
		if (Build.VERSION.SDK_INT >= 22) {
			ACTION_NOTIFICATION_LISTENER_SETTINGS = Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS;
		} else {
			ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
		}

		return new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS);
	}

	public static void toggleNotificationListenerService(Context context) {
		PackageManager pm = context.getPackageManager();
		pm.setComponentEnabledSetting(new ComponentName(context, Service.class),
				PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

		pm.setComponentEnabledSetting(new ComponentName(context, Service.class),
				PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

	}

	public static String TAG_SPREF = "spref";

	public static SharedPreferences get(final Context context) {
		SharedPreferences spref = context.getSharedPreferences(TAG_SPREF, MODE_PRIVATE);
		return spref;
	}

	public static String TAG_SPREF_TURN_OFF_GLOBALLY = "turn_off_globally";

	public static boolean getTurnOffGloballyEnabled(final Context context) {
		return get(context).getBoolean(TAG_SPREF_TURN_OFF_GLOBALLY, false);
	}

	public static void setTurnOffGloballyEnabled(final Context context, boolean value) {
		SharedPreferences.Editor editor = get(context).edit();
		editor.putBoolean(TAG_SPREF_TURN_OFF_GLOBALLY, value);
		editor.apply();
	}


}
