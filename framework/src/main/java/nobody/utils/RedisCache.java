package nobody.utils;

import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class RedisCache {

    private final RedisTemplate<Object, Object> redisTemplate;

    public RedisCache(RedisTemplate<Object, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public <T> void setCacheObject(final String key, final T value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public <T> void setCacheObject(final String key, final T value, final Integer timeout, final TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

    public boolean expire(final String key, final long timeout) {
        return expire(key, timeout, TimeUnit.SECONDS);
    }

    public boolean expire(final String key, final long timeout, final TimeUnit unit) {
        Boolean result = redisTemplate.expire(key, timeout, unit);
        return Boolean.TRUE.equals(result);
    }

    @SuppressWarnings("unchecked")
    public <T> T getCacheObject(final String key) {
        ValueOperations<Object, Object> operation = redisTemplate.opsForValue();
        return (T) operation.get(key);
    }

    public boolean deleteObject(final String key) {
        Boolean result = redisTemplate.delete(key);
        return Boolean.TRUE.equals(result);
    }

    @SuppressWarnings("unchecked")
    public long deleteObject(final Collection<String> collection) {
        Long result = redisTemplate.delete((Collection<Object>) (Collection<?>) collection);
        return result == null ? 0L : result;
    }

    public <T> long setCacheList(final String key, final List<T> dataList) {
        Long count = redisTemplate.opsForList().rightPushAll(key, dataList.toArray());
        return count == null ? 0L : count;
    }

    public void incrementCacheMapValue(String key, String hkey, int y) {
        redisTemplate.opsForHash().increment(key, hkey, y);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getCacheList(final String key) {
        return (List<T>) redisTemplate.opsForList().range(key, 0, -1);
    }

    public <T> BoundSetOperations<String, Object> setCacheSet(final String key, final Set<T> dataSet) {
        BoundSetOperations<Object, Object> setOperation = redisTemplate.boundSetOps(key);
        for (T item : dataSet) {
            setOperation.add(item);
        }
        return (BoundSetOperations<String, Object>) (BoundSetOperations<?, ?>) setOperation;
    }

    @SuppressWarnings("unchecked")
    public <T> Set<T> getCacheSet(final String key) {
        return (Set<T>) redisTemplate.opsForSet().members(key);
    }

    public <T> void setCacheMap(final String key, final Map<String, T> dataMap) {
        if (dataMap != null && !dataMap.isEmpty()) {
            redisTemplate.opsForHash().putAll(key, dataMap);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> Map<String, T> getCacheMap(final String key) {
        return (Map<String, T>) (Map<?, ?>) redisTemplate.opsForHash().entries(key);
    }

    public <T> void setCacheMapValue(final String key, final String hKey, final T value) {
        redisTemplate.opsForHash().put(key, hKey, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getCacheMapValue(final String key, final String hKey) {
        HashOperations<Object, Object, Object> opsForHash = redisTemplate.opsForHash();
        return (T) opsForHash.get(key, hKey);
    }

    public void delCacheMapValue(final String key, final String hkey) {
        HashOperations<Object, Object, Object> hashOperations = redisTemplate.opsForHash();
        hashOperations.delete(key, hkey);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getMultiCacheMapValue(final String key, final Collection<Object> hKeys) {
        return (List<T>) (List<?>) redisTemplate.opsForHash().multiGet(key, hKeys);
    }

    public Collection<String> keys(final String pattern) {
        Set<Object> keys = redisTemplate.keys(pattern);
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        Set<String> result = new LinkedHashSet<>(keys.size());
        for (Object key : keys) {
            result.add(String.valueOf(key));
        }
        return result;
    }
}
