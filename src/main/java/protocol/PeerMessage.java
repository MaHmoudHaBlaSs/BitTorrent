package protocol;

import utils.EncodingUtils;

import java.util.Arrays;

public class PeerMessage {

    private final int length; // length = 1 + payload_size
    private final MessageType messageId;
    private final byte[] payload;
    private final byte[] rawMessage;


    public PeerMessage(int length, byte[] raw){
        this.length = length;
        messageId = MessageType.getType(raw[0]);
        payload = Arrays.copyOfRange(raw, 1, length);

        rawMessage = new byte[length + 4];
        System.arraycopy(EncodingUtils.convertIntToBytes(length), 0, rawMessage, 0, 4);
        System.arraycopy(raw, 0, rawMessage, 4, raw.length);
    }
    public PeerMessage(int length, MessageType type, byte[] payload){
        this.length = length;
        messageId = type;
        this.payload = payload;

        rawMessage = new byte[length + 4];
        System.arraycopy(EncodingUtils.convertIntToBytes(length), 0, rawMessage, 0, 4);
        rawMessage[4] = (byte) messageId.label;
        System.arraycopy(payload, 0, rawMessage, 5, payload.length);
    }

    public PeerMessage(MessageType type){
        length = 1;
        messageId = type;
        payload = null;

        rawMessage = new byte[5];
        System.arraycopy(EncodingUtils.convertIntToBytes(length), 0, rawMessage, 0, 4);
        rawMessage[4] = (byte) messageId.label;
    }

    public int getLength() {
        return length;
    }
    public MessageType getMessageId() {
        return messageId;
    }
    public byte[] getPayload() {
        return payload;
    }
    public byte[] getRawMessage() {
        return rawMessage;
    }

    @Override
    public String toString() {
        return "PeerMessage { " +
                "length= " + length +
                ", messageId= " + messageId +
                ", payload= " + Arrays.toString(payload) +
                ", rawMessage= " + Arrays.toString(rawMessage) +
                '}';
    }
}
