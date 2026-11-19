package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

@SuppressWarnings("serial")
public final class NameResolutionRequest implements Serializable {

    private String targetId;
    private String requestId;

    public NameResolutionRequest(String targetId, String requestId) {
        this.targetId = targetId;
        this.requestId = requestId;
    }

    public String getTargetId() {
        return this.targetId;
    }

    public String getRequestId() {
        return this.requestId;
    }
}
