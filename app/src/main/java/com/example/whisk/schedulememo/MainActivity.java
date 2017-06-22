package com.example.whisk.schedulememo;


import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.SearchView;
import android.view.View;
import android.widget.Button;
import android.widget.SimpleAdapter;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends ListActivity implements SearchView.OnQueryTextListener{
    Button btn,btn1;
    SearchView sv;
    SimpleAdapter listAdapter;
    int index = 0;// 选中数据的索引
    String filename;
    PopupWindow mPopupWindow = null;
    SQLiteDatabase sql;
    ArrayList<HashMap<String,String>> showlist,list = Tools.getList();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);      //设置键盘不自动弹出
        setContentView(R.layout.activity_main);
        btn= (Button) findViewById(R.id.button);
        sv = (SearchView)findViewById(R.id.searchView);
        sv.setFocusable(false);
        sv.setOnQueryTextListener(this);
        filename=getSharedPreferences("file",0).getString("filename","备忘录").toString();
        TextView tv= (TextView) findViewById(R.id.textView);
        tv.setText(filename);
        sql=SQLiteDatabase.openOrCreateDatabase(this.getFilesDir().toString()+"/myDB.db3",null);
        //载入数据库的数据
        list.clear();
        loadFromDatabase(list);      //从数据库加载数据
        Tools.sort();
        Tools.MillisToDate(list);
        listAdapter = new SimpleAdapter(this,list,R.layout.list_item,new String[]{"datetime","content"},
                new int[]{R.id.datetime,R.id.content});
        setListAdapter(listAdapter);                      //将备忘录数据显示出来
        btn1 = (Button)findViewById(R.id.createButton);     //创建新的备忘录
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Tools.DateToMillis(list);
                if(filename.equals("所有备忘录"))
                    filename="备忘录";
                Intent intent = new Intent(MainActivity.this, EditActivity.class);
                Bundle b = new Bundle();
                b.putString("datetime", "");
                b.putString("content", "");
                b.putString("alerttime","");
                b.putString("filename",filename);
                intent.putExtra("android.intent.extra.INTENT", b);
                startActivity(intent);                                //启动转到的Activity
            }
        });
        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {      //编辑所点击的备忘录
                Intent itemintent = new Intent(MainActivity.this,EditActivity.class);
                Tools.DateToMillis(list);
                Bundle b = new Bundle();
                b.putString("datetime", Tools.getItem(position).get("datetime"));
                b.putString("content", Tools.getItem(position).get("content"));
                b.putString("alerttime", Tools.getItem(position).get("alerttime"));
                b.putInt("index", position);
                itemintent.putExtra("android.intent.extra.INTENT", b);
                startActivity(itemintent);                                //启动转到的Activity
                overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
            }
        });
        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {        //监听长按事件
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                index = position;
                View popupView = getLayoutInflater().inflate(R.layout.popupwindow,null);
                mPopupWindow = new PopupWindow(popupView, WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT, true);
                mPopupWindow.setAnimationStyle(R.style.popupAnimation);
                mPopupWindow.setFocusable(true);
                mPopupWindow.setBackgroundDrawable(new BitmapDrawable());   // 使得touch弹窗以外的地方或者按返回键才会消失而且Drawable不能用null代替
                mPopupWindow.showAtLocation(popupView, Gravity.BOTTOM, 0, 0);
                Button moveButton = (Button)popupView.findViewById(R.id.moveButton);
                Button deleteButton = (Button)popupView.findViewById(R.id.deleteButton);
                Button shareButton = (Button)popupView.findViewById(R.id.shareButton);
                moveButton.setVisibility(View.VISIBLE);
                shareButton.setVisibility(View.VISIBLE);
                moveButton.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        moveItem(index);
                        mPopupWindow.dismiss();
                    }
                });
                deleteButton.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        deleteItem(index);
                        mPopupWindow.dismiss();
                    }
                });
                shareButton.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        shareItem(index);
                        mPopupWindow.dismiss();
                    }
                });
                return true;
            }
        });
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {       //跳转到文件夹选择界面
                Intent intent = new Intent(MainActivity.this, FileContent.class);
                startActivity(intent);
                overridePendingTransition(R.anim.in_from_left, R.anim.out_to_right);
                onDestroy();
            }
        });
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        list = Tools.getList();
        if(newText != null){
            showlist = new ArrayList<HashMap<String,String>>();
            for(int i=0;i<list.size();i++){
                String content = list.get(i).get("content");
                if(content.contains(newText)){
                    HashMap<String,String> map = list.get(i);
                    map.put("id", String.valueOf(i));
                    showlist.add(map);
                }
            }
            listAdapter = new SimpleAdapter(this,showlist,R.layout.list_item,new String[]{"datetime","content"},
                    new int[]{R.id.datetime,R.id.content});
            setListAdapter(listAdapter);
            getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    Intent searchintent = new Intent(MainActivity.this,EditActivity.class);
                    Tools.DateToMillis(list);
                    Bundle b = new Bundle();
                    b.putString("datetime", showlist.get(position).get("datetime"));
                    b.putString("content", showlist.get(position).get("content"));
                    b.putString("alerttime",showlist.get(position).get("alerttime"));
                    b.putInt("index", Integer.parseInt(showlist.get(position).get("id")));
                    searchintent.putExtra("android.intent.extra.INTENT", b);
                    startActivity(searchintent);                                //启动转到的Activity
                    overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
                }
            });
        }
        return false;
    }
    private void loadFromDatabase(ArrayList<HashMap<String,String>> list){
        try{
            Cursor cursor;
            if(filename.equals("所有备忘录"))
                 cursor=sql.rawQuery("select * from user order by datetime desc ",null);
            else
                 cursor=sql.rawQuery("select * from user where fileIndex='"+filename+"' order by datetime desc ",null);
            while (cursor.moveToNext()) {
                for (int i = 0; i < cursor.getCount(); i++) {
                    cursor.moveToPosition(i);
                    String datetime = cursor.getString(0);
                    String content = cursor.getString(1);
                    String alerttime = cursor.getString(2);
                    String filename = cursor.getString(3);
                    HashMap<String,String> map = new HashMap<String,String>();
                    map.put("datetime", datetime);
                    map.put("content", content);
                    map.put("alerttime", alerttime);
                    map.put("filename", filename);
                    list.add(map);
                }
            }
        }
        catch (SQLException se){
            se.printStackTrace();
        }

    }

    @Override
    public void onBackPressed() {
        Tools.DateToMillis(list);
        this.finish();
        super.onBackPressed();
    }

    private void moveItem(final int index) {
        int i=0;
        final ListView listview = getListView();
        final Cursor cursor=sql.rawQuery("select * from files",null);
        final String[] s = new String[cursor.getCount()-1];
        Tools.DateToMillis(list);
        final String moveDatetime = ((HashMap<String, String>)(listview.getItemAtPosition(index))).get("datetime").toString();
        String fn = ((HashMap<String, String>)(listview.getItemAtPosition(index))).get("filename").toString();
        while (cursor.moveToNext()) {
            String str = cursor.getString(0);
            if(!str.equals(fn))
                s[i++] = str;
        }
        cursor.close();
        new AlertDialog.Builder(this)
                .setTitle("移动到")
                .setSingleChoiceItems(s,-1, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                            String fn=s[which];
                            sql.execSQL("update user set fileIndex='"+fn+"' where datetime='"+moveDatetime+"'");
                        Tools.getList().remove(index);
                        Tools.sort();
                        Tools.MillisToDate(list);
                        listAdapter.notifyDataSetChanged();                           //更新ListView的数据
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private boolean deleteItem(int index){
        Tools.DateToMillis(list);
        ListView listview = getListView();
        String deleteDatetime = ((HashMap<String, String>)(listview.getItemAtPosition(index))).get("datetime").toString();
        Tools.getList().remove(index);
        sql.execSQL("delete from user where datetime='"+deleteDatetime+"'");
        Tools.sort();
        Tools.MillisToDate(list);
        listAdapter.notifyDataSetChanged();                           //更新ListView的数据
        return true;
    }

    private void shareItem(int index) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "分享");
        intent.putExtra(Intent.EXTRA_TEXT, "来自备忘录分享："+Tools.getItem(index).get("content"));
        startActivity(Intent.createChooser(intent, "分享到"));
    }
}
