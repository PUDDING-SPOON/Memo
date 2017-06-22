package com.example.whisk.schedulememo;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public class AlarmReceiver extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "你设置的闹铃时间到了", Toast.LENGTH_LONG).show();
        intent.setClass(context, AlertDialogActivity.class);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        context.startActivity(intent);
        Bundle b = new Bundle();                                     //通知栏提醒
        b.putString("datetime",intent.getStringExtra("datetime"));
        b.putString("content", intent.getStringExtra("content"));
        b.putString("alerttime",intent.getStringExtra("alerttime"));
        b.putString("filename",intent.getStringExtra("filename"));
        intent.putExtra("android.intent.extra.INTENT", b);
        PendingIntent pending = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notify = new Notification.Builder(context)
                .setContentTitle("备忘录提醒")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText(intent.getStringExtra("content"))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis())
                .getNotification();
        notify.flags |= Notification.FLAG_AUTO_CANCEL;
        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, notify);
    }
}