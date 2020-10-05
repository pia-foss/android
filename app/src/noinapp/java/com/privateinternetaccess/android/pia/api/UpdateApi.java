package com.privateinternetaccess.android.pia.api;

import android.content.Context;

import com.privateinternetaccess.android.pia.model.response.CheckUpdateResponse;
import com.privateinternetaccess.android.pia.utils.DLog;

import org.json.JSONObject;

import okhttp3.Request;
import okhttp3.Response;

public class UpdateApi extends PiaApi {
    public static final String TAG = "UpdateApi";

    private Context context;

    public UpdateApi(Context context) {
        super();
        this.context = context;
    }

    public CheckUpdateResponse checkAvailableVersion() {
        try {
            CheckUpdateResponse updateResponse = new CheckUpdateResponse(0);
            Request request = new Request.Builder().url(getClientURL(context, "android/latest_release")).build();
            DLog.d("UpdateAPI", getClientURL(context, "android/latest_release").toString());

            Response response = getOkHttpClient().newCall(request).execute();
            int status = response.code();

            DLog.d("UpdateAPI", "status = " + status);
            String res = response.body().string();
            DLog.d("UpdateAPI", "body = " + res);

            if (status == 200) {
                JSONObject json = null;

                try {
                    json = new JSONObject(res);
                    updateResponse.setVersionCode(json.getInt("version_code"));
                    updateResponse.setVersionName(json.getString("version_name"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return updateResponse;
        }
        catch (Exception e) {
            e.printStackTrace();
            CheckUpdateResponse updateResponse = new CheckUpdateResponse(0);
            return updateResponse;
        }
    }
}
