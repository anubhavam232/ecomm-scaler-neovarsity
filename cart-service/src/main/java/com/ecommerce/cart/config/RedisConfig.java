package com.ecommerce.cart.config;

import com.ecommerce.cart.document.Cart;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Cart> cartRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Cart> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serialization for the keys
        template.setKeySerializer(new StringRedisSerializer());

        // Use JSON serialization for the Cart object values
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(Cart.class));

        return template;
    }
}