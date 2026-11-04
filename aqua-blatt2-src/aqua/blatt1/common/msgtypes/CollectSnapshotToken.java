package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

@SuppressWarnings("serial")
public class CollectSnapshotToken implements Serializable {
    private final String iniId;
    private int population = 0;

    public CollectSnapshotToken(String iniId, int population) {
        this.iniId = iniId;
        this.population = population;
    }

    public String getIniId() {
        return iniId;
    }

    public int getPopulation() {
        return population;
    }

    public void addPopulation(int population) {
        this.population += population;
    }
}
