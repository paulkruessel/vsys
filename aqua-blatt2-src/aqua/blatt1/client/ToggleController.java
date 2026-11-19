package aqua.blatt1.client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ToggleController implements ActionListener {

    private final TankModel tank;
    private final String fishId;

    public ToggleController(TankModel tank, String fishId) {
        this.tank = tank;
        this.fishId = fishId;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        tank.locateFishGlobally(fishId);
    }
    
}
