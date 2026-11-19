package aqua.blatt1.broker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * This class is not thread-safe and hence must be used in a thread-safe way, e.g. thread confined or 
 * externally synchronized. 
 */

public class ClientCollection<T> {
	private class Client {
		final String id;
		final T client;

		Client(String id, T client) {
			this.id = id;
			this.client = client;
		}
	}

	private final List<Client> clients;
	private final Map<String, Client> clientMap;

	public ClientCollection() {
		clients = new ArrayList<Client>();
		clientMap = new HashMap<>();
	}

	public ClientCollection<T> add(String id, T client) {
		clients.add(new Client(id, client));
		clientMap.put(id, new Client(id, client));
		return this;
	}

	public ClientCollection<T> remove(int index) {
		Client clientToRemove = clients.remove(index);
		clientMap.remove(clientToRemove.id);
		return this;
	}

	public int indexOf(String id) {
		for (int i = 0; i < clients.size(); i++)
			if (clients.get(i).id.equals(id))
				return i;
		return -1;
	}

	public int indexOf(T client) {
		for (int i = 0; i < clients.size(); i++)
			if (clients.get(i).client.equals(client))
				return i;
		return -1;
	}

	public T getClientByIndex(int index) {
		return clients.get(index).client;
	}

	public T getClientById(String id) {
		return clientMap.get(id).client;
	}

	public int size() {
		return clients.size();
	}

	public T getLeftNeighorOf(int index) {
		return index == 0 ? clients.get(clients.size() - 1).client : clients.get(index - 1).client;
	}

	public T getRightNeighorOf(int index) {
		return index < clients.size() - 1 ? clients.get(index + 1).client : clients.get(0).client;
	}

}
