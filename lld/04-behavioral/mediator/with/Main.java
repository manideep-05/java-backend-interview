package with;

/**
 * Users register with the room and send through it. No user holds a
 * reference to another, so the N*N tangle is gone — the room is the one
 * place that knows the membership.
 */
public class Main {
    public static void main(String[] args) {
        ChatMediator room = new ChatRoom();

        User alice = new User("Alice", room);
        User bob = new User("Bob", room);
        User carol = new User("Carol", room);

        room.register(alice);
        room.register(bob);
        room.register(carol);

        alice.send("Hello everyone"); // Bob and Carol receive it
    }
}
