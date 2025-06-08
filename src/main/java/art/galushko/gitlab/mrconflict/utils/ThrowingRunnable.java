package art.galushko.gitlab.mrconflict.utils;

@FunctionalInterface
public interface ThrowingRunnable {
    void run() throws Exception;
}
