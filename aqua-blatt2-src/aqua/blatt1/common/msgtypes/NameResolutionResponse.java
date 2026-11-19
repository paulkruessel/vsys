package aqua.blatt1.common.msgtypes;

import java.io.Serializable;
import java.net.InetSocketAddress;

@SuppressWarnings("serial")
public final class NameResolutionResponse implements Serializable {

    private InetSocketAddress targetAddress;
    private String requestId;

    public NameResolutionResponse(InetSocketAddress targetAddress, String requestId) {
        this.targetAddress = targetAddress;
        this.requestId = requestId;
    }

    public InetSocketAddress getTargetAddress() {
        return this.targetAddress;
    }

    public String getRequestId() {
        return this.requestId;
    }
}
