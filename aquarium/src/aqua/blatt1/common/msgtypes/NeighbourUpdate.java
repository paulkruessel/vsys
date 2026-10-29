package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

import aqua.blatt1.common.Direction;

@SuppressWarnings("serial")
public final class NeighbourUpdate implements Serializable {
	private final InetSocketAddress address;
    private final Direction direction;

	public NeighbourUpdate(InetSocketAddress address, Direction direction) {
		this.address = address;
        this.direction = direction;
	}

    public InetSocketAddress getAddress() {
        return this.address;
    }	

    public Direction getDirection() {
        return this.direction;
    }
}
