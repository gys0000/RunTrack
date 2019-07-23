package com.gystry.runview;

import android.graphics.Bitmap;

public class ViewData {
    private float[] mOtherCurrentPos;
    private Bitmap bitmap;

    public ViewData(float[] mOtherCurrentPos, Bitmap bitmap) {
        this.mOtherCurrentPos = mOtherCurrentPos;
        this.bitmap = bitmap;
    }

    public float[] getmOtherCurrentPos() {
        return mOtherCurrentPos;
    }

    public void setmOtherCurrentPos(float[] mOtherCurrentPos) {
        this.mOtherCurrentPos = mOtherCurrentPos;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }
}
