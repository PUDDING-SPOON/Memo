package com.example.whisk.schedulememo;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

public class FileContent extends ListActivity {
    Button addfile;
    int index;
    String fn;
    SQLiteDatabase sql;
    SharedPreferences sp;
    SharedPreferences.Editor editor;
    SimpleAdapter filelistAdapter;
    PopupWindow fPopupWindow = null;
    ArrayList<HashMap<String, String>> filelist = Tools.getList();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_content);
        addfile = (Button) findViewById(R.id.addfile);
        sp = getSharedPreferences("file", MODE_PRIVATE);
        fn = sp.getString("filename", "备忘录").toString();
        editor = sp.edit();
        sql = SQLiteDatabase.openOrCreateDatabase(this.getFilesDir().toString() + "/myDB.db3", null);
        Tools.createTable(sql);
        //载入数据库的数据
        filelist.clear();
        loadFromDatabase(filelist);      //从数据库加载文件目录和文件数量
        filelistAdapter = new SimpleAdapter(this, filelist, R.layout.list_item, new String[]{"fileIndex", "fileCounts"},
                new int[]{R.id.datetime, R.id.counts});
        setListAdapter(filelistAdapter);        //将文件夹内的备忘录数据显示出来
        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent fileintent = new Intent(FileContent.this, MainActivity.class);
                String fn = Tools.getItem(position).get("fileIndex");
                if (position == 0)
                    fn = "所有备忘录";
                editor.putString("filename", fn);
                editor.commit();
                startActivity(fileintent);                                //启动转到所选文件夹的Activity
                overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
                finish();
            }
        });

        addfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder localBuilder = new AlertDialog.Builder(FileContent.this);
                final EditText editFile = new EditText(FileContent.this);
                localBuilder.setTitle(R.string.editFile);
                localBuilder.setView(editFile);
                localBuilder.setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            Boolean flag = true;
                            String filename = editFile.getText().toString();
                            Cursor c = sql.rawQuery("select filename from  files ", null);
                            while (c.moveToNext()) {        //判断文件名是否重复
                                if (c.getString(0).equals(filename)) {
                                    Toast.makeText(getApplicationContext(), "文件夹已存在，请选取其它名称", Toast.LENGTH_LONG).show();
                                    flag = false;
                                    break;
                                }
                            }
                            if (flag) {
                                sql.execSQL("insert into files values ('" + filename + "')");
                                filelist.clear();       //重新加载列表刷新数据
                                loadFromDatabase(filelist);
                                setListAdapter(filelistAdapter);
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                            Toast.makeText(getApplicationContext(), "文件名非法，请重新输入！", Toast.LENGTH_LONG).show();
                        }
                    }
                });
                localBuilder.setNegativeButton(R.string.cancel, null);
                localBuilder.show();
            }
        });

        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                index = position;            //第一条统计数据不可编辑
                if(Tools.getItem(position).get("fileIndex").equals("所有备忘录"))
                    return true;
                View popupView = getLayoutInflater().inflate(R.layout.popupwindow, null);
                fPopupWindow = new PopupWindow(popupView, WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT, true);
                fPopupWindow.setAnimationStyle(R.style.popupAnimation);
                fPopupWindow.setFocusable(true);
                fPopupWindow.setBackgroundDrawable(new BitmapDrawable());   // 使得touch弹窗以外的地方或者按返回键才会消失而且Drawable不能用null代替
                fPopupWindow.showAtLocation(popupView, Gravity.BOTTOM, 0, 0);
                Button moveButton = (Button) popupView.findViewById(R.id.moveButton);
                Button deleteButton = (Button) popupView.findViewById(R.id.deleteButton);
                Button shareButton = (Button) popupView.findViewById(R.id.shareButton);
                moveButton.setVisibility(View.GONE);
                deleteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(FileContent.this);
                        // 设置参数
                        builder.setTitle("删除文件夹")
                                .setMessage("确认要删除文件夹及文件夹内的文件吗？")
                                .setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        // TODO Auto-generated method stub
                                        deleteFile(index);
                                        dialog.dismiss();
                                        fPopupWindow.dismiss();
                                    }
                                }).setNegativeButton(R.string.cancel, null).show();
                    }
                });
                shareButton.setVisibility(View.GONE);
                return true;
            }
        });
    }



    private void loadFromDatabase(ArrayList<HashMap<String,String>> list){
        try {
            Cursor c = sql.rawQuery("select count(*) from  user ", null);
            if(c.moveToNext()){              //第一条为所有文件数量的统计
                String counts = c.getString(0);
                HashMap<String,String> map = new HashMap<String,String>();
                map.put("fileIndex","所有备忘录" );
                map.put("fileCounts", counts);
                list.add(map);
            }
            Cursor cursor = sql.rawQuery("select filename,count(fileIndex) counts from files left join user on user.fileIndex=files.filename group by filename", null);
            while (cursor.moveToNext()) {
                String filename = cursor.getString(0);
                String counts = cursor.getString(1);
                HashMap<String,String> map = new HashMap<String,String>();
                map.put("fileIndex", filename);
                map.put("fileCounts", counts);
                list.add(map);
            }
        }
        catch (SQLiteException e){
            e.printStackTrace();
        }
    }


    private boolean deleteFile(int index){
        ListView listview = getListView();
        String deleteFile = ((HashMap<String, String>)(listview.getItemAtPosition(index))).get("fileIndex").toString();
        sql.execSQL("delete from files where filename='"+deleteFile+"'");
        sql.execSQL("delete from user where fileIndex='"+deleteFile+"'");
        filelist.clear();       //重新加载列表刷新数据
        loadFromDatabase(filelist);
        setListAdapter(filelistAdapter);
        return true;
    }


}
