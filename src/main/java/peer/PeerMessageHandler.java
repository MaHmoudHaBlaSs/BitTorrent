package peer;

import files.Block;
import files.PieceDownloader;
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
                case MessageType.UN_CHOKED -> handleUnchoke(context);
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

    private void handleUnchoke(PeerContext context) throws IOException {
        boolean isFile = (context.fileDownloader != null);
        PieceDownloader pieceDownloader;
        if (isFile)
            pieceDownloader = context.fileDownloader.nextPiece();
        else // It's just a piece without a file (piece download command)
            pieceDownloader = context.pieceDownloader;

        context.state.setChoke(false);
        Block block = pieceDownloader.nextBlock();
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
        boolean isFile = (context.fileDownloader != null);
        Block blockAcquired = ProtocolUtils.extractBlockFromPayload(message.getPayload());

        PieceDownloader pieceDownloader;
        if (isFile)
            pieceDownloader = context.fileDownloader.getPieceDownloader(blockAcquired.pieceIndex);
        else // It's just a piece without a file (piece download command)
            pieceDownloader = context.pieceDownloader;

        pieceDownloader.acquireBlock(blockAcquired);
        Block nextBlock = pieceDownloader.nextBlock();

        if (nextBlock == null){
            if (pieceDownloader.checkPiece()){
                if (isFile){
                    // Acquire a piece then Ask file downloader for another piece (if exist)
                    context.fileDownloader.acquirePiece(pieceDownloader);

                    PieceDownloader nextPieceDownloader = context.fileDownloader.nextPiece();
                    if (nextPieceDownloader != null)
                        nextBlock = nextPieceDownloader.nextBlock();
                    else // No next piece, we are done.
                        return;
                }
                else
                    return;
            }
            else {
                nextBlock = pieceDownloader.resetDownloader();
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
