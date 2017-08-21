package crazybaby.com.csrbuilder;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private TextView tv_info;
    private Command command;
    private String TAG = "MainActivity";
    private StringBuffer buffer = new StringBuffer();
    private CommanDialog dialog;
    private  boolean isUpdating=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_info = (TextView) findViewById(R.id.tv_info);
        buffer.append("start" + "\n");
        tv_info.setText(buffer.toString());
        dialog = new CommanDialog(this);
        dialog.onshow(1);
        dialog.setHandle(handler);

        command = new Command(new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (Command.HANDLId.valueOf(msg.what)) {
                    case CONNECTED:
                        Log.i(TAG, "Connnected");
                        buffer.append("connected" + "\n");
                        dialog.onshow(2);
                        isUpdating=false;
                        new FileTask().execute();
                        break;
                    case DISCONNECTED:
                        Log.i(TAG, " dis   Connnected");
                        buffer.append("dis   Connnected" + "\n");
                        if (isUpdating){
//                            dialog.onshow(5);
                        }else {
                            dialog.onshow(1);
                            isUpdating=false;
                        }

                        break;



                    case CRAFTREAD:
                        Log.i(TAG, " craft is  ok wait to order");
                        buffer.append(" craft is  ok wait to order" + "\n");
                        dialog.onshow(3);
                        break;
                    case STARTTRANSPORT:
                        Log.i(TAG, " starting STARTTRANSPORT");
                        buffer.append(" starting STARTTRANSPORT" + "\n");
                        dialog.onshow(4);
                        dialog.setProgressMax(command.getAllSize());
                        isUpdating=false;
                        break;

                    case TRANSPORTIONPROGRESS:

                        float now = (float) (msg.arg1) / (float) (command.getAllSize()) * 100;
                        DecimalFormat decimalFormat = new DecimalFormat(".0");
                        String p = decimalFormat.format(now);//
                        Log.i(TAG, " transporting " + p);
                        dialog.setProgressCurrent(msg.arg1);
                        isUpdating=false;
                        break;

                    case TRANSPORTFAIL:

                        Log.i(TAG, " TRANSPORTFAIL ");
                        buffer.append(" TRANSPORTFAIL" + "\n");
                        isUpdating=false;
                        break;
                    case TRANSPORTFINISH:

                        Log.i(TAG, " TRANSPORTFINISH ");
                        buffer.append(" TRANSPORTFINISH" + "\n");
                        isUpdating=true;
                        break;
                    case VERIFYING:
                        Log.i(TAG, " VERIFYING");
                        buffer.append(" VERIFYING" + "\n");
                        dialog.onshow(5);
                        break;

                    case VERIFYFAIL:
                        Log.i(TAG, " VERIFYFAIL");
                        buffer.append(" VERIFYFAIL" + "\n");

                        break;
                    case UPGRADING:
                        Log.i(TAG, " craft upgrading");
                        buffer.append(" craft upgrading" + "\n");

                        break;
                    default:
                        break;
                }
                tv_info.setText(buffer.toString());
                tv_info.setVisibility(View.INVISIBLE);

            }
        }, this);
        command.init();

        getconnectAddress();
    }
//    long exitTime;
//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//
//        if (keyCode == KeyEvent.KEYCODE_BACK){
//            if((System.currentTimeMillis()-exitTime) > 3000){
//                Toast.makeText(MainActivity.this, R.string.exit_tips, Toast.LENGTH_SHORT).show();
//                exitTime = System.currentTimeMillis();
//                return false;
//            } else {
////            if (SideMenuActivity.musicService!=null){
////                SideMenuActivity.musicService.stopSelf();
////            }
////            finishThis();
//
////                MobclickAgent.onKillProcess(getApplicationContext());
////                finish();
//
////
////            android.os.Process.killProcess(android.os.Process.myPid());
////                android.os.Process.killProcess(android.os.Process.myPid());
////                System.exit(1);
////            System.exit(0);
//                MainActivity.this.finish();
//                return true;
//            }
//        }
//
//        return super.onKeyDown(keyCode, event);
//    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (msg.what == 1) {
                CharSequence[] items;
                List<BluetoothDevice> devices = mBluetoothHeadset.getConnectedDevices();
                buffer.append("ble scanning" + "\n");
                tv_info.setText(buffer.toString());
                if (devices.size() == 0) {
                    handler.sendEmptyMessageDelayed(1, 1000);
                    buffer.append("ble scanning no found" + "\n");
                    tv_info.setText(buffer.toString());
                }
                for (int i = 0; i < devices.size(); i++) {
                    BluetoothDevice device = devices.get(i);
                    String address = device.getAddress();
                   String name= device.getName();

                    items = command.getList();
                    for (int a = 0; a < items.length; a++) {
                        String s[] = ((String) items[a]).split("\n");
                        if (s[1].equals(address)) {
                            buffer.append("compear succ,connect" + "\n");
                            tv_info.setText(buffer.toString());
                            command.Connect(address);
                            break;

                        }
                        //                    String b
                    }

                }
            }else if (msg.what == 2) {
                start();
            }else if (msg.what==3){
            File file = files[msg.arg1];
            command.setUdfFile(file);
            dialog.onshow(2);
            }else if (msg.what==4){
                command.disconnect();
                MainActivity.this.finish();
            }
        }
    };


    public void start() {
        command.start();
    }

    BluetoothHeadset mBluetoothHeadset;

    private void getconnectAddress() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        buffer.append("check ble" + "\n");
        tv_info.setText(buffer.toString());
        mBluetoothAdapter.getProfileProxy(this, new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                Log.d(TAG, "onServiceConnected" + "profile=" + profile + "===============BluetoothProfile.HEADSET=" + BluetoothProfile.HEADSET);
                buffer.append("onServiceConnected" + "\n");
                tv_info.setText(buffer.toString());
                if (profile == BluetoothProfile.HEADSET) {
                    mBluetoothHeadset = (BluetoothHeadset) proxy;
                    handler.sendEmptyMessage(1);
                }
            }

            @Override
            public void onServiceDisconnected(int profile) {
                if (profile == BluetoothProfile.HEADSET) {
                    mBluetoothHeadset = null;
                }

            }
        }, BluetoothProfile.HEADSET);
    }



    private String filepath;
    private String udfPathold = "custme.dfu";
    private String udfPathv25 = "v25.dfu";
    private String udfPathv27 = "v27.dfu";
    private String udfPathv27_rose = "v27_rosegold.dfu";
    private String udfPathv25_america = "mars_v25_america_demo_0705";
    private String udfPathv25_0801america = "mars_v25_america_demo_0801min";
    private String udfPathv25_gray = "v25_gray";

    private void copytSD(String assetspath,String filepath){
        File file;
        Log.d("tag",filepath);
        try {
        file=new File(filepath);
        if (!file.exists()){
//            new File(filepath).createNewFile();
            file.createNewFile();
//            return;
            boolean c= file.canWrite();
            boolean b= file.isDirectory();


        }

//            file=new File(filepath);
//            file.createNewFile();
            OutputStream myOutput = new FileOutputStream(filepath);
            InputStream myInput = getClass().getClassLoader().getResourceAsStream(assetspath);
            byte[] buffer = new byte[1024];
            int length = myInput.read(buffer);
            while (length > 0) {
                myOutput.write(buffer, 0, length);
                length = myInput.read(buffer);
            }

            myOutput.flush();
            myInput.close();
            myOutput.close();
//        }
        } catch (IOException e) {
            e.printStackTrace();
        }
    };

    class FileTask extends AsyncTask {
        File file;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
           File filea =getFilesDir();
            Log.d("filea",filea.getAbsolutePath());
            File fileb=new File(filea.getAbsolutePath()+"/dfu");
            if (!fileb.exists()){
                fileb.mkdirs();
            }else {
                File files[] = fileb.listFiles(); // 声明目录下所有的文件 files[];
                for (int i = 0; i < files.length; i++) { // 遍历目录下所有的文件
                    if (!files[i].getName().equals(udfPathv25_america)) {
                        files[i].delete(); // 把每个文件 用这个方法进行迭代
                    }
                    if (files[i].isDirectory()){
                        files[i].delete();
                    }
                }
            }
            Log.d("fileb",fileb.getAbsolutePath());
            boolean c= fileb.canWrite();
            boolean b= fileb.isDirectory();
            filepath = fileb.getAbsolutePath();
            Log.d("create dir",filepath);
        }

        @Override
        protected Object doInBackground(Object[] params) {
//            try {

                String strFilePathold = filepath+ "/"+udfPathold;
                String filePathv25=filepath+ "/"+udfPathv25;
                String filePathv27=filepath+ "/"+udfPathv27;
                String filePathv27rose=filepath+ "/"+udfPathv27_rose;
                String filePathv25america=filepath+ "/"+udfPathv25_america;
                String filePathv25_0801america=filepath+ "/"+udfPathv25_0801america;
                String filePathv25Gray=filepath+ "/"+udfPathv25_gray;


                copytSD("assets/mars_v25_custom_dfu.dfu",filePathv25);
                copytSD("assets/mars_v27_custom_dfu.dfu",filePathv27);
                copytSD("assets/mars_v27_rose_dfu.dfu",filePathv27rose);
                copytSD("assets/mars_v25_america_demo_0705.dfu",filePathv25america);
                copytSD("assets/mars_v25_america_demo_0801min.dfu",filePathv25_0801america);
                copytSD("assets/mars_v25_gray_dfu.dfu",filePathv25Gray);

//                File filev25 = new File(filePathv25);
//                File filev27 = new File(filePathv27);
//                if (file.exists()) {
//                    Log.d("TestFile", "Create the file:" + strFilePathold);
//                    file.delete();
//
//
//                }else {
//                    file.createNewFile();
//                }
//
//
//                OutputStream myOutput = new FileOutputStream(strFilePathold);
//                InputStream myInput = getClass().getClassLoader().getResourceAsStream("assets/mars_v25_custom_dfu.dfu");
//                byte[] buffer = new byte[1024];
//                int length = myInput.read(buffer);
//                while (length > 0) {
//                    myOutput.write(buffer, 0, length);
//                    length = myInput.read(buffer);
//                }
//
//                myOutput.flush();
//                myInput.close();
//                myOutput.close();
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);

            File dfuFile=new File(filepath);
            files = dfuFile.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return true;
                }
            });
            if (files==null){
                return;
            }
            if (files.length>0){
                dialog.setFile(files);
            }
            dialog.onshow(6);
//
//            String strFilePath = filepath+ "/"+udfPath;
//            File file = new File(strFilePath);
//            command.setUdfFile(file);
        }
    }

    File[] files;
    // 生成文件
    public File makeFilePath(String filePath, String fileName) {
        File file = null;
        makeRootDirectory(filePath);
        try {
            file = new File(filePath + "/"+fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

    // 生成文件夹
    public static void makeRootDirectory(String filePath) {
        File file = null;
        try {
            file = new File(filePath);
            if (!file.exists()) {
                file.mkdir();
            }
        } catch (Exception e) {
            Log.i("error:", e + "");
        }


    }
}
