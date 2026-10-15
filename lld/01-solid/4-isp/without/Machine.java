package without;

/**
 * ISP VIOLATION — a "fat" interface.
 *
 * It bundles three unrelated capabilities. Any implementer is forced
 * to provide ALL THREE, even if the device only prints.
 */
public interface Machine {
    void print(String document);
    void scan(String document);
    void fax(String document);
}
