package com.linkedin.venice.client.store;

import com.linkedin.venice.client.exceptions.VeniceClientException;
import com.linkedin.venice.client.stats.ClientStats;
import com.linkedin.venice.client.store.streaming.StreamingCallback;
import com.linkedin.venice.client.store.streaming.VeniceResponseCompletableFuture;
import com.linkedin.venice.client.store.streaming.VeniceResponseMap;
import com.linkedin.venice.client.store.streaming.VeniceResponseMapImpl;
import com.linkedin.venice.compute.ComputeRequestWrapper;
import com.linkedin.venice.compute.protocol.request.ComputeOperation;
import com.linkedin.venice.compute.protocol.request.CosineSimilarity;
import com.linkedin.venice.compute.protocol.request.DotProduct;
import com.linkedin.venice.compute.protocol.request.enums.ComputeOperationType;
import com.linkedin.venice.utils.ComputeUtils;
import com.linkedin.venice.utils.Pair;
import com.linkedin.venice.utils.concurrent.VeniceConcurrentHashMap;
import io.tehuti.utils.Time;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.JsonNodeFactory;

import static com.linkedin.venice.VeniceConstants.*;
import static com.linkedin.venice.compute.protocol.request.enums.ComputeOperationType.*;


/**
 * This abstract class contains all the common field and APIs for compute request builder;
 * for each new compute request version, there will be a new builder class that extends
 * this class and the new builder will include customized fields and APIs.
 * @param <K>
 */
abstract class AbstractAvroComputeRequestBuilder<K> implements ComputeRequestBuilder<K>  {
  /**
   * Error map field can not be a static variable; after setting the error map field in a schema, the position of the
   * field will be updated, so the next time when we set the field in a new schema, it would fail because
   * {@link Schema#setFields(List)} check whether the position is -1.
   */
  protected final Schema.Field VENICE_COMPUTATION_ERROR_MAP_FIELD = new Schema.Field(VENICE_COMPUTATION_ERROR_MAP_FIELD_NAME,
      Schema.createMap(Schema.create(Schema.Type.STRING)), "", JsonNodeFactory.instance.objectNode());

  protected static final Map< Map<String, Object>, Pair<Schema, String>> RESULT_SCHEMA_CACHE = new VeniceConcurrentHashMap<>();
  protected static final String PROJECTION_SPEC = "projection_spec";
  protected static final String DOT_PRODUCT_SPEC = "dotProduct_spec";
  protected static final String COSINE_SIMILARITY_SPEC = "cosineSimilarity_spec";

  private final Time time;
  protected final Schema latestValueSchema;
  private final InternalAvroStoreClient storeClient;
  protected final String resultSchemaName;
  private final Optional<ClientStats> stats;
  private final Optional<ClientStats> streamingStats;
  protected Set<String> projectFields = new HashSet<>();
  protected List<DotProduct> dotProducts = new LinkedList<>();
  protected List<CosineSimilarity> cosineSimilarities = new LinkedList<>();

  public AbstractAvroComputeRequestBuilder(Schema latestValueSchema, InternalAvroStoreClient storeClient,
      Optional<ClientStats> stats, Optional<ClientStats> streamingStats, Time time) {

    if (latestValueSchema.getType() != Schema.Type.RECORD) {
      throw new VeniceClientException("Only value schema with 'RECORD' type is supported");
    }
    if (latestValueSchema.getField(VENICE_COMPUTATION_ERROR_MAP_FIELD_NAME) != null) {
      throw new VeniceClientException("Field name: " + VENICE_COMPUTATION_ERROR_MAP_FIELD_NAME +
          " is reserved, please don't use it in value schema: " + latestValueSchema);
    }
    this.time = time;
    this.latestValueSchema = latestValueSchema;
    this.storeClient = storeClient;
    this.stats = stats;
    this.streamingStats = streamingStats;
    this.resultSchemaName = ComputeUtils.removeAvroIllegalCharacter(storeClient.getStoreName()) + "_VeniceComputeResult";
  }

  @Override
  public ComputeRequestBuilder<K> project(String... fieldNames) throws VeniceClientException {
    for (String fieldName : fieldNames) {
      projectFields.add(fieldName);
    }

    return this;
  }

  @Override
  public ComputeRequestBuilder<K> project(Collection<String> fieldNames) throws VeniceClientException {
    for (String fieldName : fieldNames) {
      projectFields.add(fieldName);
    }

    return this;
  }

  @Override
  public ComputeRequestBuilder<K> dotProduct(String inputFieldName, List<Float> dotProductParam, String resultFieldName)
      throws VeniceClientException {
    DotProduct dotProduct = (DotProduct) DOT_PRODUCT.getNewInstance();
    dotProduct.field = inputFieldName;
    dotProduct.dotProductParam = dotProductParam;
    dotProduct.resultFieldName = resultFieldName;
    dotProducts.add(dotProduct);

    return this;
  }

  @Override
  public ComputeRequestBuilder<K> cosineSimilarity(String inputFieldName, List<Float> cosSimilarityParam, String resultFieldName)
      throws VeniceClientException {
    CosineSimilarity cosineSimilarity = (CosineSimilarity) COSINE_SIMILARITY.getNewInstance();
    cosineSimilarity.field = inputFieldName;
    cosineSimilarity.cosSimilarityParam = cosSimilarityParam;
    cosineSimilarity.resultFieldName = resultFieldName;
    cosineSimilarities.add(cosineSimilarity);

    return this;
  }

  /**
   * Generate compute spec for projections, dot-product and cosine-similarity.
   * @return common compute spec
   */
  protected Map<String, Object> getCommonComputeSpec() {
    Map<String, Object> computeSpec = new HashMap<>();
    computeSpec.put(PROJECTION_SPEC, projectFields);
    List<Pair<CharSequence, CharSequence>> dotProductPairs = new LinkedList<>();
    dotProducts.forEach( dotProduct -> {
      dotProductPairs.add(Pair.create(dotProduct.field, dotProduct.resultFieldName));
    });
    computeSpec.put(DOT_PRODUCT_SPEC, dotProductPairs);
    List<Pair<CharSequence, CharSequence>> cosineSimilarityPairs = new LinkedList<>();
    cosineSimilarities.forEach( cosineSimilarity -> {
      cosineSimilarityPairs.add(Pair.create(cosineSimilarity.field, cosineSimilarity.resultFieldName));
    });
    computeSpec.put(COSINE_SIMILARITY_SPEC, cosineSimilarityPairs);
    return computeSpec;
  }

  /**
   * Compute operation validity check for projections, dot-product and cosine-similarity.
   * @return a set of existing operations result field name
   */
  protected Set<String> commonValidityCheck() {
    // Projection
    projectFields.forEach( projectField -> {
      if (null == latestValueSchema.getField(projectField)) {
        throw new VeniceClientException("Unknown project field: " + projectField);
      }
    });
    // DotProduct
    Set<String> computeResultFields = new HashSet<>();
    dotProducts.forEach( dotProduct ->
        checkComputeFieldValidity(dotProduct.field.toString(),
            dotProduct.resultFieldName.toString(),
            computeResultFields,
            DOT_PRODUCT));
    // CosineSimilarity
    /**
     * Use the same compute result field set because the result field name in cosine similarity couldn't collide with
     * the result field name in dot product
     */
    cosineSimilarities.forEach( cosineSimilarity ->
        checkComputeFieldValidity(cosineSimilarity.field.toString(),
            cosineSimilarity.resultFieldName.toString(),
            computeResultFields,
            COSINE_SIMILARITY));

    return computeResultFields;
  }

  /**
   * Generate compute result schema fields for projections, dot-product and cosine-similarity.
   * @return a list of existing compute result schema fields
   */
  protected List<Schema.Field> getCommonResultFields() {
    List<Schema.Field> resultSchemaFields = new LinkedList<>();
    projectFields.forEach( projectField -> {
      Schema.Field existingField = latestValueSchema.getField(projectField);
      resultSchemaFields.add(new Schema.Field(existingField.name(), existingField.schema(), "", existingField.defaultValue()));
    });
    dotProducts.forEach( dotProduct -> {
      Schema.Field dotProductField = new Schema.Field(dotProduct.resultFieldName.toString(),
          getDotProductResultSchema(),
          "",
          getDotProductResultDefaultValue());
      resultSchemaFields.add(dotProductField);
    });
    cosineSimilarities.forEach( cosineSimilarity -> {
      Schema.Field cosineSimilarityField = new Schema.Field(cosineSimilarity.resultFieldName.toString(),
          getCosineSimilarityResultSchema(),
          "",
          getCosineSimilarityResultDefaultValue());
      resultSchemaFields.add(cosineSimilarityField);
    });
    return resultSchemaFields;
  }

  /**
   * Generate compute operations for projections, dot-product and cosine-similarity.
   * @return a list of existing compute operations
   */
  protected List<ComputeOperation> getCommonComputeOperations() {
    List<ComputeOperation> operations = new LinkedList<>();
    dotProducts.forEach( dotProduct -> {
      ComputeOperation computeOperation = new ComputeOperation();
      computeOperation.operationType = DOT_PRODUCT.getValue();
      computeOperation.operation = dotProduct;
      operations.add(computeOperation);
    });
    cosineSimilarities.forEach( cosineSimilarity -> {
      ComputeOperation computeOperation = new ComputeOperation();
      computeOperation.operationType = COSINE_SIMILARITY.getValue();
      computeOperation.operation = cosineSimilarity;
      operations.add(computeOperation);
    });
    return operations;
  }

  @Override
  public CompletableFuture<Map<K, GenericRecord>> execute(Set<K> keys) throws VeniceClientException {
    long preRequestTimeInNS = time.nanoseconds();
    Pair<Schema,String> resultSchema = getResultSchema();
    // Generate ComputeRequestWrapper object
    ComputeRequestWrapper computeRequestWrapper = generateComputeRequest(resultSchema.getSecond());

    CompletableFuture<Map<K, GenericRecord>> computeFuture = storeClient.compute(computeRequestWrapper, keys,
        resultSchema.getFirst(), stats, preRequestTimeInNS);
    if (stats.isPresent()) {
      return AppTimeOutTrackingCompletableFuture.track(computeFuture, stats.get());
    }
    return computeFuture;
  }


  @Override
  public CompletableFuture<VeniceResponseMap<K, GenericRecord>> streamingExecute(Set<K> keys) {
    Map<K, GenericRecord> resultMap = new VeniceConcurrentHashMap<>(keys.size());
    Queue<K> nonExistingKeyList = new ConcurrentLinkedQueue<>();
    VeniceResponseCompletableFuture<VeniceResponseMap<K, GenericRecord>>
        resultFuture = new VeniceResponseCompletableFuture<>(
        () -> new VeniceResponseMapImpl(resultMap, nonExistingKeyList, false),
        keys.size(),
        streamingStats);
    streamingExecute(keys, new StreamingCallback<K, GenericRecord>() {
      @Override
      public void onRecordReceived(K key, GenericRecord value) {
        if (value != null) {
          /**
           * {@link java.util.concurrent.ConcurrentHashMap#put} won't take 'null' as the value.
           */
          resultMap.put(key, value);
        } else {
          nonExistingKeyList.add(key);
        }
      }

      @Override
      public void onCompletion(Optional<Exception> exception) {
        if (exception.isPresent()) {
          resultFuture.completeExceptionally(exception.get());
        } else {
          resultFuture.complete(new VeniceResponseMapImpl(resultMap, nonExistingKeyList, true));
        }
      }
    });

    return resultFuture;
  }

  @Override
  public void streamingExecute(Set<K> keys, StreamingCallback<K, GenericRecord> callback) throws VeniceClientException {
    long preRequestTimeInNS = time.nanoseconds();
    Pair<Schema,String> resultSchema = getResultSchema();
    // Generate ComputeRequest object
    ComputeRequestWrapper computeRequestWrapper = generateComputeRequest(resultSchema.getSecond());

    storeClient.compute(computeRequestWrapper, keys, resultSchema.getFirst(), callback, preRequestTimeInNS);
  }

  protected void checkComputeFieldValidity(String computeFieldName, String resultFieldName, Set<String> resultFieldsSet, ComputeOperationType computeType) {
    Schema.Field fieldSchema = latestValueSchema.getField(computeFieldName);
    if (null == fieldSchema) {
      throw new VeniceClientException("Unknown " + computeType + " field: " + computeFieldName);
    }
    if (fieldSchema.schema().getType() != Schema.Type.ARRAY) {
      throw new VeniceClientException(computeType + " field: " + computeFieldName + " isn't with 'ARRAY' type");
    }
    // TODO: is it necessary to be 'FLOAT' only?
    Schema elementSchema = fieldSchema.schema().getElementType();
    if (elementSchema.getType() != Schema.Type.FLOAT) {
      throw new VeniceClientException(computeType + " field: " + computeFieldName + " isn't an 'ARRAY' of 'FLOAT'");
    }

    if (resultFieldsSet.contains(resultFieldName)) {
      throw new VeniceClientException(computeType + " result field: " + resultFieldName +
          " has been specified more than once");
    }
    if (null != latestValueSchema.getField(resultFieldName)) {
      throw new VeniceClientException(computeType + " result field: " + resultFieldName +
          " collides with the fields defined in value schema");
    }
    if (VENICE_COMPUTATION_ERROR_MAP_FIELD_NAME.equals(resultFieldName)) {
      throw new VeniceClientException("Field name: " + resultFieldName +
          " is reserved, please choose a different name to store the computed result");
    }
    resultFieldsSet.add(resultFieldName);
  }

  /**
   * Generate result schema.
   * @return
   */
  protected abstract Pair<Schema, String> getResultSchema();

  /**
   * Generate ComputeRequestWrapper record.
   * @param resultSchemaStr
   * @return
   */
  protected abstract ComputeRequestWrapper generateComputeRequest(String resultSchemaStr);

  /**
   * Get dot product result schema;
   * V1: "double"
   * V2: ["null", "float"]
   * @return
   */
  protected abstract Schema getDotProductResultSchema();

  /**
   * Get cosine similarity result schema;
   * V1: "double"
   * V2: ["null", "float"]
   * @return
   */
  protected abstract Schema getCosineSimilarityResultSchema();

  /**
   * Get dot product result default value
   * V1: 0 (null can not be the default result for "double" schema)
   * V2: null
   * @return
   */
  protected abstract JsonNode getDotProductResultDefaultValue();

  /**
   * Get cosine similarity result default value
   * V1: 0
   * V2: null
   * @return
   */
  protected abstract JsonNode getCosineSimilarityResultDefaultValue();
}