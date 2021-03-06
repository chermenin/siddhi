/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.siddhi.core.query.output.ratelimit.snapshot;

import io.siddhi.core.config.SiddhiQueryContext;
import io.siddhi.core.event.ComplexEvent;
import io.siddhi.core.event.ComplexEventChunk;
import io.siddhi.core.event.GroupedComplexEvent;
import io.siddhi.core.event.stream.StreamEventFactory;
import io.siddhi.core.util.Scheduler;
import io.siddhi.core.util.parser.SchedulerParser;
import io.siddhi.core.util.snapshot.state.State;
import io.siddhi.core.util.snapshot.state.StateFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link PerSnapshotOutputRateLimiter} for queries with Windows.
 */
public class WindowedPerSnapshotOutputRateLimiter
        extends SnapshotOutputRateLimiter<WindowedPerSnapshotOutputRateLimiter.RateLimiterState> {
    private final Long value;
    private Comparator comparator;
    private Scheduler scheduler;

    public WindowedPerSnapshotOutputRateLimiter(Long value,
                                                WrappedSnapshotOutputRateLimiter wrappedSnapshotOutputRateLimiter,
                                                boolean groupBy, SiddhiQueryContext siddhiQueryContext) {
        super(wrappedSnapshotOutputRateLimiter, siddhiQueryContext, groupBy);
        this.value = value;
        this.comparator = (Comparator<ComplexEvent>) (event1, event2) -> {
            if (Arrays.equals(event1.getOutputData(), event2.getOutputData())) {
                return 0;
            } else {
                return 1;
            }
        };
    }


    @Override
    protected StateFactory<RateLimiterState> init() {
        this.scheduler = SchedulerParser.parse(this, siddhiQueryContext);
        this.scheduler.setStreamEventFactory(new StreamEventFactory(0, 0, 0));
        this.scheduler.init(lockWrapper, siddhiQueryContext.getName());
        return () -> new RateLimiterState();
    }

    @Override
    public void process(ComplexEventChunk complexEventChunk) {
        List<ComplexEventChunk<ComplexEvent>> outputEventChunks = new ArrayList<ComplexEventChunk<ComplexEvent>>();
        complexEventChunk.reset();
        RateLimiterState state = stateHolder.getState();
        try {
            synchronized (state) {
                while (complexEventChunk.hasNext()) {
                    ComplexEvent event = complexEventChunk.next();
                    if (event instanceof GroupedComplexEvent) {
                        event = ((GroupedComplexEvent) event).getComplexEvent();
                    }
                    if (event.getType() == ComplexEvent.Type.TIMER) {
                        tryFlushEvents(outputEventChunks, event, state);
                    } else if (event.getType() == ComplexEvent.Type.CURRENT) {
                        complexEventChunk.remove();
                        tryFlushEvents(outputEventChunks, event, state);
                        state.eventList.add(event);
                    } else if (event.getType() == ComplexEvent.Type.EXPIRED) {
                        tryFlushEvents(outputEventChunks, event, state);
                        for (Iterator<ComplexEvent> iterator = state.eventList.iterator(); iterator.hasNext(); ) {
                            ComplexEvent currentEvent = iterator.next();
                            if (comparator.compare(currentEvent, event) == 0) {
                                iterator.remove();
                                break;
                            }
                        }
                    } else if (event.getType() == ComplexEvent.Type.RESET) {
                        tryFlushEvents(outputEventChunks, event, state);
                        state.eventList.clear();
                    }
                }
            }
        } finally {
            stateHolder.returnState(state);
        }
        for (ComplexEventChunk eventChunk : outputEventChunks) {
            sendToCallBacks(eventChunk);
        }
    }

    private void tryFlushEvents(List<ComplexEventChunk<ComplexEvent>> outputEventChunks, ComplexEvent event,
                                RateLimiterState state) {
        if (event.getTimestamp() >= state.scheduledTime) {
            ComplexEventChunk<ComplexEvent> outputEventChunk = new ComplexEventChunk<ComplexEvent>(false);
            for (ComplexEvent complexEvent : state.eventList) {
                outputEventChunk.add(cloneComplexEvent(complexEvent));
            }
            outputEventChunks.add(outputEventChunk);
            state.scheduledTime = state.scheduledTime + value;
            scheduler.notifyAt(state.scheduledTime);
        }
    }

    @Override
    public void partitionCreated() {
        RateLimiterState state = stateHolder.getState();
        try {
            synchronized (state) {
                long currentTime = System.currentTimeMillis();
                state.scheduledTime = currentTime + value;
                scheduler.notifyAt(state.scheduledTime);
            }
        } finally {
            stateHolder.returnState(state);
        }
    }

    class RateLimiterState extends State {

        private List<ComplexEvent> eventList = new LinkedList<>();
        private long scheduledTime;

        @Override
        public boolean canDestroy() {
            return eventList.isEmpty() && scheduledTime == 0;
        }

        @Override
        public Map<String, Object> snapshot() {
            Map<String, Object> state = new HashMap<>();
            state.put("EventList", eventList);
            state.put("ScheduledTime", scheduledTime);
            return state;
        }

        @Override
        public void restore(Map<String, Object> state) {
            eventList = (List<ComplexEvent>) state.get("EventList");
            scheduledTime = (Long) state.get("ScheduledTime");
        }
    }

}
