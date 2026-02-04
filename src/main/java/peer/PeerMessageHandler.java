package peer;

import files.Block;
import protocol.MessageType;
import protocol.PeerMessage;
import protocol.ProtocolUtils;
import utils.NetworkUtils;

import java.io.IOException;
import java.util.BitSet;

/**
  PeerMessageHandler job is to update PeerState and send a response PeerMessage (if needed)
 */

public class PeerMessageHandler {
    public void handle(PeerMessage message, PeerContext context){
        try{
            switch (message.getMessageId()){
                case MessageType.BITFIELD -> handleBitfield(message, context);
                case MessageType.INTERESTED -> handleInterested(message, context);
                case MessageType.CHOKE -> handleChoke(message, context);
                case MessageType.UN_CHOKED -> handleUnchoke(message, context);
                case MessageType.REQUEST -> handleRequest(message, context);
                case MessageType.PIECE -> handlePiece(message, context);
                default -> throw new RuntimeException("Unknown MessageType Received!!");
            }
        } catch (Exception e){
            System.err.println(e.getMessage());
        }
    }

    private void handleBitfield(PeerMessage message, PeerContext context) throws IOException {
        context.state.setAvailablePieces(BitSet.valueOf(message.getPayload()));
        NetworkUtils.sendPeerMessage(context.out, new PeerMessage(MessageType.INTERESTED));
    }

    private void handleInterested(PeerMessage message, PeerContext context){

    }

    private void handleUnchoke(PeerMessage message, PeerContext context) throws IOException {
        context.state.setChoke(false);
        Block block = context.piece.nextBlock();
        if (block == null)
            return;

        PeerMessage requestMessage = new PeerMessage(13, MessageType.REQUEST,
                ProtocolUtils.makeRequestPayload(block.pieceIndex, block.begin, block.length));

        NetworkUtils.sendPeerMessage(context.out, requestMessage);
    }

    private void handleChoke(PeerMessage message, PeerContext context){
        context.state.setChoke(true);
    }

    private void handleRequest(PeerMessage message, PeerContext context){

    }

    private void handlePiece(PeerMessage message, PeerContext context) throws IOException {
        Block blockAcquired = ProtocolUtils.extractBlockFromPayload(message.getPayload());
        context.piece.acquireBlock(blockAcquired);

        Block nextBlock = context.piece.nextBlock();

        if (nextBlock == null){
            if (context.piece.checkPiece()){
                context.piece.saveToDisk();

                return;

            }else {
                nextBlock = context.piece.resetDownloader();
                System.out.println("Incorrect Piece Data Assembled.");
            }
        }


        PeerMessage requestMessage = new PeerMessage(13, MessageType.REQUEST,
                ProtocolUtils.makeRequestPayload(nextBlock.pieceIndex, nextBlock.begin, nextBlock.length));


        if (!context.state.isChoke()){
            NetworkUtils.sendPeerMessage(context.out, requestMessage);

            context.state.addOutstandingRequest(nextBlock.pieceIndex + nextBlock.begin + nextBlock.length);
        }
    }

}
