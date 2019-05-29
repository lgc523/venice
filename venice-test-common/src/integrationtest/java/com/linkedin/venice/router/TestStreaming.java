package com.linkedin.venice.router;

import com.linkedin.d2.balancer.D2Client;
import com.linkedin.venice.ConfigKeys;
import com.linkedin.venice.client.exceptions.VeniceClientException;
import com.linkedin.venice.client.store.AvroComputeRequestBuilder;
import com.linkedin.venice.client.store.AvroGenericStoreClient;
import com.linkedin.venice.client.store.ClientConfig;
import com.linkedin.venice.client.store.ClientFactory;
import com.linkedin.venice.client.store.StatTrackingStoreClient;
import com.linkedin.venice.client.store.streaming.StreamingCallback;
import com.linkedin.venice.compression.CompressionStrategy;
import com.linkedin.venice.compression.CompressorFactory;
import com.linkedin.venice.compression.VeniceCompressor;
import com.linkedin.venice.controllerapi.ControllerClient;
import com.linkedin.venice.controllerapi.UpdateStoreQueryParams;
import com.linkedin.venice.controllerapi.VersionCreationResponse;
import com.linkedin.venice.helix.HelixReadOnlySchemaRepository;
import com.linkedin.venice.integration.utils.D2TestUtils;
import com.linkedin.venice.integration.utils.ServiceFactory;
import com.linkedin.venice.integration.utils.VeniceClusterWrapper;
import com.linkedin.venice.integration.utils.VeniceRouterWrapper;
import com.linkedin.venice.integration.utils.VeniceServerWrapper;
import com.linkedin.venice.meta.Version;
import com.linkedin.venice.serialization.DefaultSerializer;
import com.linkedin.venice.serialization.VeniceKafkaSerializer;
import com.linkedin.venice.serialization.avro.VeniceAvroKafkaSerializer;
import com.linkedin.venice.utils.TestUtils;
import com.linkedin.venice.utils.Utils;
import com.linkedin.venice.utils.concurrent.VeniceConcurrentHashMap;
import com.linkedin.venice.writer.VeniceWriter;
import io.tehuti.Metric;
import io.tehuti.metrics.MetricsRepository;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.linkedin.venice.meta.PersistenceType.*;
import static com.linkedin.venice.router.httpclient.StorageNodeClientType.*;


public class TestStreaming {
  private static final Logger LOGGER = Logger.getLogger(TestRead.class);

  private static final int MAX_KEY_LIMIT = 1000;
  private static final String NON_EXISTING_KEY = "unknown_key";
  private VeniceClusterWrapper veniceCluster;
  private String storeVersionName;
  private int valueSchemaId;
  private String storeName;

  private VeniceKafkaSerializer keySerializer;
  private VeniceKafkaSerializer valueSerializer;
  private VeniceWriter<Object, Object> veniceWriter;

  private static final String keyPrefix = "key_";
  private static final String KEY_SCHEMA = "\"string\"";
  private static final String VALUE_SCHEMA = "{\n" + "\"type\": \"record\",\n" + "\"name\": \"test_value_schema\",\n"
      + "\"fields\": [\n" + "  {\"name\": \"int_field\", \"type\": \"int\"},\n"
      + "  {\"name\": \"float_field\", \"type\": \"float\"}\n" + "]\n" + "}";
  private static final Schema VALUE_SCHEMA_OBJECT = Schema.parse(VALUE_SCHEMA);


  @BeforeClass
  public void setUp() throws InterruptedException, ExecutionException, VeniceClientException, IOException {
    /**
     * The following config is used to detect Netty resource leaking.
     * If memory leak happens, you will see the following log message:
     *
     *  ERROR io.netty.util.ResourceLeakDetector - LEAK: ByteBuf.release() was not called before it's garbage-collected.
     *  See http://netty.io/wiki/reference-counted-objects.html for more information.
     **/

    System.setProperty("io.netty.leakDetection.maxRecords", "50");
    System.setProperty("io.netty.leakDetection.level", "paranoid");

    Utils.thisIsLocalhost();
    veniceCluster = ServiceFactory.getVeniceCluster(1, 2, 0, 2, 100, true, false);

    // By default, the storage engine is BDB, and we would like test ROCKS_DB here as well.
    Properties serverProperties = new Properties();
    serverProperties.put(ConfigKeys.PERSISTENCE_TYPE, ROCKS_DB);
    Properties serverFeatureProperties = new Properties();
    serverFeatureProperties.put(VeniceServerWrapper.SERVER_ENABLE_SSL, "true");
    veniceCluster.addVeniceServer(serverFeatureProperties, serverProperties);

    // Create test store
    VersionCreationResponse creationResponse = veniceCluster.getNewStoreVersion(KEY_SCHEMA, VALUE_SCHEMA);
    storeVersionName = creationResponse.getKafkaTopic();
    storeName = Version.parseStoreFromKafkaTopicName(storeVersionName);
    // enable compute
    veniceCluster.getControllerClient().updateStore(storeName,
        new UpdateStoreQueryParams().setReadComputationEnabled(true)
        .setReadQuotaInCU(1000000000)
    );

    valueSchemaId = HelixReadOnlySchemaRepository.VALUE_SCHEMA_STARTING_ID;

    keySerializer = new VeniceAvroKafkaSerializer(KEY_SCHEMA);
    valueSerializer = new VeniceAvroKafkaSerializer(VALUE_SCHEMA);

    CompressionStrategy compressionStrategy = CompressionStrategy.GZIP;
    VeniceCompressor compressor = CompressorFactory.getCompressor(compressionStrategy);

    veniceWriter = TestUtils.getVeniceTestWriterFactory(veniceCluster.getKafka().getAddress()).getVeniceWriter(storeVersionName, keySerializer, new DefaultSerializer());

    final int pushVersion = Version.parseVersionFromKafkaTopicName(storeVersionName);

    veniceWriter.broadcastStartOfPush(false, false, compressionStrategy, new HashMap<>());
    // Insert test record and wait synchronously for it to succeed
    for (int i = 0; i < 10000; ++i) {
      GenericRecord valueRecord = new GenericData.Record(VALUE_SCHEMA_OBJECT);
      valueRecord.put("int_field", i);
      valueRecord.put("float_field", i + 100.0f);

      byte[] value = compressor.compress(valueSerializer.serialize("", valueRecord));
      veniceWriter.put(keyPrefix + i, value, valueSchemaId).get();
    }
    // Write end of push message to make node become ONLINE from BOOTSTRAP
    veniceWriter.broadcastEndOfPush(new HashMap<>());

    // Wait for storage node to finish consuming, and new version to be activated
    String controllerUrl = veniceCluster.getAllControllersURLs();
    TestUtils.waitForNonDeterministicCompletion(30, TimeUnit.SECONDS, () -> {
      int currentVersion = ControllerClient.getStore(controllerUrl, veniceCluster.getClusterName(), storeName)
          .getStore()
          .getCurrentVersion();
      return currentVersion == pushVersion;
    });
  }

  @AfterClass
  public void cleanUp() {
    IOUtils.closeQuietly(veniceCluster);
    IOUtils.closeQuietly(veniceWriter);
  }

  private Properties getRouterProperties(boolean enableStreaming, boolean enableNettyClient, boolean enableClientCompression) {
    // To trigger long-tail retry
    Properties routerProperties = new Properties();
    routerProperties.put(ConfigKeys.ROUTER_LONG_TAIL_RETRY_FOR_SINGLE_GET_THRESHOLD_MS, 1);
    routerProperties.put(ConfigKeys.ROUTER_MAX_KEY_COUNT_IN_MULTIGET_REQ, MAX_KEY_LIMIT); // 10 keys at most in a batch-get request
    routerProperties.put(ConfigKeys.ROUTER_LONG_TAIL_RETRY_FOR_BATCH_GET_THRESHOLD_MS, "1-:100");
    routerProperties.put(ConfigKeys.ROUTER_STREAMING_ENABLED, Boolean.toString(enableStreaming));
    routerProperties.put(ConfigKeys.ROUTER_STORAGE_NODE_CLIENT_TYPE, enableNettyClient ? NETTY_4_CLIENT.name() : APACHE_HTTP_ASYNC_CLIENT.name());
    routerProperties.put(ConfigKeys.ROUTER_CLIENT_DECOMPRESSION_ENABLED, Boolean.toString(enableClientCompression));

    return routerProperties;
  }

  @DataProvider (name = "testReadStreamingDataProvider")
  private Object[][] testReadStreamingDataProvider() {
    return new Object[][] {{true}, {false}};
  }

  @Test(timeOut = 60000, dataProvider = "testReadStreamingDataProvider")
  public void testReadStreaming(boolean enableStreaming) throws Exception {
    // Start a new router every time with the right config
    // With Apache HAC on Router with client compression enabled
    VeniceRouterWrapper veniceRouterWrapperWithHttpAsyncClient = veniceCluster.addVeniceRouter(getRouterProperties(enableStreaming, false, true));
    MetricsRepository routerMetricsRepositoryWithHttpAsyncClient = veniceRouterWrapperWithHttpAsyncClient.getMetricsRepository();
    // With Netty Client on Router with client compression disabled
    VeniceRouterWrapper veniceRouterWrapperWithNettyClient = veniceCluster.addVeniceRouter(getRouterProperties(enableStreaming, true, false));
    MetricsRepository routerMetricsRepositoryWithNettyClient = veniceRouterWrapperWithNettyClient.getMetricsRepository();
    try {
      // test with D2 store client, since streaming support is only available with D2 client so far.
      D2Client d2Client = D2TestUtils.getAndStartD2Client(veniceCluster.getZk().getAddress());
      MetricsRepository d2ClientMetricsRepository = new MetricsRepository();
      AvroGenericStoreClient d2StoreClient = ClientFactory.getAndStartGenericAvroClient(ClientConfig.defaultGenericClientConfig(storeName)
          .setD2ServiceName(D2TestUtils.DEFAULT_TEST_SERVICE_NAME)
          .setD2Client(d2Client)
          .setMetricsRepository(d2ClientMetricsRepository)
          .setUseFastAvro(false));

      // Right now, all the streaming interfaces are still internal, and we will expose them once they are fully verified.
      StatTrackingStoreClient trackingStoreClient = (StatTrackingStoreClient)d2StoreClient;

      // Run multiple rounds
      int rounds = 100;
      int cur = 0;
      Set<String> keySet = new HashSet<>();
      for (int i = 0; i < MAX_KEY_LIMIT - 1; ++i) {
        keySet.add(keyPrefix + i);
      }
      keySet.add(NON_EXISTING_KEY);

      while (++cur <= rounds) {

        final Map<String, Object> finalMultiGetResultMap = new VeniceConcurrentHashMap<>();
        final AtomicInteger totalMultiGetResultCnt = new AtomicInteger(0);
        // Streaming batch-get
        CountDownLatch latch = new CountDownLatch(1);
        trackingStoreClient.batchGet(keySet, new StreamingCallback<String, Object>() {

          @Override
          public void onRecordReceived(String key, Object value) {
            if (null != value) {
              /**
               * {@link java.util.concurrent.ConcurrentHashMap#put) could not take 'null' as the value.
               */
              finalMultiGetResultMap.put(key, value);
            }
            totalMultiGetResultCnt.getAndIncrement();
          }

          @Override
          public void onCompletion(Optional<Exception> exception) {
            LOGGER.info("MultiGet onCompletion invoked with Exception: " + exception);
            latch.countDown();
            if (exception.isPresent()) {
              Assert.fail("Exception: " + exception.get() + " is not expected");
            }
          }
        });
        latch.await();
        Assert.assertEquals(totalMultiGetResultCnt.get(), MAX_KEY_LIMIT);
        Assert.assertEquals(finalMultiGetResultMap.size(), MAX_KEY_LIMIT - 1);
        // Verify the result
        verifyMultiGetResult(finalMultiGetResultMap);

        // test batch-get with streaming as the internal implementation
        CompletableFuture<Map<String, Object>> resultFuture = trackingStoreClient.streamBatchGet(keySet);
        Map<String, Object> multiGetResultMap = resultFuture.get();
        // Regular batch-get API won't return non-existing keys
        Assert.assertEquals(multiGetResultMap.size(), MAX_KEY_LIMIT - 1);
        verifyMultiGetResult(multiGetResultMap);
        // Test compute streaming
        AtomicInteger computeResultCnt = new AtomicInteger(0);
        Map<String, GenericRecord> finalComputeResultMap = new VeniceConcurrentHashMap<>();
        CountDownLatch computeLatch = new CountDownLatch(1);
        AvroComputeRequestBuilder<String> computeRequestBuilder = (AvroComputeRequestBuilder<String>)trackingStoreClient.compute().project("int_field");
        computeRequestBuilder.execute(keySet, new StreamingCallback<String, GenericRecord>() {
          @Override
          public void onRecordReceived(String key, GenericRecord value) {
            computeResultCnt.incrementAndGet();
            if (null != value) {
              finalComputeResultMap.put(key, value);
            }
          }

          @Override
          public void onCompletion(Optional<Exception> exception) {
            LOGGER.info("Compute onCompletion invoked with Venice Exception: " + exception);
            computeLatch.countDown();
            if (exception.isPresent()) {
              Assert.fail("Exception: " + exception.get() + " is not expected");
            }
          }
        });
        computeLatch.await();
        Assert.assertEquals(computeResultCnt.get(), MAX_KEY_LIMIT);
        Assert.assertEquals(finalComputeResultMap.size(), MAX_KEY_LIMIT - 1); // Without non-existing key
        verifyComputeResult(finalComputeResultMap);
        // Test compute with streaming implementation
        CompletableFuture<Map<String, GenericRecord>> computeFuture = computeRequestBuilder.streamExecute(keySet);
        Map<String, GenericRecord> computeResultMap = computeFuture.get();
        Assert.assertEquals(multiGetResultMap.size(), MAX_KEY_LIMIT - 1);
        verifyComputeResult(computeResultMap);
      }
      // Verify some client-side metrics, and we could add verification for more metrics if necessary
      String metricPrefix = "." + storeName;
      Map<String, ? extends Metric> metrics = d2ClientMetricsRepository.metrics();
      Assert.assertTrue(metrics.get(metricPrefix + "--multiget_streaming_request.OccurrenceRate").value() > 0);
      Assert.assertTrue(metrics.get(metricPrefix + "--multiget_streaming_healthy_request_latency.Avg").value() > 0);
      Assert.assertTrue(metrics.get(metricPrefix + "--multiget_streaming_response_ttfr.50thPercentile").value() > 0);
      Assert.assertTrue(metrics.get(metricPrefix + "--multiget_streaming_response_tt50pr.50thPercentile").value() > 0);
      Assert.assertTrue(metrics.get(metricPrefix + "--multiget_streaming_response_tt90pr.50thPercentile").value() > 0);
      Assert.assertTrue(metrics.get(metricPrefix + "--multiget_streaming_response_tt95pr.50thPercentile").value() > 0);
      Assert.assertTrue(metrics.get(metricPrefix + "--multiget_streaming_response_tt99pr.50thPercentile").value() > 0);
      Assert.assertTrue(metrics.get(metricPrefix + "--multiget_streaming_healthy_request.OccurrenceRate").value() > 0);
      Assert.assertTrue(metrics.get(metricPrefix + "--compute_streaming_request.OccurrenceRate").value() > 0);
      Assert.assertTrue(metrics.get(metricPrefix + "--compute_streaming_healthy_request_latency.Avg").value() > 0);
      Assert.assertTrue(metrics.get(metricPrefix + "--compute_streaming_response_ttfr.50thPercentile").value() > 0);
      Assert.assertTrue(metrics.get(metricPrefix + "--compute_streaming_response_tt50pr.50thPercentile").value() > 0);
      Assert.assertTrue(metrics.get(metricPrefix + "--compute_streaming_response_tt90pr.50thPercentile").value() > 0);
      Assert.assertTrue(metrics.get(metricPrefix + "--compute_streaming_response_tt95pr.50thPercentile").value() > 0);
      Assert.assertTrue(metrics.get(metricPrefix + "--compute_streaming_response_tt99pr.50thPercentile").value() > 0);

      LOGGER.info("The following metrics are Router metrics:");
      //Verify some router metrics
      for (MetricsRepository routerMetricsRepository : Arrays.asList(routerMetricsRepositoryWithHttpAsyncClient)) { //, routerMetricsRepositoryWithNettyClient)) {
        Map<String, ? extends Metric> routerMetrics = routerMetricsRepository.metrics();
        if (enableStreaming) {
          // The following metrics are only available when Router is running in Streaming mode.
          Assert.assertTrue(routerMetrics.get(metricPrefix + "--multiget_streaming_request.OccurrenceRate").value() > 0);
          Assert.assertTrue(routerMetrics.get(metricPrefix + "--multiget_streaming_latency.99thPercentile").value() > 0);
          Assert.assertTrue(routerMetrics.get(metricPrefix + "--multiget_streaming_fanout_request_count.Avg").value() > 0);
          Assert.assertTrue(routerMetrics.get(metricPrefix + "--compute_streaming_request.OccurrenceRate").value() > 0);
          Assert.assertTrue(routerMetrics.get(metricPrefix + "--compute_streaming_latency.99thPercentile").value() > 0);
          Assert.assertTrue(routerMetrics.get(metricPrefix + "--compute_streaming_fanout_request_count.Avg").value() > 0);
        }
      }
    } finally {
      veniceRouterWrapperWithHttpAsyncClient.close();
    }
  }

  private void verifyMultiGetResult(Map<String, Object> resultMap) {
    for (int i = 0; i < MAX_KEY_LIMIT - 1; ++i) {
      String key = keyPrefix + i;
      Object value = resultMap.get(key);
      Assert.assertTrue(value instanceof GenericRecord);
      GenericRecord record = (GenericRecord)value;
      Assert.assertEquals(record.get("int_field"), i);
      Assert.assertEquals(record.get("float_field"), i + 100.0f);
    }
  }

  private void verifyComputeResult(Map<String, GenericRecord> resultMap) {
    for (int i = 0; i < MAX_KEY_LIMIT - 1; ++i) {
      String key = keyPrefix + i;
      GenericRecord record = resultMap.get(key);
      Assert.assertEquals(record.get("int_field"), i);
      Assert.assertNull(record.get("float_field"));
    }
  }
}