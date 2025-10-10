package notify.global.config.kafka;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    // ───────────────────────────────────────────────
    // 필수 환경변수/프로퍼티 (카프카 호환 클러스터)
    // ───────────────────────────────────────────────
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;  // 예: bootstrap-xxxx.kafka.ap-chuncheon-1.oci.oraclecloud.com:9092

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;           // 예: notify-group

    @Value("${KAFKA_USERNAME}")
    private String kafkaUsername;     // OCI 콘솔의 Kafka Cluster Users에서 발급한 사용자명

    @Value("${KAFKA_PASSWORD}")
    private String kafkaPassword;     // 위 사용자 비밀번호

    @Value("${app.kafka.consumer.concurrency:2}")
    private Integer consumerConcurrency;

    // ───────────────────────────────────────────────
    // 공통 보안/네트워킹 프로퍼티 (SASL_SSL + SCRAM-SHA-512)
    // ───────────────────────────────────────────────
    private Map<String, Object> commonSecurityProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        props.put(SaslConfigs.SASL_MECHANISM, "SCRAM-SHA-512");
        props.put(SaslConfigs.SASL_JAAS_CONFIG,
                "org.apache.kafka.common.security.scram.ScramLoginModule required " +
                        "username=\"" + kafkaUsername + "\" password=\"" + kafkaPassword + "\";");
        // 서버 인증서 호스트명 검증(기본 https) - JVM 기본 CA 신뢰 사용하는 경우 그대로 OK
        props.put("ssl.endpoint.identification.algorithm", "https");
        // 네트워크 안정성
        props.put(CommonClientConfigs.REQUEST_TIMEOUT_MS_CONFIG, 30_000);
        props.put(CommonClientConfigs.RECONNECT_BACKOFF_MS_CONFIG, 500L);
        props.put(CommonClientConfigs.RECONNECT_BACKOFF_MAX_MS_CONFIG, 10_000L);
        props.put(CommonClientConfigs.RETRY_BACKOFF_MS_CONFIG, 500L);
        return props;
    }

    // ───────────────────────────────────────────────
    // 1) ConsumerFactory
    // ───────────────────────────────────────────────
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>(commonSecurityProps());
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // 오프셋/세션
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // 수동 커밋
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300_000);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30_000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3_000);

        // 연결 유지
        props.put(ConsumerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, 540_000);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    // ───────────────────────────────────────────────
    // 2) Listener Container Factory
    // ───────────────────────────────────────────────
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> f = new ConcurrentKafkaListenerContainerFactory<>();
        f.setConsumerFactory(consumerFactory());

        // 수동 ACK
        f.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // 재시도/백오프 에러 핸들러 (DLT 사용 전까지 간단 안정화)
        ExponentialBackOff backOff = new ExponentialBackOff(1_000, 2.0);
        backOff.setMaxElapsedTime(30_000);
        f.setCommonErrorHandler(new DefaultErrorHandler(backOff));

        // 동시성
        f.setConcurrency(consumerConcurrency);
        return f;
    }

    // ───────────────────────────────────────────────
    // 3) ProducerFactory
    // ───────────────────────────────────────────────
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> props = new HashMap<>(commonSecurityProps());
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // 신뢰성/성능
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.RETRIES_CONFIG, 10);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5); // idempotent와 호환
        props.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 32_768);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");

        // 타임아웃
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30_000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120_000);
        props.put(ProducerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, 540_000);
        return new DefaultKafkaProducerFactory<>(props);
    }

    // ───────────────────────────────────────────────
    // 4) KafkaTemplate
    // ───────────────────────────────────────────────
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
