package com.linkedin.davinci.replication.merge;

import com.linkedin.venice.meta.ReadOnlySchemaRepository;
import com.linkedin.venice.schema.merge.CollectionTimestampMergeRecordHelper;
import com.linkedin.venice.schema.merge.MergeRecordHelper;
import com.linkedin.venice.schema.writecompute.WriteComputeProcessor;
import org.apache.avro.generic.GenericData;


public class MergeConflictResolverFactory {
  private static final MergeConflictResolverFactory INSTANCE = new MergeConflictResolverFactory();

  public static MergeConflictResolverFactory getInstance() {
    return INSTANCE;
  }

  private MergeConflictResolverFactory() {
    // Singleton class
  }

  public MergeConflictResolver createMergeConflictResolver(
      ReadOnlySchemaRepository schemaRepository,
      RmdSerDe rmdSerDe,
      String storeName) {
    return createMergeConflictResolver(schemaRepository, rmdSerDe, storeName, false);
  }

  public MergeConflictResolver createMergeConflictResolver(
      ReadOnlySchemaRepository schemaRepository,
      RmdSerDe rmdSerDe,
      String storeName,
      boolean enableHandlingUpdate) {
    MergeRecordHelper mergeRecordHelper = new CollectionTimestampMergeRecordHelper();
    return new MergeConflictResolver(
        schemaRepository,
        storeName,
        valueSchemaID -> new GenericData.Record(rmdSerDe.getRmdSchema(valueSchemaID)),
        new MergeGenericRecord(new WriteComputeProcessor(mergeRecordHelper), mergeRecordHelper),
        new MergeByteBuffer(),
        new MergeResultValueSchemaResolverImpl(schemaRepository, storeName),
        rmdSerDe,
        enableHandlingUpdate);
  }
}
