package nobody.utils;

import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BeanCopyUtils {

    private BeanCopyUtils() {
    }

    public static <V> V copyBean(Object source, Class<V> clazz) {
        if (source == null || clazz == null) {
            return null;
        }
        try {
            V target = clazz.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target);
            return target;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to copy bean to " + clazz.getName(), e);
        }
    }

    public static <O, V> List<V> copyBean(List<O> list, Class<V> clazz) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        List<V> result = new ArrayList<>(list.size());
        for (O item : list) {
            V copied = copyBean(item, clazz);
            if (copied != null) {
                result.add(copied);
            }
        }
        return result;
    }
}
