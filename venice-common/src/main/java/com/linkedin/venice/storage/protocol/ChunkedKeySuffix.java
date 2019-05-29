/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package com.linkedin.venice.storage.protocol;

@SuppressWarnings("all")
/** This record is appended to the end of keys in a store-version where chunking is enabled. N.B.: This is NOT a versioned protocol, hence, it does not support evolution. Special care should be taken if there is ever a need to evolve this. */
public class ChunkedKeySuffix extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  public static final org.apache.avro.Schema SCHEMA$ = org.apache.avro.Schema.parse("{\"type\":\"record\",\"name\":\"ChunkedKeySuffix\",\"namespace\":\"com.linkedin.venice.storage.protocol\",\"fields\":[{\"name\":\"chunkId\",\"type\":[{\"type\":\"record\",\"name\":\"ChunkId\",\"fields\":[{\"name\":\"producerGUID\",\"type\":{\"type\":\"fixed\",\"name\":\"GUID\",\"namespace\":\"com.linkedin.venice.kafka.protocol\",\"size\":16},\"doc\":\"The GUID belonging to the producer of this value.\"},{\"name\":\"segmentNumber\",\"type\":\"int\",\"doc\":\"The segment number of the first chunk sent as part of this multi-chunk value.\"},{\"name\":\"messageSequenceNumber\",\"type\":\"int\",\"doc\":\"The sequence number of the first chunk sent as part of this multi-chunk value.\"},{\"name\":\"chunkIndex\",\"type\":\"int\",\"doc\":\"The index of the current chunk. Valid values are between zero and numberOfChunks - 1.\"}]},\"null\"],\"doc\":\"This is an optional record which, if null, means that the value associated to this key does not correspond to a chunk (i.e.: it is associated to a normal non-chunked fully self-contained value, or to a ChunkedValueManifest).\"},{\"name\":\"isChunk\",\"type\":\"boolean\",\"doc\":\"This is used to reliably disambiguate between chunks and non-chunks. If false, it means that the value associated to this key does not correspond to a chunk (i.e.: it is associated to a normal non-chunked fully self-contained value, or to a ChunkedValueManifest).\"}]}");
  /** This is an optional record which, if null, means that the value associated to this key does not correspond to a chunk (i.e.: it is associated to a normal non-chunked fully self-contained value, or to a ChunkedValueManifest). */
  public com.linkedin.venice.storage.protocol.ChunkId chunkId;
  /** This is used to reliably disambiguate between chunks and non-chunks. If false, it means that the value associated to this key does not correspond to a chunk (i.e.: it is associated to a normal non-chunked fully self-contained value, or to a ChunkedValueManifest). */
  public boolean isChunk;
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return chunkId;
    case 1: return isChunk;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: chunkId = (com.linkedin.venice.storage.protocol.ChunkId)value$; break;
    case 1: isChunk = (java.lang.Boolean)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
}