package com.pokescanner.profiles;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.pokescanner.R;
import com.pokescanner.settings.Settings;

import java.util.ArrayList;

public class ProfilesAdapter extends RecyclerView.Adapter<ProfilesAdapter.MBViewHolder> {

    private ArrayList<String> profiles;
    private Context mContext;
    private profileRemovalListener mRemovalListener;
    private profileClickListener mClickListener;
    private String profile;

    public interface profileRemovalListener {
        void onRemove(String profile);
    }

    public interface profileClickListener {
        void onClick(String profile);
    }

    public ProfilesAdapter(Context context, ArrayList<String> profiles, profileRemovalListener removalListener, profileClickListener clickListener) {
        mContext = context;
        this.mRemovalListener = removalListener;
        this.mClickListener = clickListener;
        this.profiles = profiles;
    }

    @Override
    public ProfilesAdapter.MBViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_profile_list_row, parent, false);
        return new MBViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ProfilesAdapter.MBViewHolder holder, final int position) {
        profile = profiles.get(position);
        holder.bind(profile, mRemovalListener, mClickListener);
    }

    @Override
    public int getItemCount() {
        return profiles.size();
    }


    public class MBViewHolder extends RecyclerView.ViewHolder {
        public TextView tvProfile;
        public ImageButton btnRemoveProfile;
        public ImageView ivProfile;
        public LinearLayout llProfile;

        public MBViewHolder(View view) {
            super(view);
            tvProfile = (TextView) view.findViewById(R.id.tvProfileName);
            btnRemoveProfile = (ImageButton) view.findViewById(R.id.btnRemoveProfile);
            ivProfile = (ImageView) view.findViewById(R.id.ivProfile);
            llProfile = (LinearLayout) view.findViewById(R.id.llProfile);
        }

        public void bind(final String profile, final profileRemovalListener removalListener, final profileClickListener clickListener) {
            tvProfile.setText(profile);
            if (profile.matches(Settings.getPreferenceString(mContext, Settings.PROFILE))){
                ivProfile.setVisibility(View.VISIBLE);
            } else {
                ivProfile.setVisibility(View.INVISIBLE);
            }
            btnRemoveProfile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    removalListener.onRemove(profile);
                }
            });
            llProfile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clickListener.onClick(profile);
                }
            });


        }
    }


}
