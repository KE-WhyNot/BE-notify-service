package notify.domain.notify.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;
import notify.global.common.BaseEntity;
import org.hibernate.annotations.Comment;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {
    @Column(nullable = false, length = 50)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String message;

    @Column(name = "read_flag", nullable = false)
    @Builder.Default
    private boolean read = false;

    // 타입별 확장 필드
    @Column(name = "rank_no")
    @Comment("RANKING")
    private Integer rank;

    @Comment("공통 종목 식별")
    private String stockCode;

    @Comment("DIVIDEND/TRADE")
    private String stockName;

    @Comment("TRADE only")
    @Enumerated(EnumType.STRING)
    private TradeSide side;     // BUY/SELL

    @Comment("TRADE only")
    private Integer quantity;

    @Comment("TRADE only (체결가)")
    private Long price;

    @Comment("TRADE only")
    private String orderId;

    @Comment("TRADE only")
    private Instant filledAt;


    @Comment("DIVIDEND only")
    private Integer dividendAmount;

    @Comment("DIVIDEND only")
    private LocalDateTime paymentDate;

    public void markRead() { this.read = true; }


}
