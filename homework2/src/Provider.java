import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Provider {

    private static int orderNumber = 0;
    private static String providerName = "";

    public static void main(String[] args) throws Exception {

        // info
        System.out.println("Provider");
        System.out.println("Enter unique provider name");
        Scanner scanner = new Scanner(System.in);
        providerName = scanner.next();

        // connection & channel
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.basicQos(1);

        String RESPONSES = "Responses";
        channel.exchangeDeclare(RESPONSES, BuiltinExchangeType.TOPIC);

        String ADMIN_MESSAGES = "Admin_messages";
        channel.exchangeDeclare(ADMIN_MESSAGES, BuiltinExchangeType.DIRECT);

        String adminQueueName = channel.queueDeclare().getQueue();
        channel.queueBind(adminQueueName, ADMIN_MESSAGES, "provider");
        channel.queueBind(adminQueueName, ADMIN_MESSAGES, "all");

        //  (admin message handling)
        Consumer adminConsumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                System.out.println("Received message " + message + " from admin ");
                channel.basicAck(envelope.getDeliveryTag(), false);
            }
        };

        channel.basicConsume(adminQueueName, false, adminConsumer);

        // exchange
        String ORDERS = "Orders";
        channel.exchangeDeclare(ORDERS, BuiltinExchangeType.TOPIC);

        //  (message handling)
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String groupName = envelope.getRoutingKey().split("\\.")[0];
                String typeOfOrder = envelope.getRoutingKey().split("\\.")[1];
                System.out.println("Handled order for " + typeOfOrder + " from group " + groupName);
                String orderNumber = getNextOrderNumber();
                System.out.println("Sending respose to " + groupName + " with order number " + orderNumber);
                String response = typeOfOrder + " " + orderNumber;
                channel.basicPublish(RESPONSES, groupName, null, response.getBytes(StandardCharsets.UTF_8));
                channel.basicAck(envelope.getDeliveryTag(), false);
            }
        };

        // queues
        for (String product: args) {
            channel.queueDeclare(product, false, false, false, null);
            channel.queueBind(product, ORDERS, "*." + product);
            System.out.println("created queue: " + product);
            channel.basicConsume(product, false, consumer);
        }


        // start listening
        System.out.println("Waiting for messages...");
    }

    private synchronized static String getNextOrderNumber() {
        orderNumber++;
        return providerName + "_" + orderNumber;
    }

}
