import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Admin {

    public static void main(String[] args) throws Exception {

        System.out.println("Admin");

        // connection & channel
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        String ADMIN_MESSAGES = "Admin_messages";
        channel.exchangeDeclare(ADMIN_MESSAGES, BuiltinExchangeType.DIRECT);

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                System.out.println("Received message " + message);
                channel.basicAck(envelope.getDeliveryTag(), false);
            }
        };

        String ORDERS = "Orders";
        channel.exchangeDeclare(ORDERS, BuiltinExchangeType.TOPIC);

        String ordersQueueName = channel.queueDeclare().getQueue();
        channel.queueBind(ordersQueueName, ORDERS, "#");
        System.out.println("created queue: " + ordersQueueName);
        channel.basicConsume(ordersQueueName, false, consumer);

        String RESPONSES = "Responses";
        channel.exchangeDeclare(RESPONSES, BuiltinExchangeType.TOPIC);

        String responsesQueueName = channel.queueDeclare().getQueue();
        channel.queueBind(responsesQueueName, RESPONSES, "#");
        System.out.println("created queue: " + responsesQueueName);
        channel.basicConsume(responsesQueueName, false, consumer);


        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("Enter admin message - exit ends messaging");
            String message = scanner.next();

            if(message.equals("exit")) {
                System.exit(0);
            }

            System.out.println("Enter receiver group:\n - provider for providers \n - group for groups\n - all for everyone");
            String receiverKey = scanner.next().strip();
            System.out.println("Sending message" + message + " with key " + receiverKey);

            channel.basicPublish(ADMIN_MESSAGES, receiverKey, null, message.getBytes(StandardCharsets.UTF_8));

        }


    }
}
