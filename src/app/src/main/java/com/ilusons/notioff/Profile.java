package com.ilusons.notioff;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import java.util.Collection;
import java.util.UUID;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.Sort;
import io.realm.annotations.PrimaryKey;

import static android.content.Context.MODE_PRIVATE;

public class Profile extends RealmObject {

	@PrimaryKey
	public String Title;

	public String getTitle() {
		return Title;
	}

	public void setTitle(String value) {
		Title = value;
	}

	public RealmList<ProfileItem> Items;

	public RealmList<ProfileItem> getItems() {
		return Items;
	}

	public void setItems(Collection<ProfileItem> items) {
		Items.clear();
		Items.addAll(items);
	}

	public Profile() {
		Title = "Profile";
		Items = new RealmList<>();
	}

	@Override
	public boolean equals(Object obj) {
		Profile other = (Profile) obj;

		if (other == null)
			return false;

		if (getTitle().equals(other.getTitle()))
			return true;

		return false;
	}

	public static RealmResults<Profile> get(Realm realm, String title) {
		RealmResults<Profile> result = realm.where(Profile.class).equalTo("Title", title).findAll();

		return result;
	}

	public static RealmResults<Profile> getAll(Realm realm) {
		RealmResults<Profile> result = realm.where(Profile.class).findAll();

		return result;
	}

	public static void create(Realm realm, final String title) {
		realm.executeTransaction(new Realm.Transaction() {
			@Override
			public void execute(@NonNull Realm realm) {
				Profile value = realm.createObject(Profile.class, title);
			}
		});
	}

	public static RealmResults<Profile> getOrCreate(Realm realm, String title) {
		RealmResults<Profile> result = realm.where(Profile.class).equalTo("Title", title).findAll();
		if (result == null || result.size() == 0) {
			create(realm, title);
			return getOrCreate(realm, title);
		}
		return result;
	}

	public static void remove(Realm realm, final Profile o) {
		realm.executeTransaction(new Realm.Transaction() {
			@Override
			public void execute(Realm realm) {
				realm.copyToRealmOrUpdate(o).deleteFromRealm();
			}
		});
	}

	public static String TAG_SPREF = "spref";

	public static SharedPreferences get(final Context context) {
		SharedPreferences spref = context.getSharedPreferences(TAG_SPREF, MODE_PRIVATE);
		return spref;
	}

	public static String TAG_SPREF_ACTIVE_PROFILE = "active_profile";

	public static String getActiveProfileKey(final Context context) {
		return get(context).getString(TAG_SPREF_ACTIVE_PROFILE, null);
	}

	public static void setActiveProfileKey(final Context context, String value) {
		SharedPreferences.Editor editor = get(context).edit();
		editor.putString(TAG_SPREF_ACTIVE_PROFILE, value);
		editor.apply();
	}

	public static Profile getActiveProfile(final Context context) {
		try (Realm realm = RealmEx.get(context)) {
			String key = getActiveProfileKey(context);
			Profile profile = realm.where(Profile.class).equalTo("Title", key).findFirst();
			profile = realm.copyFromRealm(profile);
			return profile;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void setActiveProfile(final Context context, Profile profile) {
		setActiveProfileKey(context, profile.getTitle());
	}

}

