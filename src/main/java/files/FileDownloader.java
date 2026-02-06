package files;

import protocol.ProtocolUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;

public class FileDownloader{
    int fileLength;
    int pieceLength;
    byte[] pieceHashes;
    byte[] buffer;

    PieceDownloader[] pieceDownloader;
    BitSet receivedPieces;
    int totalPieces;

    boolean completed;
    String downloadDir;

    public FileDownloader(int fileLength, int pieceLength, byte[] pieceHashes, String downloadDir){
        this.fileLength = fileLength;
        this.pieceLength = pieceLength;
        this.pieceHashes = pieceHashes;
        this.downloadDir = downloadDir;
        buffer = new byte[fileLength];

        totalPieces = (fileLength % pieceLength == 0)? fileLength / pieceLength: (fileLength/pieceLength) + 1;
        receivedPieces = new BitSet(totalPieces);
        completed = false;

        pieceDownloader = new PieceDownloader[totalPieces];
        for (int index = 0; index < totalPieces; index++){

            int nextPieceLength;
            if (index == totalPieces -1){
                int pieceBegin = index * pieceLength;
                nextPieceLength = fileLength - pieceBegin;
            }
            else
                nextPieceLength = pieceLength;


            pieceDownloader[index] = new PieceDownloader(
                    index,
                    index * pieceLength,
                    nextPieceLength,
                    ProtocolUtils.extractPieceShaHash(pieceHashes, index));

        }
    }

    public PieceDownloader nextPiece() {
        int nextPieceIndex = receivedPieces.nextClearBit(0);
        if (nextPieceIndex == totalPieces) {
            completed = true;
            return null;
        }

        return pieceDownloader[nextPieceIndex];
    }

    public void acquirePiece(PieceDownloader piece) {
        receivedPieces.set(piece.pieceIndex);
        System.arraycopy(piece.buffer, 0, buffer, piece.pieceBegin, piece.pieceLength);
    }

    public void saveToDisk(){
        try (FileOutputStream os = new FileOutputStream(downloadDir)){
            os.write(buffer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isCompleted() {
        return completed;
    }

    public PieceDownloader getPieceDownloader(int pieceIndex){
        return pieceDownloader[pieceIndex];
    }
}
