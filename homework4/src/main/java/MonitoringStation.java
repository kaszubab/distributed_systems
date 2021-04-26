import DB.DatabaseQueryActor;
import Satellite.SatelliteQueryActor;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class MonitoringStation extends AbstractBehavior<MonitoringStation.Command> {

    public interface Command {}

    public static final class Response implements Command {
        SatelliteQueryActor.SatelliteQueryResponse response;

        public Response(SatelliteQueryActor.SatelliteQueryResponse response) {
            this.response = response;
        }
    }

    public static final class DBResponse implements Command {
        DatabaseQueryActor.DatabaseResponse response;

        public DBResponse(DatabaseQueryActor.DatabaseResponse response) {
            this.response = response;
        }

    }


    public static Behavior<MonitoringStation.Command> create(
            int firstId,
            int range,
            int timeout,
            ActorRef<Dispatcher.DispatcherCommand> dispatcher,
            String name
    ) {
        return Behaviors.setup(context -> new MonitoringStation(firstId, range, timeout, name, dispatcher, context));
    }

    private final int firstId;
    private final int range;
    private final int timeout;
    private final String name;
    private int queryId = 0;

    private Map<String, Long> requests;

    private MonitoringStation(
            int firstId,
            int range,
            int timeout,
            String name,
            ActorRef<Dispatcher.DispatcherCommand> dispatcher,
            ActorContext<MonitoringStation.Command> context
    ) {
        super(context);
        this.firstId = firstId;
        this.range = range;
        this.timeout = timeout;
        this.name = name;

        ActorRef<SatelliteQueryActor.SatelliteQueryResponse> responseAdapter =
                context.messageAdapter(SatelliteQueryActor.SatelliteQueryResponse.class, MonitoringStation.Response::new);
        ActorRef<DatabaseQueryActor.DatabaseResponse> dbResponseAdapter =
                context.messageAdapter(DatabaseQueryActor.DatabaseResponse.class, MonitoringStation.DBResponse::new);
        Random random = new Random();

        requests = new HashMap<>();

        try{
            TimeUnit.SECONDS.sleep(3);
        } catch (Exception e) {
            e.printStackTrace();
        }

        long startTime = System.currentTimeMillis();
        requests.put(name + String.format("_%d", queryId), startTime);
        dispatcher.tell(new Dispatcher.SatelliteQuery(responseAdapter, name + String.format("_%d", queryId), firstId + random.nextInt(50), 50, timeout));
        queryId++;

        startTime = System.currentTimeMillis();
        requests.put(name + String.format("_%d", queryId), startTime);
        dispatcher.tell(new Dispatcher.SatelliteQuery(responseAdapter, name + String.format("_%d", queryId), firstId + random.nextInt(50), range, timeout));
        queryId++;

        try {
            if(name.equals("Alfa")) {
                TimeUnit.SECONDS.sleep(25);
                for(int i = 0; i < 100; i++)
                    dispatcher.tell(new Dispatcher.DatabaseQuery(i + 100, dbResponseAdapter));
            }


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public Receive<MonitoringStation.Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(MonitoringStation.Response.class, this::onResponse)
                .onMessage(MonitoringStation.DBResponse.class, this::onDBResponse)

                .build();
    }

    private Behavior<MonitoringStation.Command> onResponse(MonitoringStation.Response response) {
        long endTime = System.currentTimeMillis();
        System.out.println(response.response.queryId);
        long startTime = requests.get(response.response.queryId);
        System.out.println("Response percentage " + response.response.percentage);
        System.out.println(String.format("Time to answer in ms %d", endTime - startTime));
        System.out.println("Errors count " + response.response.statuses.size());

        response.response.statuses.forEach((satellite, status) -> System.out.println("SateliteID " + satellite + " error " + status));
        return this;
    }

    private Behavior<MonitoringStation.Command> onDBResponse(MonitoringStation.DBResponse response) {
        if(response.response.errorCount != 0)
            System.out.println("SatelliteID  " + response.response.satelliteId + " " + response.response.errorCount);
        return this;
    }



}
