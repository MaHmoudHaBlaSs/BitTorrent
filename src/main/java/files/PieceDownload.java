package files;

import org.apache.commons.codec.digest.DigestUtils;
import utils.EncodingUtils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;

public class PieceDownload {
    public static final int BLOCK_SIZE = 16 * 1024; // 16Kb

    int pieceIndex;
    int pieceLength;
    byte[] pieceHash;
    byte[] buffer;
    BitSet receivedBlocks;
    boolean completed;

    public int getTotalBlocks() {
        return totalBlocks;
    }

    int totalBlocks;

    final String downloadDir;

    public PieceDownload(int pieceIndex, int pieceLength, byte[] pieceHash, String downloadDir){
        this.pieceIndex = pieceIndex;
        this.pieceLength = pieceLength;
        this.pieceHash = pieceHash;
        buffer = new byte[pieceLength];
        this.downloadDir = downloadDir;

        totalBlocks = (pieceLength % BLOCK_SIZE == 0)? pieceLength / BLOCK_SIZE: (pieceLength / BLOCK_SIZE) + 1;
        receivedBlocks = new BitSet(totalBlocks); // By default, all bits are set to 0
        completed = false;
    }

    public Block nextBlock(){
        int blockIndex = receivedBlocks.nextClearBit(0);
        if (blockIndex == totalBlocks) // All blocks are collected
            return null;

        int blockBegin = blockIndex * BLOCK_SIZE;

        // Check if last block
        int length = (blockIndex == receivedBlocks.size()-1)? pieceLength - blockBegin: BLOCK_SIZE;
        return new Block(pieceIndex, blockBegin, length);
    }

    public void acquireBlock(Block block){
        int blockIndex = block.begin / BLOCK_SIZE;
        receivedBlocks.set(blockIndex);

        System.arraycopy(block.data, 0, buffer, block.begin, block.length);
    }

    public boolean checkPiece(){
        byte[] hashedBuffer = DigestUtils.sha1(buffer);
        return Arrays.equals(hashedBuffer, pieceHash);
    }

    public void saveToDisk() throws IOException {
        completed = true;
        FileOutputStream fos = new FileOutputStream(downloadDir);
        fos.write(buffer);
    }

    public Block resetDownloader(){
        Arrays.fill(buffer, (byte)0);
        receivedBlocks.clear();

        return nextBlock();
    }

    public boolean isCompleted(){
        return completed;
    }
}
