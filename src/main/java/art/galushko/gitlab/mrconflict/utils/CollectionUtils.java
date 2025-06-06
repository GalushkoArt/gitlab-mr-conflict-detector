package art.galushko.gitlab.mrconflict.utils;

import java.util.List;
import java.util.function.Function;

public class CollectionUtils {
    public static <T, R> Function<List<T>, List<R>> mapCollection(Function<T, R> mapper) {
        return input -> input.stream().map(mapper).toList();
    }
}
