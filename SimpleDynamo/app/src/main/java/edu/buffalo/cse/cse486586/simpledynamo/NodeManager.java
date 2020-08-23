package edu.buffalo.cse.cse486586.simpledynamo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class NodeManager {

    private String myHash;
    private String myAfter;
    private String myAfterAfter;
    private String myBefore;
    private String myRemote;
    private String myAfterRemote;
    private String myAfterAfterRemote;
    private String myBeforeRemote;
    private HashMap<String, String> remoteMapping  = new HashMap<String, String>();
    private HashMap<String,String> hashMapping  = new HashMap<String, String>();
    private List<NodeManager> allManagers = new ArrayList<NodeManager>();

    public String getMyHash() {
        return myHash;
    }

    public void setMyHash(String myHash) {
        this.myHash = myHash;
    }

    public String getMyAfter() {
        return myAfter;
    }

    public void setMyAfter(String myAfter) {
        this.myAfter = myAfter;
    }

    public String getMyBefore() {
        return myBefore;
    }

    public void setMyBefore(String myBefore) {
        this.myBefore = myBefore;
    }

    public String getMyRemote() {
        return myRemote;
    }

    public void setMyRemote(String myRemote) {
        this.myRemote = myRemote;
    }

    public String getMyAfterRemote() {
        return myAfterRemote;
    }

    public void setMyAfterRemote(String myAfterRemote) {
        this.myAfterRemote = myAfterRemote;
    }

    public String getMyBeforeRemote() {
        return myBeforeRemote;
    }

    public void setMyBeforeRemote(String myBeforeRemote) {
        this.myBeforeRemote = myBeforeRemote;
    }

    public HashMap<String, String> getRemoteMapping() {
        return remoteMapping;
    }

    public void setRemoteMapping(HashMap<String, String> remoteMapping) {
        this.remoteMapping = remoteMapping;
    }

    public HashMap<String, String> getHashMapping() {
        return hashMapping;
    }

    public void setHashMapping(HashMap<String, String> hashMapping) {
        this.hashMapping = hashMapping;
    }

    public List<NodeManager> getAllManagers() {
        return allManagers;
    }

    public void setAllManagers(List<NodeManager> allManagers) {
        this.allManagers = allManagers;
    }

    public String getMyAfterAfter() {
        return myAfterAfter;
    }

    public void setMyAfterAfter(String myAfterAfter) {
        this.myAfterAfter = myAfterAfter;
    }

    public String getMyAfterAfterRemote() {
        return myAfterAfterRemote;
    }

    public void setMyAfterAfterRemote(String myAfterAfterRemote) {
        this.myAfterAfterRemote = myAfterAfterRemote;
    }

    public static Comparator<NodeManager> managerComparator = new Comparator<NodeManager>() {

        public int compare(NodeManager node1, NodeManager node2) {
            String firstHash = node1.getMyHash();
            String secondHash = node2.getMyHash();
            int decider=firstHash.compareTo(secondHash);
            int value;

            if(decider<0) {
                value =-1;
            }
            else if (decider>0) {
                value =  1;
            }
            else{
                value = 0;
            }

            switch (value){
                case 1:
                    return 1;
                case -1:
                    return -1;
                default:
                    return 0;


            }
        }};
}