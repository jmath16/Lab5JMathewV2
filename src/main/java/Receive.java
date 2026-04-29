import com.rabbitmq.client.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class Receive {

    private final static String QUEUE_NAME = "hmacQueue";

    public static void main(String[] args) throws Exception {

        String key = "secret_key";

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(QUEUE_NAME, false, false, false, null);

        System.out.println("Waiting for messages from RabbitMQ...");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            try {
                String received = new String(delivery.getBody());

                String[] parts = received.split("\\|");
                String receivedMessage = parts[0];
                String receivedHmac = parts[1];

                Mac mac = Mac.getInstance("HmacSHA256");
                SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "HmacSHA256");
                mac.init(keySpec);

                byte[] rawHmac = mac.doFinal(receivedMessage.getBytes());
                String generatedHmac = Base64.getEncoder().encodeToString(rawHmac);

                System.out.println("Message received: " + receivedMessage);
                System.out.println("HMAC received: " + receivedHmac);
                System.out.println("HMAC generated: " + generatedHmac);

                if (generatedHmac.equals(receivedHmac)) {
                    System.out.println("Integrity check passed. Message was not changed.");
                } else {
                    System.out.println("Integrity check failed. Message was changed.");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> { });
    }
}