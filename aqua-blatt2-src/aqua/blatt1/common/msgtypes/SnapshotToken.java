package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

public final class SnapshotToken implements Serializable {
    private String initiatorId;
    private int sum;

    public SnapshotToken(String initiatorId, int sum) {
        this.initiatorId = initiatorId;
        this.sum = sum;
    }

    public String getInitiatorId() {
        return this.initiatorId;
    }

    public int getSum() {
        return this.sum;
    }

    public void setInitiatorId(String id) {
        this.initiatorId = id;
    }

    public void setSum(int sum) {
        this.sum = sum;
    }
}
