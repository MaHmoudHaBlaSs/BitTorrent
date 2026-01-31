package files;

public class Block {
    public int pieceIndex;
    public int begin;
    public int length;
    public byte[] data;

    public void setData(byte[] data) {
        this.data = data;
    }

    public Block(int pieceIndex, int begin, int length) {
        this.pieceIndex = pieceIndex;
        this.begin = begin;
        this.length = length;
    }
    public Block(int pieceIndex, int begin, int length, byte[] data) {
        this(pieceIndex, begin, length);
        this.data = data;
    }
}
