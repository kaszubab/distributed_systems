package DB;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.*;

import java.sql.ResultSet;

public class DatabaseQueryActor extends AbstractBehavior<DatabaseQueryActor.Command> {

    public interface Command {}

    public static final class DatabaseResponse implements Command {
        public final int satelliteId;
        public final int errorCount;

        public DatabaseResponse(int satelliteId, int errorCount) {
            this.satelliteId = satelliteId;
            this.errorCount = errorCount;
        }
    }


    public static Behavior<Command> create(
            ActorRef<DatabaseResponse> replyTo,
            int satelliteId,
            DatabaseService databaseService
    ) {
        return Behaviors.setup(context -> new DatabaseQueryActor(replyTo, satelliteId, context, databaseService));
    }


    private DatabaseQueryActor(
            ActorRef<DatabaseResponse> replyTo,
            int satelliteID,
            ActorContext<Command> context,
            DatabaseService databaseService
    ) {
        super(context);
        ResultSet rs = databaseService.executeStatement(String.format("Select * from Satellites where SatelliteID = %d", satelliteID));
        try{
            while (rs.next()) {
                String line = rs.getString(2);
                replyTo.tell(new DatabaseResponse(satelliteID, Integer.parseInt(line)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Behaviors.stopped();
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder().build();
    }
}
