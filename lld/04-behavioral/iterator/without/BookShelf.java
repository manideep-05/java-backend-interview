package without;

/**
 * ITERATOR NOT USED — the problem.
 *
 * The shelf leaks its internal storage: clients must know it's a raw
 * array and loop by index (getCount/getAt). If we later switch to a
 * List or a tree, every client loop breaks.
 */
public class BookShelf {

    private final String[] books;
    private int count = 0;

    public BookShelf(int capacity) {
        this.books = new String[capacity];
    }

    public void add(String book) {
        books[count++] = book;
    }

    // Internal representation leaked to clients:
    public int getCount() {
        return count;
    }

    public String getAt(int index) {
        return books[index];
    }

    public static void main(String[] args) {
        BookShelf shelf = new BookShelf(3);
        shelf.add("DDD");
        shelf.add("Clean Code");

        // Client is coupled to the array/index representation.
        for (int i = 0; i < shelf.getCount(); i++) {
            System.out.println(shelf.getAt(i));
        }
    }
}
