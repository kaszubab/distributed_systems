package DB;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.sql.ResultSet;
import java.util.Set;

public class DatabaseWriteActor extends AbstractBehavior<DatabaseWriteActor.Command> {


    public interface Command {}


    public static Behavior<DatabaseWriteActor.Command> create(
            Set<Integer> satelliteIds,
            DatabaseService databaseService
//            ActorRef<WriteResponse> replyTo
    ) {
        return Behaviors.setup(context -> new DatabaseWriteActor(satelliteIds, databaseService, context));
    }


    private DatabaseWriteActor(
            Set<Integer> satelliteIDs,
            DatabaseService databaseService,
//            ActorRef<WriteResponse> replyTo,
            ActorContext<DatabaseWriteActor.Command> context
    ) {
        super(context);
        satelliteIDs.forEach(id -> {
            ResultSet rs = databaseService.executeStatement(String.format("Select * from Satellites where SatelliteID = %d", id));
            try{
                while (rs.next()) {
                    String line = rs.getString(2);
                    String sql = String.format("UPDATE Satellites SET errorCount = %d WHERE SatelliteID = %d; ", Integer.parseInt(line) + 1, id);

                    databaseService.manipulateData(sql);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Behaviors.stopped();
    }

    @Override
    public Receive<DatabaseWriteActor.Command> createReceive() {
        return newReceiveBuilder().build();
    }
}
