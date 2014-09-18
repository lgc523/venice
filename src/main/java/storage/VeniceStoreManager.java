package storage;

import message.VeniceMessage;
import metadata.KeyCache;
import org.apache.log4j.Logger;
import venice.VeniceClient;

import java.util.Map;
import java.util.HashMap;

/**
 * A singleton class for managing storage nodes and their locations
 * Created by clfung on 9/17/14.
 */
public class VeniceStoreManager {

  static final Logger logger = Logger.getLogger(VeniceStoreNode.class.getName());

  private static VeniceStoreManager instance = null;
  private Map<Integer, VeniceStoreNode> storeNodeMap = null;
  private static int nodeCount;
  private static KeyCache keyCache;

  /* Constructor: Cannot externally instantiate a singleton */
  private VeniceStoreManager() {
    storeNodeMap = new HashMap<Integer, VeniceStoreNode>();
    nodeCount = 0;
    keyCache = KeyCache.getInstance();
  }

  /*
   * Return the instance of the VeniceStoreManager
   * */
  public static synchronized VeniceStoreManager getInstance() {

    if (null == instance) {
      instance = new VeniceStoreManager();
    }

    return instance;

  }

  /**
   * Creates a new node in the registry
   * @param node - The storage node to be registered
   * */
  public synchronized void registerNode(VeniceStoreNode node) {
    nodeCount++;
    storeNodeMap.put(nodeCount, node);
  }

  /**
   * Returns a value from the storage
   * @param key - the key for the KV pair
   */
  public Object getValue(String key) {

    int nodeId = keyCache.getKeyAddress(key).getPartitionId();

    if (!storeNodeMap.containsKey(nodeId)) {
      logger.error("Cannot find node id: " + nodeId);
      return null;
    }

    return storeNodeMap.get(nodeId).get(key);

  }

  /**
   * Returns a value from the storage
   * @param key - the key for the KV pair
   * @param msg - A VeniceMessage to be added to storage
   */
  public void storeValue(String key, VeniceMessage msg) {

    int nodeId = keyCache.getKeyAddress(key).getPartitionId();

    if (!storeNodeMap.containsKey(nodeId)) {
      //registerNode(new InMemoryStoreNode());
      logger.error("Cannot find node id: " + nodeId);
    }

    switch(msg.getOperationType()) {

      // adding new values
      case PUT:
        storeNodeMap.get(nodeId).put(VeniceClient.TEST_KEY, msg.getPayload());
        break;

      // deleting values
      case DELETE:
        break;

      // partial update
      case PARTIAL_PUT:
        break;

      // error
      default:
        logger.error("Invalid operation type submitted: " + msg.getOperationType());
        break;
    }

  }

}
