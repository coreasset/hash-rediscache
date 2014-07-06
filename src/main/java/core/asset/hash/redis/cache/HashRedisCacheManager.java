package core.asset.hash.redis.cache;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.DefaultRedisCachePrefix;
import org.springframework.data.redis.cache.RedisCachePrefix;
import org.springframework.data.redis.core.RedisTemplate;

public class HashRedisCacheManager implements CacheManager {

	// fast lookup by name map
	private final ConcurrentMap<String, Cache> caches = new ConcurrentHashMap<String, Cache>();
	private final Collection<String> names = Collections.unmodifiableSet(caches.keySet());
	
	
	private RedisTemplate readRedisTemplate;
	private RedisTemplate writeRedisTemplate;

	private boolean usePrefix = false;
	private RedisCachePrefix cachePrefix = new DefaultRedisCachePrefix();

	// 0 - never expire
	private long defaultExpiration = 600;
	private Map<String, Long> expires = null;

	public HashRedisCacheManager(RedisTemplate redisTemplate) {
		this.readRedisTemplate = redisTemplate;
		this.writeRedisTemplate = redisTemplate;
	}
	public HashRedisCacheManager(RedisTemplate readRedisTemplate, RedisTemplate writeRedisTemplate) {
		this.readRedisTemplate = readRedisTemplate;
		this.writeRedisTemplate = writeRedisTemplate;
	}

	public Cache getCache(String name) {
		Cache c = caches.get(name);
		if (c == null) {
			long expiration = computeExpiration(name);
			c = new HashRedisCache(name, (usePrefix ? cachePrefix.prefix(name) : null), readRedisTemplate, writeRedisTemplate, expiration);
			caches.put(name, c);
		}

		return c;
	}

	private long computeExpiration(String name) {
		Long expiration = null;
		if (expires != null) {
			expiration = expires.get(name);
		}
		return (expiration != null ? expiration.longValue() : defaultExpiration);
	}

	public Collection<String> getCacheNames() {
		return names;
	}

	public void setUsePrefix(boolean usePrefix) {
		this.usePrefix = usePrefix;
	}

	/**
	 * Sets the cachePrefix. Defaults to 'DefaultRedisCachePrefix').
	 *
	 * @param cachePrefix the cachePrefix to set
	 */
	public void setCachePrefix(RedisCachePrefix cachePrefix) {
		this.cachePrefix = cachePrefix;
	}

	/**
	 * Sets the default expire time (in seconds).
	 *
	 * @param defaultExpireTime time in seconds.
	 */
	public void setDefaultExpiration(long defaultExpireTime) {
		this.defaultExpiration = defaultExpireTime;
	}

	/**
	 * Sets the expire time (in seconds) for cache regions (by key).
	 *
	 * @param expires time in seconds
	 */
	public void setExpires(Map<String, Long> expires) {
		this.expires = (expires != null ? new ConcurrentHashMap<String, Long>(expires) : null);
	}
}