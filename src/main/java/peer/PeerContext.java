package peer;

import files.FileDownloader;
import files.PieceDownloader;
import utils.Torrent;

import java.io.OutputStream;

/**
 * PeerContext is just a wrapper to control which objects we need to expose to the handler
 * to achieve a safer access and reduce repeating same arguments passing.
 */

public class PeerContext {
    PeerState state;
    Torrent torrentFile;
    OutputStream out;
    PieceDownloader pieceDownloader;
    FileDownloader fileDownloader;

    public PeerContext(PeerState state, Torrent torrent, OutputStream out, PieceDownloader pieceDownloader){
        this.out = out;
        this.state = state;
        this.torrentFile = torrent;
        this.pieceDownloader = pieceDownloader;
        this.fileDownloader = null;
    }

    public PeerContext(PeerState state, Torrent torrent, OutputStream out, FileDownloader fileDownloader){
        this.out = out;
        this.state = state;
        this.torrentFile = torrent;
        this.fileDownloader = fileDownloader;
        this.pieceDownloader = null;
    }
}

