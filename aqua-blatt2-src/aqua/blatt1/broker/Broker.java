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
import aqua.blatt1.common.msgtypes.RegisterRequest;
import aqua.blatt1.common.msgtypes.RegisterResponse;

import messaging.Endpoint;
import messaging.Message;
import aqua.blatt1.common.Direction;

public class Broker {
    private ClientCollection<InetSocketAddress> clients;
    private final Endpoint ENDPOINT = new Endpoint(4711);
    private int idCounter = 0;

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
            // System.exit(0);
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
        InetSocketAddress addr = msg.getSender();
        String id = "Tank" + idCounter++;

        writeData(id, addr);

        RegisterResponse regRes = new RegisterResponse(id);
        ENDPOINT.send(addr, regRes);
    }

    private void deregister(Message msg) {
        DeregisterRequest req = (DeregisterRequest) msg.getPayload();
        int index = clients.indexOf(req.getId());
        deleteData(index);
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
        handoffFish(msg);
        rwLock.readLock().unlock();
    }

    private void writeData(String id, InetSocketAddress addr) {
        rwLock.writeLock().lock();
        clients.add(id, addr);
        rwLock.writeLock().unlock();
    }

    private void deleteData(int index) {
        rwLock.writeLock().lock();
        clients.remove(index);
        rwLock.writeLock().unlock();
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
