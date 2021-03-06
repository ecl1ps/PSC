package com.pokescanner.settings;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.pokescanner.BlacklistActivity;
import com.pokescanner.BuildConfig;
import com.pokescanner.ExpirationFilters;
import com.pokescanner.NotificationActivity;
import com.pokescanner.R;
import com.pokescanner.events.AppUpdateEvent;
import com.pokescanner.objects.Gym;
import com.pokescanner.objects.PokeStop;
import com.pokescanner.objects.Pokemons;
import com.pokescanner.profiles.ProfilesActivity;
import com.pokescanner.service.PokeReceiver;
import com.pokescanner.updater.AppUpdate;
import com.pokescanner.updater.AppUpdateDialog;
import com.pokescanner.updater.AppUpdateLoader;
import com.pokescanner.utils.PermissionUtils;
import com.pokescanner.utils.UiUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import io.realm.Realm;

import static android.app.Activity.RESULT_OK;

public class SettingsFragment extends PreferenceFragment {
    private static final int CUSTOM_LOCATION_REQUEST = 1111;
    public static final int BACKGROUND_RADIUS = 0;
    public static final int FOREGROUND_RADIUS = 1;
    Preference scan_dialog;
    Preference background_scan_dialog;
    Preference gym_cp_filter;
    Preference expiration_filter;
    Preference clear_pokemon;
    Preference clear_gyms;
    Preference clear_pokestops;
    Preference pokemon_blacklist;
    Preference update;
    Preference serve_refresh_rate_dialog;
    Preference pokemon_notification;
    Preference profile;
    ListPreference service_refresh_rate;
    SwitchPreference enable_service;
    Preference custom_location;
    Realm realm;
    private Context mContext;
    private View rootView;
    boolean donation = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            String page = getArguments().getString("page");
            if (page != null)
                switch (page) {
                    case "filterOptions":
                        addPreferencesFromResource(R.xml.settings_filter);
                        break;
                    case "notificationOptions":
                        addPreferencesFromResource(R.xml.settings_notification);
                        break;
                    case "mapOptions":
                        addPreferencesFromResource(R.xml.settings_map);
                        break;
                    case "advancedMapOptions":
                        addPreferencesFromResource(R.xml.settings_advanced_map);
                        break;
                    case "advancedIconOptions":
                        addPreferencesFromResource(R.xml.settings_advanced_icon);
                        break;
                    case "clearOptions":
                        addPreferencesFromResource(R.xml.settings_clear);
                        break;
                    case "miscOptions":
                        addPreferencesFromResource(R.xml.settings_miscellaneous);
                        break;
                    case "donatePage":
                        donationIntent();
                        donation = true;
                        break;
                }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.settings_page, container, false);
        mContext = getActivity();
        setupToolbar();

        realm = Realm.getDefaultInstance();

        if (getArguments() != null) {
            String page = getArguments().getString("page");
            if (page != null)
                switch (page) {
                    case "filterOptions":
                        setupFilterOptions();
                        break;
                    case "notificationOptions":
                        setupNotificationOptions();
                        break;
                    case "mapOptions":
                        setupMapOptions();
                        break;
                    case "advancedMapOptions":
                        setupAdvanceMapOptions();
                        break;
                    case "clearOptions":
                        setupClearOptions();
                        break;
                    case "miscOptions":
                        setupMiscOptions();
                        break;
                }
        }
        return rootView;
    }

    public static void searchRadiusDialog(final Context context, final int type) {
        int scanValue;
        if (type == FOREGROUND_RADIUS) {
            scanValue = Settings.getPreferenceInt(context, Settings.SCAN_VALUE);
        } else {
            scanValue = Settings.getPreferenceInt(context, Settings.BACKGROUND_SEARCH_RADIUS);
        }
        final AppCompatDialog dialog = new AppCompatDialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_search_radius);

        final SeekBar seekBar = (SeekBar) dialog.findViewById(R.id.seekBar);
        Button btnSave = (Button) dialog.findViewById(R.id.btnAccept);
        Button btnCancel = (Button) dialog.findViewById(R.id.btnCancel);
        final TextView tvNumber = (TextView) dialog.findViewById(R.id.tvNumber);
        final TextView tvEstimate = (TextView) dialog.findViewById(R.id.tvEstimate);
        final TextView tvDescription = (TextView) dialog.findViewById(R.id.tvDescription);

        assert seekBar != null;
        assert btnCancel != null;
        assert btnSave != null;
        assert tvNumber != null;
        assert tvEstimate != null;

        tvNumber.setText(String.valueOf(scanValue));
        tvEstimate.setText(context.getString(R.string.timeEstimate) + " " + UiUtils.getSearchTimeString(scanValue, context));
        tvDescription.setText(String.format(context.getString(R.string.approx_distance), (scanValue * 140)));
        seekBar.setProgress(scanValue);
        seekBar.setMax(12);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                tvNumber.setText(String.valueOf(i));
                tvEstimate.setText(context.getString(R.string.timeEstimate) + " " + UiUtils.getSearchTimeString(i, context));
                tvDescription.setText(String.format(context.getString(R.string.approx_distance), (i * 140)));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int savedValue = seekBar.getProgress();
                int scanOut = (savedValue == 0 ? 1 : savedValue);
                if(type == FOREGROUND_RADIUS){
                    Settings.setPreference(context, Settings.SCAN_VALUE, scanOut);
                } else {
                    Settings.setPreference(context, Settings.BACKGROUND_SEARCH_RADIUS, scanOut);
                }
                dialog.dismiss();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void setupToolbar() {
        AppCompatPreferenceActivity activity = (AppCompatPreferenceActivity) getActivity();
        Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.settingsToolbar);
        activity.setSupportActionBar(toolbar);

        ActionBar bar = activity.getSupportActionBar();
        bar.setHomeButtonEnabled(true);
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setDisplayShowTitleEnabled(true);
        bar.setHomeAsUpIndicator(R.drawable.back_button);
        if (!donation) {
            bar.setTitle(getPreferenceScreen().getTitle());
        }
    }

    private void setupFilterOptions() {
        gym_cp_filter = getPreferenceManager().findPreference("gym_cp_filter");
        gym_cp_filter.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                GymFilters.showGymCpFilterDialog(mContext);
                return true;
            }
        });

        expiration_filter = getPreferenceManager().findPreference("expiration_filter");
        expiration_filter.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                ExpirationFilters.showExpirationFiltersDialog(mContext);
                return true;
            }
        });

        pokemon_blacklist = getPreferenceManager().findPreference("pokemon_blacklist");
        pokemon_blacklist.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent filterIntent = new Intent(mContext, BlacklistActivity.class);
                startActivity(filterIntent);
                return true;
            }
        });
    }


    private void setupNotificationOptions() {
        enable_service = (SwitchPreference) getPreferenceManager().findPreference("enableService");
        enable_service.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object enableService) {
                if ((Boolean) enableService) {
                    PokeReceiver.scheduleAlarm(mContext);
                } else {
                    PokeReceiver.cancelAlarm(mContext);
                }
                return true;
            }
        });

        service_refresh_rate = (ListPreference) getPreferenceManager().findPreference("serviceRefresh");
        service_refresh_rate.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                if (enable_service.isChecked()) {
                    PokeReceiver.scheduleAlarm(mContext);
                }
                return true;
            }
        });

        pokemon_notification = getPreferenceManager().findPreference("pokemon_notification");
        pokemon_notification.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent filterIntent = new Intent(mContext, NotificationActivity.class);
                startActivity(filterIntent);
                return true;
            }
        });

        custom_location = getPreferenceManager().findPreference("customLocation");
        if (Settings.getCustomLocation(mContext) != null) {
            custom_location.setSummary(Settings.getCustomLocationString(mContext));
        }
        custom_location.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
                try {
                    startActivityForResult(builder.build(getActivity()), CUSTOM_LOCATION_REQUEST);
                } catch (GooglePlayServicesRepairableException e) {
                    e.printStackTrace();
                } catch (GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                }
                return true;
            }
        });
        profile = getPreferenceManager().findPreference("profile");
        profile.setSummary(Settings.getPreferenceString(mContext, Settings.PROFILE));
        profile.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(mContext, ProfilesActivity.class);
                startActivity(intent);
                return true;
            }
        });

        background_scan_dialog = getPreferenceManager().findPreference("backgroundSearchRadius");
        background_scan_dialog.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                SettingsFragment.searchRadiusDialog(mContext, BACKGROUND_RADIUS);
                return true;
            }
        });
    }

    private void setupMapOptions() {
        scan_dialog = getPreferenceManager().findPreference("scan_dialog");
        scan_dialog.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                SettingsFragment.searchRadiusDialog(mContext, FOREGROUND_RADIUS);
                return true;
            }
        });
    }

    private void setupAdvanceMapOptions() {
        serve_refresh_rate_dialog = getPreferenceManager().findPreference("server_refresh_rate_dialog");
        serve_refresh_rate_dialog.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                serverRefreshDialog();
                return true;
            }
        });
    }

    private void setupClearOptions() {
        clear_gyms = getPreferenceManager().findPreference("clear_gyms");
        clear_gyms.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        if (realm.where(Gym.class).findAll().deleteAllFromRealm()) {
                            Toast.makeText(mContext, getString(R.string.gyms_cleared), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                return true;
            }
        });

        clear_pokemon = getPreferenceManager().findPreference("clear_pokemon");
        clear_pokemon.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        if (realm.where(Pokemons.class).findAll().deleteAllFromRealm()) {
                            Toast.makeText(mContext, getString(R.string.pokemon_cleared), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                return true;
            }
        });

        clear_pokestops = getPreferenceManager().findPreference("clear_pokestops");
        clear_pokestops.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        if (realm.where(PokeStop.class).findAll().deleteAllFromRealm()) {
                            Toast.makeText(mContext, getString(R.string.pokestops_cleared), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                return true;
            }
        });
    }

    private void setupMiscOptions() {
        update = getPreferenceManager().findPreference("update");
        update.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                new AppUpdateLoader().start();
                return true;
            }
        });

        Preference license = getPreferenceManager().findPreference("viewLicense");
        getPreferenceScreen().removePreference(license);

        //For alpha versions, remove the update settings
        if (!BuildConfig.enableUpdater) {
            PreferenceScreen screen = getPreferenceScreen();
            PreferenceCategory updateCategory = (PreferenceCategory) getPreferenceManager().findPreference("category_update");
            ListPreference serverRefresh = (ListPreference) getPreferenceManager().findPreference("serverRefreshRate");
            if (serverRefresh != null) {
                screen.removePreference(serverRefresh);
            }
            if (updateCategory != null) {
                screen.removePreference(updateCategory);
            }

        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAppUpdateEvent(AppUpdateEvent event) {
        switch (event.getStatus()) {
            case AppUpdateEvent.OK:
                if (PermissionUtils.doWeHaveReadWritePermission(mContext)) {
                    showAppUpdateDialog(mContext, event.getAppUpdate());
                }
                break;
            case AppUpdateEvent.FAILED:
                Toast.makeText(mContext, getString(R.string.update_check_failed), Toast.LENGTH_SHORT).show();
                break;
            case AppUpdateEvent.UPTODATE:
                Toast.makeText(mContext, getString(R.string.up_to_date), Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void showAppUpdateDialog(final Context context, final AppUpdate update) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.update_available_title)
                .setMessage(context.getString(R.string.app_name) + " " + update.getVersion() + " " + context.getString(R.string.update_available_long) + "\n\n" + context.getString(R.string.changes) + "\n\n" + update.getChangelog())
                .setIcon(R.mipmap.ic_launcher)
                .setPositiveButton(context.getString(R.string.update), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        AppUpdateDialog.downloadAndInstallAppUpdate(context, update);
                    }
                })
                .setNegativeButton(context.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void donationIntent() {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.me/loserskater"));
        startActivity(i);
    }

    public void serverRefreshDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Server Refresh Rate");

        final EditText input = new EditText(getActivity());

        input.setText(Settings.getPreferenceString(mContext, Settings.SERVER_REFRESH_RATE));

        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Settings.setPreference(mContext, Settings.SERVER_REFRESH_RATE, input.getText().toString());
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CUSTOM_LOCATION_REQUEST) {
            if (resultCode == RESULT_OK && data != null) {
                Place place = PlacePicker.getPlace(mContext, data);
                Settings.setCustomLocation(mContext, place.getLatLng());
                custom_location.setSummary(place.getLatLng().toString());
            }
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void onDestroy() {
        realm.close();
        super.onDestroy();
    }

    @Override
    public void onResume() {
        realm = Realm.getDefaultInstance();
        super.onResume();
        if (getView() != null) {
            View frame = (View) getView().getParent();
            if (frame != null) {
                frame.setPadding(0, 0, 0, 0);
            }
        }
        if (getArguments() != null) {
            String page = getArguments().getString("page");
            if (page != null)
                switch (page) {
                    case "filterOptions":
                        setupFilterOptions();
                        break;
                    case "notificationOptions":
                        setupNotificationOptions();
                        break;
                    case "mapOptions":
                        setupMapOptions();
                        break;
                    case "advancedMapOptions":
                        setupAdvanceMapOptions();
                        break;
                    case "clearOptions":
                        setupClearOptions();
                        break;
                    case "miscOptions":
                        setupMiscOptions();
                        break;
                }
        }
    }
}
