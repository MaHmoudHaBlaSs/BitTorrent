# BitTorrent Client

A minimal BitTorrent client implemented from scratch in Java.  
This project demonstrates how peer-to-peer file sharing works internally by implementing the core BitTorrent protocol, 
including tracker communication, peer connections, piece downloading, and file reconstruction.

>This project was done with help of [Codecrafters.io](https://app.codecrafters.io/) they provided the tracker behavior and peers to connect with.


## Features

- Parse and decode `.torrent` files (bencode format)
- Communicate with HTTP trackers to retrieve peer lists
- Establish TCP connections with peers
- Perform BitTorrent handshake
- Exchange protocol messages:
   - choke / unchoke
   - interested / not interested
   - have
   - bitfield
   - request
   - piece
- Download torrent data in pieces
- Verify piece integrity using SHA-1 hashes
- Reconstruct original files from downloaded pieces
- Support multi-file torrents

## How It Works

BitTorrent splits the entire torrent payload into fixed-size pieces.  
The client:

1. Parses the `.torrent` file
2. Contacts the tracker to get peers
3. Connects to peers via TCP
4. Requests pieces from peers
5. Verifies each piece using SHA-1
6. Writes verified data to disk
7. Reconstructs the original file(s)

Peers exchange pieces independently of file boundaries.

Check this Notion notebook for more explanation: [BitTorrent Client Notebook](https://walnut-crocus-562.notion.site/BitTorrent-2ed6ce80c3168064b64ad6fbd92ace8c?source=copy_link)