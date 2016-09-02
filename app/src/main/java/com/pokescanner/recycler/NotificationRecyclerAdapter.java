
package com.pokescanner.recycler;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pokescanner.R;
import com.pokescanner.objects.NotificationItem;

import java.util.ArrayList;

/**
 * Created by Brian on 7/21/2016.
 */
public class NotificationRecyclerAdapter extends RecyclerView.Adapter<NotificationViewHolder> {
    private onCheckedListener listener;
    private ArrayList<NotificationItem> notificationItems;

    public interface onCheckedListener {
        void onChecked(NotificationItem notificationItem);
    }

    public NotificationRecyclerAdapter(ArrayList<NotificationItem> notificationItems, onCheckedListener listener) {
        this.notificationItems = notificationItems;
        this.listener = listener;
    }


    @Override
    public NotificationViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.recycler_pokemon_filter_row, viewGroup, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(NotificationViewHolder notificationViewHolder, int i) {
        notificationViewHolder.bind(notificationItems.get(i), listener);
    }

    @Override
    public int getItemCount() {
        return notificationItems.size();
    }
}
