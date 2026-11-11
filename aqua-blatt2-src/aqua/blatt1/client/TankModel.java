package aqua.blatt1.client;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
//import aqua.blatt4.client.RecordingMode;
import aqua.blatt1.common.msgtypes.CollectSnapshotToken;

import java.net.InetSocketAddress;

public interface TankModel extends Iterable<FishModel> {
    int WIDTH = 600;
    int HEIGHT = 350;

    String getId();

    int getFishCounter();

    void newFish(int x, int y);

    default void onRegistration(String id) {
        throw new RuntimeException("Not implemented yet");
    }

    default void onRegistration(String id, long lease) {
        throw new RuntimeException("Not implemented yet");
    }

    void receiveFish(FishModel fish, InetSocketAddress sender);

    void updateNeighbor(Direction direction, InetSocketAddress address);

    void receiveToken();

    boolean hasToken();

    void initiateSnapshot();   

    void receiveSnapshotMarker(InetSocketAddress sender);

    void receiveCollectSnapshotToken(CollectSnapshotToken token);

    default void receiveSnapshotResult(final String initiator, final int result) {
        throw new RuntimeException("Not implemented yet");
    }

    default int getGlobalSnapshot() {
        throw new RuntimeException("Not implemented yet");
    }

    default boolean checkAndClearGlobalSnapshotFlag() {
        throw new RuntimeException("Not implemented yet");
    }

    void locateFishGlobally(String fishId);

    void locateFishLocally(String fishId);


    void receiveNameResolutionResponse(String requestId, InetSocketAddress address);

    void receiveLocationUpdate(String fishId, InetSocketAddress sender);

    void start();

    void update();

    void finish();
}
