package com.groove.megaapp.utils;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pixplicity.easyprefs.library.Prefs;

import java.util.List;

import com.groove.megaapp.MainApplication;
import com.groove.megaapp.SharedConstants;
import com.groove.megaapp.data.models.Advertisement;

final public class AdsUtil {

    private static final String TAG = "AdsUtil";

    @Nullable
    public static Advertisement findByLocationAndType(String location, String type) {
        String json = Prefs.getString(SharedConstants.PREF_ADS_CONFIG, null);
        if (!TextUtils.isEmpty(json)) {
            ObjectMapper om = MainApplication.getContainer().get(ObjectMapper.class);
            try {
                List<Advertisement> ads =
                        om.readValue(json, new TypeReference<List<Advertisement>>(){});
                for (Advertisement ad : ads) {
                    if (TextUtils.equals(ad.location, location) && TextUtils.equals(ad.type, type)) {
                        return ad;
                    }
                }
            } catch (JsonProcessingException e) {
                Log.e(TAG, "Could not parse ads config from shared preferences.", e);
            }
        }

        return null;
    }
}
