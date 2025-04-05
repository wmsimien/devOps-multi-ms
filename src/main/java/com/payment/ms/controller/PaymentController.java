package com.payment.ms.controller;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payment.ms.dto.CustomerOrder;
import com.payment.ms.dto.OrderEvent;
import com.payment.ms.dto.PaymentEvent;
import com.payment.ms.entity.Payment;
import com.payment.ms.entity.PaymentRepository;


@Controller
public class PaymentController {

	Logger logger = LoggerFactory.getLogger(PaymentController.class);

	@Autowired
	private PaymentRepository repository;

	@Autowired
	private KafkaTemplate<String, PaymentEvent> kafkaTemplate;

	@Autowired
	private KafkaTemplate<String, OrderEvent> kafkaOrderTemplate;

	@KafkaListener(topics = "new-orders", groupId = "orders-group")
	public void processPayment(String event) throws JsonMappingException, JsonProcessingException {
//		System.out.println("Recieved event for payment " + event);
        logger.info("Received event for payment {}", event);
		OrderEvent orderEvent = new ObjectMapper().readValue(event, OrderEvent.class);

		CustomerOrder order = orderEvent.getOrder();
		Payment payment = new Payment();
		
		try {
			payment.setAmount(order.getAmount());
			payment.setMode(order.getPaymentMode());
			payment.setOrderId(order.getOrderId());
			payment.setStatus("SUCCESS");
			repository.save(payment);
			logger.info("Payment successfully saved");
			PaymentEvent paymentEvent = new PaymentEvent();
			paymentEvent.setOrder(orderEvent.getOrder());
			paymentEvent.setType("PAYMENT_CREATED");
			kafkaTemplate.send("new-payments", paymentEvent);
			logger.info("Payment created");
		} catch (Exception e) {
			payment.setOrderId(order.getOrderId());
			payment.setStatus("FAILED");
			repository.save(payment);
			logger.info("Payment failed");

			OrderEvent oe = new OrderEvent();
			oe.setOrder(order);
			oe.setType("ORDER_REVERSED");
			kafkaOrderTemplate.send("reversed-orders", orderEvent);
			logger.info("Order reverse successfully saved");
		}
	}
}
