package core.asset.hash.redis.cache;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "hashRedisCacheContext.xml")
//CacheManager에서 캐시를 InMemory에 올려놓아 이를 초기화하여 테스트 하기 위해 컨텍스트를 계속 갱신한다.
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class HashRedisCacheTest {
	
	@Autowired
	CacheManager cacheManager;
	
	@Autowired
	AnnotationCacheData annotationCacheData;
	
	@Before
	public void setUp() throws Exception {
		
//		테스트 수행시마다 Redis 캐시내에 데이터를 모두 삭제한다.
		cacheManager.getCache("cacheData").clear();
		cacheManager.getCache("cacheData2").clear();
	}
	
	@Test
	public void initTest() throws Exception {
		assertNotNull(cacheManager);
		assertNotNull(annotationCacheData);
	}

	@Test
	public void cachePutAndGetTest() throws Exception {
		
		String cacheKey = "cacheKey";
		
		assertEquals(0, annotationCacheData.cacheDataCallCount());	
		
		annotationCacheData.getCacheData(cacheKey);
		assertEquals("캐시에 데이터가 저장되기 전 실제 메소드가 호출됐으므로 호출수 증가 ", 
					1, annotationCacheData.cacheDataCallCount());	
		
		annotationCacheData.getCacheData(cacheKey);
		assertEquals("캐시에서 데이터를 조회했으므로 실제 메소드가 호출되지 않음 ", 
				1, annotationCacheData.cacheDataCallCount());	
		
	}
	
	@Test
	public void cache2PutAndGetTest() throws Exception {
		
		String cacheKey = "cacheKey";
		
		assertEquals(0, annotationCacheData.cacheData2CallCount());	
		
		annotationCacheData.getCacheData2(cacheKey);
		assertEquals("캐시에 데이터가 저장되기 전 실제 메소드가 호출됐으므로 호출수 증가 ", 
				1, annotationCacheData.cacheData2CallCount());	
		
		annotationCacheData.getCacheData2(cacheKey);
		assertEquals("캐시에서 데이터를 조회했으므로 실제 메소드가 호출되지 않음 ", 
				1, annotationCacheData.cacheData2CallCount());	
	}
	
	@Test
	public void defaultRedisCacheBugFixTest() throws Exception {
		
//		다른 캐시명임에도 같은 key를 통해 호출 했을경우 같은 캐시데이터가 반환되는 버그해결을 검증 
		
		String cacheKey = "cacheKey";
		String cacheKey2 = "cachekey2";
		String sameCacheKey = "sameCacheKey";
		
		assertEquals(0, annotationCacheData.cacheDataCallCount());
		assertEquals(0, annotationCacheData.cacheData2CallCount());
		
		String cacheData = annotationCacheData.getCacheData(cacheKey);
		String cacheData2 = annotationCacheData.getCacheData2(cacheKey2);
		
//		다른Key로 호출했기 때문에 각각 실제 메소드가 호출되었다.
		assertEquals(1, annotationCacheData.cacheDataCallCount());
		assertEquals(1, annotationCacheData.cacheData2CallCount());
		assertNotEquals(cacheData, cacheData2);

//		캐시에서 각각 데이터를 호출했으므로 호출카운트가 증가하지 않는다.
		assertEquals(1, annotationCacheData.cacheDataCallCount());
		assertEquals(1, annotationCacheData.cacheData2CallCount());
		
		cacheData = annotationCacheData.getCacheData(sameCacheKey);
		cacheData2 = annotationCacheData.getCacheData2(sameCacheKey);
		
		assertEquals("다른Key로 다시 호출 했기 때문에 메소드 호출 카운트 증가 ", 
					2, annotationCacheData.cacheDataCallCount());
		assertEquals("다른 캐시명, 동일한 캐시Key로 다시 호출하여 실제 메소드가 호출되지 않음(버그 수정 !!!)", 
					2, annotationCacheData.cacheData2CallCount());
		
		assertNotEquals("실제 메소드는 다른 데이터를 반환하나, 다른캐시명/동일한 캐시key일 경우 동일한 캐시데이터가 반환되는 버그로 동일한 데이터가 호출",
					cacheData, cacheData2);
		
	}

}
