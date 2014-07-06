package core.asset.hash.redis.cache;

import org.springframework.cache.annotation.Cacheable;

public class AnnotationCacheDataTest implements AnnotationCacheData {
	
	private int cacheDataCallCount;
	
	private int cacheData2CallCount;
	
	
	public AnnotationCacheDataTest() {
		super();
		// TODO Auto-generated constructor stub
	}

	@Cacheable("cacheData")
	public String getCacheData(String cacheKey) {
		cacheDataCallCount++;
		return "cacheData";
	}

	@Cacheable("cacheData2")
	public String getCacheData2(String cacheKey) {
		cacheData2CallCount++;
		return "cacheData2";
	}

	public int cacheDataCallCount() {
		return cacheDataCallCount;
	}

	public int cacheData2CallCount() {
		return cacheData2CallCount;
	}
	
	
	
}
