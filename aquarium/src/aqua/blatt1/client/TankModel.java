package aqua.blatt1.client;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;

import java.net.InetSocketAddress;

public interface TankModel extends Iterable<FishModel> {
    int WIDTH = 600;
    int HEIGHT = 350;

    String getId();
    int getFishCounter();
    void newFish(int x, int y);
    default void onRegistration(String id) { throw new RuntimeException("Not implemented yet");}
    default void onRegistration(String id, long lease) { throw new RuntimeException("Not implemented yet");}
    void receiveFish(FishModel fish);
    default void updateNeighbor(Direction direction, InetSocketAddress address) {
        throw new RuntimeException("Not implemented yet");
    }
    default void receiveToken() { throw new RuntimeException("Not implemented yet");}
    default boolean hasToken() { throw new RuntimeException("Not implemented yet"); }
    default void initiateSnapshot() { throw new RuntimeException("Not implemented yet"); }
    default void receiveSnapshotMarker(InetSocketAddress sender) {
        throw new RuntimeException("Not implemented yet");
    }
    default void receiveSnapshotResult(final String initiator, final int result) {
        throw new RuntimeException("Not implemented yet");
    }

    default int getGlobalSnapshot() { throw new RuntimeException("Not implemented yet"); }
    default boolean checkAndClearGlobalSnapshotFlag() {
        throw new RuntimeException("Not implemented yet");
    }
    default void locateFishGlobally(String fishId) { throw new RuntimeException("Not implemented yet"); }
    default void receiveNameResolutionResponse(String requestId, InetSocketAddress address) {
        throw new RuntimeException("Not implemented yet");
    }
    default void receiveLocationUpdate(String fishId, InetSocketAddress sender) {
        throw new RuntimeException("Not implemented yet");
    }

    void start();
    void update();
    void finish();
}

