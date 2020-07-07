package com.privateinternetaccess.android.pia.tasks;

import android.content.Context;
import android.os.AsyncTask;

import com.privateinternetaccess.android.pia.api.UpdateApi;
import com.privateinternetaccess.android.pia.model.response.CheckUpdateResponse;
import com.privateinternetaccess.core.utils.IPIACallback;

import org.greenrobot.eventbus.EventBus;

public class CheckUpdateTask extends AsyncTask<Void, Void, CheckUpdateResponse> {

    private Context context;
    private IPIACallback<CheckUpdateResponse> callback;

    public CheckUpdateTask(Context context) {
        this.context = context;
    }

    @Override
    protected CheckUpdateResponse doInBackground(Void... trialData) {
        UpdateApi api = new UpdateApi(context);
        return api.checkAvailableVersion();
    }

    @Override
    protected void onPostExecute(CheckUpdateResponse updateResponse) {
        super.onPostExecute(updateResponse);
        //EventBus.getDefault().postSticky(new TrialEvent(trialResponse));

        if(callback != null)
            callback.apiReturn(updateResponse);
    }

    public void setCallback(IPIACallback<CheckUpdateResponse> callback) {
        this.callback = callback;
    }
}
