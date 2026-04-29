import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class HMACSend {

    private final static String QUEUE_NAME = "hmacQueue";

    public static void main(String[] args) throws Exception {

        String key = "secret_key";
        String message = "hello world";

        // 🔐 Generate HMAC
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "HmacSHA256");
        mac.init(keySpec);

        byte[] rawHmac = mac.doFinal(message.getBytes());
        String sentHmac = Base64.getEncoder().encodeToString(rawHmac);

        // Combine message + HMAC
        String fullMessage = message + "|" + sentHmac;

        // 📡 Send using RabbitMQ
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            channel.basicPublish("", QUEUE_NAME, null, fullMessage.getBytes());

            System.out.println("Message sent: " + message);
            System.out.println("HMAC sent: " + sentHmac);
            System.out.println("Sent through RabbitMQ queue: " + QUEUE_NAME);
        }
    }
}