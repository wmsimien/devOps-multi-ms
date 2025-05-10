package com.delivery.ms.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;

import org.springframework.stereotype.Controller;

import com.delivery.ms.dto.CustomerOrder;
import com.delivery.ms.dto.DeliveryEvent;
import com.delivery.ms.entity.Delivery;
import com.delivery.ms.entity.DeliveryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
public class DeliveryController {

	Logger logger = LoggerFactory.getLogger(DeliveryController.class);

	@Autowired
	private DeliveryRepository repository;

	@Autowired
	private KafkaTemplate<String, DeliveryEvent> kafkaTemplate;

	@KafkaListener(topics = "new-stock", groupId = "stock-group")
	public void deliverOrder(String event) throws JsonMappingException, JsonProcessingException {
//		System.out.println("Inside ship order for order "+event);
		logger.info("Inside ship order for order "+event);
		Delivery shipment = new Delivery();
		DeliveryEvent inventoryEvent = new ObjectMapper().readValue(event, DeliveryEvent.class);
		CustomerOrder order = inventoryEvent.getOrder();

		try {
			if (order.getAddress() == null) {
				logger.info("Order address is null");
				throw new Exception("Address not present");
			}

			shipment.setAddress(order.getAddress());
			shipment.setOrderId(order.getOrderId());

			shipment.setStatus("success");

			repository.save(shipment);
			logger.info("Order shipped");
		} catch (Exception e) {
			shipment.setOrderId(order.getOrderId());
			shipment.setStatus("failed");
			repository.save(shipment);
			logger.info("Order failed to ship {}", order.getOrderId());
			System.out.println(order);

			DeliveryEvent reverseEvent = new DeliveryEvent();
			reverseEvent.setType("STOCK_REVERSED");
			reverseEvent.setOrder(order);
			logger.info("Order reversed {}", order);
			kafkaTemplate.send("reversed-stock", reverseEvent);
		}
	}
}
