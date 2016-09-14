package com.pokescanner.settings;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pokescanner.LoginActivity;
import com.pokescanner.R;
import com.pokescanner.helper.PokemonListLoader;
import com.pokescanner.multiboxing.MultiboxingActivity;
import com.pokescanner.objects.FilterItem;
import com.pokescanner.objects.Gym;
import com.pokescanner.objects.NotificationItem;
import com.pokescanner.objects.PokeStop;
import com.pokescanner.objects.Pokemons;
import com.pokescanner.objects.User;
import com.pokescanner.utils.FileUtils;
import com.pokescanner.utils.JsonUtils;
import com.pokescanner.utils.PermissionUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.exceptions.RealmException;

public class SettingsActivity extends AppCompatPreferenceActivity {
    private static final String CORE_SETTINGS_KEY = "coreSettings";
    private static final String ACCOUNTS_KEY = "accounts";
    private static final String POKEMON_FILTERS_KEY = "pokemonFilters";
    private static final String POKEMON_NOTIFICATION_KEY = "pokemonNotifications";

    private Realm realm;
    private Context mContext;
    private static final int READ_PERMISSION_REQUESTED = 1300;
    private static final int WRITE_PERMISSION_REQUESTED = 1400;
    private static final int PICKFILE_RESULT_CODE = 1500;
    private String backupContents, backupFileName;

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.settings_headers, target);

        setContentView(R.layout.settings_page);
        Toolbar toolbar = (Toolbar) findViewById(R.id.settingsToolbar);
        setSupportActionBar(toolbar);

        ActionBar bar = getSupportActionBar();
        bar.setHomeButtonEnabled(true);
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setDisplayShowTitleEnabled(true);
        bar.setHomeAsUpIndicator(R.drawable.back_button);
        bar.setTitle("Settings");
    }

    @Override
    public void onHeaderClick(Header header, int position) {
        super.onHeaderClick(header, position);
        if (header.id == R.id.logoutHeader)
            logOut();
        else if (header.id == R.id.multiboxingHeader) {
            Intent i = new Intent(mContext, MultiboxingActivity.class);
            startActivity(i);
        } else if (header.id == R.id.createBackup)
            createBackupContents();
        else if (header.id == R.id.loadSettings) {
            if (PermissionUtils.doWeHaveReadExternalStoragePermission(mContext))
                openFilePicker();
            else
                getReadPermission();
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, PICKFILE_RESULT_CODE);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PICKFILE_RESULT_CODE:
                if (resultCode == RESULT_OK) {
                    try {
                        String FilePath = FileUtils.getFilePath(mContext, data.getData());
                        String settingsBackup = FileUtils.readFromSettingsFile(FilePath);
                        loadBackup(settingsBackup);
                    } catch (IOException | URISyntaxException e) {
                        showToast(R.string.backup_load_error);
                        Log.e("Backup error : ", e.getMessage());
                    }
                }
                break;
        }
    }

    private void showToast(int resId) {
        Toast.makeText(mContext, getResources().getString(resId), Toast.LENGTH_SHORT).show();
    }

    private void createBackupContents() {
        JSONObject backupObject = new JSONObject();

        try {
            //Get settings
            JSONObject coreSettings = Settings.toJSONObject(this);
            backupObject.put(CORE_SETTINGS_KEY, coreSettings);

            //Get pokemon filters
            ArrayList<FilterItem> currentPokeFilters = PokemonListLoader.getFilteredList();
            JSONArray filtersArray = new JSONArray();
            for (FilterItem filterItem : currentPokeFilters)
                filtersArray.put(filterItem.toJSONObject());
            backupObject.put(POKEMON_FILTERS_KEY, filtersArray);

            ArrayList<NotificationItem> currentPokeNotifs = PokemonListLoader.getNotificationList();
            JSONArray notificationsArray = new JSONArray();
            for (NotificationItem notifItem : currentPokeNotifs)
                notificationsArray.put(notifItem.toJSONObject());
            backupObject.put(POKEMON_NOTIFICATION_KEY, notificationsArray);

            //Get current users
            RealmResults<User> currentUsers = realm.where(User.class).findAll();
            JSONArray usersArray = new JSONArray();
            for (User user : currentUsers)
                usersArray.put(user.toJSONObject());
            backupObject.put(ACCOUNTS_KEY, usersArray);

            createFileNameDialog(backupObject.toString(4));
        } catch (RealmException | JSONException e) {
            showToast(R.string.backup_creation_error);
            Log.e("Backup error ", e.getMessage());
        }
    }

    private void loadBackup(String backupString) {
        try {
            final JSONObject backupObject = new JSONObject(backupString);
            Map<String, Object> coreSettings = JsonUtils.toMap(backupObject.getJSONObject(CORE_SETTINGS_KEY));
            Settings.loadSharedPreferencesFromFile(this, coreSettings);
            final JSONArray users = getJsonArray(backupObject, ACCOUNTS_KEY);
            final JSONArray pokemonFilters = handleOldBackup(getJsonArray(backupObject, POKEMON_FILTERS_KEY));
            final JSONArray pokemonNotifs = handleOldBackup(getJsonArray(backupObject, POKEMON_NOTIFICATION_KEY));

            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    if (users != null) {
                        realm.where(User.class).findAll().deleteAllFromRealm();
                        realm.createOrUpdateAllFromJson(User.class, users);
                    }
                    if (pokemonFilters != null) {
                        realm.where(FilterItem.class).findAll().deleteAllFromRealm();
                        realm.createOrUpdateAllFromJson(FilterItem.class, pokemonFilters);

                    }
                    if (pokemonNotifs != null) {
                        realm.where(NotificationItem.class).findAll().deleteAllFromRealm();
                        realm.createOrUpdateAllFromJson(NotificationItem.class, pokemonNotifs);
                    }
                }
            });
            showToast(R.string.backup_load_success);
        } catch (RealmException | JSONException e) {
            showToast(R.string.backup_load_error);
            Log.e("Backup error ", e.getMessage());
        }
    }

    private JSONArray handleOldBackup(JSONArray array) throws JSONException {
        if (array == null) {
            return null;
        }
        JSONArray newArray = new JSONArray();
        JSONObject object;
        for (int i = 0; i < array.length(); i++) {
            object = array.getJSONObject(i);
            if (object.has("number")) {
                object.put("Number", object.remove("number"));
            }
            if (object.has("name")) {
                object.put("Name", object.remove("name"));
            }
            newArray.put(object);
        }
        return newArray;
    }

    private JSONArray getJsonArray(JSONObject jsonObject, String key) throws JSONException {
        if (jsonObject.has(key)) {
            return jsonObject.getJSONArray(key);
        }
        return null;
    }

    private void createFileNameDialog(final String backupString) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        final EditText fileNameBox = new EditText(mContext);
        builder.setMessage(R.string.request_file_name);
        builder.setTitle(R.string.backup_location_message);

        builder.setView(fileNameBox);

        builder.setPositiveButton(getResources().getString(R.string.create_backup), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String fileName = fileNameBox.getText().toString().trim();
                if (fileName.equals(""))
                    showToast(R.string.file_name_error);
                else {
                    backupContents = backupString;
                    backupFileName = fileName;
                    if (PermissionUtils.doWeHaveWriteExternalStoragePermission(mContext))
                        createBackup();
                    else
                        getWritePermission();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    public void createBackup() {
        try {
            FileUtils.createSettingsBackupFile(backupContents, backupFileName);
            showToast(R.string.backup_creation_successful);
        } catch (IOException ioe) {
            showToast(R.string.backup_creation_error);
            Log.e("Backup error ", ioe.getMessage());
        }
    }

    public void getReadPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
            dialog.setCancelable(false)
                    .setMessage(getResources().getString(R.string.backup_read_permission))
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ActivityCompat.requestPermissions(SettingsActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_PERMISSION_REQUESTED);
                        }
                    })
                    .show();
        } else {
            ActivityCompat.requestPermissions(SettingsActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_PERMISSION_REQUESTED);
        }
    }

    public void getWritePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
            dialog.setCancelable(false)
                    .setMessage(getResources().getString(R.string.backup_write_permission))
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ActivityCompat.requestPermissions(SettingsActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_PERMISSION_REQUESTED);
                        }
                    })
                    .show();
        } else {
            ActivityCompat.requestPermissions(SettingsActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_PERMISSION_REQUESTED);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case READ_PERMISSION_REQUESTED: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showToast(R.string.PERMISSION_OK);
                    openFilePicker();
                } else {
                    showToast(R.string.backup_load_error);
                }
            }
            break;
            case WRITE_PERMISSION_REQUESTED: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    createBackup();
                } else {
                    showToast(R.string.backup_creation_error);
                }
            }
            break;
        }
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void logOut() {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.where(User.class).findAll().deleteAllFromRealm();
                realm.where(PokeStop.class).findAll().deleteAllFromRealm();
                realm.where(Pokemons.class).findAll().deleteAllFromRealm();
                realm.where(Gym.class).findAll().deleteAllFromRealm();
                Intent intent = new Intent(mContext, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        realm = Realm.getDefaultInstance();
        mContext = SettingsActivity.this;
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        realm.close();
        super.onDestroy();
    }
}
