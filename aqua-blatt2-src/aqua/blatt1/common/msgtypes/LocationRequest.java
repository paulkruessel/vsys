package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

@SuppressWarnings("serial")
public class LocationRequest implements Serializable {
    final String fishId;

    public LocationRequest(String fishId) {
        this.fishId = fishId;
    }

    public String getFishId() {
        return fishId;
    }
}
