package edu.buffalo.cse.cse486586.simpledynamo;

import java.util.Comparator;

public class Hasher implements Comparator<NodeManager>
{
    @Override
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
    }

}
