package edu.buffalo.cse.cse486586.simpledynamo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Array;
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

public class SimpleDynamoProvider extends ContentProvider {

	static final String TAG = SimpleDynamoActivity.class.getSimpleName();
	static final String[] REMOTES = {"11108","11112","11116","11120","11124"};
	static List<String> hashes_list = new ArrayList<String>();
	static final  Integer[] REMOTE_ORDER = {4,1,0,2,3};
	static final int tenThousand = 10000;
	static String myRemote;
	static final String location = "home";
	static NodeManager myRemoteManager =new NodeManager();
	static List<NodeManager> allTheRemote =new ArrayList<NodeManager>();
	private static final String QUESTION = "key";
	private static final String ANSWER = "value";
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
			myDelete.delete(location, QUESTION +"=?",objects);
			Log.i(TAG,"Deleted selection:"+selection);

		}
		else {
			matrixCursor = (MatrixCursor) myRead.query(location,null,null,null,null,null,null,null);
			try{
				while (matrixCursor.moveToNext()) {
					Log.i(TAG,matrixCursor.getString(0)+" with contents:"+ matrixCursor.getString(1));
				}
			}catch (Exception e){
				Log.i(TAG,e.getMessage());
			}finally {
				matrixCursor.close();
			}
			myDelete.delete(location,null,null);
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
			try{
				for(int i=0;i<3;i++){
					if(i==0){
						myInsert = new FileHolder(getContext());
					}
					else if (i==1){
						myInsertable = myInsert.getWritableDatabase();
					}
					else if(i==2){
						myInsertable.insert(location,null,values);
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
		else {
			synchronized (this){
				try{
					String myKField=(String)values.get(QUESTION);
					String myHField=null;
					Log.i("In insert else ",myKField);
					try{
						myHField=genHash(myKField);
					} catch (NoSuchAlgorithmException e) {
						Log.v("in insert else",e.getMessage());
					}
					Integer nodeCompare = myRemoteManager.getMyHash().compareTo(myRemoteManager.getMyBefore());
					Integer keyNode = myHField.compareTo(myRemoteManager.getMyHash());
					Integer keyBefore = myHField.compareTo(myRemoteManager.getMyBefore());
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
					String question = (String)values.get(QUESTION);
					String answer = (String)values.get(ANSWER);
					if((boundary && (keyisNowLessEq || keyisPastMore)) || (!boundary && (keyisNowLessEq && keyisPastMore))){
						FileHolder sqliteDB = new FileHolder(getContext());
						for(int i=0;i<3;i++){
							if(i==0){
								sqliteDB.getWritableDatabase().insert(location,null,values);
							}
							else if (i==1){
								replicate(question+":"+answer,Integer.parseInt(myRemoteManager.getMyAfterRemote()));
							}
							else if(i==2){
								replicate(question+":"+answer,Integer.parseInt(myRemoteManager.getMyAfterAfterRemote()));
							}
						}
					}
					else if ((boundary && (!keyisNowLessEq||(keyisLess && keisLessBefore)))|| (!boundary && (!keyisNowLessEq||(keyisLess && keisLessBefore)))){
						String location = hashes_list.get(0);
						ListIterator<String> hashMover = hashes_list.listIterator();
						Integer variable = null;
						Integer main = null;
						Integer mainOne = null;
						Integer mainTwo = null;
						List<Integer> receivers = new ArrayList<Integer>();
						while (hashMover.hasNext()){
							String now  = hashMover.next();
							Log.i("Won",now);
							Integer actualKeyNode = now.compareTo(myHField);
							boolean lessThan;
							if (actualKeyNode > 0) lessThan = true;
							else lessThan = false;
							if (lessThan) {
								location = now;
								while(hashMover.hasNext()){
									hashMover.next();
								}
							}
						}
						for(int i=0;i<7;i++){
							if(i==0){
								variable = hashes_list.indexOf(location);
							}
							else if (i==1){
								main = Integer.parseInt(REMOTES[REMOTE_ORDER[variable]]);
							}
							else if(i==2){
								mainOne = Integer.parseInt(REMOTES[REMOTE_ORDER[(variable+1)%REMOTE_ORDER.length]]);
							}
							else if(i==3){
								mainTwo = Integer.parseInt(REMOTES[REMOTE_ORDER[(variable+2)%REMOTE_ORDER.length]]);
							}
							else if(i==4){
								receivers.add(main);
							}
							else if(i==5){
								receivers.add(mainOne);
							}
							else if(i==6){
								receivers.add(mainTwo);
							}
						}
						for(Integer port : receivers){
							replicate(question+":"+answer,port);
						}
					}
				} catch (SQLException e) {
					e.printStackTrace();
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
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
			ServerSocket serverSocket = new ServerSocket(tenThousand);
			new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
		} catch (IOException e) {
			e.printStackTrace();
		}

		for(Integer s : REMOTE_ORDER){
			String portValue = REMOTES[s];
			String half = String.valueOf(Integer.parseInt(portValue)/2);
			try {
				hashes_list.add(genHash(half));
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
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
		new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"", myRemote);
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
			SQLiteDatabase myReadable = null;
			try{
				FileHolder myRead=new FileHolder(getContext());
				myReadable = myRead.getReadableDatabase();
			} catch (SQLException e) {
				Log.e(TAG, e.getMessage());
			}
			String[] objects= new String[1];
			objects[0]=selection;
			if(!selection.equalsIgnoreCase("*") && !selection.equalsIgnoreCase("@")) {
				queryResult=myReadable.query(location,null,"key=?",objects,null,null,null,null);
			}
			else {
				queryResult = myReadable.query(location,null,null,null,null,null,null,null);
			}
			Log.i(TAG,"Database read for "+selection);
		}
		else if(!selection.equals("*")) {
			/*https://www.baeldung.com/java-synchronized*/
			synchronized (this){
				try{
					String qSel="";
					try{
						qSel=genHash(selection);
						Log.i("Selection (key) hash: ",qSel);
					} catch (NoSuchAlgorithmException e) {
						e.printStackTrace();
					}
					Integer nodeBefore = null;
					Integer keyBefore = null;
					Integer keyNode = null;
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
						SQLiteDatabase qSearchable=null;
						try{
							FileHolder qSearch = new FileHolder(getContext());
							qSearchable = qSearch.getReadableDatabase();
						} catch (SQLException e) {
							Log.e(TAG, e.getMessage());
						}
						String [] objects = new String[1];
						objects[0] = selection;
						queryResult = qSearchable.query(location, null, "key=?", objects, null, null, null, null);
						queryResult.moveToFirst();
					}
					else if((boundary && (!keyisNowLessEq||(keyisLess && keisLessBefore)))|| (!boundary && (!keyisNowLessEq||(keyisLess && keisLessBefore)))){
						String location = null;
						for(int i=0;i<2;i++){
							if(i==0){
								location = hashes_list.get(0);
								continue;
							}
						}
						ListIterator<String> hashMover = hashes_list.listIterator();
						while (hashMover.hasNext()){
							String now  = hashMover.next();
							Log.i("Now",now);
							Integer actualKeyNode = null;
							for(int i=0;i<2;i++){
								if(i==0){
									actualKeyNode = now.compareTo(qSel);
									continue;
								}
							}
							boolean lessThan;
							if (actualKeyNode > 0) lessThan = true;
							else lessThan = false;
							if (lessThan) {
								location = now;
								while(hashMover.hasNext()){
									hashMover.next();
								}
							}
						}
						Integer desti = hashes_list.indexOf(location);
						Integer destiPort = Integer.parseInt(REMOTES[REMOTE_ORDER[desti]]);
						queryResult=enquire(selection,destiPort);

					}
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
			}
		}
		else if(selection.equals("*")) {
			Socket qSock;
			try {
				qSock= new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(hashToNode.get(myRemoteManager.getMyAfter())));
				PrintWriter qPrint = new PrintWriter(qSock.getOutputStream(),true);
				BufferedReader qBuffBack=new BufferedReader(new InputStreamReader(qSock.getInputStream()));
				qPrint.println("myStar"+regex+ myRemoteManager.getMyBefore());
				String qReply=qBuffBack.readLine();
				boolean isEmpty = qReply.contains("NothingHere");
				if(isEmpty) {
					qSock.close();
					SQLiteDatabase qEmpable = null;
					try{
						FileHolder qEmp=new FileHolder(getContext());
						qEmpable = qEmp.getReadableDatabase();
					} catch (SQLException e) {
						Log.e(TAG, e.getMessage());
					}
					queryResult=qEmpable.query(location,null,null,null,null,null,null,null);
				}
				else{
					SQLiteDatabase qNEmpable = null;
					try{
						FileHolder qNEmp=new FileHolder(getContext());
						qNEmpable = qNEmp.getReadableDatabase();
					} catch (SQLException e) {
						Log.e(TAG, e.getMessage());
					}
					queryResult=qNEmpable.query(location,null,null,null,null,null,null,null);
					String qResp=qReply;
					String[] keysValues = qResp.split(regex);
					String vKey = null;
					String kValue;
					String[] columnNames = {QUESTION, ANSWER};
					MatrixCursor matrixCursor = new MatrixCursor(columnNames);
					for(int l=0;l<keysValues.length;l++){
						if(l%2==0){
							vKey = (String)Array.get(keysValues,l);
						}
						else{
							kValue = (String)Array.get(keysValues,l);
							matrixCursor.addRow(new String[]{vKey, kValue});
						}
					}
					queryResult = new MergeCursor(new Cursor[] { matrixCursor, queryResult });
				}
			} catch (UnknownHostException e) {
				Log.e("StarQ", e.getMessage());
			} catch (IOException e) {
				Log.e("StarQ", e.getMessage());
			}
		}

		return queryResult;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}


	private class ClientTask extends AsyncTask<String, Void, Void> {

		@Override
		protected Void doInBackground(String... msgs) {
			try {
				Socket client;
				client = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(REMOTES[0]));
				PrintWriter clientPrint = new PrintWriter(client.getOutputStream(),true);
				BufferedReader clBuffBack = new BufferedReader(new InputStreamReader(client.getInputStream()));
				String reqFrom= String.valueOf(Integer.parseInt(msgs[1])/2);
				clientPrint.println("Connect"+regex+reqFrom);
				String clReply=clBuffBack.readLine();
				Log.v("In client", clReply);
				client.close();
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
					String[] serverSplits=serverResponse.split(regex);
					boolean myDetailsLength;
					boolean myInsertLength;
					boolean myQueryLength;
					boolean mySky;
					boolean myConnection;
					if (serverResponse.contains("Connect")) myConnection = true;
					else myConnection = false;
					if (serverResponse.startsWith("myStar")) mySky = true;
					else mySky = false;
					if (serverSplits.length == 3) myQueryLength = true;
					else myQueryLength = false;
					if (serverResponse.startsWith("myINS")) myInsertLength = true;
					else myInsertLength = false;
					if (serverResponse.contains("Details")) myDetailsLength = true;
					else myDetailsLength = false;
					if(myConnection && myRemote.equalsIgnoreCase(REMOTES[0])){
						String pnumber=serverSplits[1];
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
						String qSenderManager=serverSplits[1];
						boolean iStarted = myRemoteManager.getMyHash().equalsIgnoreCase(qSenderManager);
						if(iStarted) {
							String serMessage="";
							SQLiteDatabase serveReable = null;
							try{
								FileHolder servRe=new FileHolder(getContext());
								serveReable = servRe.getReadableDatabase();
							} catch (SQLException e) {
								e.printStackTrace();
							}
							serverResult=serveReable.query(location,null,null,null,null,null,null,null);
							PrintWriter serPrint = new PrintWriter(sScok.getOutputStream(), true);
							boolean isNEmpty;
							if (serverResult.getCount() != 0) isNEmpty = true;
							else isNEmpty = false;
							if(isNEmpty) {
								String response = cursorToString(serverResult,1);
								serMessage = response.substring(0, response.length() - 1);
								serPrint.println(serMessage);
							}
							else if(!isNEmpty) {
								serPrint.println("NothingHere");
							};
							sScok.close();
						}
						else if(!iStarted) {
							Socket oneZero;
							try {
								oneZero= new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(hashToNode.get(myRemoteManager.getMyAfter())));
								PrintWriter servPrintElse  = new PrintWriter(oneZero.getOutputStream(),true);
								BufferedReader servBuffElseBack=new BufferedReader(new InputStreamReader(oneZero.getInputStream()));
								String sendingStar = "";
								for(int i=0;i<3;i++){
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
								servPrintElse.println(sendingStar);
								String readLine=servBuffElseBack.readLine();
								String[] regexSplit=readLine.split(regex);
								Integer zeroCase;
								if (regexSplit[0].equals("NothingHere")) zeroCase = 1;
								else zeroCase = 0;
								PrintWriter onePrint  = new PrintWriter(sScok.getOutputStream(),true);
								switch(zeroCase){
									case 1:
										oneZero.close();
										FileHolder sqOne=new FileHolder(getContext());
										SQLiteDatabase sqOneable=sqOne.getReadableDatabase();
										serverResult=sqOneable.query(location,null,null,null,null,null,null,null);

										if(serverResult.getCount()!=0) {
											String response = cursorToString(serverResult,1);
											onePrint.println(response.substring(0,response.length()-1));
										}
										else {
											onePrint.println("NothingHere");
										}
										break;
									default:
										oneZero.close();
										String zero;
										FileHolder sqZero=new FileHolder(getContext());
										SQLiteDatabase sqZeroable=sqZero.getReadableDatabase();
										serverResult=sqZeroable.query(location,null,null,null,null,null,null,null);
										boolean isServerEmpty;
										if (serverResult.getCount() != 0) isServerEmpty = true;
										else isServerEmpty = false;
										if(isServerEmpty) {
											String response = cursorToString(serverResult,0);
											zero = readLine.concat(response);
											onePrint.println(zero);
										}
										else if (!isServerEmpty){
											onePrint.println(readLine);
										}

								}
								sScok.close();
							} catch (IOException e) {
								Log.e(TAG, e.getMessage());
							} catch (NumberFormatException e) {
								Log.e(TAG, e.getMessage());
							} catch (SQLException e) {
								Log.e(TAG, e.getMessage());
							}

						}
					}
					else if(myInsertLength){
						synchronized (this){
							ContentValues contentValues = new ContentValues();
							contentValues.put(QUESTION,serverSplits[2]);
							contentValues.put(ANSWER,serverSplits[3]);
							insert(providerLink,contentValues);
							unanimousReply(sScok);
						}
					}
					else if(serverResponse.contains("JustInsert")){
						ContentValues contentValues = new ContentValues();
						contentValues.put(QUESTION,serverSplits[1]);
						contentValues.put(ANSWER,serverSplits[2]);
						FileHolder sqliteDB = new FileHolder(getContext());
						synchronized (this){
							sqliteDB.getWritableDatabase().insertWithOnConflict(location,null,contentValues,SQLiteDatabase.CONFLICT_REPLACE);
						}
						unanimousReply(sScok);
					}
					else if(serverResponse.contains("JustQuery")){
						String[] objects = new String[1];
						objects[0] = serverSplits[1];
						FileHolder sqliteDB = new FileHolder(getContext());
						serverResult = sqliteDB.getReadableDatabase().query(location, null, "key=?", objects, null, null, null, null);
						String returnResponse = "";
						Log.i("Hehe", String.valueOf(serverResult.getCount()));
						serverResult.moveToFirst();
						returnResponse+=serverResult.getString(0)+":"+serverResult.getString(1);
						PrintWriter justQueryPrint  = new PrintWriter(sScok.getOutputStream(),true);
						justQueryPrint.println(returnResponse);
						unanimousReply(sScok);
					}
					else if(myDetailsLength){
						String myPort = null;
						String myPortHash = null;
						String myBeforeHash = null;
						String myAfterHash = null;
						String myBeforePort = null;
						String myAfterPort = null;
						for(int i=0;i<6;i++){
							if(i==0){
								myPort = serverSplits[0];
							}
							else if (i==1){
								myPortHash = serverSplits[1];
							}
							else if (i==2){
								myBeforeHash = serverSplits[2];
							}
							else if (i==3){
								myAfterHash = serverSplits[3];
							}
							else if (i==4){
								myBeforePort = serverSplits[4];
							}
							else if (i==5){
								myAfterPort = serverSplits[5];
							}
						}
						setDetails(myPort, myPortHash, myBeforeHash, myAfterHash, myBeforePort, myAfterPort);
						unanimousReply(sScok);
					}
					else if(myQueryLength) {
						synchronized (this){
							String selection=serverSplits[2];
							serverResult = query(providerLink,null,selection,null,null);
							boolean isNull;
							if (serverResult.getCount() != 0) isNull = true;
							else isNull = false;
							if(isNull){
								PrintWriter unaniPrint = new PrintWriter(sScok.getOutputStream(),true);
								unaniPrint.println(serverResult.getString(0)+regex+serverResult.getString(1));
								sScok.close();
							}
						}


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
		for(int i=0;i<6;i++){
			if(i==0){
				myRemoteManager.setMyRemote(a);
			}
			else if (i==1){
				myRemoteManager.setMyHash(b);
			}
			else if (i==2){
				myRemoteManager.setMyBefore(c);
			}
			else if (i==3){
				myRemoteManager.setMyAfter(d);
			}
			else if (i==4){
				myRemoteManager.setMyBeforeRemote(e);
			}
			else if (i==5){
				myRemoteManager.setMyAfterRemote(f);
			}
		}
		Integer position  = hashes_list.indexOf(myRemoteManager.getMyHash());
		Integer nextNextPosition = (position+2)%hashes_list.size();
		myRemoteManager.setMyAfterAfter(hashes_list.get(nextNextPosition));
		myRemoteManager.setMyAfterAfterRemote(REMOTES[REMOTE_ORDER[nextNextPosition]]);
		Log.i("Check for 10","My Port:"+myRemoteManager.getMyRemote());
		Log.i("Check for 10","My +1 Port:"+myRemoteManager.getMyAfterRemote());
		Log.i("Check for 10","My +2 Port:"+myRemoteManager.getMyAfterAfterRemote());

		return  false;
	}

	private boolean changeDetails(NodeManager n, String a, String b, String c, String d){
		for(int i=0;i<4;i++){
			if(i==0){
				n.setMyBefore(a);
			}
			else if (i==1){
				n.setMyAfter(b);
			}
			else if (i==2){
				n.setMyBeforeRemote(c);
			}
			else if (i==3){
				n.setMyAfterRemote(d);
			}
		}
		return  false;
	}

	private void replicate(String toSend, Integer port){

		Socket replication;
		try {
			replication = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), port);
			PrintWriter replicatePrint = new PrintWriter(replication.getOutputStream(),true);
			BufferedReader replicateBuff  = new BufferedReader(new InputStreamReader(replication.getInputStream()));

			replicatePrint.println("JustInsert:"+toSend);
			String replicateReply = replicateBuff.readLine();
			if(replicateReply!=null){
				Log.i("in replicate",replicateReply);
			}
			replicateBuff.close();
			replicatePrint.close();
			replication.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private MatrixCursor enquire(String toSend, Integer port){

		Socket enquiry;
		String[] columnNames = {QUESTION, ANSWER};
		MatrixCursor matrixCursor = new MatrixCursor(columnNames);
		try {
			enquiry = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), port);
			PrintWriter enquiryPrint = new PrintWriter(enquiry.getOutputStream(),true);
			BufferedReader enquiryBuff  = new BufferedReader(new InputStreamReader(enquiry.getInputStream()));
			Log.i("Justing",toSend);
			enquiryPrint.println("JustQuery:"+toSend);
			String enquireReply = enquiryBuff.readLine();
			Log.i("Query Rpely",enquireReply);
			if(enquireReply!=null){
				String[] variables = enquireReply.split(":");
				matrixCursor.addRow(new String[]{variables[0], variables[1]});
			}
			enquiryBuff.close();
			enquiryPrint.close();
			enquiry.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return matrixCursor;
	}

	private String cursorToString(Cursor change, Integer type){
		String result = "";
		change.moveToFirst();
		switch(type){
			case 0:
				while(!change.isAfterLast()) {
					result = result.concat(regex);
					result = result.concat(change.getString(0));
					result = result.concat(regex);
					result = result.concat(change.getString(1));
					change.moveToNext();
				}
				break;
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
		try{
			ListIterator<NodeManager> iterator = managers.listIterator();
			while(iterator.hasNext()){
				NodeManager exampleNode = iterator.next();
				if(exampleNode.getMyRemote().equals(REMOTES[0])){
					Log.i("Setting self","Details for 11108");
					String myPort = null;
					String myPortHash = null;
					String myBeforeHash = null;
					String myAfterHash = null;
					String myBeforePort = null;
					String myAfterPort = null;
					for(int i=0;i<6;i++){
						if(i==0){
							myPort = exampleNode.getMyRemote();
						}
						else if (i==1){
							myPortHash = exampleNode.getMyHash();
						}
						else if (i==2){
							myBeforeHash = exampleNode.getMyBefore();
						}
						else if (i==3){
							myAfterHash = exampleNode.getMyAfter();
						}
						else if (i==4){
							myBeforePort = exampleNode.getMyBeforeRemote();
						}
						else if (i==5){
							myAfterPort = exampleNode.getMyAfterRemote();
						}
					}
					setDetails(myPort, myPortHash, myBeforeHash, myAfterHash, myBeforePort, myAfterPort);
				}
				else{
					List<String> nodeDetails = new ArrayList<String>();
					Socket sock = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(exampleNode.getMyRemote()));
					PrintWriter detPrint  = new PrintWriter(sock.getOutputStream(),true);
					for(int i=0;i<6;i++){
						if(i==0){
							nodeDetails.add(exampleNode.getMyRemote());
						}
						else if (i==1){
							nodeDetails.add(exampleNode.getMyHash());
						}
						else if (i==2){
							nodeDetails.add(exampleNode.getMyBefore());
						}
						else if (i==3){
							nodeDetails.add(exampleNode.getMyAfter());
						}
						else if (i==4){
							nodeDetails.add(exampleNode.getMyBeforeRemote());
						}
						else if (i==5){
							nodeDetails.add(exampleNode.getMyAfterRemote());
						}
					}
					ListIterator<String> nodeDetailsIterator = nodeDetails.listIterator();
					String details = "";
					while (nodeDetailsIterator.hasNext()){
						details = details.concat(nodeDetailsIterator.next());
						details = details.concat(regex);
					}
					detPrint.println(details.concat("Details"));
					BufferedReader brClient1 = new BufferedReader(new InputStreamReader(sock.getInputStream()));
					String readLine=brClient1.readLine();
					Log.i("Details reply",readLine);
					sock.close();

				}
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
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

