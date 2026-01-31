package peer;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

/**
 * PeerState job is to hold a memory for the PeerConnection that is gained from
 * previous messages exchanged over time
 */

public class PeerState {
    private boolean interest;
    private boolean choke;
    private BitSet availablePieces;
    private Set<Integer> outstandingRequests;


    public PeerState(){
        interest = false;
        choke = true;
        availablePieces = new BitSet();
        outstandingRequests = new HashSet<>();
    }

    public void addOutstandingRequest(int val){
        outstandingRequests.add(val);
    }

    public boolean isInterest() {
        return interest;
    }

    public void setInterest(boolean interest) {
        this.interest = interest;
    }

    public boolean isChoke() {
        return choke;
    }

    public void setChoke(boolean choke) {
        this.choke = choke;
    }

    public BitSet getAvailablePieces() {
        return availablePieces;
    }

    public void setAvailablePieces(BitSet availablePieces) {
        this.availablePieces = availablePieces;
    }

    public Set<Integer> getOutstandingRequests() {
        return outstandingRequests;
    }

    public void setOutstandingRequests(Set<Integer> outstandingRequests) {
        this.outstandingRequests = outstandingRequests;
    }
}
