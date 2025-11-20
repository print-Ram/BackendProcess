package com.choco.home.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Service
@EnableScheduling
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // Store delivered orders to send review reminders later
    private final ConcurrentHashMap<String, LocalDateTime> deliveredOrders = new ConcurrentHashMap<>();

    public void sendOrderStatusEmail(String to, String orderId, String status) {
        sendOrderStatusEmail(to, orderId, status, null);
    }

    public void sendOrderStatusEmail(String to, String orderId, String status, String shippingDetails) {
        String subject = "Your Order #" + orderId + " Status Update";

        String body = switch (status) {
            case "CONFIRMED" -> """
                    Hello,

                    We're happy to inform you that your order has been confirmed successfully.
                    Thank you for your payment.

                    Order ID: """ + orderId + """

                    We'll notify you when it's shipped.

                    Regards,
                    Krishva Homemade Team
                    """;

            case "SHIPPED" -> """
                    Hello,

                    Your order has been shipped and is on its way!

                    Order ID: """ + orderId + """

                    Shipping Details:
                    """ + shippingDetails + """

                    Thank you for shopping with us.

                    Regards,
                    Krishva Homemade Team
                    """;

            case "DELIVERED" -> {
                // Save delivery timestamp for review reminder
                deliveredOrders.put(orderId + ":" + to, LocalDateTime.now());
                yield """
                    Hello,

                    Your order has been delivered successfully.

                    Order ID: """ + orderId + """

                    We hope you enjoyed the service. Feel free to reach out if you need anything further.

                    Regards,
                    Krishva Homemade Team
                    """;
            }

            case "CANCELLED" -> """
                    Hello,

                    Unfortunately, your order has been cancelled.

                    Order ID: """ + orderId + """

                    If you have any questions or need support, feel free to contact us.

                    Regards,
                    Krishva Homemade Team
                    """;

            default -> """
                    Hello,

                    Your order status has been updated.

                    Order ID: """ + orderId + """
                    Current Status: """ + status + """

                    Thank you for choosing Homemade.

                    Regards,
                    Krishva Homemade Team
                    """;
        };

        // Send to customer
        sendMail(to, subject, body);

        // Send to admin
        String adminBody = """
                Admin Alert:

                An order has been updated.

                User: """ + to + """
                Order ID: """ + orderId + """
                Status: """ + status + (shippingDetails != null ? """

                Shipping Details:
                """ + shippingDetails : "") + """

                -- End of Update --
                """;
        sendMail("tejuveeduluri@gmail.com", "Order Status Update: #" + orderId + " - " + status, adminBody);
    }

    private void sendMail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    /**
     * Scheduled task runs every 6 hours to check for orders delivered 2–3 days ago.
     */
    @Scheduled(fixedRate = 6 * 60 * 60 * 1000) // every 6 hours
    public void sendReviewReminders() {
        LocalDateTime now = LocalDateTime.now();
        deliveredOrders.forEach((key, deliveryTime) -> {
            if (deliveryTime.plusDays(2).isBefore(now) && deliveryTime.plusDays(3).isAfter(now)) {
                String[] parts = key.split(":");
                String orderId = parts[0];
                String email = parts[1];
                sendReviewRequest(email, orderId);
                deliveredOrders.remove(key); // Remove after sending reminder
            }
        });
    }

    private void sendReviewRequest(String to, String orderId) {
        String subject = "Share your feedback on your recent order!";
        String body = """
                Hello,

                Hope you're enjoying your recent purchase from Krishva Homemade! 🍫

                We'd love to hear your thoughts. Please take a moment to rate or review your product.

                Order ID: """ + orderId + """

                Click here to review: https://yourwebsite.com/review/""" + orderId + """

                Your feedback helps us serve you better 💖

                Regards,
                Krishva Homemade Team
                """;
        sendMail(to, subject, body);
    }
}
