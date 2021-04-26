import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.Terminated;
import akka.actor.typed.javadsl.Behaviors;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.File;

public class Main {
    public static Behavior<Void> create() {
        return Behaviors.setup(
                context -> {
                    // create text service
                    ActorRef<Dispatcher.DispatcherCommand> dispatcher = context.spawn(Dispatcher.create(), "dispatcher");

                    // create workers (they will register by themselves)
                    context.spawn(MonitoringStation.create(100, 50, 1500, dispatcher, "Alfa"), "monitoring_station_1");
                    context.spawn(MonitoringStation.create(100, 50, 1500, dispatcher, "Beta"), "monitoring_station_2");
                    context.spawn(MonitoringStation.create(100, 50, 1500, dispatcher, "Gamma"), "monitoring_station_3");

                    return Behaviors.receive(Void.class)
                            .onSignal(Terminated.class, sig -> Behaviors.stopped())
                            .build();
                });
    }

    public static void main(String[] args) {
        File configFile = new File("src/main/nodeA.conf");
        Config config = ConfigFactory.parseFile(configFile);

        ActorSystem.create(Main.create(), "main", config);
    }
}
