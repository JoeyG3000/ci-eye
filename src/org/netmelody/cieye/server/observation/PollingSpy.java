package org.netmelody.cieye.server.observation;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.netmelody.cieye.core.domain.Feature;
import org.netmelody.cieye.core.domain.TargetGroup;
import org.netmelody.cieye.core.observation.CiSpy;

import com.google.common.collect.MapMaker;

public final class PollingSpy implements CiSpy {

    private final CiSpy delegate;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private final ConcurrentMap<Feature, Long> trackedFeatures = new MapMaker()
                                                                    .expireAfterWrite(10, TimeUnit.MINUTES)
                                                                    .makeMap();
    
    private final ConcurrentMap<Feature, TargetGroup> statuses = new MapMaker().makeMap();
    
    public PollingSpy(CiSpy delegate) {
        this.delegate = delegate;
        executor.scheduleWithFixedDelay(new StatusUpdater(), 0L, 10L, TimeUnit.SECONDS);
    }
    
    @Override
    public TargetGroup statusOf(Feature feature) {
        trackedFeatures.put(feature, System.currentTimeMillis());
        return statuses.putIfAbsent(feature, new TargetGroup());
    }

    @Override
    public long millisecondsUntilNextUpdate(Feature feature) {
        return delegate.millisecondsUntilNextUpdate(feature);
    }

    @Override
    public boolean takeNoteOf(String targetId, String note) {
        return delegate.takeNoteOf(targetId, note);
    }
    
    private void update() {
        for (Feature feature : trackedFeatures.keySet()) {
            statuses.put(feature, delegate.statusOf(feature));
        }
    }
    
    private final class StatusUpdater implements Runnable {
        @Override
        public void run() {
            update();
        }
    }
}