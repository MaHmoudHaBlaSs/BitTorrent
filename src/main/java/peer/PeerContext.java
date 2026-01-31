package peer;

import files.PieceDownload;
import utils.Torrent;

import java.io.OutputStream;

/**
 * PeerContext is just a wrapper to control which objects we need to expose to the handler
 * to achieve a safer access and reduce repeating same arguments passing.
 */

public class PeerContext {
    final PeerState state;
    final Torrent torrentFile;
    final OutputStream out;
    final PieceDownload piece;

    public PeerContext(PeerState state, Torrent torrent, OutputStream out, PieceDownload piece){
        this.out = out;
        this.state = state;
        this.torrentFile = torrent;
        this.piece = piece;
    }
}
