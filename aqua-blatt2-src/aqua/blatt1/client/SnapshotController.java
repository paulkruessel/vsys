package aqua.blatt1.client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SnapshotController implements ActionListener {

    private TankModel tank;

    public SnapshotController(TankModel tank) {
        this.tank = tank;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        tank.initiateSnapshot();
    }
    
}
