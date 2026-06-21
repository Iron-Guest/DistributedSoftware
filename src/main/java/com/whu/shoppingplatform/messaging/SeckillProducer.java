package com.whu.shoppingplatform.messaging;

import com.whu.shoppingplatform.dto.SeckillMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class SeckillProducer {

    private static final Logger log = LoggerFactory.getLogger(SeckillProducer.class);
    private static final String TOPIC = "seckill-order";

    private final KafkaTemplate<String, SeckillMessage> kafkaTemplate;

    public SeckillProducer(KafkaTemplate<String, SeckillMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendSeckillMessage(SeckillMessage message) {
        CompletableFuture<SendResult<String, SeckillMessage>> future =
                kafkaTemplate.send(TOPIC, String.valueOf(message.getGoodsId()), message);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send seckill message: orderNo={}, goodsId={}, userId={}",
                        message.getOrderNo(), message.getGoodsId(), message.getUserId(), ex);
            } else {
                log.info("Seckill message sent: orderNo={}, partition={}, offset={}",
                        message.getOrderNo(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}