
package com.pokescanner.recycler;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.pokescanner.R;
import com.pokescanner.objects.NotificationItem;
import com.pokescanner.utils.SettingsUtil;

/**
 * Created by Brian on 7/21/2016.
 */
public class NotificationViewHolder extends RecyclerView.ViewHolder {
    ImageView imageFilterRow;
    TextView pokemonName;
    TextView tvStatus;
    CheckBox checkBox;
    Context context;

    public NotificationViewHolder(View itemView) {
        super(itemView);

        pokemonName = (TextView) itemView.findViewById(R.id.tvName);
        checkBox = (CheckBox) itemView.findViewById(R.id.checkBox);
        imageFilterRow = (ImageView) itemView.findViewById(R.id.imageFilterRow);

        this.context = itemView.getContext();

        checkBox.setClickable(true);
    }

    public void bind(final NotificationItem notificationItem, final NotificationRecyclerAdapter.onCheckedListener listener) {
        checkBox.setOnCheckedChangeListener(null);

        pokemonName.setText(notificationItem.getName());
        checkBox.setChecked(notificationItem.isNotification());

        String uri;
        int pokemonnumber = notificationItem.getNumber();

        if (SettingsUtil.getSettings().isShuffleIcons()) {
            uri = "ps" + pokemonnumber;
        }
        else uri = "p" + pokemonnumber;

        int resourceID = context.getResources().getIdentifier(uri, "drawable", context.getPackageName());
        Bitmap bm = BitmapFactory.decodeResource(context.getResources(), resourceID);

        imageFilterRow.setImageBitmap(bm);

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                notificationItem.setNotification(b);
                listener.onChecked(notificationItem);
            }
        });
    }
}
