package aqua.blatt1.client;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import aqua.blatt1.common.FishModel;

@SuppressWarnings("serial")
public class TankView extends JPanel {
	private final TankModel tankModel;
	private final FishView fishView;

	public TankView(final TankModel tankModel) {
		this.tankModel = tankModel;
		fishView = new FishView();

		setPreferredSize(new Dimension(TankModel.WIDTH, TankModel.HEIGHT));
		setBackground(new Color(175, 200, 235));

		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				tankModel.newFish(e.getX(), e.getY());
			}
		});
	}

	private void drawBorders(Graphics2D g2d) {
		// dick und rot, sonst verschwindet die 1px-Linie im Fensterrahmen
		g2d.setColor(Color.RED);
		g2d.setStroke(new BasicStroke(6));
		g2d.drawLine(0, 0, 0, TankModelImpl.HEIGHT);
		g2d.drawLine(TankModel.WIDTH - 1, 0, TankModel.WIDTH - 1, TankModel.HEIGHT);
	}

	private void doDrawing(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;

		for (FishModel fishModel : tankModel) {
			g2d.drawImage(fishView.getImage(fishModel), fishModel.getX(), fishModel.getY(), null);
			g2d.drawString(fishModel.getId(), fishModel.getX(), fishModel.getY());

		}
		
		if (!tankModel.hasToken()) {
			drawBorders(g2d);
		}

	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		doDrawing(g);
	}

}
