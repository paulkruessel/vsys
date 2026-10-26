package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

import aqua.blatt1.common.Direction;

@SuppressWarnings("serial")
public class NeighbourUpdate implements Serializable {
    private final InetSocketAddress newNeighborAddress;
    private final Direction direction;

    public NeighbourUpdate(InetSocketAddress newNeighborAddress, Direction direction) {
        this.newNeighborAddress = newNeighborAddress;
        this.direction = direction;
    }

    public InetSocketAddress getNewNeighborAddress() {
        return newNeighborAddress;
    }

    public Direction getDirection() {
        return direction;
    }
}
