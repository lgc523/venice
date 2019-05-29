package com.linkedin.venice.listener;

import com.linkedin.venice.utils.DaemonThreadFactory;
import com.linkedin.venice.utils.Utils;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import org.apache.log4j.Logger;


/**
 * This class provides the unified way to manager venice listener.
 *
 * @param <T> T should be a type of listener
 */
public class ListenerManager<T> {

  private ConcurrentMap<String, Set<T>> listenerMap;

  private ExecutorService threadPool;

  //TODO make thread couent and keepAlive time configurable.
  private int threadCount = 1;

  private static final Logger logger = Logger.getLogger(ListenerManager.class);

  public ListenerManager() {
    listenerMap = new ConcurrentHashMap<>();
    //TODO maybe we can share the thread pool with other use-cases.
    threadPool = Executors.newFixedThreadPool(threadCount, new DaemonThreadFactory("Venice-controller"));
  }

  public synchronized void subscribe(String key, T listener) {
    Set<T> set;
    if (!listenerMap.containsKey(key)) {
      set = new HashSet<>();
      listenerMap.put(key, set);
    } else {
      set = listenerMap.get(key);
    }
    set.add(listener);
  }

  public synchronized void unsubscribe(String key, T listener) {
    if (!listenerMap.containsKey(key)) {
      logger.debug("Not listeners are found for given key:" + key);
      return;
    } else {
      listenerMap.get(key).remove(listener);
      if (listenerMap.get(key).isEmpty()) {
        listenerMap.remove(key);
      }
    }
  }

  /**
   * Trigger notification and execute the given handler.
   *
   * @param key
   * @param handler The function really handle the event. It accepts listener and call the corresponding handle method
   *                of this listener.
   */
  public synchronized void trigger(String key, Function<T, Void> handler) {
    // Trigger listeners which registered with given key and wild char.
    trigger(listenerMap.get(key), handler);
    trigger(listenerMap.get(Utils.WILDCARD_MATCH_ANY), handler);
  }

  private void trigger(Set<T> listeners, Function<T, Void> handler) {
    if (listeners != null) {
      listeners.stream().forEach(listener -> threadPool.execute(() -> handler.apply(listener)));
    }
  }

  ConcurrentMap<String, Set<T>> getListenerMap() {
    return listenerMap;
  }
}