package com.kanapa4.shop.order.adapter;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
public class OrderItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private OrderEntity order;

    @Column(nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private String productName;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount",
                    column = @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)),
            @AttributeOverride(name = "currency",
                    column = @Column(name = "currency", nullable = false, length = 3)),
            @AttributeOverride(name = "vatRate",
                    column = @Column(name = "vat_rate", nullable = false, length = 16))
    })
    private PriceEmbeddable unitPrice;

    @Column(nullable = false)
    private int quantity;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<SelectedOptionJson> selectedOptions = new ArrayList<>();
}
