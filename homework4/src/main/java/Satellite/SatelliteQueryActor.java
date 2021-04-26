package Satellite;

import DB.DatabaseQueryActor;
import DB.DatabaseService;
import DB.DatabaseWriteActor;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

import javax.xml.crypto.Data;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SatelliteQueryActor extends AbstractBehavior<SatelliteQueryActor.SatelliteQueryCommand> {

    public interface SatelliteQueryCommand {}

    private static final class SatelliteTimeout implements SatelliteQueryCommand {
        final int satelliteId;

        public SatelliteTimeout(int satelliteId) {
            this.satelliteId = satelliteId;
        }
    }

    public static final class SatelliteQueryResponse implements SatelliteQueryCommand {
        public final String queryId;
        public final Map<Integer, SatelliteAPI.Status> statuses;
        public final double percentage;

        public SatelliteQueryResponse(String queryId, Map<Integer, SatelliteAPI.Status> statuses, double percentage)
        {
            this.queryId = queryId;
            this.statuses = statuses;
            this.percentage = percentage;
        }
    }

    public static final class WrappedStatusResponse implements SatelliteQueryCommand {
        final SatelliteActor.StatusResponse response;

        public WrappedStatusResponse(SatelliteActor.StatusResponse response)
        {
            this.response = response;
        }
    }

//    public static final class WrappedWriteResponse implements SatelliteQueryCommand {
//        final SatelliteActor.StatusResponse response;
//
//        public WrappedWriteResponse(SatelliteActor.StatusResponse response)
//        {
//            this.response = response;
//        }
//    }

    public static Behavior<SatelliteQueryCommand> create(
            String queryId,
            Map<Integer, ActorRef<SatelliteActor.SatelliteCommand>> satellites,
            int timeout,
            ActorRef<SatelliteQueryResponse> replyTo,
            DatabaseService databaseService
    ) {
        return Behaviors.setup(context -> Behaviors.withTimers(
                timers ->
                        new SatelliteQueryActor(
                                timers, context, queryId, satellites, timeout, replyTo, databaseService)
                )
        );
    }

    private String queryId;
    private Map<Integer, ActorRef<SatelliteActor.SatelliteCommand>> satellites;
    private Map<Integer, SatelliteAPI.Status> satellitesErrors;
    private Set<Integer> satellitesTimeouts;
    private Set<Integer> satellitesThatResponded;
    private int timeout;
    private ActorRef<SatelliteQueryResponse> replyTo;
    private DatabaseService databaseService;

    private SatelliteQueryActor(
            TimerScheduler<SatelliteQueryCommand> timers,
            ActorContext<SatelliteQueryCommand> context,
            String queryId,
            Map<Integer, ActorRef<SatelliteActor.SatelliteCommand>> satellites,
            int timeout,
            ActorRef<SatelliteQueryResponse> replyTo,
            DatabaseService databaseService
    ) {
        super(context);
        this.queryId = queryId;
        this.satellites = satellites;
        this.timeout = timeout;
        this.replyTo = replyTo;
        this.satellitesErrors = new HashMap<>();
        this.satellitesThatResponded = new HashSet<>();
        this.satellitesTimeouts = new HashSet<>();
        this.databaseService = databaseService;



        ActorRef<SatelliteActor.StatusResponse> responseAdapter =
                context.messageAdapter(SatelliteActor.StatusResponse.class, WrappedStatusResponse::new);

//        ActorRef<SatelliteActor.StatusResponse> databaseWriterAdapter =
//                context.messageAdapter(DatabaseWriteActor.WriteResponse.class, WrappedWriteResponse::new);

        satellites.forEach((satelliteId, satellite) -> {
            timers.startSingleTimer(new SatelliteTimeout(satelliteId), Duration.ofMillis(timeout));
            satellite.tell(new SatelliteActor.StatusRequest(queryId, responseAdapter));
        });


    }

    private Behavior<SatelliteQueryCommand> respondWhenAllCollected() {
        if(satellitesThatResponded.size() + satellitesTimeouts.size() == satellites.size()) {
            replyTo.tell(new SatelliteQueryResponse(queryId, satellitesErrors, 1.0 * satellitesThatResponded.size() / satellites.size()));
            getContext()
                    .spawnAnonymous(
                            DatabaseWriteActor.create(satellitesErrors.keySet(), databaseService));

            return Behaviors.stopped();
        }
        return this;
    }

    @Override
    public Receive<SatelliteQueryCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(WrappedStatusResponse.class, this::onWrappedStatusResponse)
                .onMessage(SatelliteTimeout.class, this::onSatelliteTimeout)
                .build();
    }

    private Behavior<SatelliteQueryCommand> onWrappedStatusResponse(WrappedStatusResponse response) {
        if(satellitesTimeouts.contains(response.response.satelliteId)) return respondWhenAllCollected();
        satellitesThatResponded.add(response.response.satelliteId);
        if(response.response.status != SatelliteAPI.Status.OK) {
            satellitesErrors.put(response.response.satelliteId, response.response.status);
        }
        return respondWhenAllCollected();
    }

    private Behavior<SatelliteQueryCommand> onSatelliteTimeout(SatelliteTimeout timeout) {
        if(!satellitesThatResponded.contains(timeout.satelliteId)) {
            satellitesTimeouts.add(timeout.satelliteId);
        }
        return respondWhenAllCollected();
    }

}
