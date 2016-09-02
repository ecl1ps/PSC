
package com.pokescanner.objects;

import org.json.JSONException;
import org.json.JSONObject;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * Created by Jason on 8/19/2016.
 */
@Data
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"Name","notification"}, callSuper = false)
public class NotificationItem extends RealmObject{
    @PrimaryKey
    int Number;
    String Name;
    boolean notification;

    public NotificationItem() {}

    public NotificationItem(int number) {
        setNumber(number);
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject result = new JSONObject();
        result.put("number", Number);
        result.put("name", Name);
        result.put("notification", notification);
        return result;
    }
}
