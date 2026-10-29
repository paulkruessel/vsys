package aqua.blatt1.broker;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.swing.JOptionPane;

import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.HandoffRequest;
import aqua.blatt1.common.msgtypes.NeighbourUpdate;
import aqua.blatt1.common.msgtypes.RegisterRequest;
import aqua.blatt1.common.msgtypes.RegisterResponse;
import aqua.blatt1.common.msgtypes.Token;
import messaging.Endpoint;
import messaging.Message;
import aqua.blatt1.common.Direction;

public class Broker {
    private ClientCollection<InetSocketAddress> clients;
    private final Endpoint ENDPOINT = new Endpoint(4711);
    private int idCounter = 0;
    private boolean isFirstClient = true;

    private boolean stopRequested = false;
    private ReadWriteLock rwLock = new ReentrantReadWriteLock();

    private Thread stopThread;

    public Broker() {
        clients = new ClientCollection<InetSocketAddress>();

        stopThread = new Thread(() -> {
            try {
                JOptionPane.showMessageDialog(null, "Press OK button to stop server!");
            } catch (Exception e) {
                e.printStackTrace();
            }
            stopRequested = true;
            System.exit(0);
        });

        stopThread.start();
    }

    public void broker() {
        ExecutorService executor = Executors.newFixedThreadPool(4);

        while (!stopRequested) {
            Message msg = ENDPOINT.blockingReceive();
            BrokerTask bt = new BrokerTask(msg);
            executor.execute(bt);
        }
        executor.shutdown();
    }

    private void register(Message msg) {
        rwLock.writeLock().lock();
        try {
            InetSocketAddress addr = msg.getSender();
            String id = "Tank" + idCounter++;

            clients.add(id, addr);

            RegisterResponse regRes = new RegisterResponse(id);
            ENDPOINT.send(addr, regRes);

            // Send new neighbor update to the clients
            int newClientIndex = clients.indexOf(id);
            InetSocketAddress leftNeighbour = clients.getLeftNeighorOf(newClientIndex);
            InetSocketAddress rightNeighbour = clients.getRightNeighorOf(newClientIndex);
            
            //an den neuen
            ENDPOINT.send(addr, new NeighbourUpdate(rightNeighbour, Direction.RIGHT));
            ENDPOINT.send(addr, new NeighbourUpdate(leftNeighbour, Direction.LEFT));

            //an den linken Nachbarn
            ENDPOINT.send(leftNeighbour, new NeighbourUpdate(addr, Direction.RIGHT));
            // Unnötig? Der Linke Nachbarn vom linken Nachbarn ändert sich ja nicht.
            ENDPOINT.send(leftNeighbour, new NeighbourUpdate(clients.getLeftNeighorOf(clients.indexOf(leftNeighbour)), Direction.LEFT));  

            //an den rechten Nachbarn
            // Hier genauso?
            ENDPOINT.send(rightNeighbour, new NeighbourUpdate(clients.getRightNeighorOf(clients.indexOf(rightNeighbour)), Direction.RIGHT));
            ENDPOINT.send(rightNeighbour, new NeighbourUpdate(addr, Direction.LEFT));

            if (isFirstClient) {
                isFirstClient = false;
                ENDPOINT.send(addr, new Token());
            }
        } finally {
            rwLock.writeLock().unlock();
        } 
    }

    private void deregister(Message msg) {
        rwLock.writeLock().lock();
        try {
            DeregisterRequest req = (DeregisterRequest) msg.getPayload();
            int index = clients.indexOf(req.getId());

            InetSocketAddress leftNeighbour = clients.getLeftNeighorOf(index);
            InetSocketAddress rightNeighbour = clients.getRightNeighorOf(index);

            clients.remove(index);

            if (clients.size() == 0) {
                isFirstClient = true;
                return;
            }

            //an den rechten Nachbarn
            ENDPOINT.send(rightNeighbour, new NeighbourUpdate(leftNeighbour, Direction.LEFT));
            ENDPOINT.send(rightNeighbour, new NeighbourUpdate(clients.getRightNeighorOf(clients.indexOf(rightNeighbour)), Direction.RIGHT));

            //an den linken Nachbarn
            ENDPOINT.send(leftNeighbour, new NeighbourUpdate(clients.getLeftNeighorOf(clients.indexOf(leftNeighbour)), Direction.LEFT));
            ENDPOINT.send(leftNeighbour, new NeighbourUpdate(rightNeighbour, Direction.RIGHT));
        } finally {
            rwLock.writeLock().unlock();
        } 
    }

    private void handoffFish(Message msg) {
        HandoffRequest req = (HandoffRequest) msg.getPayload();
        FishModel fish = req.getFish();
        Direction dir = fish.getDirection();
        InetSocketAddress addr = msg.getSender();
        int index = clients.indexOf(addr);

        if (dir == Direction.LEFT) {
            int leftIndex = (index == 0) ? clients.size() - 1 : index - 1;
            InetSocketAddress leftAddr = (InetSocketAddress) clients.getClient(leftIndex);
            ENDPOINT.send(leftAddr, req);
        } else {
            int rightIndex = (index == clients.size() - 1) ? 0 : index + 1;
            InetSocketAddress rightAddr = (InetSocketAddress) clients.getClient(rightIndex);
            ENDPOINT.send(rightAddr, req);
        }
    }

    private void readAndUseData(Message msg) {
        rwLock.readLock().lock();
        try {
            handoffFish(msg);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    private class BrokerTask implements Runnable {
        private final Message msg;

        public BrokerTask(Message msg) {
            this.msg = msg;
        }

        public void run() {
            if (msg.getPayload() instanceof RegisterRequest) {
                register(msg);
            }
            if (msg.getPayload() instanceof DeregisterRequest) {
                deregister(msg);
            }
            if (msg.getPayload() instanceof HandoffRequest) {
                readAndUseData(msg);
            }
            if (msg.getPayload() instanceof PoisonPill) {
                System.exit(0);
            }
        }
    }

}
