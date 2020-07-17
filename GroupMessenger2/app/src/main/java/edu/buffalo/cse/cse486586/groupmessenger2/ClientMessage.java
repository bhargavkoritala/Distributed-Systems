package edu.buffalo.cse.cse486586.groupmessenger2;


public class ClientMessage{
    private String clientPort;
    private String message;
    private int seqNum;
    private boolean deliverable;

    public ClientMessage(String message){
        this.message = message;
    }

    public ClientMessage(String message, String clientPort){
        this.message = message;
        this.clientPort = clientPort;
    }

    public ClientMessage(String clientPort, String message, int seqNum, boolean deliverable){
        this.clientPort = clientPort;
        this.message = message;
        this.seqNum = seqNum;
        this.deliverable = deliverable;
    }

    public void setMessage(String message){
        this.message = message;
    }

    public String getMessage(){
        return this.message;
    }

    public void setClientPort(String clientPort){
        this.clientPort = clientPort;
    }

    public String getClientPort(){
        return this.clientPort;
    }

    public void setSeqNum(int seqNum){
        this.seqNum = seqNum;
    }

    public int getSeqNum(){
        return this.seqNum;
    }

    public void setDeliverable(boolean deliverable){
        this.deliverable = deliverable;
    }

    public boolean getDeliverable(){
        return this.deliverable;
    }









}


