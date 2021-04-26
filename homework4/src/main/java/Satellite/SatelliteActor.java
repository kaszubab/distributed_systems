package Satellite;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class SatelliteActor extends AbstractBehavior<SatelliteActor.SatelliteCommand> {

    public interface SatelliteCommand {}

    public static final class StatusRequest implements SatelliteCommand {
        final ActorRef<StatusResponse> replyTo;
        final String requestId;

        public StatusRequest(String requestId, ActorRef<StatusResponse> replyTo) {
            this.replyTo = replyTo;
            this.requestId = requestId;
        }
    }

    public static final class StatusResponse implements SatelliteCommand {
        final SatelliteAPI.Status status;
        final int satelliteId;
        final String requestId;

        public StatusResponse(String requestId, SatelliteAPI.Status status, int satelliteId) {
            this.status = status;
            this.satelliteId = satelliteId;
            this.requestId = requestId;
        }
    }

    private final int satelliteId;

    public static Behavior<SatelliteCommand> create(int satelliteId) {
        return Behaviors.setup(context -> new SatelliteActor(satelliteId, context));
    }

    private SatelliteActor(int satelliteId, ActorContext<SatelliteCommand> context) {
        super(context);
        this.satelliteId = satelliteId;
    }

    @Override
    public Receive<SatelliteCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(StatusRequest.class, this::onStatusRequest)
                .build();
    }

    private Behavior<SatelliteCommand> onStatusRequest(StatusRequest request) {
        request.replyTo.tell(new StatusResponse(request.requestId, SatelliteAPI.getStatus(satelliteId), this.satelliteId));
        return this;
    }
}
