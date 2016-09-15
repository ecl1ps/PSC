package com.pokescanner.profiles;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.ColorInt;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.pavelsikun.vintagechroma.ChromaDialog;
import com.pavelsikun.vintagechroma.IndicatorMode;
import com.pavelsikun.vintagechroma.OnColorSelectedListener;
import com.pavelsikun.vintagechroma.colormode.ColorMode;
import com.pokescanner.NotificationActivity;
import com.pokescanner.R;
import com.pokescanner.helper.PokemonListLoader;
import com.pokescanner.loaders.AuthAccountsLoader;
import com.pokescanner.loaders.AuthSingleAccountLoader;
import com.pokescanner.multiboxing.MultiboxingAdapter;
import com.pokescanner.objects.NotificationItem;
import com.pokescanner.objects.User;
import com.pokescanner.settings.Settings;
import com.pokescanner.utils.PermissionUtils;
import com.pokescanner.utils.UiUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmConfiguration;

public class ProfilesActivity extends AppCompatActivity {

    @BindView(R.id.rvProfileList)
    RecyclerView profileRecycler;
    private ArrayList<String> profileList;
    private ProfilesAdapter profileAdapter;

    private Realm realm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profiles);
        ButterKnife.bind(this);

        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder(this)
                .name(Realm.DEFAULT_REALM_NAME)
                .deleteRealmIfMigrationNeeded()
                .build();
        Realm.setDefaultConfiguration(realmConfiguration);
        realm = Realm.getDefaultInstance();

        realm.addChangeListener(new RealmChangeListener<Realm>() {
            @Override
            public void onChange(Realm element) {
                loadProfiles();
            }
        });

        profileList = new ArrayList<>();

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        profileRecycler.setLayoutManager(mLayoutManager);

        profileAdapter = new ProfilesAdapter(this, profileList, new ProfilesAdapter.profileRemovalListener() {
            @Override
            public void onRemove(String profile) {
                removeProfile(profile);
            }
        }, new ProfilesAdapter.profileClickListener() {
            @Override
            public void onClick(String profile) {
                setProfile(profile);
            }
        });

        profileRecycler.setAdapter(profileAdapter);

    }

    private void removeProfile(final String profile) {
        if (profileList.size() <= 1) {
            final AlertDialog builder = new AlertDialog.Builder(this).create();
            builder.setTitle(getString(R.string.cannot_remove_profile));
            builder.setMessage(getString(R.string.need_one_profile));
            builder.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    builder.dismiss();
                }
            });
            builder.show();
        } else {
            for (final NotificationItem item : PokemonListLoader.getNotificationList()) {
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        item.removeProfile(profile);
                        realm.copyToRealmOrUpdate(item);
                    }
                });
            }
            int index = profileList.indexOf(profile);
            //Move the current profile up or down the list.
            if (profile.matches(Settings.getPreferenceString(this, Settings.PROFILE))){
                int tempIndex = index-1;
                try{
                    profileList.get(tempIndex);
                }catch (ArrayIndexOutOfBoundsException e){
                    tempIndex = index+1;
                }
                Settings.setPreference(this, Settings.PROFILE, profileList.get(tempIndex));
            }
            profileList.remove(index);
            profileAdapter.notifyItemRemoved(index);
        }
    }

    private void setProfile(String profile) {
        Settings.setPreference(this, Settings.PROFILE, profile);
        profileAdapter.notifyDataSetChanged();
    }

    private void loadProfiles() {
        profileList.clear();
        profileList.addAll(UiUtils.getAllProfiles(this));
        profileAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        realm = Realm.getDefaultInstance();
        loadProfiles();
    }


    @OnClick(R.id.btnAddProfile)
    public void addProfileDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_add_profile, null);
        final AlertDialog builder = new AlertDialog.Builder(this).create();

        final TextView etProfile = (TextView) view.findViewById(R.id.etAddProfile);

        Button btnAdd = (Button) view.findViewById(R.id.btnOk);
        Button btnCancel = (Button) view.findViewById(R.id.btnCancel);


        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String profile = etProfile.getText().toString();
                if (checkifExists(profile)) {
                    Toast.makeText(ProfilesActivity.this, getString(R.string.profile_exists), Toast.LENGTH_SHORT).show();
                } else {
                    Settings.setPreference(ProfilesActivity.this, Settings.PROFILE, profile);
                    profileList.add(profile);
                    profileAdapter.notifyDataSetChanged();
                    builder.dismiss();
                    startActivity(new Intent(ProfilesActivity.this, NotificationActivity.class));
                }
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                builder.dismiss();
            }
        });


        builder.setView(view);
        builder.show();
    }

    private boolean checkifExists(String profile) {
        return profileList.contains(profile);
    }


    @Override
    protected void onDestroy() {
        realm.close();
        super.onDestroy();
    }

}
