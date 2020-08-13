package edu.buffalo.cse.cse486586.simpledht;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;
/*Sri Sai Bhargav Koritala*/
public class SimpleDhtProvider extends ContentProvider {

    static final String TAG = SimpleDhtProvider.class.getSimpleName();
    static final String[] REMOTES = {"11108","11112","11116","11120","11124"};
    static String myRemote;
    static final String bank = "RoyalMintofSpain";
    static NodeManager myRemoteManager =new NodeManager();
    static List<NodeManager> allTheRemote =new ArrayList<NodeManager>();
    static HashMap<String, String> nodeToHash = new HashMap<String,String>();
    static HashMap<String, String> hashToNode = new HashMap<String,String>();
    static Uri providerLink;
    static final String regex = ":";

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        FileHolder deleteDataBase=new FileHolder(getContext());
        SQLiteDatabase myDelete=deleteDataBase.getWritableDatabase();
        FileHolder readDataBase = new FileHolder(getContext());
        SQLiteDatabase myRead = readDataBase.getReadableDatabase();
        String[] objects = new String[1];
        objects[0]=selection;
        MatrixCursor matrixCursor;
        if(!selection.equalsIgnoreCase("*") && !selection.equalsIgnoreCase("@")) {
            myDelete.delete(bank, "key=?",objects);
            Log.i(TAG,"Deleted selection:"+selection);

        }
        else {
            matrixCursor = (MatrixCursor) myRead.query(bank,null,null,null,null,null,null,null);
            try{
                while (matrixCursor.moveToNext()) {
                    Log.i(TAG,matrixCursor.getString(0)+" with contents:"+ matrixCursor.getString(1));
                }
            }catch (Exception e){
                Log.i(TAG,e.getMessage());
            }finally {
                matrixCursor.close();
            }
            myDelete.delete(bank,null,null);
            Log.i(TAG,"Deleted Using */ @");
        }
        for(String string : objects){
            Log.i("Delete Statement", string+" is deleted from the database");
        }
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values){
        String toString = values.toString();

        if(myRemoteManager.getMyHash()==null|| myRemote.equalsIgnoreCase(hashToNode.get(myRemoteManager.getMyAfter()))) {
            FileHolder myInsert = null;
            SQLiteDatabase myInsertable = null;
            String sending = "";
            for(int i =0;i<3;i++){
                if(i==0){
                    sending = sending.concat(values.getAsString("key"));
                }
                else if (i==1){
                    sending = sending.concat(":");
                }
                else if (i==2){
                    sending = sending.concat(values.getAsString("value"));
                }
            }
            try{
                for(int i =0;i<3;i++){
                    if(i==0){
                        myInsert = new FileHolder(getContext());
                    }
                    else if (i==1){
                        myInsertable = myInsert.getWritableDatabase();
                    }
                    else if (i==2){
                        myInsert.put(sending);
                    }
                }
            } catch (SQLException e) {
                Log.e("Insert SQL  Error",e.getMessage());
            }catch (Exception e){
                Log.e("Insert Error",e.getMessage());
            }finally {
                myInsertable.close();
            }
            Log.i("In insert if", toString);
        }
        else
        {

            String myKField=(String)values.get("key");
            String myVField=(String)values.get("value");
            String myHField=null;
            Log.i("In insert else ",myKField);
            try{
                myHField=genHash(myKField);
            } catch (NoSuchAlgorithmException e) {
                Log.v("in insert else",e.getMessage());
            }
            Integer nodeCompare = null;
            Integer keyNode = null;
            Integer keyBefore = null;
            for(int i =0;i<3;i++){
                if(i==0){
                    nodeCompare = myRemoteManager.getMyHash().compareTo(myRemoteManager.getMyBefore());
                }
                else if (i==1){
                    keyNode = myHField.compareTo(myRemoteManager.getMyHash());
                }
                else if (i==2){
                    keyBefore = myHField.compareTo(myRemoteManager.getMyBefore());
                }
            }
            boolean boundary;
            if (nodeCompare < 0) boundary = true;
            else boundary = false;
            boolean keyisNowLessEq;
            if (keyNode <= 0) keyisNowLessEq = true;
            else keyisNowLessEq = false;
            boolean keyisPastMore;
            if (keyBefore > 0) keyisPastMore = true;
            else keyisPastMore = false;
            boolean keyisLess;
            if (keyNode < 0) keyisLess = true;
            else keyisLess = false;
            boolean keisLessBefore;
            if (keyBefore < 0) keisLessBefore = true;
            else keisLessBefore = false;
            if((boundary && (keyisNowLessEq || keyisPastMore)) || (!boundary && (keyisNowLessEq && keyisPastMore))) {
                FileHolder myInsert;
                SQLiteDatabase myInsertable = null;
                String sending = "";
                for(int i =0;i<3;i++){
                    if(i==0){
                        sending = sending.concat(values.getAsString("key"));
                    }
                    else if (i==1){
                        sending = sending.concat(regex);
                    }
                    else if (i==2){
                        sending = sending.concat(values.getAsString("value"));
                    }
                }
                try{
                    myInsert = new FileHolder(getContext());
                    myInsertable = myInsert.getWritableDatabase();
                    myInsert.put(sending);
                } catch (SQLException e) {
                    Log.e("Insert SQL  Error",e.getMessage());
                }catch (Exception e){
                    Log.e("Insert Error",e.getMessage());
                }finally {
                    myInsertable.close();
                }
                Log.i("in insert else", toString);
            }
            else if((boundary && (!keyisNowLessEq||(keyisLess && keisLessBefore)))|| (!boundary && (!keyisNowLessEq||(keyisLess && keisLessBefore)))) {
                List<String> insertMessage = new ArrayList<String>();
                String insert="myINS";
                for(int i =0;i<3;i++){
                    if(i==0){
                        insertMessage.add(insert);
                    }
                    else if (i==1){
                        insertMessage.add(myKField);
                    }
                    else if (i==2){
                        insertMessage.add(myVField);
                    }
                }
                ListIterator<String> insertIterator = insertMessage.listIterator();
                String send = "";
                while (insertIterator.hasNext()){
                    send = send.concat(insertIterator.next());
                    send = send.concat(regex);
                }
                send = send.substring(0,send.length()-1);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,send,hashToNode.get(myRemoteManager.getMyAfter()));
            }
        }
        return uri;
    }

    @Override
    public boolean onCreate() {
        providerLink = buildUri("content","edu.buffalo.cse.cse486586.simpledht.provider");
        TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myRemote = String.valueOf((Integer.parseInt(portStr) * 2));
        try {
            ServerSocket serverSocket = new ServerSocket(10000);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            e.printStackTrace();
        }
        for(String rPort : REMOTES){
            try {
                nodeToHash.put(rPort,genHash(String.valueOf(Integer.parseInt(rPort)/2)));
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
        for(String kSet : nodeToHash.keySet()){
            Log.i("KeyHash", "Node:"+kSet+",HashedNode:"+nodeToHash.get(kSet));
        }
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"Connect", myRemote);
        for(String hPort : REMOTES){
            try {
                hashToNode.put(genHash(String.valueOf(Integer.parseInt(hPort)/2)),hPort);
            } catch (NoSuchAlgorithmException e) {
                Log.e(TAG, e.getMessage());
            }
        }
        for(String hSet : hashToNode.keySet()){
            Log.i("KeyHash", "Node:"+hSet+",HashedNode:"+hashToNode.get(hSet));
        }
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor queryResult=null;
        boolean at = selection.equalsIgnoreCase("@");
        boolean iamSuccessor = myRemote.equalsIgnoreCase(hashToNode.get(myRemoteManager.getMyAfter()));
        Log.i("Query Check",at+regex+iamSuccessor);
        if(myRemoteManager.getMyHash()==null||at|| iamSuccessor) {
            FileHolder myRead = null;
            try{
                myRead = new FileHolder(getContext());
            } catch (SQLException e) {
                Log.e(TAG, e.getMessage());
            }
            String[] objects= new String[1];
            objects[0]=selection;
            for(int l=0;l<2;l++){
                if(!selection.equalsIgnoreCase("*") && !selection.equalsIgnoreCase("@")) {
                    queryResult=myRead.retrieve(selection);
                    break;
                }
                else {
                    queryResult = myRead.retrieveAll();
                    break;
                }
            }

            Log.i(TAG,"Database read for "+selection);
        }
        else if(!selection.equals("*")) {
            String qSel="";
            try{
                qSel=genHash(selection);
                Log.i("Selection (key) hash: ",qSel);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            Integer nodeBefore = null;
            Integer keyNode = null;
            Integer keyBefore = null;
            for(int i =0;i<3;i++){
                if(i==0){
                    nodeBefore = myRemoteManager.getMyHash().compareTo(myRemoteManager.getMyBefore());
                }
                else if (i==1){
                    keyNode = qSel.compareTo(myRemoteManager.getMyHash());
                }
                else if (i==2){
                    keyBefore = qSel.compareTo(myRemoteManager.getMyBefore());
                }
            }
            boolean boundary;
            if (nodeBefore < 0) boundary = true;
            else boundary = false;
            boolean keyisNowLessEq;
            if (keyNode <= 0) keyisNowLessEq = true;
            else keyisNowLessEq = false;
            boolean keyisPastMore;
            if (keyBefore > 0) keyisPastMore = true;
            else keyisPastMore = false;
            boolean keyisLess;
            if (keyNode < 0) keyisLess = true;
            else keyisLess = false;
            boolean keisLessBefore;
            if (keyBefore < 0) keisLessBefore = true;
            else keisLessBefore = false;
            if((boundary && (keyisNowLessEq || keyisPastMore)) || (!boundary && (keyisNowLessEq && keyisPastMore))) {
                FileHolder qSearch = null;
                try{
                    qSearch = new FileHolder(getContext());
                } catch (SQLException e) {
                    Log.e(TAG, e.getMessage());
                }
                String [] objects = new String[1];
                objects[0] = selection;
                queryResult = qSearch.retrieve(selection);
                queryResult.moveToFirst();
            }
            else if((boundary && (!keyisNowLessEq||(keyisLess && keisLessBefore)))|| (!boundary && (!keyisNowLessEq||(keyisLess && keisLessBefore)))){
                AsyncTask<String, String, String> qBack;
                try {
                    List<String> qMess = new ArrayList<String>();
                    String qMove = "";
                    String query="Query";
                    for(int i =0;i<2;i++){
                        if(i==0){
                            qMess.add(query);
                        }
                        else if (i==1){
                            qMess.add(selection);
                        }
                    }
                    ListIterator<String> qIter = qMess.listIterator();
                    while(qIter.hasNext()){
                        qMove = qMove.concat(qIter.next());
                        qMove = qMove.concat(regex);
                    }
                    qMove = qMove.substring(0,qMove.length()-1);
                    qBack = new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,qMove,hashToNode.get(myRemoteManager.getMyAfter()));
                    StringTokenizer qToken = new StringTokenizer(qBack.get(),regex);
                    String oneValue = null;
                    String twoValue = null;
                    for(int i =0;i<2;i++){
                        if(i==0){
                            oneValue = qToken.nextToken();
                        }
                        else if (i==1){
                            twoValue = qToken.nextToken();
                        }
                    }
                    String[] columnNames = {"key", "value"};
                    MatrixCursor matrixCursor = new MatrixCursor(columnNames);
                    matrixCursor.addRow(new String[]{oneValue, twoValue});
                    queryResult = matrixCursor;
                    queryResult.moveToFirst();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }
        else if(selection.equals("*")) {
            AsyncTask<String, String, String> qReply;
            try {
                qReply = new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"myStar"+regex+ myRemoteManager.getMyBefore(),hashToNode.get(myRemoteManager.getMyAfter()));
                String qRepget = qReply.get();
                boolean isEmpty = qRepget.contains("NothingHere");
                if(isEmpty) {
                    FileHolder qEmp = null;
                    try{
                        qEmp = new FileHolder(getContext());
                    } catch (SQLException e) {
                        Log.e(TAG, e.getMessage());
                    }
                    queryResult = qEmp.retrieveAll();
                }
                else{
                    FileHolder qNEmp = null;
                    try{
                        qNEmp = new FileHolder(getContext());
                    } catch (SQLException e) {
                        Log.e(TAG, e.getMessage());
                    }
                    queryResult = qNEmp.retrieveAll();
                    String qResp=qRepget;
                    StringTokenizer valuesKeys = new StringTokenizer(qResp,regex);
                    String vKey;
                    String kValue;
                    String[] columnNames = {"key", "value"};
                    MatrixCursor matrixCursor = new MatrixCursor(columnNames);
                    while(valuesKeys.hasMoreTokens()){
                        vKey = valuesKeys.nextToken();
                        kValue = valuesKeys.nextToken();
                        matrixCursor.addRow(new String[]{vKey,kValue});
                    }
                    queryResult = new MergeCursor(new Cursor[] { matrixCursor, queryResult });
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        return queryResult;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }


    private class ClientTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... msgs) {
            try {
                if(msgs[0].contains("Connect")){
                    Socket client;
                    client = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(REMOTES[0]));
                    PrintWriter clientPrint = new PrintWriter(client.getOutputStream(),true);
                    BufferedReader clBuffBack = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    String reqFrom= String.valueOf(Integer.parseInt(msgs[1])/2);
                    clientPrint.println("Connect"+regex+reqFrom);
                    String clReply=clBuffBack.readLine();
                    Log.v("In client for conn", clReply);
                    client.close();
                    return "Connect";
                }
                else if(msgs[0].contains("myINS")){
                    Socket client;
                    client = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(msgs[1]));
                    PrintWriter clientPrint = new PrintWriter(client.getOutputStream(),true);
                    BufferedReader clBuffBack = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    clientPrint.println(msgs[0]);
                    String clReply=clBuffBack.readLine();
                    Log.v("In client for insert", clReply);
                    client.close();
                    return "Inserted";
                }
                else if(msgs[0].contains("Query")){
                    Socket client;
                    client = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(msgs[1]));
                    PrintWriter clientPrint = new PrintWriter(client.getOutputStream(),true);
                    BufferedReader clBuffBack = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    clientPrint.println(msgs[0]);
                    String clReply=clBuffBack.readLine();
                    Log.v("In client for sQuery", clReply);
                    client.close();
                    return clReply;
                }
                else if(msgs[0].contains("myStar")){
                    Socket client;
                    client = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(msgs[1]));
                    PrintWriter clientPrint = new PrintWriter(client.getOutputStream(),true);
                    BufferedReader clBuffBack = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    clientPrint.println(msgs[0]);
                    String clReply=clBuffBack.readLine();
                    Log.v("In client for aQuery", clReply);
                    client.close();
                    return clReply;
                }
                else if(msgs[0].contains("Details")){
                    Socket client;
                    client = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(msgs[1]));
                    PrintWriter clientPrint = new PrintWriter(client.getOutputStream(),true);
                    BufferedReader clBuffBack = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    clientPrint.println(msgs[0]);
                    String clReply=clBuffBack.readLine();
                    Log.v("In client for details", clReply);
                    client.close();
                    return clReply;
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            try{
                Socket sScok;
                String serverResponse;
                Cursor serverResult;
                while(true) {
                    sScok=serverSocket.accept();
                    BufferedReader serverBuff=new BufferedReader(new InputStreamReader(sScok.getInputStream()));
                    serverResponse=serverBuff.readLine();
                    StringTokenizer serverTokens = new StringTokenizer(serverResponse,regex);
                    boolean myDetailsLength;
                    boolean myInsertLength;
                    boolean myQueryLength;
                    boolean mySky;
                    boolean myConnection;
                    if (serverResponse.contains("Connect")) myConnection = true;
                    else myConnection = false;
                    if (serverResponse.startsWith("myStar")) mySky = true;
                    else mySky = false;
                    if (serverResponse.startsWith("Query")) myQueryLength = true;
                    else myQueryLength = false;
                    if (serverResponse.startsWith("myINS")) myInsertLength = true;
                    else myInsertLength = false;
                    if (serverResponse.contains("Details")) myDetailsLength = true;
                    else myDetailsLength = false;
                    if(myConnection && myRemote.equalsIgnoreCase(REMOTES[0])){
                        String thisOperation  = serverTokens.nextToken();
                        String pnumber=serverTokens.nextToken();
                        Log.i("Operation Check",thisOperation);
                        NodeManager newnode=new NodeManager();
                        newnode.setMyRemote(String.valueOf(Integer.parseInt(pnumber)*2));
                        newnode.setMyHash(genHash(pnumber));
                        newnode.setRemoteMapping(nodeToHash);
                        newnode.setHashMapping(hashToNode);
                        addSort(allTheRemote,newnode);
                        newnode.setAllManagers(allTheRemote);
                        joinReq(allTheRemote);
                        sendDetails(allTheRemote);
                        unanimousReply(sScok);
                    }
                    else if(mySky) {
                        String thisOperation = serverTokens.nextToken();
                        String qSenderManager=serverTokens.nextToken();
                        Log.i("Operation Check",thisOperation);
                        boolean iStarted = myRemoteManager.getMyHash().equalsIgnoreCase(qSenderManager);
                        if(iStarted) {
                            String serMessage="";
                            FileHolder servRe = null;
                            try{
                                servRe = new FileHolder(getContext());
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                            serverResult = servRe.retrieveAll();
                            PrintWriter serPrint = new PrintWriter(sScok.getOutputStream(), true);
                            boolean isNEmpty;
                            if (serverResult.getCount() != 0) isNEmpty = true;
                            else isNEmpty = false;
                            if(isNEmpty) {
                                String response = null;
                                for(int i =0;i<2;i++){
                                    if(i==0){
                                        response = cursorToString(serverResult,1);
                                    }
                                    else if (i==1){
                                        serMessage = response.substring(0, response.length() - 1);
                                    }
                                }
                                serPrint.println(serMessage);
                            }
                            else if(!isNEmpty) {
                                serPrint.println("NothingHere");
                            };
                            sScok.close();
                        }
                        else if(!iStarted) {
                            AsyncTask<String, String, String> thisReply;
                            try {
                                String sendingStar = "";
                                for(int i =0;i<3;i++){
                                    if(i==0){
                                        sendingStar = sendingStar.concat("myStar");
                                    }
                                    else if (i==1){
                                        sendingStar = sendingStar.concat(regex);
                                    }
                                    else if (i==2){
                                        sendingStar = sendingStar.concat(qSenderManager);
                                    }
                                }
                                thisReply = new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,sendingStar,hashToNode.get(myRemoteManager.getMyAfter()));
                                String getReply = thisReply.get();
                                StringTokenizer regexToken = new StringTokenizer(getReply,regex);
                                Integer zeroCase;
                                String zeroethValue = regexToken.nextToken();
                                if (zeroethValue.equals("NothingHere")) zeroCase = 1;
                                else zeroCase = 0;
                                PrintWriter onePrint  = new PrintWriter(sScok.getOutputStream(),true);
                                FileHolder sqOneZero = new FileHolder(getContext());
                                boolean isServerEmpty;
                                switch(zeroCase){
                                    case 1:
                                        serverResult = sqOneZero.retrieveAll();
                                        if (serverResult.getCount() != 0) isServerEmpty = true;
                                        else isServerEmpty = false;
                                        if(isServerEmpty) {
                                            String response = null;
                                            for(int i =0;i<2;i++){
                                                if(i==0){
                                                    response = cursorToString(serverResult,1);
                                                }
                                                else if (i==1){
                                                    onePrint.println(response.substring(0,response.length()-1));
                                                }
                                            }
                                        }
                                        else if (!isServerEmpty) {
                                            onePrint.println("NothingHere");
                                        }
                                        break;
                                    default:
                                        String zero = null;
                                        serverResult = sqOneZero.retrieveAll();
                                        if (serverResult.getCount() != 0) isServerEmpty = true;
                                        else isServerEmpty = false;
                                        if(isServerEmpty) {
                                            String response = null;
                                            for(int i =0;i<2;i++){
                                                if(i==0){
                                                    response = cursorToString(serverResult,0);
                                                }
                                                else if (i==1){
                                                    zero = getReply.concat(response);
                                                }
                                            }
                                            onePrint.println(zero);
                                        }
                                        else if (!isServerEmpty){
                                            onePrint.println(getReply);
                                        }

                                }
                                sScok.close();
                            } catch (IOException e) {
                                Log.e(TAG, e.getMessage());
                            } catch (NumberFormatException e) {
                                Log.e(TAG, e.getMessage());
                            } catch (SQLException e) {
                                Log.e(TAG, e.getMessage());
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (ExecutionException e) {
                                e.printStackTrace();
                            }

                        }
                    }
                    else if(myInsertLength){
                        String thisOperation = serverTokens.nextToken();
                        Log.i("Operation Check", thisOperation);
                        ContentValues contentValues = new ContentValues();
                        contentValues.put("key",serverTokens.nextToken());
                        contentValues.put("value",serverTokens.nextToken());
                        insert(providerLink,contentValues);
                        unanimousReply(sScok);
                    }
                    else if(myDetailsLength){
                        String myPort = serverTokens.nextToken();
                        String myPortHash = serverTokens.nextToken();
                        String myBeforeHash = serverTokens.nextToken();
                        String myAfterHash = serverTokens.nextToken();
                        String myBeforePort = serverTokens.nextToken();
                        String myAfterPort = serverTokens.nextToken();
                        setDetails(myPort, myPortHash, myBeforeHash, myAfterHash, myBeforePort, myAfterPort);
                        unanimousReply(sScok);
                    }
                    else if(myQueryLength) {
                        String thisOperation = serverTokens.nextToken();
                        String selection = serverTokens.nextToken();
                        Log.i("Operation Check", thisOperation);
                        serverResult = query(providerLink,null,selection,null,null);
                        boolean isNull;
                        if (serverResult.getCount() != 0) isNull = true;
                        else isNull = false;
                        PrintWriter unaniPrint = new PrintWriter(sScok.getOutputStream(),true);
                        if(isNull){
                            String sendThis = "";
                            sendThis = sendThis.concat(serverResult.getString(0));
                            sendThis = sendThis.concat(regex);
                            sendThis = sendThis.concat(serverResult.getString(1));
                            unaniPrint.println(sendThis);
                        }
                        sScok.close();
                    }
                }
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;

        }

    }

    private boolean unanimousReply(Socket replySocket) {
        try{
            PrintWriter unaniPrint = new PrintWriter(replySocket.getOutputStream(),true);
            unaniPrint.println("BhargavKoritala");
            replySocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    private boolean addSort(List<NodeManager> scheme, NodeManager veryNew) {
        scheme.add(veryNew);
        Object hehe = NodeManager.managerComparator;
        Collections.sort(scheme, (Comparator<? super NodeManager>) hehe);
        return false;
    }

    private String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    private boolean setDetails(String a, String b, String c, String d, String e, String f){
        myRemoteManager.setMyRemote(a);
        myRemoteManager.setMyHash(b);
        myRemoteManager.setMyBefore(c);
        myRemoteManager.setMyAfter(d);
        myRemoteManager.setMyBeforeRemote(e);
        myRemoteManager.setMyAfterRemote(f);
        return  false;
    }

    private boolean changeDetails(NodeManager n, String a, String b, String c, String d){
        n.setMyBefore(a);
        n.setMyAfter(b);
        n.setMyBeforeRemote(c);
        n.setMyAfterRemote(d);
        return  false;
    }

    private String cursorToString(Cursor change, Integer type){
        String result = "";
        change.moveToFirst();
        switch(type){
            //for adding to already existing
            case 0:
                while(!change.isAfterLast()) {
                    result = result.concat(regex);
                    result = result.concat(change.getString(0));
                    result = result.concat(regex);
                    result = result.concat(change.getString(1));
                    change.moveToNext();
                }
                break;
            //for new ones
            default:
                while(!change.isAfterLast()) {
                    result = result.concat(change.getString(0));
                    result = result.concat(regex);
                    result = result.concat(change.getString(1));
                    result = result.concat(regex);
                    change.moveToNext();
                }

        }
        return result;
    }

    private boolean sendDetails(List<NodeManager> managers){
        ListIterator<NodeManager> iterator = managers.listIterator();
        while(iterator.hasNext()){
            NodeManager exampleNode = iterator.next();
            String myPort = null;
            String myPortHash = null;
            String myBeforeHash = null;
            String myAfterHash = null;
            String myBeforePort = null;
            String myAfterPort = null;
            if(exampleNode.getMyRemote().equals(REMOTES[0])){
                //myRemoteManager = exampleNode;
                Log.i("Setting self","Details for 11108");
                for (int i = 0;i<6;i++){
                    if(i==1){
                        myPort = exampleNode.getMyRemote();
                    }
                    else if (i==2){
                        myPortHash = exampleNode.getMyHash();
                    }
                    else if (i==3){
                        myBeforeHash = exampleNode.getMyBefore();
                    }
                    else if (i==4){
                        myAfterHash = exampleNode.getMyAfter();
                    }
                    else if (i==5){
                        myBeforePort = exampleNode.getMyBeforeRemote();
                    }
                    else if (i==6){
                        myAfterPort = exampleNode.getMyAfterRemote();
                    }
                }
                setDetails(myPort, myPortHash, myBeforeHash, myAfterHash, myBeforePort, myAfterPort);
            }
            else{
                List<String> nodeDetails = new ArrayList<String>();
                nodeDetails.add(exampleNode.getMyRemote());
                nodeDetails.add(exampleNode.getMyHash());
                nodeDetails.add(exampleNode.getMyBefore());
                nodeDetails.add(exampleNode.getMyAfter());
                nodeDetails.add(exampleNode.getMyBeforeRemote());
                nodeDetails.add(exampleNode.getMyAfterRemote());
                ListIterator<String> nodeDetailsIterator = nodeDetails.listIterator();
                String details = "";
                while (nodeDetailsIterator.hasNext()){
                    details = details.concat(nodeDetailsIterator.next());
                    details = details.concat(regex);
                }
                details = details.concat("Details");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,details,exampleNode.getMyRemote());
            }
        }
        return false;
    }

    private boolean joinReq(List<NodeManager> nodeManagers){
        for(int i=0;i<nodeManagers.size();i++) {
            NodeManager joinNodeManager = nodeManagers.get(i);
            String setMyBeforehash;
            String setMyAfterhash;
            String setMyBeforePort;
            String setMyAfterPort;
            Integer past;
            Integer future;
            Integer present = i;
            if(i-1<0){
                past = nodeManagers.size()-1;
            }
            else{
                past = present-1;
            }
            future = (present+1)%nodeManagers.size();
            //using past here
            setMyBeforehash = nodeManagers.get(past).getMyHash();
            setMyBeforePort = nodeManagers.get(past).getMyRemote();
            //using future here
            setMyAfterhash = nodeManagers.get(future).getMyHash();
            setMyAfterPort = nodeManagers.get(future).getMyRemote();
            //changing present here
            changeDetails(joinNodeManager, setMyBeforehash, setMyAfterhash, setMyBeforePort, setMyAfterPort);
        }
        return false;
    }
}


