package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import static java.util.Collections.max;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static String[] PORTS = {"11108", "11112", "11116", "11120", "11124"};
    static  List<String> REMOTE_PORTS = new ArrayList<String>(Arrays.asList(PORTS));
    static List<String> failures = new ArrayList<String>();
    String im_failed = "";
    static final int SERVER_PORT = 10000;
    static HashMap<String, InetSocketAddress> sockets = new HashMap<String,InetSocketAddress>();
    static int clientMessageId = -1;
    static int wow_number = 0;
    static int universalAgreedSequence = -1;
    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TelephonyManager telManager = (TelephonyManager) this.getSystemService(TELEPHONY_SERVICE);
        String portString = telManager.getLine1Number().substring(telManager.getLine1Number().length() - 4);
        final String portNumber = String.valueOf((Integer.parseInt(portString)) * 2);
        Log.i("This avd port is : ",portNumber);

        try{
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (Exception e){
            Log.e(TAG, e.toString());
        }
        final TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        final Button sendButton = (Button) findViewById(R.id.button4);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editText = (EditText) findViewById(R.id.editText1);
                Log.i(TAG, "From EditText1 : " +editText.getText().toString());
                String msg = editText.getText().toString();
                //tv.append(msg + NEW_LINE);
                editText.setText(" ");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, portNumber);
                Log.i(TAG, "Client "+portNumber+" created");
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

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            int avdLocalSequence = -1;
            int received_seq;
            //https://docs.oracle.com/javase/7/docs/api/java/util/PriorityQueue.html
            PriorityQueue<ClientMessage> clientMessages = new PriorityQueue<ClientMessage>(20000, new ClientMessagesComparator());
            Map<String,Integer> mapMsg = new HashMap<String, Integer>();
            try{
                while(true) {
                    try{
                        Socket client = serverSocket.accept();
                        BufferedReader messageReader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                        PrintWriter seqSender = new PrintWriter(client.getOutputStream(), true);
                        String msg= messageReader.readLine();
                        if (msg!= null) {
                            if (msg.contains("+")) {
                                if (msg.contains(":")) {

                                    //this marks the arrival of messages that contain the final sequence
                                    Log.i("Noting sequence", msg);
                                    received_seq = Integer.parseInt(msg.split(":")[0]);
                                    String r_messg = msg.split(":")[1].split("[+]")[0];
                                    String r_port = msg.split(":")[1].split("[+]")[1];
                                    //if mapMsg.containsKey(r_messg)
                                    if (mapMsg.containsKey(r_messg)){
                                        mapMsg.remove(r_messg);
                                    }
                                            mapMsg.put(r_messg,received_seq);
                                    Log.i("checking for message", "Message : " + r_messg);

                                    synchronized (clientMessages){
                                        Iterator<ClientMessage> messageChecker = clientMessages.iterator();
                                        ClientMessage clientMsg = clientMessages.peek();
                                        while (messageChecker.hasNext()) {
                                            clientMsg = messageChecker.next();
                                            Log.i("Message CHeck", "To be checked : " + clientMsg.getMessage() + " with : " + r_messg);
                                            if (clientMsg.getMessage().equals(r_messg)) {
                                                Log.i("Removing ", "Message foung");
                                                clientMessages.remove(clientMsg);
                                                break;
                                            }

                                        }
                                    }

                                    //create a new message similiar to that message which was found/removed
                                    ClientMessage rec_msg = new ClientMessage(r_port, r_messg, received_seq, true);
                                    synchronized (clientMessages){
                                        clientMessages.add(rec_msg);
                                    }


                                    ClientMessage top;
                                    Log.i("outside publish loop", clientMessages.peek().getMessage() + " : " + clientMessages.peek().getDeliverable());
                                    while ((top = clientMessages.peek()) != null && top.getDeliverable()) {
                                        universalAgreedSequence += 1;
                                        ContentResolver mContentResolver = getContentResolver();
                                        Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
                                        ContentValues mContentValues = new ContentValues();
                                        mContentValues.put(KEY_FIELD, String.valueOf(universalAgreedSequence));
                                        mContentValues.put(VALUE_FIELD, top.getMessage());
                                        mContentResolver.insert(mUri, mContentValues);
                                        Log.i("About to publish", top.getMessage());
                                        publishProgress(top.getMessage());
                                        Log.i("publishing", top.getMessage() + " with seq : " + top.getSeqNum());
                                        synchronized (clientMessages){
                                            clientMessages.remove();
                                        }

                                    }

                                    Iterator<ClientMessage> faultsRemover = clientMessages.iterator();
                                    ClientMessage faults = clientMessages.peek();
                                    while(faultsRemover.hasNext()){
                                         faults = faultsRemover.next();
                                         //remove each and every message from the queue which is from the failed port
                                         if(faults.getClientPort().equals(im_failed)){
                                             synchronized (clientMessages){
                                                 clientMessages.remove(faults);
                                             }
                                         }
                                    }


                                } else {
                                    Log.i("Proposing Sequence", String.valueOf(avdLocalSequence + 1) + "for message " + msg.split("[+]")[0]);
                                    avdLocalSequence = Math.max(avdLocalSequence, universalAgreedSequence) + 1;
                                    seqSender.println(avdLocalSequence);

                                    ClientMessage newClientMessage = new ClientMessage(msg.split("[+]")[1],
                                            msg.split("[+]")[0],
                                            Integer.parseInt(msg.split("[+]")[2]),
                                            false);
                                    synchronized (clientMessages){
                                        clientMessages.add(newClientMessage);
                                    }

                                    mapMsg.put(newClientMessage.getMessage(), avdLocalSequence);
                                    Log.i("added to queue", newClientMessage.getMessage() + newClientMessage.getClientPort());
                                }
                            } else {
                                throw new IOException("irrelevant message format");
                            }


                        }
                    }catch(Exception e){
                        Log.i("Server Task","Server Task");
                    }
                }

            } catch (Exception e){
                Log.e(TAG, e.toString());
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            String displayMessage = values[0].trim();
            TextView textView = (TextView) findViewById(R.id.textView1);
            textView.append(displayMessage + "/n");

        }
    }

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }



    private class ClientTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... strings) {

            //idea is to use the same client connection to decrease latency

            List<Integer> proposals = new ArrayList<Integer>();
            Integer final_message_number;
            //for sending the message to the multicast
            for (String remote_port : REMOTE_PORTS) {
                Log.d("port",remote_port);
                Socket client = new Socket();
                InetSocketAddress socketAddress = null;
                try {
                    socketAddress = new InetSocketAddress(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remote_port));
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                try {
                    client.connect(socketAddress);
                    client.setSoTimeout(500);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {

                    sockets.put(remote_port,socketAddress);

                    PrintWriter pingMessage = new PrintWriter(client.getOutputStream(), true);
                    BufferedReader pingAck = new BufferedReader(new InputStreamReader(client.getInputStream()));

                    //Sending a message in the format (Message + This avd port + Message Identification Number)
                    //clientMessageId serves the purpose of distinguishing messages of a particular client.
                    pingMessage.println(strings[0]+ "+" + strings[1]+ "+" + clientMessageId);
                    //Expecting proposals here
                    String reply = pingAck.readLine();
                    //adding proposals to a list.
                    proposals.add(Integer.parseInt(reply));
                    Thread.sleep(500);
                } catch (Exception e) {
//                    Log.e("getproposals", e.getMessage());
                    //e.printStackTrace();
                    //if an instance is failed, the port is captured in the failed field
                    im_failed = remote_port;
                    try {
                        //failed port is closed, so as to revoke the connection made
                        client.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }

            //As a precaution to future messages, the failed port is removed from the
            if(im_failed!=null && REMOTE_PORTS.contains(im_failed)){
                REMOTE_PORTS.remove(im_failed);
            }

            //select the maximum from the proposals
            final_message_number = Collections.max(proposals);
            Log.i("Sequence","Message : " + strings[0] + " Sequence : " +final_message_number.toString());

            //Sending the selected proposal to all the avd's
            for (String remote_port : REMOTE_PORTS) {
                Socket client_final = new Socket();
                try {
                    client_final.connect(sockets.get(remote_port));
//                    client_final.setSoTimeout(500);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                PrintWriter seqSend = null;
                try {
                    seqSend = new PrintWriter(client_final.getOutputStream(), true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //BufferedReader ackGet = new BufferedReader(new InputStreamReader(sockets.get(remote_port).getInputStream()));
                try {
                    Log.i("Sequence Sending", "Message : "+strings[0] + "Sequence : "+final_message_number + " Port : "+remote_port);



                    //format = Sequence + Message + This avd port
                    seqSend.println(final_message_number + ":" + strings[0] + "+" + strings[1]);
                    Thread.sleep(500);
                } catch (Exception e) {
                    Log.e("Sending final message", e.toString());
                }
            }
            //remove all the connections made.
            sockets.clear();
            return null;
        }
    }
}