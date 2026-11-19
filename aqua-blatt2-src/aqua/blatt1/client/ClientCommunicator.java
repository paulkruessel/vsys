package aqua.blatt1.client;

import java.net.InetSocketAddress;

import messaging.Endpoint;
import messaging.Message;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.Properties;
import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.HandoffRequest;
import aqua.blatt1.common.msgtypes.LocationRequest;
import aqua.blatt1.common.msgtypes.LocationUpdate;
import aqua.blatt1.common.msgtypes.NameResolutionRequest;
import aqua.blatt1.common.msgtypes.NameResolutionResponse;
import aqua.blatt1.common.msgtypes.RegisterRequest;
import aqua.blatt1.common.msgtypes.RegisterResponse;
import aqua.blatt1.common.msgtypes.SnapshotMarker;
import aqua.blatt1.common.msgtypes.SnapshotToken;
import aqua.blatt1.common.msgtypes.NeighbourUpdate;
import aqua.blatt1.common.msgtypes.Token;

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

		public void sendSnapshotToken(InetSocketAddress neighbour, SnapshotToken token) {
			endpoint.send(neighbour, token);
		}

		public void sendLocationRequest(InetSocketAddress address, String fishId) {
			endpoint.send(address, new LocationRequest(fishId));
		}

		public void sendNameResolveRequest(String tankId, String fishId) {
			endpoint.send(broker, new NameResolutionRequest(tankId, fishId));
		}
		
		public void sendLocationUpdate(InetSocketAddress homeTank, String fishId) {
			endpoint.send(homeTank, new LocationUpdate(fishId));
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
					tankModel.receiveFish(((HandoffRequest) msg.getPayload()).getFish());

				if (msg.getPayload() instanceof NeighbourUpdate) {
					tankModel.updateNeighbor(((NeighbourUpdate) msg.getPayload()).getDirection(), ((NeighbourUpdate) msg.getPayload()).getNewNeighborAddress());
				}

				if (msg.getPayload() instanceof Token) {
					tankModel.receiveToken();
				}

				if (msg.getPayload() instanceof SnapshotMarker) {
					tankModel.receiveSnapshotMarker(msg.getSender());
				}

				if (msg.getPayload() instanceof SnapshotToken token) {
					tankModel.receiveSnapshotToken(token);
				}

				if (msg.getPayload() instanceof LocationRequest request) {
					tankModel.locateFishGlobally(request.getFishId());
				}

				if (msg.getPayload() instanceof NameResolutionResponse response) {
					tankModel.receiveNameResolutionResponse(response.getRequestId(), response.getTargetAddress());
				}

				if (msg.getPayload() instanceof LocationUpdate update) {
					tankModel.receiveLocationUpdate(update.getFishId(), msg.getSender());
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
