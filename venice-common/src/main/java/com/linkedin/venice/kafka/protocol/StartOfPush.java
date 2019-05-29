/**
 * Autogenerated by Avro
 * 
 * DO NOT EDIT DIRECTLY
 */
package com.linkedin.venice.kafka.protocol;

@SuppressWarnings("all")
/** This ControlMessage is sent once per partition, at the beginning of a bulk load, before any of the data producers come online. This does not contain any data beyond the one which is common to all ControlMessageType. */
public class StartOfPush extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  public static final org.apache.avro.Schema SCHEMA$ = org.apache.avro.Schema.parse("{\"type\":\"record\",\"name\":\"StartOfPush\",\"namespace\":\"com.linkedin.venice.kafka.protocol\",\"fields\":[{\"name\":\"sorted\",\"type\":\"boolean\",\"doc\":\"Whether the messages inside current topic partition between 'StartOfPush' control message and 'EndOfPush' control message is lexicographically sorted by key bytes\",\"default\":false},{\"name\":\"chunked\",\"type\":\"boolean\",\"doc\":\"Whether the messages inside the current push are encoded with chunking support. If true, this means keys will be prefixed with ChunkId, and values may contain a ChunkedValueManifest (if schema is defined as -20).\",\"default\":false},{\"name\":\"compressionStrategy\",\"type\":\"int\",\"doc\":\"What type of compression strategy the current push uses. Using int because Avro Enums are not evolvable. The mapping is the following: 0 => NO_OP, 1 => GZIP\",\"default\":0}]}");
  /** Whether the messages inside current topic partition between 'StartOfPush' control message and 'EndOfPush' control message is lexicographically sorted by key bytes */
  public boolean sorted;
  /** Whether the messages inside the current push are encoded with chunking support. If true, this means keys will be prefixed with ChunkId, and values may contain a ChunkedValueManifest (if schema is defined as -20). */
  public boolean chunked;
  /** What type of compression strategy the current push uses. Using int because Avro Enums are not evolvable. The mapping is the following: 0 => NO_OP, 1 => GZIP */
  public int compressionStrategy;
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return sorted;
    case 1: return chunked;
    case 2: return compressionStrategy;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: sorted = (java.lang.Boolean)value$; break;
    case 1: chunked = (java.lang.Boolean)value$; break;
    case 2: compressionStrategy = (java.lang.Integer)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
}