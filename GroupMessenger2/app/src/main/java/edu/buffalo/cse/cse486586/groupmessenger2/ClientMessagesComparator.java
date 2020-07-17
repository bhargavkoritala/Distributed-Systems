package edu.buffalo.cse.cse486586.groupmessenger2;


import java.util.Comparator;

public class ClientMessagesComparator implements Comparator<ClientMessage> {
    public int compare(ClientMessage c1, ClientMessage c2) {
        if (c1.getSeqNum() < c2.getSeqNum())
            return -1;
        else if (c1.getSeqNum() > c2.getSeqNum())
            return 1;
        else {
            if (Integer.parseInt(c1.getClientPort()) < Integer.parseInt(c2.getClientPort()))
                return -1;
            else
                return 1;
            }
        }
    }
