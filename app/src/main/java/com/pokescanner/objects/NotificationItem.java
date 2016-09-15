
package com.pokescanner.objects;

import android.content.Context;

import com.pokescanner.settings.Settings;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

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
@EqualsAndHashCode(exclude = {"Name"}, callSuper = false)
public class NotificationItem extends RealmObject {
    @PrimaryKey
    int Number;
    String Name;
    String profiles = "Default";

    public NotificationItem() {
    }

    public NotificationItem(int number) {
        setNumber(number);
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject result = new JSONObject();
        result.put("Number", Number);
        result.put("Name", Name);
        result.put("profiles", getProfilesString());
        return result;
    }

    public String getProfilesString() {
        return profiles;
    }

    public ArrayList<String> getProfiles() {
        ArrayList<String> profiles = new ArrayList<>();
        if (this.profiles != null) {
            String[] profileSplit = this.profiles.split(",");
            for (String profile : profileSplit) {
                if (!profile.isEmpty()) {
                    profiles.add(profile);
                }
            }
        }
        return profiles;
    }

    public void setProfiles(ArrayList<String> profiles) {
        StringBuilder sb = new StringBuilder();
        for (String profile : profiles) {
            if (!profile.isEmpty()) {
                sb.append(profile).append(",");
            }
        }
        this.profiles = sb.toString();
    }

    public void addProfile(Context context) {
        ArrayList<String> profiles = getProfiles();
        profiles.add(Settings.getPreferenceString(context, Settings.PROFILE));
        setProfiles(profiles);
    }

    public void removeProfile(Context context) {
        ArrayList<String> profiles = getProfiles();
        profiles.remove(Settings.getPreferenceString(context, Settings.PROFILE));
        setProfiles(profiles);
    }

    public boolean isProfile(Context context) {
        return profiles.contains(Settings.getPreferenceString(context, Settings.PROFILE));
    }
}
