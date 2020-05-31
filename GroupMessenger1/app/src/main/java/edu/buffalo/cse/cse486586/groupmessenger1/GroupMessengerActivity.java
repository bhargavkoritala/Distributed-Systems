package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final int SERVER_PORT = 10000;
    static final String[] REMOTE_PORT = {"11108", "11112", "11116", "11120", "11124"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException ioe) {
            Log.e(TAG, ioe.getMessage());
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        final EditText editText = (EditText) findViewById(R.id.editText1);

        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */



        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                String msg = editText.getText().toString() + "\n";
                editText.setText(""); // This is one way to reset the input box.
                TextView localTextView = (TextView) findViewById(R.id.textView1);
                localTextView.append("\t" + msg); // This is one way to display a string.
                TextView remoteTextView = (TextView) findViewById(R.id.textView1);
                remoteTextView.append("\n");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
                return;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }


    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {
        int sequence = 0;
        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            System.out.println(serverSocket.getLocalPort());
            Socket clientSocket = null;
            BufferedReader message = null;
            String message_line = null;
            PrintWriter out = null;
            try{
//                 https://docs.oracle.com/javase/tutorial/networking/sockets/readingWriting.html
                while(true) {
                    clientSocket = serverSocket.accept();
                    message = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    message_line = message.readLine();

//                    out = new PrintWriter(clientSocket.getOutputStream(), true);
//                    out.println(message_line);
                    if(message_line!=null){
                        publishProgress(message_line);
                    }


                    clientSocket.close();
                    message.close();
                }


            }
            catch(Exception e){
                System.out.println(e.getMessage());
            }
            return null;
        }

        protected void onProgressUpdate(String...strings){
            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);   //using same textview for both received and sent messages
            remoteTextView.append(strReceived + "\t\n");
            //TextView localTextView = (TextView) findViewById(R.id.textView1);
            //localTextView.append("\n");
            if(strReceived!=null){
                ContentValues keyValueToInsert = new ContentValues();
                // inserting <”key-to-insert”, “value-to-insert”>
                keyValueToInsert.put("key", String.valueOf(sequence));
                keyValueToInsert.put("value", strReceived);
                Uri.Builder uriBuilder = new Uri.Builder();
                uriBuilder.authority("edu.buffalo.cse.cse486586.groupmessenger1.provider");    //from onPTestClickListener.java
                uriBuilder.scheme("content");
                getContentResolver().insert(
                        uriBuilder.build(),    // assume we already created a Uri object with our provider URI
                        keyValueToInsert
                );
                sequence+=1;
                Log.i(TAG, uriBuilder.build().toString());
            }

        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            try {

                int i;
                for(i = 0; i<REMOTE_PORT.length; i++){
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORT[i]));
                    /* https://docs.oracle.com/javase/tutorial/networking/sockets/clientServer.html */
                    String msgToSend = msgs[0];
                    PrintWriter out = new PrintWriter(socket.getOutputStream(),true);
                    out.println(msgToSend);
                    //socket.close();
                    //out.close();
                }
            } catch (UnknownHostException e) {
                System.out.println(e.getMessage());
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                System.out.println(e.getMessage());
                Log.e(TAG, "ClientTask socket IOException");
            }catch (Exception e){
                Log.e(TAG, e.getMessage());
            }

            return null;
        }
    }
}
