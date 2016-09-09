package com.pokescanner.multiboxing;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.ColorInt;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import com.pokescanner.R;
import com.pokescanner.loaders.AuthAccountsLoader;
import com.pokescanner.loaders.AuthSingleAccountLoader;
import com.pokescanner.objects.User;
import com.pokescanner.utils.PermissionUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmConfiguration;

public class MultiboxingActivity extends AppCompatActivity {

    public static final String ACCOUNT_FILE_NAME = "pokealert_accounts.txt";
    @BindView(R.id.rvMultiboxingAccountList)
    RecyclerView userRecycler;
    private ArrayList<User> userList;
    private MultiboxingAdapter userAdapter;

    private Realm realm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiboxing);
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
                loadAccounts();
            }
        });

        userList = new ArrayList<User>();

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        userRecycler.setLayoutManager(mLayoutManager);

        userAdapter = new MultiboxingAdapter(this, userList, new MultiboxingAdapter.accountRemovalListener() {
            @Override
            public void onRemove(User user) {
                removeAccount(user);
            }
        }, new MultiboxingAdapter.accountChangeColorListener() {
            @Override
            public void changeColor(User user) {
                changeAccountColor(user);
            }
        });

        userRecycler.setAdapter(userAdapter);

    }

    private void changeAccountColor(final User user) {
        new ChromaDialog.Builder()
                .initialColor(user.getAccountColor())
                .colorMode(ColorMode.ARGB) // RGB, ARGB, HVS, CMYK, CMYK255, HSL
                .indicatorMode(IndicatorMode.HEX) //HEX or DECIMAL; Note that (HSV || HSL || CMYK) && IndicatorMode.HEX is a bad idea
                .onColorSelected(new OnColorSelectedListener() {
                    @Override
                    public void onColorSelected(@ColorInt final int color) {
                        realm.executeTransaction(new Realm.Transaction() {
                            @Override
                            public void execute(Realm realm) {
                                user.setAccountColor(color);
                                realm.copyToRealmOrUpdate(user);
                            }
                        });
                    }
                })
                .create()
                .show(getSupportFragmentManager(), "ChromaDialog");
    }

    private void removeAccount(final User user) {
        int realmSize = realm.where(User.class).findAll().size();
        if (realmSize != 1) {
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    if (realm.where(User.class).equalTo("username", user.getUsername())
                            .findAll().deleteAllFromRealm()) {
                        int index = userList.indexOf(user);
                        userList.remove(index);
                        userAdapter.notifyItemRemoved(index);
                    }
                }
            });
        }
    }


    private void loadAccounts() {
        userList.clear();
        userList.addAll(realm.copyFromRealm(realm.where(User.class).findAll()));
        userAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        realm = Realm.getDefaultInstance();
        loadAccounts();
        refreshAccounts();
    }


    @OnClick(R.id.btnAddAccount)
    public void addAccountDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_add_account, null);
        final AlertDialog builder = new AlertDialog.Builder(this).create();

        final TextView etUsername = (TextView) view.findViewById(R.id.etAddUsername);
        final TextView etPassword = (TextView) view.findViewById(R.id.etAddPassword);

        Button btnAdd = (Button) view.findViewById(R.id.btnOk);
        Button btnCancel = (Button) view.findViewById(R.id.btnCancel);
        Button btnAddFromFile = (Button) view.findViewById(R.id.btnAddFromFile);


        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String username = etUsername.getText().toString();
                String password = etPassword.getText().toString();

                User user = new User(username, password, null, User.PTC, User.STATUS_UNKNOWN);

                realm.beginTransaction();
                realm.copyToRealmOrUpdate(user);
                realm.commitTransaction();

                AuthSingleAccountLoader singleloader = new AuthSingleAccountLoader(user);
                singleloader.start();

                builder.dismiss();
            }
        });

        btnAddFromFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadAccountFromFile();
                builder.dismiss();
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

    @OnClick(R.id.btnRefresh)
    public void refreshAccounts() {
        new AuthAccountsLoader().start();
    }

    @Override
    protected void onDestroy() {
        realm.close();
        super.onDestroy();
    }

    public void loadAccountFromFile() {
        if (!PermissionUtils.doWeHaveReadWritePermission(this)) {
            PermissionUtils.requestWritePermission(this);
        }
        BufferedReader bw = null;
        File file = new File((Environment.getExternalStorageDirectory() + "/") + ACCOUNT_FILE_NAME);
        try {
            if (file.exists()) {
                bw = new BufferedReader(new FileReader(file));
                String line;
                while ((line = bw.readLine()) != null) {
                    String[] accountInfo = line.split(" ");
                    if (accountInfo.length >= 3 && accountInfo[2].equals("PTC")) {
                        User user = new User(accountInfo[0].trim(), accountInfo[1].trim(), null, User.PTC, User.STATUS_UNKNOWN);

                        realm.beginTransaction();
                        realm.copyToRealmOrUpdate(user);
                        realm.commitTransaction();
                    }
                }
                refreshAccounts();
            } else {
                Toast.makeText(this, "No file found", Toast.LENGTH_SHORT).show();
            }
            if (bw != null) {
                bw.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


//                Settings.saveAccounts(getApplicationContext(), new HashSet(this.accounts));


    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PermissionUtils.MY_PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    loadAccountFromFile();
                }
            }
        }
    }


}
