package com.gystry.runview;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

public class OtherOneData implements Parcelable {
    private Bitmap bitmap;
    private int distance;
    private String imgUrl;

    public OtherOneData(int distance, String imgUrl) {
        this.distance = distance;
        this.imgUrl = imgUrl;
    }

    protected OtherOneData(Parcel in) {
        bitmap = in.readParcelable(Bitmap.class.getClassLoader());
        distance = in.readInt();
        imgUrl = in.readString();
    }

    public static final Creator<OtherOneData> CREATOR = new Creator<OtherOneData>() {
        @Override
        public OtherOneData createFromParcel(Parcel in) {
            return new OtherOneData(in);
        }

        @Override
        public OtherOneData[] newArray(int size) {
            return new OtherOneData[size];
        }
    };

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            this.bitmap = bitmap;
        }
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(bitmap, flags);
        dest.writeInt(distance);
        dest.writeString(imgUrl);
    }
}
