package com.example.whisk.schedulememo;

import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

public class AlertDialogActivity extends Activity implements OnClickListener{

    public static AlertDialogActivity context = null;
    private MediaPlayer player = new MediaPlayer();
    WakeLock mWakelock;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mWakelock = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP |PowerManager.FULL_WAKE_LOCK, "AlertDialog");
        mWakelock.acquire();
        context = this;
        try{
            Uri localUri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM);
            if((player != null) && (localUri != null))
            {
                player.setDataSource(context,localUri);
                player.prepare();
                player.setLooping(false);
                player.start();
            }

            AlertDialog.Builder localBuilder = new AlertDialog.Builder(context);    //弹窗提醒
            localBuilder.setTitle(R.string.alertName);
            localBuilder.setMessage(getIntent().getStringExtra("content"));
            localBuilder.setPositiveButton(R.string.positiveButton,this);
            localBuilder.setNegativeButton(R.string.negativeButton,this);
            localBuilder.show();


        }catch (IllegalArgumentException localIllegalArgumentException)
        {
            localIllegalArgumentException.printStackTrace();
        }
        catch (SecurityException localSecurityException)
        {
            localSecurityException.printStackTrace();
        }
        catch (IllegalStateException localIllegalStateException)
        {
            localIllegalStateException.printStackTrace();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch(which){
            case DialogInterface.BUTTON1:
            {
                Intent intent = new Intent(AlertDialogActivity.this, EditActivity.class);
                Bundle b = new Bundle();
                b.putString("datetime",getIntent().getStringExtra("datetime"));
                b.putString("content", getIntent().getStringExtra("content"));
                b.putString("alerttime",getIntent().getStringExtra("alerttime"));
                b.putString("filename",getIntent().getStringExtra("filename"));
                intent.putExtra("android.intent.extra.INTENT", b);
                startActivity(intent);                                //启动转到的Activity
                finish();
            }
            case DialogInterface.BUTTON2:
            {
                player.stop();
                finish();
            }
        }
    }
}