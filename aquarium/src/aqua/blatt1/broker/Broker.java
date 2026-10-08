package aqua.blatt1.broker;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.swing.JOptionPane;

import aqua.blatt1.common.Properties;
import aqua.blatt2.broker.PoisonPill;
import messaging.Endpoint;
import messaging.Message;

public class Broker {

    private final Endpoint endpoint;
    private final ClientCollection<InetSocketAddress> clients;
    private final AtomicInteger currentIndex = new AtomicInteger(0);

    private ExecutorService executor = Executors.newFixedThreadPool(3 + 1); // 3 Threads to work + 1 to display dialog
    private ReadWriteLock clientLock = new ReentrantReadWriteLock();

    public Broker(Endpoint endpoint, ClientCollection<InetSocketAddress> clients) {
        this.endpoint = endpoint;
        this.clients = clients;
    }

    private void broker() {
        Future<?> dialogFuture = executor.submit(() -> JOptionPane.showMessageDialog(null, "Press OK to Stop the Server!"));
        try {
            while (true) {
                if (dialogFuture.isDone()) {
                    System.out.println("Pressed the shutdown button! Shutting down...");
                    break;
                }
                Message message = endpoint.nonBlockingReceive();
                if (message != null) {
                    if (message.getPayload() instanceof PoisonPill) {
                        System.out.println("Received Poison Pill. Shutting down...");
                        break;
                    }
                    BrokerTask task = new BrokerTask(clients, endpoint, clientLock, currentIndex);
                    executor.submit(() -> task.workRequest(message));
                } else {
                    Thread.sleep(10);
                }
            }
        } catch (InterruptedException e) {
            System.out.println(e.toString());
        } finally {
            System.out.println("Shutting down executor...");
            dialogFuture.cancel(true);
            executor.shutdown();
        }
    }

    public static void main(String[] args) {
        Broker broker = new Broker(new Endpoint(Properties.PORT), new ClientCollection<>());
        broker.broker();
    }

}
