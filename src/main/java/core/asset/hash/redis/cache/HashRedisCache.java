package core.asset.hash.redis.cache;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.Assert;

@SuppressWarnings("unchecked")
class HashRedisCache implements Cache {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private long WAIT_FOR_LOCK = 300;
	private final byte[] cacheLockName;

	private final String name;
	@SuppressWarnings("rawtypes")
	private RedisTemplate readRedisTemplate;
	@SuppressWarnings("rawtypes")
	private RedisTemplate writeRedisTemplate;
	private final byte[] prefix;
	private final long expiration;

	/**
	 * 
	 * Constructs a new <code>RedisCache</code> instance.
	 * 
	 * @param name
	 *            cache name
	 * @param prefix
	 * @param template
	 * @param expiration
	 */
	HashRedisCache(String name, byte[] prefix, RedisTemplate<? extends Object, ? extends Object> readRedisTemplate,
			RedisTemplate<? extends Object, ? extends Object> writeRedistTemplate, long expiration) {

		Assert.hasText(name, "non-empty cache name is required");
		this.name = name;
		this.readRedisTemplate = readRedisTemplate;
		this.writeRedisTemplate = writeRedistTemplate;
		this.prefix = prefix;
		this.expiration = expiration;

		this.cacheLockName = readRedisTemplate.getStringSerializer().serialize(name + "~lock");
	}

	public void setReadRedisTemplate(RedisTemplate readRedisTemplate) {
		this.readRedisTemplate = readRedisTemplate;
	}

	public void setWriteRedisTemplate(RedisTemplate writeRedisTemplate) {
		this.writeRedisTemplate = writeRedisTemplate;
	}

	public String getName() {
		return name;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * This implementation simply returns the RedisTemplate used for configuring
	 * the cache, giving access to the underlying Redis store.
	 */
	public Object getNativeCache() {
		return readRedisTemplate;
	}

	public ValueWrapper get(final Object key) {

		final byte[] rawKey = readRedisTemplate.getKeySerializer().serialize(name);
		final byte[] rawHashKey = readRedisTemplate.getHashKeySerializer().serialize(key);

		byte[] rawHashValue = (byte[]) readRedisTemplate.execute(new RedisCallback<byte[]>() {

			public byte[] doInRedis(RedisConnection connection) {
				waitForLock(connection);
				return connection.hGet(rawKey, rawHashKey);
			}
		}, true);

		if(rawHashValue != null) {
			return new SimpleValueWrapper(readRedisTemplate.getHashValueSerializer().deserialize(rawHashValue)) ;
		}


		return null;
	}

	public void put(Object key, Object value) {

		final byte[] rawKey = writeRedisTemplate.getKeySerializer().serialize(name);
		final byte[] rawHashKey = writeRedisTemplate.getHashKeySerializer().serialize(key);
		final byte[] rawHashValue = writeRedisTemplate.getHashValueSerializer().serialize(value);

		writeRedisTemplate.execute(new RedisCallback<Object>() {
			public Object doInRedis(RedisConnection connection) throws DataAccessException {
				waitForLock(connection);
				connection.multi();
				connection.hSet(rawKey, rawHashKey, rawHashValue);

				if (expiration > 0) {
					connection.expire(rawKey, expiration);
				}
				connection.exec();

				return null;
			}
		}, true);
	}

	public void evict(Object key) {

		writeRedisTemplate.opsForHash().delete(name, key);
	}

	public void clear() {

		final byte[] rawKey = writeRedisTemplate.getKeySerializer().serialize(name);

		// need to del each key individually
		writeRedisTemplate.execute(new RedisCallback<Object>() {
			public Object doInRedis(RedisConnection connection) throws DataAccessException {
				// another clear is on-going
				if (connection.exists(cacheLockName)) {
					return null;
				}

				try {
					connection.set(cacheLockName, cacheLockName);

					Set<byte[]> hKeys = connection.hKeys(rawKey);
					if(hKeys != null) {
						for(byte[] hKey : hKeys) {
							connection.hDel(rawKey, hKey);
						}
					}

					return null;

				} finally {
					connection.del(cacheLockName);
				}
			}
		}, true);
	}

	private boolean waitForLock(RedisConnection connection) {
		boolean retry;
		boolean foundLock = false;
		do {
			retry = false;
			if (connection.exists(cacheLockName)) {
				foundLock = true;
				try {
					Thread.sleep(WAIT_FOR_LOCK);
				} catch (InterruptedException ex) {
					// ignore
				}
				retry = true;
			}
		} while (retry);
		return foundLock;
	}
}