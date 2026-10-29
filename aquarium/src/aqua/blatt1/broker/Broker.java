package aqua.blatt1.broker;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.NoSuchElementException;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.Properties;
import aqua.blatt1.common.msgtypes.DeregisterRequest;
import aqua.blatt1.common.msgtypes.HandoffRequest;
import aqua.blatt1.common.msgtypes.RegisterRequest;
import aqua.blatt1.common.msgtypes.RegisterResponse;
import aqua.blatt1.common.msgtypes.NeighbourUpdate;
import messaging.Endpoint;
import messaging.Message;

public class Broker {

    private final Endpoint endpoint;
    private final ClientCollection<InetSocketAddress> clients;
    private int nextClientId = 0;

    public Broker(Endpoint endpoint, ClientCollection<InetSocketAddress> clients) {
        this.endpoint = endpoint;
        this.clients = clients;
    }

    private void broker() {
        while (true) {
            Message message = endpoint.blockingReceive();
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
    }

    private String register(Message message) {
        String clientId = "tank" + nextClientId++;
        InetSocketAddress clientAddress = message.getSender();

        clients.add(clientId, clientAddress);

        endpoint.send(clientAddress, new RegisterResponse(clientId));
        System.out.println("Registered new client with id " + clientId);

        InetSocketAddress leftClientAddress = clients.getLeftNeighorOf(clients.size() - 1);
        InetSocketAddress rightClientAddress = clients.getRightNeighorOf(clients.size() - 1);

        endpoint.send(clientAddress, new NeighbourUpdate(leftClientAddress, Direction.LEFT));
        endpoint.send(clientAddress, new NeighbourUpdate(rightClientAddress, Direction.RIGHT));
        endpoint.send(leftClientAddress, new NeighbourUpdate(clientAddress, Direction.RIGHT));
        endpoint.send(rightClientAddress, new NeighbourUpdate(clientAddress, Direction.LEFT));
        
        return clientId;
    }

    private ClientCollection<InetSocketAddress> deregister(DeregisterRequest request) {
        String clientId = request.getId();
        int index = clients.indexOf(clientId);
        if (index == -1) {
            System.out.println("Unknown client id " + clientId);
            return clients;
        }

        System.out.println("Client deregistered with id " + clientId);

        InetSocketAddress leftClientAddress = clients.getLeftNeighorOf(index);
        InetSocketAddress rightClientAddress = clients.getRightNeighorOf(index);

        endpoint.send(leftClientAddress, new NeighbourUpdate(rightClientAddress, Direction.RIGHT));
        endpoint.send(rightClientAddress, new NeighbourUpdate(leftClientAddress , Direction.LEFT));

        return clients.remove(index);
    }

    private void handoffFish(HandoffRequest request, Message message) {
        Direction fishDirection = request.getFish().getDirection();
        InetSocketAddress targetClient;

        Integer clientIndex = getIndexOfClient(message.getSender());
        if (clientIndex == null) {
            throw new NoSuchElementException("Could not find client index");
        }

        if (fishDirection == Direction.LEFT) {
            targetClient = clients.getLeftNeighorOf(clientIndex);
        } else {
            targetClient = clients.getRightNeighorOf(clientIndex);
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

    public static void main(String[] args) {
        Broker broker = new Broker(new Endpoint(Properties.PORT), new ClientCollection<>());
        broker.broker();
    }

}
