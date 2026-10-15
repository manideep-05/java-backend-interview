package without;

import java.util.ArrayList;
import java.util.List;

/**
 * MEDIATOR NOT USED — the problem.
 *
 * Each User keeps direct references to every OTHER user to broadcast.
 * This is the N-to-N tangle: adding or removing a user means updating
 * the peer list of everyone, and the broadcast logic is duplicated in
 * each User.
 */
public class Main {

    static class User {
        private final String name;
        private final List<User> peers = new ArrayList<>(); // direct references to others

        User(String name) { this.name = name; }

        void addPeer(User peer) { peers.add(peer); }

        void send(String message) {
            for (User peer : peers) {
                peer.receive(name, message);
            }
        }

        void receive(String from, String message) {
            System.out.println("[" + name + "] " + from + ": " + message);
        }
    }

    public static void main(String[] args) {
        User alice = new User("Alice");
        User bob = new User("Bob");
        User carol = new User("Carol");

        // Everyone must be wired to everyone else — N*N connections.
        alice.addPeer(bob); alice.addPeer(carol);
        bob.addPeer(alice); bob.addPeer(carol);
        carol.addPeer(alice); carol.addPeer(bob);

        alice.send("Hello everyone");
    }
}
