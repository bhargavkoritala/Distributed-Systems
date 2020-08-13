package edu.buffalo.cse.cse486586.simpledht;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import java.util.StringTokenizer;

/*Sri Sai Bhargav Koritala*/
public class FileHolder extends SQLiteOpenHelper {

    private static final String moneyheist ="LaCasaDePapel.db";
    private static String robbery ="RoyalMintofSpain";

    public FileHolder(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    public FileHolder(Context context, String name, SQLiteDatabase.CursorFactory factory, int version, DatabaseErrorHandler errorHandler) {
        super(context, name, factory, version, errorHandler);
    }

    public FileHolder(Context context) throws SQLException {
        super(context, moneyheist,null,50316982);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE "+ robbery +"(key VARCHAR PRIMARY KEY,value VARCHAR);");
    }

    public Cursor retrieve(String getValueForThis){
        Cursor cursor;
        String[] objects = new String[1];
        objects[0] = getValueForThis;
        cursor = this.getReadableDatabase().query(robbery, null, "key=?", objects, null, null, null, null);
        return cursor;
    }

    public Cursor retrieveAll(){
        Cursor cursor;
        cursor = this.getReadableDatabase().query(robbery, null, null, null, null, null, null, null);
        return cursor;
    }

    public void put(String values){
        StringTokenizer tokens = new StringTokenizer(values,":");
        String key = null;
        String value = null;
        while(tokens.hasMoreElements()){
            key = tokens.nextToken();
            value = tokens.nextToken();
        }
        Log.i("Values in db",values);
        Log.i("Key in db",key);
        Log.i("value in db",value);
        ContentValues contentValues = new ContentValues();
        contentValues.put("key",key);
        contentValues.put("value",value);
        this.getWritableDatabase().insert(robbery,null,contentValues);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }


}