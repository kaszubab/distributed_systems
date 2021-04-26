import DB.DatabaseQueryActor;
import DB.DatabaseService;
import Satellite.SatelliteActor;
import Satellite.SatelliteQueryActor;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Dispatcher extends AbstractBehavior<Dispatcher.DispatcherCommand> {

    public interface DispatcherCommand {}

    public static final class SatelliteQuery implements  DispatcherCommand, SatelliteQueryActor.SatelliteQueryCommand {
        final String queryId;
        final int firstSatId;
        final int range;
        final int timeout;
        final ActorRef<SatelliteQueryActor.SatelliteQueryResponse> replyTo;

        public SatelliteQuery(ActorRef<SatelliteQueryActor.SatelliteQueryResponse> replyTo, String queryId, int firstSatId, int range, int timeout) {
            this.queryId = queryId;
            this.firstSatId = firstSatId;
            this.range = range;
            this.timeout = timeout;
            this.replyTo = replyTo;
        }
    };

    public static final class DatabaseQuery implements DispatcherCommand {
        final int satelliteId;
        final ActorRef<DatabaseQueryActor.DatabaseResponse> replyTo;


        public DatabaseQuery(int satelliteId, ActorRef<DatabaseQueryActor.DatabaseResponse> replyTo) {
            this.satelliteId = satelliteId;
            this.replyTo = replyTo;
        }
    }

    private List<ActorRef<SatelliteActor.SatelliteCommand>> satellites;
    private DatabaseService databaseService;

    public static Behavior<DispatcherCommand> create() {
        return Behaviors.setup(Dispatcher::new);
    }

    private Dispatcher(ActorContext<Dispatcher.DispatcherCommand> context) {
        super(context);

        satellites = new LinkedList<>();

        IntStream.range(100, 200)
                .forEach(id -> satellites.add(context.spawn(SatelliteActor.create(id), String.format("Satellite_%d", id) + id)));

        databaseService = new DatabaseService(IntStream.range(100, 200).boxed().collect(Collectors.toList()));


    }

    @Override
    public Receive<DispatcherCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(SatelliteQuery.class, this::onSatelliteQuery)
                .onMessage(DatabaseQuery.class, this::onDatabaseQuery)
                .build();
    }


    private Behavior<DispatcherCommand> onSatelliteQuery(SatelliteQuery query) {
        Map<Integer, ActorRef<SatelliteActor.SatelliteCommand>> satellitesToQuery = new HashMap<>();
        for(int i = query.firstSatId; i < query.firstSatId + query.range; i++) {
            satellitesToQuery.put(i, satellites.get(i - 100));
        }
        getContext()
                .spawnAnonymous(
                        SatelliteQueryActor.create(query.queryId, satellitesToQuery, query.timeout, query.replyTo, databaseService));
        return this;
    }

    private Behavior<DispatcherCommand> onDatabaseQuery(DatabaseQuery query) {
        getContext()
                .spawnAnonymous(
                        DatabaseQueryActor.create(query.replyTo, query.satelliteId, databaseService));
        return this;
    }

}
