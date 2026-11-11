package aqua.blatt1.client;

import java.net.InetSocketAddress;

import messaging.Endpoint;
import messaging.Message;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.Properties;
import aqua.blatt1.common.msgtypes.*;

public class ClientCommunicator {
	private final Endpoint endpoint;

	public ClientCommunicator() {
		endpoint = new Endpoint();
	}

	public class ClientForwarder {
		private final InetSocketAddress broker;

		private ClientForwarder() {
			this.broker = new InetSocketAddress(Properties.HOST, Properties.PORT);
		}

		public void register() {
			endpoint.send(broker, new RegisterRequest());
		}

		public void deregister(String id) {
			endpoint.send(broker, new DeregisterRequest(id));
		}

		public void handOff(FishModel fish, InetSocketAddress neighbour) {
			endpoint.send(neighbour, new HandoffRequest(fish));
		}

		public void sendToken(InetSocketAddress neighbour) {
			endpoint.send(neighbour, new Token());
		}

		public void sendSnapshotMarker(InetSocketAddress neighbour) {
			endpoint.send(neighbour, new SnapshotMarker());
		}

		public void sendCollectSnapshotToken(CollectSnapshotToken token, InetSocketAddress neighbour) {
			endpoint.send(neighbour, token);
		}

		public void sendLocationRequest(LocationRequest token, InetSocketAddress neighbour) {
			endpoint.send(neighbour, token);
		}
	}

	public class ClientReceiver extends Thread {
		private final TankModel tankModel;

		private ClientReceiver(TankModel tankModel) {
			this.tankModel = tankModel;
		}

		@Override
		public void run() {
			while (!isInterrupted()) {
				Message msg = endpoint.blockingReceive();

				if (msg.getPayload() instanceof RegisterResponse)
					tankModel.onRegistration(((RegisterResponse) msg.getPayload()).getId());

				if (msg.getPayload() instanceof HandoffRequest)
					tankModel.receiveFish(((HandoffRequest) msg.getPayload()).getFish(), msg.getSender());

				if (msg.getPayload() instanceof NeighbourUpdate) {
					tankModel.updateNeighbor(((NeighbourUpdate) msg.getPayload()).getDirection(), ((NeighbourUpdate) msg.getPayload()).getNewNeighborAddress());
				}

				if (msg.getPayload() instanceof Token) {
					tankModel.receiveToken();
				}

				if (msg.getPayload() instanceof SnapshotMarker) {
					tankModel.receiveSnapshotMarker(msg.getSender());
				}

				if (msg.getPayload() instanceof CollectSnapshotToken) {
					tankModel.receiveCollectSnapshotToken((CollectSnapshotToken) msg.getPayload());
				}

				if (msg.getPayload() instanceof LocationRequest) {
					tankModel.locateFishGlobally(((LocationRequest) msg.getPayload()).getFishId());
				}

			}
			System.out.println("Receiver stopped.");
		}
	}

	public ClientForwarder newClientForwarder() {
		return new ClientForwarder();
	}

	public ClientReceiver newClientReceiver(TankModel tankModel) {
		return new ClientReceiver(tankModel);
	}

}
