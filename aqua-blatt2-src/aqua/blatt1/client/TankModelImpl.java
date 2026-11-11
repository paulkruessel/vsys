package aqua.blatt1.client;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.msgtypes.CollectSnapshotToken;
import aqua.blatt1.common.msgtypes.LocationRequest;
import aqua.blatt1.common.msgtypes.LocationUpdate;
import aqua.blatt1.common.msgtypes.NameResolutionRequest;

public class TankModelImpl implements TankModel {
	private enum RecordingMode {
		IDLE, LEFT, RIGHT, BOTH
	}

	// private enum Location {
	// 	LEFT, RIGHT, HERE
	// }

	protected static final int MAX_FISHIES = 5;
	protected static final Random rand = new Random();
	protected volatile String id;
	protected final Set<FishModel> fishies;
	protected int fishCounter = 0;
	protected final ClientCommunicator.ClientForwarder forwarder;

	private InetSocketAddress leftNeighbor;
	private InetSocketAddress rightNeighbor;

	private boolean hasToken = false;
	private final Timer timer = new Timer("TokenTimer", true);

	private boolean snapshotStarted;
	private boolean snapshotComplete;
	private int lokalSnapshot;
	private int globalSnapshot;
	private boolean globalSnapshotAvailable;
	private RecordingMode recordingMode = RecordingMode.IDLE;
	private CollectSnapshotToken pendingCollectSnapshotToken;
	private final Set<FishModel> handedOffFishies = new HashSet<>();

	//private Map<String, Location> fishLocation = new HashMap<>();
	private final Map<String, InetSocketAddress> homeAgent = new HashMap<>();

	public TankModelImpl(ClientCommunicator.ClientForwarder forwarder) {
		this.fishies = Collections.newSetFromMap(new ConcurrentHashMap<FishModel, Boolean>());
		this.forwarder = forwarder;
	}

	@Override 
	public synchronized void updateNeighbor(Direction direction, InetSocketAddress address) {
		if (direction == Direction.LEFT) {
			this.leftNeighbor = address;
		} else if (direction == Direction.RIGHT) {
			this.rightNeighbor = address;
		}
	}

	@Override
	synchronized public void onRegistration(String id) {
		this.id = id;
		newFish(WIDTH - FishModel.getXSize(), rand.nextInt(HEIGHT - FishModel.getYSize()));
	}

	@Override
	public synchronized void newFish(int x, int y) {
		if (fishies.size() < MAX_FISHIES) {
			x = x > WIDTH - FishModel.getXSize() - 1 ? WIDTH - FishModel.getXSize() - 1 : x;
			y = y > HEIGHT - FishModel.getYSize() ? HEIGHT - FishModel.getYSize() : y;

			FishModel fish = new FishModel("fish" + (++fishCounter) + "@" + getId(), x, y,
					rand.nextBoolean() ? Direction.LEFT : Direction.RIGHT);

			fishies.add(fish);
			//fishLocation.put(fish.getId(), Location.HERE);
			homeAgent.put(fish.getId(), null);
		}
	}

	@Override
	public synchronized void receiveFish(FishModel fish, InetSocketAddress sender) {
		if (isRecordingFrom(sender)) {
			lokalSnapshot++;
		}

		fish.setToStart();
		fishies.add(fish);
		//fishLocation.put(fish.getId(), Location.HERE);
		if (homeAgent.containsKey(fish.getId())) {
			homeAgent.put(fish.getId(), null);
		} else {
			forwarder.sendNameResolutionRequest(new NameResolutionRequest(fish.getTankId(), fish.getId()));
		}
	}

	public String getId() {
		return id;
	}

	public synchronized int getFishCounter() {
		return fishCounter;
	}

	public synchronized Iterator<FishModel> iterator() {
		return fishies.iterator();
	}

	private synchronized void updateFishies() {
		for (Iterator<FishModel> it = iterator(); it.hasNext();) {
			FishModel fish = it.next();

			fish.update();

			if (fish.hitsEdge()) {
				if (hasToken()) {
					if (fish.getDirection() == Direction.LEFT) {
						forwarder.handOff(fish, leftNeighbor);
						//fishLocation.put(fish.getId(), Location.LEFT);
					} else {
						forwarder.handOff(fish, rightNeighbor);
						//fishLocation.put(fish.getId(), Location.RIGHT);
					}
					handedOffFishies.add(fish);
				} else {
					fish.reverse();
				}
			}

			if (fish.disappears()) {
				it.remove();
				handedOffFishies.remove(fish);
			}
		}
	}

	@Override
	public void start() {
		forwarder.register();
	}

	@Override
	public void update() {
		updateFishies();
	}

	@Override
	public synchronized void finish() {
		timer.cancel();
		hasToken = false;
		forwarder.sendToken(leftNeighbor);
		forwarder.deregister(id);
	}

	@Override
	synchronized public void receiveToken() {
		hasToken = true;

		timer.schedule(new TimerTask() {
			@Override 
			public void run() {
				synchronized (TankModelImpl.this) {
					hasToken = false;
					forwarder.sendToken(leftNeighbor);
				}
			}
    	}, 2000L);
		
	}

	@Override
	public synchronized boolean hasToken() {
		return hasToken;
	}

	@Override
	public synchronized void initiateSnapshot() {
		if (snapshotStarted) {
			return;
		}
		snapshotStarted = true;
		lokalSnapshot = countLocalSnapshotFish();
		startRecordingIncomingChannels();

		sendSnapshotMarkers();

		CollectSnapshotToken token = new CollectSnapshotToken(id, 0);

		forwarder.sendCollectSnapshotToken(token, leftNeighbor);
	}

	@Override
	public synchronized void receiveSnapshotMarker(InetSocketAddress sender) {
		if (!snapshotStarted) {
			snapshotStarted = true;
			lokalSnapshot = countLocalSnapshotFish();
			startRecordingIncomingChannels();

			sendSnapshotMarkers();
		}

		stopRecordingFrom(sender);
		if (recordingMode == RecordingMode.IDLE) {
			snapshotComplete = true;
			processPendingCollectSnapshotToken();
		}
	}

	private void startRecordingIncomingChannels() {
		boolean left = leftNeighbor != null;
		boolean right = rightNeighbor != null;

		if (left && right) {
			recordingMode = RecordingMode.BOTH;
		} else if (left) {
			recordingMode = RecordingMode.LEFT;
		} else if (right) {
			recordingMode = RecordingMode.RIGHT;
		} else {
			recordingMode = RecordingMode.IDLE;
		}

		snapshotComplete = recordingMode == RecordingMode.IDLE;
	}

	private int countLocalSnapshotFish() {
		int count = 0;
		for (FishModel fish : fishies) {
			if (!handedOffFishies.contains(fish)) {
				count++;
			}
		}
		return count;
	}

	private void sendSnapshotMarkers() {
		if (leftNeighbor != null) {
			forwarder.sendSnapshotMarker(leftNeighbor);
		}
		if (rightNeighbor != null) {
			forwarder.sendSnapshotMarker(rightNeighbor);
		}
	}

	private boolean isRecordingFrom(InetSocketAddress sender) {
		boolean fromLeft = leftNeighbor != null && leftNeighbor.equals(sender);
		boolean fromRight = rightNeighbor != null && rightNeighbor.equals(sender);

		return (fromLeft && (recordingMode == RecordingMode.LEFT
				|| recordingMode == RecordingMode.BOTH))
				|| (fromRight && (recordingMode == RecordingMode.RIGHT
				|| recordingMode == RecordingMode.BOTH));
	}

	private void stopRecordingFrom(InetSocketAddress sender) {
		boolean fromLeft = leftNeighbor != null && leftNeighbor.equals(sender);
		boolean fromRight = rightNeighbor != null && rightNeighbor.equals(sender);

		switch (recordingMode) {
			case BOTH:
				if (fromLeft) {
					recordingMode = RecordingMode.RIGHT;
				} else if (fromRight) {
					recordingMode = RecordingMode.LEFT;
				}
				break;
			case LEFT:
				if (fromLeft) {
					recordingMode = RecordingMode.IDLE;
				}
				break;
			case RIGHT:
				if (fromRight) {
					recordingMode = RecordingMode.IDLE;
				}
				break;
			case IDLE:
				break;
		}
	}

	@Override
	public synchronized void receiveCollectSnapshotToken(CollectSnapshotToken token) {
		if (!snapshotComplete) {
			pendingCollectSnapshotToken = token;
			return;
		}

		processCollectSnapshotToken(token);
	}

	private void processCollectSnapshotToken(CollectSnapshotToken token) {
		if (token.getIniId().equals(id)) {
			receiveSnapshotResult(
				token.getIniId(),
				token.getPopulation() + lokalSnapshot
			);
		} else {
			token.addPopulation(lokalSnapshot);

			if (leftNeighbor == null) {
				throw new IllegalStateException(
					"Cannot forward snapshot token without a left neighbor"
				);
			}

			forwarder.sendCollectSnapshotToken(token, leftNeighbor);
		}

		snapshotStarted = false;
		snapshotComplete = false;
		recordingMode = RecordingMode.IDLE;
		lokalSnapshot = 0;
	}

	private void processPendingCollectSnapshotToken() {
		if (pendingCollectSnapshotToken != null) {
			CollectSnapshotToken token = pendingCollectSnapshotToken;
			pendingCollectSnapshotToken = null;
			processCollectSnapshotToken(token);
		}
	}

	@Override
	public synchronized void receiveSnapshotResult(String initiator, int result) {
		globalSnapshot = result;
		globalSnapshotAvailable = true;
	}

	@Override
	public synchronized int getGlobalSnapshot() {
		return globalSnapshot;
	}

	@Override
	public synchronized boolean checkAndClearGlobalSnapshotFlag() {
		boolean available = globalSnapshotAvailable;
		globalSnapshotAvailable = false;
		return available;
	}

	/*
	@Override
	public synchronized void locateFishGlobally(String fishId) {
		assert fishLocation.containsKey(fishId);
		System.out.println("Locating fish " + fishId + " globally. Location: " + fishLocation.get(fishId));

		if (fishLocation.get(fishId) == Location.HERE) {
			locateFishLocally(fishId);
		} else if (fishLocation.get(fishId) == Location.LEFT) {
			forwarder.sendLocationRequest(new LocationRequest(fishId), leftNeighbor);
		} else if (fishLocation.get(fishId) == Location.RIGHT) {
			forwarder.sendLocationRequest(new LocationRequest(fishId), rightNeighbor);
		}

	}
	*/

	@Override
	public synchronized void locateFishGlobally(String fishId) {
		if (!homeAgent.containsKey(fishId)) {
			return;
		}

		InetSocketAddress location = homeAgent.get(fishId);
		if (location == null) {
			locateFishLocally(fishId);
		} else {
			forwarder.sendLocationRequest(new LocationRequest(fishId), location);
		}
	}

	@Override
	public synchronized void locateFishLocally(String fishId) {
		for (FishModel fish : fishies) {
			if (fish.getId().equals(fishId) && !handedOffFishies.contains(fish)) {
				fish.toggle();
				return;
			}
		}
	}
	
	@Override
	public synchronized void receiveNameResolutionResponse(String requestId, InetSocketAddress address) {
		if (address != null) {
			forwarder.sendLocationUpdate(new LocationUpdate(requestId), address);
		}
	}

	@Override
	public synchronized void receiveLocationUpdate(String fishId, InetSocketAddress sender) {
		if (homeAgent.containsKey(fishId)) {
			homeAgent.put(fishId, sender);
		}
	}
}