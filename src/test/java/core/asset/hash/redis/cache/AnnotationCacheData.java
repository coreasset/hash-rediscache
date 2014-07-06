package core.asset.hash.redis.cache;

public interface AnnotationCacheData {
	
	String getCacheData(String cacheKey);

	String getCacheData2(String cacheKey);
	
	int cacheDataCallCount();
	
	int cacheData2CallCount();
}
