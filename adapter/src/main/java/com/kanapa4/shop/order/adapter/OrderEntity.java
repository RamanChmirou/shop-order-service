package com.kanapa4.shop.order.adapter;

import com.kanapa4.shop.order.model.DeliveryMethod;
import com.kanapa4.shop.order.model.OrderStatus;
import com.kanapa4.shop.order.model.PaymentMethod;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 32)
    private String orderNumber;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OrderStatus status = OrderStatus.NEW;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DeliveryMethod deliveryMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentMethod paymentMethod;

    @Embedded
    private CustomerEmbeddable customer;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "street", column = @Column(name = "ship_street")),
            @AttributeOverride(name = "city", column = @Column(name = "ship_city")),
            @AttributeOverride(name = "postalCode", column = @Column(name = "ship_postal_code")),
            @AttributeOverride(name = "country", column = @Column(name = "ship_country"))
    })
    private AddressEmbeddable shippingAddress;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "street", column = @Column(name = "bill_street")),
            @AttributeOverride(name = "city", column = @Column(name = "bill_city")),
            @AttributeOverride(name = "postalCode", column = @Column(name = "bill_postal_code")),
            @AttributeOverride(name = "country", column = @Column(name = "bill_country"))
    })
    private AddressEmbeddable billingAddress;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal itemsTotal;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount",
                    column = @Column(name = "delivery_cost", nullable = false, precision = 19, scale = 2)),
            @AttributeOverride(name = "currency",
                    column = @Column(name = "delivery_currency", nullable = false, length = 3)),
            @AttributeOverride(name = "vatRate",
                    column = @Column(name = "delivery_vat_rate", nullable = false, length = 16))
    })
    private PriceEmbeddable deliveryCost;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal grandTotal;

    @Column(nullable = false, length = 3)
    private String currency;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemEntity> items = new ArrayList<>();

    @Version
    private Long version;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public void addItem(OrderItemEntity item) {
        items.add(item);
        item.setOrder(this);
    }
}
