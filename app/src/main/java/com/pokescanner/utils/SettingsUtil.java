package com.pokescanner.utils;

import com.pokescanner.settings.Settings;

import io.realm.Realm;


public class SettingsUtil {

    public static final String ENABLE_UPDATES = "updatesEnabled";
    public static final String KEY_BOUNDING_BOX = "boundingBoxEnabled";
    public static final String ENABLE_LOW_MEMORY = "enableLowMemory";
    public static final String SHOW_LURED_POKEMON = "showLuredPokemon";
    public static final String KEY_OLD_MARKER = "useOldMapMarker";
    public static final String DRIVING_MODE = "drivingModeEnabled";
    public static final String FORCE_ENGLISH_NAMES = "forceEnglishNames";
    public static final String SERVER_REFRESH_RATE = "serverRefreshRate";
    public static final String MAP_REFRESH_RATE = "mapRefreshRate";
    public static final String POKEMON_ICON_SCALE = "pokemonIconScale";
    public static final String SCAN_VALUE = "scanValue";
    public static final String LAST_USERNAME = "lastUsername";
    public static final String SHOW_NEUTRAL_GYMS = "showNeutralGyms";
    public static final String SHOW_YELLOW_GYMS = "showYellowGyms";
    public static final String SHOW_BLUE_GYMS = "showBlueGyms";
    public static final String SHOW_RED_GYMS = "showRedGyms";
    public static final String GUARD_MIN_CP = "guardPokemonMinCp";
    public static final String GUARD_MAX_CP = "guardPokemonMaxCp";
    public static final String SHUFFLE_ICONS = "shuffleIcons";
    public static final String SHOW_LURED_POKESTOPS = "showLuredPokestops";
    public static final String SHOW_NORMAL_POKESTOPS = "showNormalPokestops";

    public static void searchRadiusDialog(final Context context) {
        int scanValue = Settings.get(context).getScanValue();

        final AppCompatDialog dialog = new AppCompatDialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_search_radius);

        final SeekBar seekBar = (SeekBar) dialog.findViewById(R.id.seekBar);
        Button btnSave = (Button) dialog.findViewById(R.id.btnAccept);
        Button btnCancel = (Button) dialog.findViewById(R.id.btnCancel);
        final TextView tvNumber = (TextView) dialog.findViewById(R.id.tvNumber);
        final TextView tvEstimate = (TextView) dialog.findViewById(R.id.tvEstimate);
        tvNumber.setText(String.valueOf(scanValue));
        tvEstimate.setText(context.getString(R.string.timeEstimate) + " " + UiUtils.getSearchTime(scanValue,context));
        seekBar.setProgress(scanValue);
        seekBar.setMax(12);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                tvNumber.setText(String.valueOf(i));
                tvEstimate.setText(context.getString(R.string.timeEstimate) + " " + UiUtils.getSearchTime(i,context));
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
                int scanOut = 4;
                int saveValue = seekBar.getProgress();
                if (saveValue == 0) {
                    scanOut = 1;
                } else {
                    scanOut = saveValue;
                }
                context.getSharedPreferences(context.getString(R.string.shared_key), Context.MODE_PRIVATE)
                        .edit()
                        .putInt(SCAN_VALUE,scanOut)
                        .apply();
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

    public static Settings getSettings(Context context) {
        Realm realm = Realm.getDefaultInstance();
        Settings currentSettings = realm.where(Settings.class).findFirst();
        realm.close();
        if(currentSettings == null)
            currentSettings = new Settings("new");
        return new Settings(currentSettings);
    }

    public static void saveSettings(Context context, final Settings settings) {
        Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.copyToRealmOrUpdate(settings);
            }
        });
        realm.close();
    }
}
