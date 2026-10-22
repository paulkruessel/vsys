package aqua.blatt1.client;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

@SuppressWarnings("serial")
public class AquaGui extends JFrame {
	private final List<JMenuItem> fishMenuItems = Collections
			.synchronizedList(new ArrayList<JMenuItem>());

	private final TankModel tankModel;
	private final TankView tankView;

	private final JMenu searchMenu;

	public AquaGui(final TankModel tankModel) {
		this.tankModel = tankModel;
		tankView = new TankView(tankModel);
		add(tankView);

		pack();

		setLocationRelativeTo(null);
		setResizable(false);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent e) {
				tankModel.finish();
				System.exit(0);
			}
		});

		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		JMenu toolsMenu = new JMenu("Tools");
		menuBar.add(toolsMenu);

		JMenuItem gsMenuItem = new JMenuItem("Global Snapshot");
		toolsMenu.add(gsMenuItem);

		gsMenuItem.addActionListener(new NotImplementedYetController(this));
		setVisible(true);

		searchMenu = new JMenu("Toggle Fish Color...");
		toolsMenu.add(searchMenu);
	}

	public void update() {
		tankView.repaint();
		setTitle(tankModel.getId());

		int size = fishMenuItems.size();
		while (tankModel.getFishCounter() > size) {
			String fishId = "fish" + (++size) + "@" + tankModel.getId();
			JMenuItem fishMenuItem = new JMenuItem(fishId);
			fishMenuItem.addActionListener(new NotImplementedYetController(this));
			fishMenuItems.add(fishMenuItem);
			searchMenu.add(fishMenuItem);
		}
	}
}
