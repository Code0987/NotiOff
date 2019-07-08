package com.ilusons.notioff;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import jp.wasabeef.recyclerview.animators.SlideInLeftAnimator;

public class PackagesDialog extends Dialog {

	private static final String TAG = PackagesDialog.class.getSimpleName();

	private Context context;
	private Events events;
	private Profile profile;

	public PackagesDialog(Context context, Events events, Profile profile) {
		super(context);

		this.context = context;
		this.events = events;
		this.profile = profile;
	}

	private CardView root;

	private RecyclerView recyclerView;
	private RecyclerViewAdapter adapter;

	private ImageButton ok;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.dialog_packages);

		root = findViewById(R.id.root);

		recyclerView = findViewById(R.id.recyclerView);

		ok = findViewById(R.id.ok);

		ok.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});

		adapter = new RecyclerViewAdapter();

		recyclerView = findViewById(R.id.recyclerView);
		recyclerView.setLayoutManager(new LinearLayoutManager(context));
		recyclerView.setAdapter(adapter);
		SlideInLeftAnimator animator = new SlideInLeftAnimator();
		animator.setInterpolator(new OvershootInterpolator());
		recyclerView.setItemAnimator(animator);

		adapter.refresh();

		Toast.makeText(getContext(), "Tap to select or de-select.", Toast.LENGTH_LONG).show();
	}

	@Override
	public void dismiss() {
		if (events != null) {
			events.itemsSelected(adapter.dataSelected);
		}

		super.dismiss();
	}

	public class RecyclerViewAdapter extends RecyclerView.Adapter<ViewHolder> {

		private final String TAG_SELECTED = "selected";

		private ArrayList<ProfileItem> data;
		private ArrayList<ProfileItem> dataSelected;

		public RecyclerViewAdapter() {
			data = new ArrayList<>();
			dataSelected = new ArrayList<>();
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

			final CardView root = v.findViewById(R.id.root);
			if (profile.getItems().contains(d))
				toggleSelected(root, d, true);
			else
				root.setCardBackgroundColor(getContext().getColor(R.color.transparent));

			v.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					toggleSelected(root, d, false);
				}
			});

			TextView text1 = v.findViewById(R.id.text1);
			text1.setText(d.getTitle());

			TextView text2 = v.findViewById(R.id.text2);
			text2.setText(d.getPackage());

			ImageView image = v.findViewById(R.id.image);
			Drawable icon = d.getIcon(context);
			if (icon != null)
				image.setImageDrawable(icon);
			else
				image.setImageDrawable(context.getDrawable(R.mipmap.ic_launcher));

		}

		@Override
		public int getItemCount() {
			return data.size();
		}

		public void refresh() {
			try {
				data.clear();

				PackageManager pm = context.getPackageManager();
				List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

				for (ApplicationInfo app : apps) {
					if (pm.getLaunchIntentForPackage(app.packageName) != null) {

						ProfileItem item = new ProfileItem();
						item.setTitle(pm.getApplicationLabel(app).toString());
						item.setPackage(app.packageName);

						data.add(item);
					}
				}

				Collections.sort(data, new Comparator<ProfileItem>() {
					@Override
					public int compare(ProfileItem x, ProfileItem y) {
						return x.getTitle().compareTo(y.getTitle());
					}
				});

			} catch (Exception e) {
				Log.w(TAG, e);
			}

			notifyDataSetChanged();
		}

		private void toggleSelected(CardView root, ProfileItem d, boolean force) {
			if (!force && (root.getTag() != null && root.getTag().equals(TAG_SELECTED))) {
				root.setTag(null);
				root.setCardBackgroundColor(getContext().getColor(R.color.transparent));
				if (dataSelected.contains(d))
					dataSelected.remove(d);
			} else {
				root.setTag(TAG_SELECTED);
				root.setCardBackgroundColor(getContext().getColor(R.color.translucent_accent));
				if (!dataSelected.contains(d))
					dataSelected.add(d);
			}
		}

	}

	public class ViewHolder extends RecyclerView.ViewHolder {
		public View view;

		public ViewHolder(View view) {
			super(view);

			this.view = view;
		}

	}

	public interface Events {

		void itemsSelected(Collection<ProfileItem> items);
	}

}
