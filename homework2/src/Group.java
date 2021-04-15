import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Group {

    public static void main(String[] args) throws Exception {

        System.out.println("Group");

        // connection & channel
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        //  (message handling)
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                String typeOfOrder = message.split(" ")[0];
                String orderNumber = message.split(" ")[1];
                System.out.println("Received response for " + typeOfOrder + " with number " + orderNumber);

                channel.basicAck(envelope.getDeliveryTag(), false);
            }
        };

        String RESPONSES = "Responses";
        channel.exchangeDeclare(RESPONSES, BuiltinExchangeType.TOPIC);

        Scanner scanner = new Scanner(System.in);

        System.out.println("Enter name of the group");
        String groupName = scanner.next();
        String queueName = channel.queueDeclare().getQueue();

        channel.queueBind(queueName, RESPONSES, groupName);
        System.out.println("created queue: " + queueName);
        channel.basicConsume(queueName, false, consumer);

        String ADMIN_MESSAGES = "Admin_messages";
        channel.exchangeDeclare(ADMIN_MESSAGES, BuiltinExchangeType.DIRECT);

        String adminQueueName = channel.queueDeclare().getQueue();
        channel.queueBind(adminQueueName, ADMIN_MESSAGES, "group");
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

        for(String product: args) {
            channel.basicPublish(ORDERS, groupName + "." + product, null, "".getBytes(StandardCharsets.UTF_8));
        }


    }
}
