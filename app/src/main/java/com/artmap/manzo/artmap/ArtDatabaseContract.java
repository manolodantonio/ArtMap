package com.artmap.manzo.artmap;

import android.provider.BaseColumns;

/**
 * Created by Manzo on 06/01/2015.
 */
public final class ArtDatabaseContract {
    // costruttore vuoto per evitare instanze accidentali
    public ArtDatabaseContract(){}

    // classe interna che definisce i contenuti delle tabelle

    public static abstract class ArtEntries implements BaseColumns{
        public static final String TABLE_NAME = "artDb";
        public static final String COLUMN_NAME_USER = "user";
        public static final String COLUMN_NAME_LATITUDE = "latitude";
        public static final String COLUMN_NAME_LONGITUDE = "longitude";
        public static final String COLUMN_NAME_SNIPPET = "snippet";
        public static final String COLUMN_NAME_TITLE = "title";

        public static final String COLUMN_NAME_AUTHOR = "author";
        public static final String COLUMN_NAME_AUTHORLINK = "authorLink";
        public static final String COLUMN_NAME_YEAR = "year";
        public static final String COLUMN_NAME_VISIBILITY = "visibility";
        public static final String COLUMN_NAME_TAG = "tag";
        public static final String COLUMN_NAME_CHECKIN = "checkin";
    }

    //creazione tabelle con linguaggio SQL

    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ", ";

    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + ArtEntries.TABLE_NAME + " (" +
            ArtEntries._ID + " INTEGER PRIMARY KEY," +
            ArtEntries.COLUMN_NAME_USER + TEXT_TYPE + COMMA_SEP +
            ArtEntries.COLUMN_NAME_LATITUDE + TEXT_TYPE + COMMA_SEP +
            ArtEntries.COLUMN_NAME_LONGITUDE + TEXT_TYPE + COMMA_SEP +
            ArtEntries.COLUMN_NAME_SNIPPET + TEXT_TYPE + COMMA_SEP +
            ArtEntries.COLUMN_NAME_TITLE + TEXT_TYPE + COMMA_SEP +
            ArtEntries.COLUMN_NAME_AUTHOR + TEXT_TYPE + COMMA_SEP +
            ArtEntries.COLUMN_NAME_AUTHORLINK + TEXT_TYPE + COMMA_SEP +
            ArtEntries.COLUMN_NAME_YEAR + TEXT_TYPE + COMMA_SEP +
            ArtEntries.COLUMN_NAME_VISIBILITY + TEXT_TYPE + COMMA_SEP +
            ArtEntries.COLUMN_NAME_TAG + TEXT_TYPE + COMMA_SEP +
            ArtEntries.COLUMN_NAME_CHECKIN + TEXT_TYPE +
    " )";

    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + ArtEntries.TABLE_NAME;

}
