package com.ilusons.notioff;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.UUID;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.annotations.PrimaryKey;

public class ProfileItem extends RealmObject {

	// Logger TAG
	private static final String TAG = ProfileItem.class.getSimpleName();

	public String Title;

	public String getTitle() {
		return Title;
	}

	public void setTitle(String value) {
		Title = value;
	}

	@PrimaryKey
	public String Package;

	public String getPackage() {
		return Package;
	}

	public void setPackage(String value) {
		Package = value;
	}

	public ProfileItem() {
	}

	@Override
	public boolean equals(Object obj) {
		ProfileItem other = (ProfileItem) obj;

		if (other == null)
			return false;

		if (getPackage().equals(other.getPackage()))
			return true;

		return false;
	}

	public Drawable getIcon(Context context) {
		try {
			return context.getPackageManager().getApplicationIcon(Package);
		} catch (PackageManager.NameNotFoundException e) {
			Log.w(TAG, e);
		}
		return null;
	}

	public static void create(Realm realm, final String pkg) {
		realm.executeTransaction(new Realm.Transaction() {
			@Override
			public void execute(@NonNull Realm realm) {
				ProfileItem value = realm.createObject(ProfileItem.class, pkg);
			}
		});
	}

	public static RealmResults<Profile> getOrCreate(Realm realm, String pkg) {
		RealmResults<Profile> result = realm.where(Profile.class).equalTo("Package", pkg).findAll();
		if (result == null || result.size() == 0) {
			create(realm, pkg);
			return getOrCreate(realm, pkg);
		}
		return result;
	}

	public static void remove(Realm realm, final ProfileItem o) {
		realm.executeTransaction(new Realm.Transaction() {
			@Override
			public void execute(Realm realm) {
				o.deleteFromRealm();
			}
		});
	}

}
