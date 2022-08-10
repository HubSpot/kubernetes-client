/**
 * Copyright (C) 2015 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.kubernetes.client.informers.cache;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.informers.ListerWatcher;
import io.fabric8.kubernetes.client.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class Reflector<T extends HasMetadata, L extends KubernetesResourceList<T>> {

  private static final Logger log = LoggerFactory.getLogger(Reflector.class);

  private volatile String lastSyncResourceVersion;
  private final Class<T> apiTypeClass;
  private final ListerWatcher<T, L> listerWatcher;
  private final SyncableStore<T> store;
  private final ReflectorWatcher watcher;
  private volatile boolean running;
  private volatile boolean watching;
  private final AtomicReference<Watch> watch;

  public Reflector(Class<T> apiTypeClass, ListerWatcher<T, L> listerWatcher, SyncableStore<T> store) {
    this.apiTypeClass = apiTypeClass;
    this.listerWatcher = listerWatcher;
    this.store = store;
    this.watcher = new ReflectorWatcher();
    this.watch = new AtomicReference<>(null);
  }

  public void stop() {
    running = false;
    stopWatcher();
  }

  private synchronized void stopWatcher() {
    Watch theWatch = watch.getAndSet(null);
    if (theWatch != null) {
      String ns = listerWatcher.getNamespace();
      log.debug("Stopping watcher for resource {} v{} in namespace {}", apiTypeClass, lastSyncResourceVersion, ns);
      theWatch.close();
      watchStopped(); // proactively report as stopped
    }
  }

  /**
   * <br>Starts the watch with a fresh store state.
   * <br>Should be called only at start and when HttpGone is seen.
   */
  public void listSyncAndWatch() {
    log.debug("Listing items for resource {}", apiTypeClass);
    running = true;
    KubernetesResourceList<T> result = null;
    String continueVal = null;
    Set<String> nextKeys = new LinkedHashSet<>();
    long listStartTimeNano = System.nanoTime();
    do {
      long chunkStartTimeNano = System.nanoTime();
      result = listerWatcher
          .list(new ListOptionsBuilder().withLimit(listerWatcher.getLimit()).withContinue(continueVal).withAllowWatchBookmarks(false).build());
      result.getItems().forEach(i -> {
        String key = store.getKey(i);
        // process the updates immediately so we don't need to hold the item
        store.update(i);
        nextKeys.add(key);
      });
      continueVal = result.getMetadata().getContinue();
      log.debug(
        "--- Listed chunk of {} items for resource {} in {}ms, result resource version: {}",
        result.getItems().size(),
        apiTypeClass,
        (System.nanoTime() - chunkStartTimeNano) * 1e-6,
        result.getMetadata().getResourceVersion()
      );
    } while (Utils.isNotNullOrEmpty(continueVal));
    long totalListTimeNano = System.nanoTime() - listStartTimeNano;
    
    store.retainAll(nextKeys);
    
    final String latestResourceVersion = result.getMetadata().getResourceVersion();
    lastSyncResourceVersion = latestResourceVersion;
    log.debug("Listed items ({}) for resource {} v{} in {}ms", nextKeys.size(), apiTypeClass, latestResourceVersion, totalListTimeNano * 1e-6);
    startWatcher(latestResourceVersion);
  }

  private synchronized void startWatcher(final String latestResourceVersion) {
    if (!running) {
        return;
    }
    log.debug("Starting watcher for resource {} v{}. Existing watch maybe: {}", apiTypeClass, latestResourceVersion, watch.get());
    // there's no need to stop the old watch, that will happen automatically when this call completes
    watch.set(
        listerWatcher.watch(new ListOptionsBuilder().withResourceVersion(latestResourceVersion)
            .withTimeoutSeconds(null)
            .withAllowWatchBookmarks(false)
            .build(), watcher));
    watching = true;
  }
  
  private synchronized void watchStopped() {
    watching = false;
  }

  public String getLastSyncResourceVersion() {
    return lastSyncResourceVersion;
  }
  
  public boolean isRunning() {
    return running;
  }
  
  public boolean isWatching() {
    return watching;
  }
  
  class ReflectorWatcher implements Watcher<T> {

    @Override
    public void eventReceived(Action action, T resource) {
      if (action == null) {
        log.error("Watcher {} v{} received null action, throwing Unrecognized event error", apiTypeClass, lastSyncResourceVersion);
        throw new KubernetesClientException("Unrecognized event");
      }
      if (resource == null) {
        log.error("Watcher {} v{} received null resource, throwing Unrecognized resource error", apiTypeClass, lastSyncResourceVersion);
        throw new KubernetesClientException("Unrecognized resource");  
      }
      if (log.isDebugEnabled()) {
        log.debug(
          "Event received {} {} {}: resourceVersion {}, resource: {}",
          action.name(),
          resource.getKind(),
          resource.getMetadata().getName(),
          resource.getMetadata().getResourceVersion(),
          resource
        );
      }
      switch (action) {
        case ERROR:
          log.error(
            "ERROR event received for watcher of {} v{}. Action: {}, resource: {}/{}, version: {}",
            apiTypeClass,
            lastSyncResourceVersion,
            action.name(),
            resource.getKind(),
            resource.getMetadata().getName(),
            resource.getMetadata().getResourceVersion()
          );
          throw new KubernetesClientException("ERROR event");
        case ADDED:
          store.add(resource);
          break;
        case MODIFIED:
          store.update(resource);
          break;
        case DELETED:
          store.delete(resource);
          break;
      }
      lastSyncResourceVersion = resource.getMetadata().getResourceVersion();
    }

    @Override
    public void onClose(WatcherException exception) {
      // this close was triggered by an exception,
      // not the user, it is expected that the watch retry will handle this
      boolean restarted = false;
      try {
        if (exception.isHttpGone()) {
          log.debug("Watch for {} v{} restarting due to http gone.", apiTypeClass, lastSyncResourceVersion);
          listSyncAndWatch();
          restarted = true;
        } else {
          log.warn("Watch for {} v{} closing with exception", apiTypeClass, lastSyncResourceVersion, exception);
          running = false; // shouldn't happen, but it means the watch won't restart
        }
      } finally {
        if (!restarted) {
          log.error("Watch for {} v{} wasn't restarted after an exception triggered the close", apiTypeClass, lastSyncResourceVersion);
          watchStopped(); // report the watch as stopped after a problem
        }
      }
    }

    @Override
    public void onClose() {
      watchStopped();
      log.debug("Watch for {} v{} gracefully closed", apiTypeClass, lastSyncResourceVersion);
    }

    @Override
    public boolean reconnecting() {
      return true;
    }
    
  }
  
  public ReflectorWatcher getWatcher() {
    return watcher;
  }

}
