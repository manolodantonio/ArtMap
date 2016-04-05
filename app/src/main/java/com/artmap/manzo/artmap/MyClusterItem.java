package com.artmap.manzo.artmap;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

/**
 * Created by Manzo on 27/01/2015.
 */
public class MyClusterItem implements ClusterItem, Parcelable {

    private LatLng mPosition;
    private Integer mId;
    private String mUser;
    private String mSnippet = "";
    private Double mLatitude;
    private Double mLongitude;
    private String mTitle;
    private String mAuthor;
    private String mAuthorLink;
    private Integer mYear;
    private Integer mVisibility;
    private String mTag;
    private boolean mCheckIn;




    public MyClusterItem(Integer id,
                         String user,
                         double lat,
                         double lng,
                         String snippet,
                         String title,
                         String author,
                         String authorLink,
                         Integer year,
                         Integer visibility,
                         String tag,
                         String checkin) {


        mId = id;
        mUser = user;
        mLatitude = lat;
        mLongitude = lng;
        if (snippet!=null) {mSnippet = snippet;}
        mTitle = title;
        mAuthor = author;
        mAuthorLink = authorLink;
        mYear = year;
        mVisibility = visibility;
        mTag = tag;
        mCheckIn = Boolean.parseBoolean(checkin);

        mPosition = new LatLng(lat, lng);
    }

    public MyClusterItem(Parcel in) {
        readFromParcel(in);
    }

    @Override
    public LatLng getPosition() {
        return mPosition;
    }

    public Double getmLatitude() {
        return mLatitude;
    }

    public Double getmLongitude() {
        return mLongitude;
    }

    public String getmTitle() {
        return mTitle;
    }

    public Integer getmId() {
        return mId;
    }

    public String getmSnippet() {
        return mSnippet;
    }

    public String getmUser() {
        return mUser;
    }

    public String getmTag() {
        return mTag;
    }

    public Integer getmVisibility() {
        return mVisibility;
    }

    public Integer getmYear() {
        return mYear;
    }

    public String getmAuthorLink() {
        return mAuthorLink;
    }

    public String getmAuthor() {
        return mAuthor;
    }

    public boolean getmCheckIn() {
        return mCheckIn;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mId);
        dest.writeString(mUser);
        dest.writeDouble(mLatitude);
        dest.writeDouble(mLongitude);
        dest.writeString(mSnippet);
        dest.writeString(mTitle);

        dest.writeString(mAuthor);
        dest.writeString(mAuthorLink);
        dest.writeInt(mYear);
        dest.writeInt(mVisibility);
        dest.writeString(mTag);


    }

    private void readFromParcel (Parcel in) {
        mId = in.readInt();
        mUser = in.readString();
        mLatitude = in.readDouble();
        mLongitude = in.readDouble();
        mSnippet = in.readString();
        mTitle = in.readString();

        mAuthor = in.readString();
        mAuthorLink = in.readString();
        mYear = in.readInt();
        mVisibility = in.readInt();
        mTag = in.readString();

    }

   public static final Creator CREATOR =
           new Creator() {
               @Override
               public Object createFromParcel(Parcel source) {
                   return new MyClusterItem(source);
               }

               @Override
               public Object[] newArray(int size) {
                   return new Object[size];
               }
           };

}
