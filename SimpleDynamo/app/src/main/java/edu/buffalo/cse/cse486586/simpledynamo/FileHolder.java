package edu.buffalo.cse.cse486586.simpledynamo;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class FileHolder extends SQLiteOpenHelper {

    private static final String database="Anonymous";
    private static final int ubit=50316982;
    private static final String tableName="home";
    private static final String magicHome="CREATE TABLE "+tableName+"(key VARCHAR PRIMARY KEY,value VARCHAR);";

    public FileHolder(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    public FileHolder(Context context, String name, SQLiteDatabase.CursorFactory factory, int version, DatabaseErrorHandler errorHandler) {
        super(context, name, factory, version, errorHandler);
    }

    public FileHolder(Context context) throws SQLException {
        super(context,database,null,ubit);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(magicHome);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}