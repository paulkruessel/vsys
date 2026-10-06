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
import messaging.Endpoint;
import messaging.Message;

public class Broker {

    private final Endpoint endpoint;
    private final ClientCollection<InetSocketAddress> clients;

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
        String clientId = "tank" + clients.size();
        clients.add(clientId, message.getSender());
        endpoint.send(message.getSender(), new RegisterResponse(clientId));
        System.out.println("Registered new client with id " + clientId);
        return clientId;
    }

    private ClientCollection<InetSocketAddress> deregister(DeregisterRequest request) {
        String clientId = request.getId();
        int index = Integer.parseInt(clientId.replace("tank", ""));
        System.out.println("Client deregistered with id " + clientId);
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
