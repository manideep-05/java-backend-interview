package with;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * ITERATOR APPLIED.
 *
 * BookShelf implements Iterable<String> and returns an Iterator. The
 * internal storage (here an ArrayList) is completely hidden — we could
 * switch it to a tree or array and clients wouldn't change at all.
 */
public class BookShelf implements Iterable<String> {

    // Internal representation is private and could be anything.
    private final List<String> books = new ArrayList<>();

    public void add(String book) {
        books.add(book);
    }

    @Override
    public Iterator<String> iterator() {
        // Return an iterator that walks our storage. Clients never see
        // how storage is organized.
        return new Iterator<>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < books.size();
            }

            @Override
            public String next() {
                return books.get(index++);
            }
        };
    }

    public static void main(String[] args) {
        BookShelf shelf = new BookShelf();
        shelf.add("DDD");
        shelf.add("Clean Code");

        // Clients traverse uniformly. Because it's Iterable, the
        // enhanced for-loop works too.
        for (String book : shelf) {
            System.out.println(book);
        }
    }
}
