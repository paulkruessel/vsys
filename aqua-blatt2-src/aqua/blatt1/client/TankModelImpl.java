package aqua.blatt1.client;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Iterator;
import java.util.Observable;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.msgtypes.SnapshotToken;
import aqua.blatt4.client.RecordingMode;

public class TankModelImpl implements TankModel {

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

	// Blatt 4
	private int localState;
	private RecordingMode recordingMode = RecordingMode.IDLE;
	private boolean isInitiator = false;
	private boolean localSnapshotTaken = false;
	private boolean localSnapshotDone = false;
	private SnapshotToken snapshotToken;

	private boolean globalSnapshotFlag = false;
	private int globalSnapshot;

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
		}
	}

	@Override
	synchronized public void receiveFish(FishModel fish) {
		fish.setToStart();
		fishies.add(fish);

		if (localSnapshotTaken && !localSnapshotDone) {
			RecordingMode from = (fish.getDirection() == Direction.RIGHT)
				? RecordingMode.LEFT
				: RecordingMode.RIGHT;

			if (recordingMode == RecordingMode.BOTH || recordingMode == from) {
				localState++;
			}
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
					} else {
						forwarder.handOff(fish, rightNeighbor);
					}
				} else {
					fish.reverse();
				}
			}

			if (fish.disappears())
				it.remove();
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
		if (localSnapshotTaken) return;

		localState = (int) fishies.stream().filter(f -> !f.isDeparting()).count();
		isInitiator = true;
		localSnapshotTaken = true;
		recordingMode = RecordingMode.BOTH;

		forwarder.sendSnapshotMarker(leftNeighbor);
		forwarder.sendSnapshotMarker(rightNeighbor);
	}

	@Override
	public synchronized void receiveSnapshotMarker(InetSocketAddress sender) {

		RecordingMode channel;
		RecordingMode otherChannel;
		if (sender.equals(leftNeighbor)) {
			channel = RecordingMode.LEFT;
			otherChannel = RecordingMode.RIGHT;
		} else {
			channel = RecordingMode.RIGHT;
			otherChannel = RecordingMode.LEFT;
		}

		if (!localSnapshotTaken) {
			localSnapshotTaken = true;
			localState = (int) fishies.stream().filter(f -> !f.isDeparting()).count();

			recordingMode = otherChannel;
			forwarder.sendSnapshotMarker(leftNeighbor);
			forwarder.sendSnapshotMarker(rightNeighbor);
		} else if (recordingMode == RecordingMode.BOTH) {
			recordingMode = otherChannel;
		} else if (recordingMode == channel || leftNeighbor.equals(rightNeighbor) ) {
			recordingMode = RecordingMode.IDLE;
		}

		if (!localSnapshotDone && recordingMode == RecordingMode.IDLE) {
			localSnapshotDone = true;

			if (isInitiator) {
				forwarder.sendSnapshotToken(leftNeighbor, new SnapshotToken(this.id, localState));
			} else if (this.snapshotToken != null) {
				this.snapshotToken.setSum(this.snapshotToken.getSum() + localState);
				forwarder.sendSnapshotToken(leftNeighbor, this.snapshotToken);
				resetSnapshot();
			}
		}
	}

	@Override
	public synchronized void receiveSnapshotToken(SnapshotToken token) {
		if (token.getInitiatorId().equals(id)) {
			globalSnapshot = token.getSum();
			globalSnapshotFlag = true;
			resetSnapshot();
			return;
		}

		if (localSnapshotDone) {
			token.setSum(token.getSum() + localState);
			forwarder.sendSnapshotToken(leftNeighbor, token);
			resetSnapshot();
		} else {
			snapshotToken = token;
		}
	}

	@Override 
	public synchronized int getGlobalSnapshot() {
		return globalSnapshot;
	}

	@Override
	public synchronized boolean checkAndClearGlobalSnapshotFlag() {
        boolean flag = globalSnapshotFlag;
		globalSnapshotFlag = false;
		return flag;
    }

	private void resetSnapshot() {
		localState = 0;
		recordingMode = RecordingMode.IDLE;
		isInitiator = false;
		localSnapshotTaken = false;
		localSnapshotDone = false;
		snapshotToken = null;
	}

}