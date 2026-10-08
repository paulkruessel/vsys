package aqua.blatt1.broker;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.HandoffRequest;
import aqua.blatt1.common.msgtypes.RegisterRequest;
import aqua.blatt1.common.msgtypes.RegisterResponse;
import messaging.Endpoint;
import messaging.Message;

public class BrokerTask {

    private final ClientCollection<InetSocketAddress> clients;
    private final Endpoint endpoint;
    private final ReadWriteLock clientLock;
    private final AtomicInteger currentIndex;

    public BrokerTask(ClientCollection<InetSocketAddress> clients, Endpoint endpoint, ReadWriteLock clientLock, AtomicInteger currentIndex) {
        this.clients = clients;
        this.endpoint = endpoint;
        this.clientLock = clientLock;
        this.currentIndex = currentIndex;
    }
    
    public void workRequest(Message message) {
        Serializable request = message.getPayload();
            if (request instanceof RegisterRequest) {
                this.register(message);
            }
            
            if (request instanceof DeregisterRequest deregisterRequest) {
                this.deregister(deregisterRequest);
            }

            if (request instanceof HandoffRequest handoffRequest) {
                this.handoffFish(handoffRequest, message);
            }
    }

    private String register(Message message) {
        String clientId = "tank" + currentIndex.addAndGet(1);
        clientLock.writeLock().lock();
        try {
            clients.add(clientId, message.getSender());
        } finally {
            clientLock.writeLock().unlock();
        }

        endpoint.send(message.getSender(), new RegisterResponse(clientId));
        System.out.println("Registered new client with id " + clientId);
        return clientId;
    }

    private ClientCollection<InetSocketAddress> deregister(DeregisterRequest request) {
        String clientId = request.getId();
        
        clientLock.writeLock().lock();
        try {
            int index = clients.indexOf(clientId);
            if (index == -1) {
                throw new NoSuchElementException("Could not find Element for ID " + clientId);
            }

            System.out.println("Client deregistered with id " + clientId);
            return clients.remove(index);
        } finally {
            clientLock.writeLock().unlock();
        }
    }

    private void handoffFish(HandoffRequest request, Message message) {
        Direction fishDirection = request.getFish().getDirection();
        InetSocketAddress targetClient;

        
        clientLock.readLock().lock();
        try {
            Integer clientIndex = getIndexOfClient(message.getSender());
            if (clientIndex == null) {
                throw new NoSuchElementException("Could not find client index");
            }
            
            if (fishDirection == Direction.LEFT) {
                targetClient = clients.getLeftNeighorOf(clientIndex);
            } else {
                targetClient = clients.getRightNeighorOf(clientIndex);
            }
        } finally {
            clientLock.readLock().unlock();
        }

        endpoint.send(targetClient, request);
    }

    private Integer getIndexOfClient(InetSocketAddress client) {
        for (int i = 0; i < clients.size(); i++) {
            if (clients.getClient(i).equals(client))
                return i;
        }
        return null;
    }



}
