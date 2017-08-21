package crazybaby.com.csrbuilder;

import android.app.Dialog;
import android.content.Context;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by walle on 2016/4/19.
 */
public class CommanDialog extends Dialog {
    Context context;
    private TextView describle,action,timer;
    private ProgressBar progressBar;
    private  Handler handler;
    private  File[] file;
    private ListView listView;

    public void setFile(File[] file) {
        this.file = file;
    }

    public CommanDialog(Context context) {
        super(context);
        context=getContext();
        init(context);
    }

    public CommanDialog(Context context, int themeResId) {
        super(context, themeResId);
        init(context);
    }

    public  void setHandle(Handler handler){
        this.handler=handler;
    }
    private void init(Context context){
        setContentView(R.layout.dialog_main);
//        title= (TextView) findViewById(R.id.tv_title);
        describle= (TextView) findViewById(R.id.tv_describle);
        timer= (TextView) findViewById(R.id.timer);
        action= (TextView) findViewById(R.id.tv_action);
        listView= (ListView) findViewById(R.id.listview);
        progressBar= (ProgressBar) findViewById(R.id.progressBar);
        setCanceledOnTouchOutside(false);
        action.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handler.sendEmptyMessage(2);

            }
        });
        describle.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                dismiss();
                return true;
            }
        });
        timer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (timer.getText().toString().equals("完成")){
                    (CommanDialog.this.getOwnerActivity()).finish();
                }
            }
        });



    }
    public void setProgressMax(int max){
        progressBar.setMax(max);
    }
    public  void setProgressCurrent(int process){
        progressBar.setProgress(process);
    }

    /**
     *
     * @param type 1=等待连接完成，2=等待飞碟进入配对状态，3=飞碟已经准备好
     */
    public  void onshow(int type){
        describle.setTextColor(0xff6baafc);

        switch (type){
            case 1:
//                title.setText("等待连接完成");
                describle.setText(R.string.confirmtoconnect_tips);
                action.setVisibility(View.INVISIBLE);
                progressBar.setVisibility(View.INVISIBLE);
                timer.setVisibility(View.INVISIBLE);
                listView.setVisibility(View.INVISIBLE);
                setTitle(R.string.confirmtoconnect_title);
                break;
            case 2:
                setTitle(R.string.waitfordeviceok_title);
//                title.setText("等待设备进入升级状态");
                describle.setText(R.string.waitfordeviceok_tips);
                action.setVisibility(View.INVISIBLE);
                progressBar.setVisibility(View.INVISIBLE);
                timer.setVisibility(View.INVISIBLE);
                listView.setVisibility(View.INVISIBLE);
                break;

            case 3:
                setTitle(R.string.waittostart_title);
//                title.setText("一切就绪");
                describle.setText("");
                action.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.INVISIBLE);
                timer.setVisibility(View.INVISIBLE);
                listView.setVisibility(View.INVISIBLE);
                break;
            case 4:
                setTitle(R.string.transporting_title);
//                title.setText("正在传输");
                describle.setText(R.string.transporting_tips);
                action.setVisibility(View.INVISIBLE);
                timer.setVisibility(View.INVISIBLE);
                progressBar.setVisibility(View.VISIBLE);
                listView.setVisibility(View.INVISIBLE);
                break;
            case 5:
                setTitle(R.string.updating_title);
//                title.setText("正在升级");
                describle.setTextColor(0xfffc0303);
                describle.setText(R.string.updating_tips);
                action.setVisibility(View.INVISIBLE);
                progressBar.setVisibility(View.INVISIBLE);
                timer.setVisibility(View.VISIBLE);
                listView.setVisibility(View.INVISIBLE);
                new CountDownTimer(2*60*1000,1000){
                    @Override
                    public void onTick(long millisUntilFinished) {
                        int timesencend= (int) (millisUntilFinished/1000);
                        int min=timesencend/60;
                        int send=timesencend%60;
                        String time=min+":"+send;
                        timer.setText(time);
                    }

                    @Override
                    public void onFinish() {
                        timer.setText(R.string.finish_tips);

                    }
                }.start();

                break;
            case 6:
                setTitle(R.string.selectversion_title);
                describle.setText("");
                action.setVisibility(View.INVISIBLE);
                progressBar.setVisibility(View.INVISIBLE);
                timer.setVisibility(View.INVISIBLE);
                listView.setVisibility(View.VISIBLE);
                List<Map<String, Object>> map = new ArrayList<Map<String, Object>>();

                for (int i = 0; i <file.length ; i++) {
                    Map<String, Object> listem = new HashMap<String, Object>();
                    listem.put("name",file[i].getName());
                    map.add(listem);
                }
                SimpleAdapter simpleAdapter=new SimpleAdapter(getContext(),map,R.layout.list_item, new String[] { "name" },
                        new int[] {R.id.tv_item});
                listView.setAdapter(simpleAdapter);
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        Message msg=handler.obtainMessage();
                        msg.what=3;
                        msg.arg1=position;
                        handler.sendMessage(msg);
                    }
                });

        }
        if (isShowing()){
            dismiss();
        }
        show();

    }
    long exitTime;
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK){
            if((System.currentTimeMillis()-exitTime) > 3000){
                Toast.makeText(getContext(), R.string.exit_tips, Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
                return true;
            } else {
//            if (SideMenuActivity.musicService!=null){
//                SideMenuActivity.musicService.stopSelf();
//            }
//            finishThis();

//                MobclickAgent.onKillProcess(getApplicationContext());
//                finish();

//
//            android.os.Process.killProcess(android.os.Process.myPid());
//                android.os.Process.killProcess(android.os.Process.myPid());
//                System.exit(1);
//            System.exit(0);
                handler.sendEmptyMessage(4);
//                getOwnerActivity().finish();
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }
}
