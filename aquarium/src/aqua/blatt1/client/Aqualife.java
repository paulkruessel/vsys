package aqua.blatt1.client;

import java.util.concurrent.TimeUnit;

public class Aqualife {

	public static void main(String[] args) {
		ClientCommunicator communicator = new ClientCommunicator();
		TankModel model = new TankModelImpl(communicator.newClientForwarder());
		communicator.newClientReceiver(model).start();
		AquaGui gui = new AquaGui(model);
		model.start();

		try {
			while (!Thread.currentThread().isInterrupted()) {
				model.update();
				gui.update();
				TimeUnit.MILLISECONDS.sleep(10);
			}
		} catch (InterruptedException consumed) {
			// allow method to terminate
		}
	}
}
