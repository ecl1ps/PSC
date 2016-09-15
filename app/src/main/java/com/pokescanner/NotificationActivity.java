package com.pokescanner;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;

import com.pokescanner.helper.PokemonListLoader;
import com.pokescanner.objects.NotificationItem;
import com.pokescanner.recycler.NotificationRecyclerAdapter;
import com.pokescanner.settings.Settings;

import java.io.IOException;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.realm.Case;
import io.realm.Realm;

/**
 * Created by Brian on 7/22/2016.
 */
public class NotificationActivity extends AppCompatActivity implements TextWatcher {
    @BindView(R.id.etSearch)
    EditText etSearch;
    @BindView(R.id.filterRecycler)
    RecyclerView notificationRecycler;
    @BindView(R.id.btnNone)
    Button btnNone;
    @BindView(R.id.btnAll)
    Button btnAll;

    ArrayList<NotificationItem> notificationItems = new ArrayList<>();
    RecyclerView.Adapter mAdapter;
    Realm realm;
    String profile;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);
        ButterKnife.bind(this);

        Bundle extras = getIntent().getExtras();
        realm = Realm.getDefaultInstance();

        profile = Settings.getPreferenceString(this, Settings.PROFILE);

        etSearch.addTextChangedListener(this);

        try {
            notificationItems = PokemonListLoader.getPokelist(this, PokemonListLoader.NOTIFICATION);
        } catch (IOException e) {
            e.printStackTrace();
        }

        setupRecycler();
    }

    public void setupRecycler() {
        RecyclerView.LayoutManager mLayoutManager;
        notificationRecycler.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        notificationRecycler.setLayoutManager(mLayoutManager);

        mAdapter = new NotificationRecyclerAdapter(notificationItems, new NotificationRecyclerAdapter.onCheckedListener() {
            @Override
            public void onChecked(final NotificationItem notificationItem) {
                realm.executeTransaction(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        realm.copyToRealmOrUpdate(notificationItem);
                    }
                });
            }
        });


        notificationRecycler.setAdapter(mAdapter);

    }

    @Override
    public void onBackPressed() {
        finishActivity();
    }

    public void finishActivity() {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.copyToRealmOrUpdate(notificationItems);
            }
        });
        finish();
    }


    @OnClick(R.id.btnNone)
    public void selectNoneButton() {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                notificationItems.clear();
                notificationItems.addAll(realm.copyFromRealm(realm.where(NotificationItem.class)
                        .findAll()
                        .sort("Number")));
                for (NotificationItem notificationItem : notificationItems) {
                    notificationItem.removeProfile(profile);
                }
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    @OnClick(R.id.btnAll)
    public void selectAllButton() {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                notificationItems.clear();
                notificationItems.addAll(realm.copyFromRealm(realm.where(NotificationItem.class)
                        .findAll()
                        .sort("Number")));
                for (NotificationItem notificationItem : notificationItems) {
                    notificationItem.addProfile(profile);
                }
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }

    @Override
    public void onTextChanged(final CharSequence charSequence, int i, int i1, int i2) {
        if (charSequence.length() > 0) {
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    notificationItems.clear();
                    notificationItems.addAll(realm.copyFromRealm(realm.where(NotificationItem.class)
                            .contains("Name", charSequence.toString(), Case.INSENSITIVE)
                            .findAll()));
                    mAdapter.notifyDataSetChanged();
                }
            });
        } else {
            notificationItems.clear();
            notificationItems.addAll(realm.copyFromRealm(realm.where(NotificationItem.class).findAll()));
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void afterTextChanged(Editable editable) {
    }

    @Override
    protected void onResume() {
        realm = Realm.getDefaultInstance();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        realm.close();
        super.onDestroy();
    }
}
