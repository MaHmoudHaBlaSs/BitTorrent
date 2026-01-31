package protocol;

public enum MessageType{
    CHOKE(0), UN_CHOKED(1), INTERESTED(2), NOT_INTERESTED(3),
    HAVE(4), BITFIELD(5), REQUEST(6), PIECE(7), CANCEL(8);

    public final int label;

    private static final MessageType[] LOOKUP = new MessageType[9];
    static {
        for (MessageType t: values())
            LOOKUP[t.label] = t;
    }

    MessageType(int label) {
        this.label = label;
    }

    public static MessageType getType(int label){
        return LOOKUP[label];
    }
}