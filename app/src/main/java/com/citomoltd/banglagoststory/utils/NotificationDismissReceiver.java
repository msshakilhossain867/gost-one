package com.citomoltd.banglagoststory.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationManagerCompat;

public class NotificationDismissReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int id = intent.getIntExtra("NOTIFICATION_ID", -1);
        if(id != -1){
            NotificationManagerCompat.from(context).cancel(id);
        }
    }
}

