package com.gystry.runview;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private RunwayView runView;
    private volatile Bitmap bitmap = null;
    private List<OtherOneData> mList;
    Bundle bundle;
    private TextView tvContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        runView = (RunwayView) findViewById(R.id.rv_runview);
        tvContent = (TextView) findViewById(R.id.tv_content);
        tvContent.setText(null);
        mList = new ArrayList<>();
        bundle = new Bundle();
        mList.add(new OtherOneData(200, "http://img1.imgtn.bdimg.com/it/u=1412195272,3821185777&fm=26&gp=0.jpg"));
        mList.add(new OtherOneData(100, "http://img1.imgtn.bdimg.com/it/u=3771844506,3707807471&fm=26&gp=0.jpg"));
        mList.add(new OtherOneData(240, "http://www.caisheng.net/UploadFiles/img_3_3370409597_881988130_27.jpg"));
        mList.add(new OtherOneData(70, "http://img2.imgtn.bdimg.com/it/u=2620274141,1246758567&fm=26&gp=0.jpg"));
        mList.add(new OtherOneData(345, "http://img4.imgtn.bdimg.com/it/u=4147604174,1642687028&fm=214&gp=0.jpg"));
        mList.add(new OtherOneData(600, "http://s9.rr.itc.cn/r/wapChange/20165_16_9/a5zar895979378246596.jpg"));
        mList.add(new OtherOneData(200, "http://b-ssl.duitang.com/uploads/item/201701/20/20170120123909_RW8FP.jpeg"));
        OtherDataCache.getInstance().setList(mList);
        new Thread(new Runnable() {
            int i = 0;
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    i++;
                    Message message = handler.obtainMessage(0);
                    message.arg1 = i;
                    handler.sendMessage(message);

                    for (OtherOneData otherOneData : mList) {
                        if (otherOneData.getBitmap() == null) {
                            try {
                                Bitmap myBitmap = Glide.with(getApplication()).asBitmap().load(otherOneData.getImgUrl()).submit(40, 40).get();
                                bitmap = Bitmap.createBitmap(myBitmap, 0, 0, myBitmap.getWidth(), myBitmap.getHeight());
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (ExecutionException e) {
                                e.printStackTrace();
                            }
                            otherOneData.setBitmap(bitmap);
                            Log.e("MainActivity", "run: 1111111111 :" + otherOneData.getBitmap() + " : " + otherOneData.getDistance() + " : " + otherOneData.getImgUrl());
                        }
                        otherOneData.setDistance(otherOneData.getDistance() + 1);
                    }
                    OtherDataCache.getInstance().setList(mList);

                    Message message1 = handler.obtainMessage(1);
//                    bundle.putParcelableArrayList("otherData", (ArrayList<? extends Parcelable>) mList);
                    bundle.putString("otherData", "" + i);
                    Log.e("MainActivity", "&*&*:run: " + bundle);
                    message.setData(bundle);
                    handler.sendMessage(message1);
                }
            }
        }).start();
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    int arg1 = msg.arg1;
                    runView.setmMovingDistance(arg1);
                    break;
                case 1:
                    Bundle data = msg.peekData();
                    //                    List<OtherOneData> otherData = (List<OtherOneData>) data.get("otherData");
                    Log.e("MainActivity", "&*&*:handleMessage: " + data);

                    List<OtherOneData> list = OtherDataCache.getInstance().getList();
                    runView.setDataList(list);
                    break;
            }

        }
    };
}
