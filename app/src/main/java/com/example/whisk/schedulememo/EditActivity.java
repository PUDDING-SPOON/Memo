package com.example.whisk.schedulememo;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class EditActivity extends AppCompatActivity {
     String alerttime = "";
     String datetime;
     String content;
     String filename;
     String tempContent,tempDatetime,tempAlerttime;
     ArrayList<HashMap<String,String>> list = Tools.getList();
     int index=0;
     UserInfo user;
     TimeSetDialog timeSetDialog=null;
     Button backButton,timeSetButton;
     TextView alertTextView;
     EditText edittext;
     SQLiteDatabase sql;
     Calendar calendar = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        backButton = (Button)findViewById(R.id.backButton);
        timeSetButton = (Button)findViewById(R.id.timeSet);
        edittext = (EditText)findViewById(R.id.editText);
        alertTextView = (TextView)findViewById(R.id.timeText);
        sql=SQLiteDatabase.openOrCreateDatabase(this.getFilesDir().toString()+"/myDB.db3",null);
        user = new UserInfo();
        user.setAlerttime(alerttime);   //初始化提醒时间
        Intent intent = getIntent();
        Bundle bundle = intent.getBundleExtra("android.intent.extra.INTENT");
        datetime = bundle.getString("datetime");
        content = bundle.getString("content");
        alerttime = bundle.getString("alerttime");
        filename=bundle.getString("filename");
        index = bundle.getInt("index");
        tempContent = new String(content);
        tempDatetime = new String(datetime);
        tempAlerttime = new String(alerttime);
        Tools.createTable(sql);
        Time time = new Time();
        //判断该记录是新建还是修改
        if(datetime.equals(""))
        {
            time.setToNow();
        }
        else{
            time.set(Long.parseLong(datetime));
        }
        edittext.setText(content);
        String tempS = new String(alerttime);
        if(!alerttime.equals(""))
            alertTextView.setText(Tools.timeTransfer(tempS));
        else
            alertTextView.setText("");
        edittext.setCursorVisible(false);
        edittext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                edittext.setCursorVisible(true);
                edittext.setSelection(content.length());  //设置光标在文字末尾
            }
        });

        timeSetButton.setOnClickListener( new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                timeSetDialog = new TimeSetDialog(EditActivity.this);
                timeSetDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        alerttime = timeSetDialog.alerttime;
                        if(alerttime != null)
                            alertTextView.setText(Tools.timeTransfer(alerttime));
                        else
                            alertTextView.setText("");
                        calendar = timeSetDialog.calendar;
                        user.setAlerttime(alerttime);
                    }
                });
                timeSetDialog.show();

            }
        });
        backButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                save();
                Intent intent = new Intent(EditActivity.this, MainActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.in_from_left, R.anim.out_to_right);
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        save();
        Intent intent = new Intent(EditActivity.this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.in_from_left, R.anim.out_to_right);
    }
    private void save(){
        edittext = (EditText)findViewById(R.id.editText);
        Time time = new Time();
        time.setToNow();
        user.setAlerttime(alerttime);
        datetime =""+time.toMillis(true);
        user.setDatetime(datetime);
        time.set(time.toMillis(true));
        user.setFileIndex(filename);
        content = edittext.getText().toString();
        user.setContent(content);
        if((!content.isEmpty() && !tempContent.equals(content)) || !alerttime.equals("") && !alerttime.equals(tempAlerttime)){                   //如果内容非空且已经被更改，则更新显示和数据库
            ArrayList<HashMap<String,String>> list = Tools.getList();
            HashMap<String,String> map = new HashMap<String,String>();
            map.put("datetime",user.getDatetime());
            map.put("content",user.getContent());
            map.put("alerttime",user.getAlerttime());
            if(tempContent.isEmpty())  {					//若为新建记录则添加
                list.add(map);
                sql.execSQL("insert into user values('" + user.getDatetime() + "','" + user.getContent() + "','" + user.getAlerttime() + "','"+user.getFileIndex()+"')");
                if(user.getFileIndex().equals("备忘录")){
                    try {
                        Cursor cursor=sql.rawQuery("select * from files where filename='备忘录'",null);
                        if(!cursor.moveToNext()) {
                            sql.execSQL("insert into files values ('备忘录')");
                        }
                    }
                    catch (SQLException e){
                        sql.execSQL("create table files (filename varchar(30))");
                        Cursor cursor=sql.rawQuery("select * from files where filename='备忘录'",null);
                        if(!cursor.moveToNext()) {
                            sql.execSQL("insert into files values ('备忘录')");
                        }
                    }
                }
            }
            else {
                list.set(index, map);          //若为修改,则替换掉原来的记录
                sql.execSQL("update user set datetime='" + user.getDatetime() + "',content='" + user.getContent() + "',alerttime='" + user.getAlerttime() + "' where datetime='"+ tempDatetime+ "'");
            }
        }
        //设置闹钟提醒
        if(!alerttime.equals(tempAlerttime) && !alerttime.equals(""))
            alertSet();

    }

    private void alertSet(){
        Intent intent = new Intent("android.intent.action.ALARMRECEIVER");
        intent.putExtra("datetime", datetime);
        intent.putExtra("content", content);
        intent.putExtra("alerttime",alerttime);
        intent.putExtra("filename",filename);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(EditActivity.this, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP,calendar.getTimeInMillis(), pendingIntent);
    }
}
