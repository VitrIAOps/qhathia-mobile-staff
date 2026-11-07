package com.qhathia.mobile.staff.core.testing

import com.qhathia.mobile.staff.core.domain.model.PairedDevice
import com.qhathia.mobile.staff.core.domain.model.AuthTokens
import com.qhathia.mobile.staff.core.domain.model.Availability
import com.qhathia.mobile.staff.core.domain.model.Category
import com.qhathia.mobile.staff.core.domain.model.BrightnessMode
import com.qhathia.mobile.staff.core.domain.model.Device
import com.qhathia.mobile.staff.core.domain.model.DeviceSettings
import com.qhathia.mobile.staff.core.domain.model.DeviceStatus
import com.qhathia.mobile.staff.core.domain.model.MissingProductSuggestion
import com.qhathia.mobile.staff.core.domain.model.NewProduct
import com.qhathia.mobile.staff.core.domain.model.Order
import com.qhathia.mobile.staff.core.domain.model.OrderDetail
import com.qhathia.mobile.staff.core.domain.model.OrderItem
import com.qhathia.mobile.staff.core.domain.model.OrderStatus
import com.qhathia.mobile.staff.core.domain.model.Product
import com.qhathia.mobile.staff.core.domain.model.ProductImage
import com.qhathia.mobile.staff.core.domain.model.ProductDetail
import com.qhathia.mobile.staff.core.domain.model.ProductUpdate
import com.qhathia.mobile.staff.core.domain.model.RestockSuggestion
import com.qhathia.mobile.staff.core.domain.model.SalesBucket
import com.qhathia.mobile.staff.core.domain.model.SalesKpis
import com.qhathia.mobile.staff.core.domain.model.SalesPeriod
import com.qhathia.mobile.staff.core.domain.model.SalesRanking
import com.qhathia.mobile.staff.core.domain.model.SalesSummary
import com.qhathia.mobile.staff.core.domain.model.StalledProduct
import com.qhathia.mobile.staff.core.domain.model.StoreActivity
import com.qhathia.mobile.staff.core.domain.model.TopSellingProduct
import com.qhathia.mobile.staff.core.domain.model.SelectedImage
import com.qhathia.mobile.staff.core.domain.model.Session
import com.qhathia.mobile.staff.core.domain.model.StoreSuggestions
import com.qhathia.mobile.staff.core.domain.model.Store
import com.qhathia.mobile.staff.core.domain.model.User
import com.qhathia.mobile.staff.core.domain.model.UserRole
import com.qhathia.mobile.staff.core.domain.model.ActiveSession
import com.qhathia.mobile.staff.core.domain.model.AppLanguage
import com.qhathia.mobile.staff.core.domain.model.DeviceCommand
import com.qhathia.mobile.staff.core.domain.model.DeviceCommandStatus
import com.qhathia.mobile.staff.core.domain.model.DeviceCommandType
import com.qhathia.mobile.staff.core.domain.model.NotificationPreference
import com.qhathia.mobile.staff.core.domain.model.NotificationPreferences
import com.qhathia.mobile.staff.core.domain.model.PlanFeatures
import com.qhathia.mobile.staff.core.domain.model.StoreSettings
import com.qhathia.mobile.staff.core.domain.model.Subscription
import com.qhathia.mobile.staff.core.domain.model.SubscriptionStatus
import com.qhathia.mobile.staff.core.domain.model.UserProfile
import com.qhathia.mobile.staff.core.domain.model.Discount
import com.qhathia.mobile.staff.core.domain.model.DiscountType
import com.qhathia.mobile.staff.core.domain.model.ProductDiscount
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

/** Shared domain fixtures for tests. */
object TestFixtures {

    /** Fixed "now" the order fixtures are built around, so expiry assertions stay deterministic. */
    val NOW: Instant = Instant.parse("2026-06-29T20:00:00Z")

    fun tokens(
        accessToken: String = "access-token",
        refreshToken: String = "refresh-token",
        expiresInSeconds: Long = 86_400L,
    ) = AuthTokens(accessToken, refreshToken, expiresInSeconds)

    fun user(
        id: String = "user-1",
        email: String = "owner@tienda.com",
        fullName: String = "Juan Pérez",
        role: UserRole = UserRole.OWNER,
    ) = User(id, email, fullName, role)

    fun store(
        id: String = "store-1",
        shoppingCenterId: String? = "sc-1",
        name: String = "Tienda Gamarra",
        category: String = "TEXTILE",
    ) = Store(id, shoppingCenterId, name, category)

    fun session(
        tokens: AuthTokens = tokens(),
        user: User = user(),
        activeStore: Store? = store(),
        sessionId: String? = "session-1",
    ) = Session(tokens, user, activeStore, sessionId)

    fun product(
        id: String = "product-1",
        categoryId: String? = "category-1",
        categoryName: String? = "Electrónica",
        name: String = "Taladro Percutor Bosch GSB 13 RE",
        price: Double = 189.0,
        availability: Availability = Availability.AVAILABLE,
        primaryImageUrl: String? = null,
        stock: Int? = null,
        discountedPrice: Double? = null,
        discountEndsAt: Instant? = null,
    ) = Product(
        id, categoryId, categoryName, name, price, availability, primaryImageUrl,
        stock, discountedPrice, discountEndsAt,
    )

    fun productDetail(
        id: String = "product-1",
        name: String = "Taladro Percutor Bosch GSB 13 RE",
        description: String? = "Taladro percutor de 650W con maletín",
        price: Double = 189.0,
        stock: Int? = 12,
        categoryId: String? = "category-1",
        categoryName: String? = "Electrónica",
        tags: List<String> = listOf("herramientas", "bosch"),
        images: List<ProductImage> = listOf(productImage()),
        allowDiscountInquiry: Boolean = false,
        position: Int = 0,
        discountedPrice: Double? = null,
        discountEndsAt: Instant? = null,
        version: Long? = 7,
        active: Boolean = true,
    ) = ProductDetail(
        id = id,
        name = name,
        description = description,
        price = price,
        stock = stock,
        categoryId = categoryId,
        categoryName = categoryName,
        tags = tags,
        images = images,
        allowDiscountInquiry = allowDiscountInquiry,
        position = position,
        discountedPrice = discountedPrice,
        discountEndsAt = discountEndsAt,
        version = version,
        active = active,
    )

    fun deviceSettings(
        idleShowcaseEnabled: Boolean = true,
        voiceAssistantEnabled: Boolean = true,
        brightnessMode: BrightnessMode = BrightnessMode.AUTO,
        kioskMode: Boolean = true,
        exitPinSet: Boolean = false,
        autoSyncEnabled: Boolean = true,
    ) = DeviceSettings(
        idleShowcaseEnabled = idleShowcaseEnabled,
        voiceAssistantEnabled = voiceAssistantEnabled,
        brightnessMode = brightnessMode,
        kioskMode = kioskMode,
        exitPinSet = exitPinSet,
        autoSyncEnabled = autoSyncEnabled,
    )

    fun productImage(
        id: String = "media-1",
        url: String = "https://cdn.qhathia.test/product-1.jpg",
    ) = ProductImage(id = id, url = url)

    fun discount(
        type: DiscountType = DiscountType.PERCENTAGE,
        value: Double = 20.0,
    ) = Discount(type, value)

    /** A stored discount that is active and still running at [NOW] unless told otherwise. */
    fun productDiscount(
        id: String = "discount-1",
        discount: Discount = discount(),
        startsAt: Instant = NOW.minus(Duration.ofDays(1)),
        endsAt: Instant = NOW.plus(Duration.ofDays(6)),
        active: Boolean = true,
    ) = ProductDiscount(id, discount, startsAt, endsAt, active)

    fun productUpdate(
        name: String = "Taladro Percutor Bosch GSB 13 RE",
        price: Double = 189.0,
        stock: Int? = 12,
        description: String = "Taladro percutor de 650W con maletín",
        categoryId: String = "category-1",
        tags: List<String> = listOf("herramientas", "bosch"),
        version: Long? = 7,
    ) = ProductUpdate(
        name = name,
        price = price,
        stock = stock,
        description = description,
        categoryId = categoryId,
        tags = tags,
        version = version,
    )

    fun category(
        id: String = "category-1",
        name: String = "Electrónica",
    ) = Category(id, name)

    fun pairedDevice(
        deviceId: String = "device-new",
        sessionId: String = "session-1",
        name: String? = "Tablet 2",
    ) = PairedDevice(deviceId = deviceId, sessionId = sessionId, name = name)

    fun device(
        id: String = "device-1",
        serialNumber: String? = "SN-001",
        status: DeviceStatus = DeviceStatus.ACTIVE,
        appVersion: String? = "1.0.0",
        linkedAt: String? = "2026-04-15T18:30:00Z",
        name: String? = "Tablet vitrina 1",
        batteryLevel: Int? = 82,
        networkStatus: String? = "WIFI",
        storeName: String? = "Stand 1",
        lastPingAt: Instant? = NOW.minusSeconds(180),
        lastSyncAt: Instant? = NOW.minusSeconds(240),
        model: String? = "Lenovo Tab M11",
    ) = Device(
        id = id,
        serialNumber = serialNumber,
        status = status,
        appVersion = appVersion,
        linkedAt = linkedAt,
        name = name,
        batteryLevel = batteryLevel,
        networkStatus = networkStatus,
        storeName = storeName,
        lastPingAt = lastPingAt,
        lastSyncAt = lastSyncAt,
        model = model,
    )

    fun deviceCommand(
        id: String = "cmd-1",
        type: DeviceCommandType = DeviceCommandType.REFRESH_CATALOG,
        status: DeviceCommandStatus = DeviceCommandStatus.PENDING,
        requestedAt: Instant? = NOW,
        expiresAt: Instant? = requestedAt?.plus(Duration.ofMinutes(10)),
        resultDetail: String? = null,
        deliveredAt: Instant? = null,
        ackedAt: Instant? = null,
    ) = DeviceCommand(id, type, status, requestedAt, expiresAt, resultDetail, deliveredAt, ackedAt)

    fun userProfile(
        id: String = "user-1",
        fullName: String = "Carla Mendoza",
        email: String = "carla.mendoza@gmail.com",
        phone: String? = "999888777",
        passwordUpdatedAt: Instant? = null,
        role: UserRole = UserRole.OWNER,
        storeCount: Int = 3,
    ) = UserProfile(id, fullName, email, phone, passwordUpdatedAt, role, storeCount)

    fun subscription(
        planType: String = "PRO",
        planName: String = "Plan Pro",
        status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
        priceMonthlyPen: BigDecimal = BigDecimal("89.00"),
        features: PlanFeatures = PlanFeatures(
            productLimit = 200,
            hasAdvancedAnalytics = true,
            hasVideoProducts = false,
            hasBulkPriceUpdate = false,
            hasPrioritySupport = false,
        ),
        startedAt: Instant? = Instant.parse("2025-08-15T05:00:00Z"),
        endsAt: Instant? = Instant.parse("2026-09-15T05:00:00Z"),
    ) = Subscription(planType, planName, status, priceMonthlyPen, features, startedAt, endsAt)

    fun notificationPreferences(
        values: Map<NotificationPreference, Boolean> =
            NotificationPreference.entries.associateWith { it != NotificationPreference.LOW_BATTERY },
    ) = NotificationPreferences(
        values = values,
        activeCount = values.count { it.value },
        totalCount = NotificationPreference.entries.size,
    )

    fun activeSession(
        sessionId: String = "session-1",
        deviceLabel: String = "Redmi Note 12",
        lastUsedAt: Instant? = NOW.minusSeconds(60),
        isCurrent: Boolean = false,
    ) = ActiveSession(sessionId, deviceLabel, lastUsedAt, isCurrent)

    fun storeSettings(
        storeId: String = "store-1",
        language: AppLanguage? = AppLanguage.SPANISH,
    ) = StoreSettings(storeId, language)

    fun selectedImage(
        uri: String = "content://images/1",
        mimeType: String = "image/jpeg",
    ) = SelectedImage(uri, mimeType)

    fun newProduct(
        name: String = "Lámpara de Pared Vintage",
        price: Double = 189.0,
        quantity: Int = 10,
        description: String? = "Estilo vintage",
        categoryId: String = "category-1",
        tags: List<String> = listOf("vintage"),
        images: List<SelectedImage> = listOf(selectedImage()),
        idempotencyKey: String = "8a3f6f8e-1c2d-4e5f-9a0b-1c2d3e4f5a6b",
    ) = NewProduct(name, price, quantity, description, categoryId, tags, images, idempotencyKey)

    fun salesBucket(
        label: String = "Sem 1",
        amount: Long = 1240,
    ) = SalesBucket(label, BigDecimal(amount))

    /**
     * The design's weekly summary (Figma node `1954-6799`): four week buckets whose sum is the
     * headline total, with the fourth the best of the period.
     */
    /** A week whose period in progress (the last bucket) is also its best, 24.3% above the one before. */
    fun salesSummary(
        period: SalesPeriod = SalesPeriod.WEEKLY,
        buckets: List<SalesBucket> = listOf(
            salesBucket("Sem 10", 1240),
            salesBucket("Sem 11", 1480),
            salesBucket("Sem 12", 1360),
            salesBucket("Sem 13", 1690),
        ),
        total: BigDecimal = buckets.lastOrNull()?.amount ?: BigDecimal.ZERO,
        deltaPercent: Double? = 24.3,
        highlightedBucketIndex: Int = buckets.indices.maxByOrNull { buckets[it].amount } ?: -1,
        highlightedDeltaPercent: Double? = 24.3,
        generatedAt: Instant = NOW,
        periodStart: Instant? = NOW,
        periodEnd: Instant? = NOW.plusSeconds(WEEK_SECONDS),
        kpis: SalesKpis = salesKpis(),
    ) = SalesSummary(
        period = period,
        total = total,
        deltaPercent = deltaPercent,
        buckets = buckets,
        highlightedBucketIndex = highlightedBucketIndex,
        highlightedDeltaPercent = highlightedDeltaPercent,
        generatedAt = generatedAt,
        periodStart = periodStart,
        periodEnd = periodEnd,
        kpis = kpis,
    )

    fun salesKpis(
        salesCount: Int = 24,
        averageTicket: Long = 70,
        unitsSold: Int = 55,
    ) = SalesKpis(salesCount, BigDecimal(averageTicket), unitsSold)

    fun topSellingProduct(
        id: String = "product-1",
        name: String = "Taladro percutor Bosch GSB",
        imageUrl: String? = null,
        revenue: Long = 1870,
        unitsSold: Int = 34,
    ) = TopSellingProduct(id, name, imageUrl, BigDecimal(revenue), unitsSold)

    fun stalledProduct(
        id: String = "product-9",
        name: String = "Sierra caladora Stanley",
        imageUrl: String? = null,
        stock: Int = 34,
        price: Long = 100,
        immobilizedAmount: Long = stock * price,
    ) = StalledProduct(id, name, imageUrl, stock, BigDecimal(price), BigDecimal(immobilizedAmount))

    fun salesRanking(
        topSellers: List<TopSellingProduct> = listOf(topSellingProduct()),
        needsAttention: List<StalledProduct> = listOf(stalledProduct()),
        periodStart: Instant? = NOW,
        periodEnd: Instant? = NOW.plusSeconds(WEEK_SECONDS),
        needsAttentionTotalCount: Int? = needsAttention.size,
    ) = SalesRanking(
        periodStart = periodStart,
        periodEnd = periodEnd,
        topSellersRevenue = topSellers.fold(BigDecimal.ZERO) { acc, product -> acc + product.revenue },
        topSellers = topSellers,
        immobilizedTotal = needsAttention.fold(BigDecimal.ZERO) { acc, product ->
            acc + product.immobilizedAmount
        },
        needsAttention = needsAttention,
        needsAttentionTotalCount = needsAttentionTotalCount,
    )

    fun storeActivity(
        uniqueSessions: Int = 63,
        uniqueSessionsDeltaPercent: Double? = 12.5,
        productViews: Int = 148,
        productViewsDeltaPercent: Double? = 8.8,
        confirmedOrders: Int? = 9,
        confirmedOrdersDeltaPercent: Double? = 12.5,
        periodStart: Instant? = NOW,
        periodEnd: Instant? = NOW.plusSeconds(WEEK_SECONDS),
    ) = StoreActivity(
        periodStart = periodStart,
        periodEnd = periodEnd,
        uniqueSessions = uniqueSessions,
        uniqueSessionsDeltaPercent = uniqueSessionsDeltaPercent,
        productViews = productViews,
        productViewsDeltaPercent = productViewsDeltaPercent,
        confirmedOrders = confirmedOrders,
        confirmedOrdersDeltaPercent = confirmedOrdersDeltaPercent,
    )

    private const val WEEK_SECONDS = 7 * 24 * 60 * 60L

    fun missingProductSuggestion(
        id: String = "taladro",
        name: String = "Taladro percutor bosch",
        personCount: Int = 9,
        example: String? = null,
    ) = MissingProductSuggestion(id, name, personCount, example)

    fun restockSuggestion(
        id: String = "restock-1",
        name: String = "Taladro Percutor Bosch GSB 13 RE",
        imageUrl: String? = null,
        remainingStock: Int = 1,
    ) = RestockSuggestion(id, name, imageUrl, remainingStock)

    fun storeSuggestions(
        missingProducts: List<MissingProductSuggestion> = listOf(missingProductSuggestion()),
        restockProducts: List<RestockSuggestion> = listOf(restockSuggestion()),
        otherRequests: List<MissingProductSuggestion> = emptyList(),
    ) = StoreSuggestions(missingProducts, restockProducts, otherRequests)

    /** A pending order expiring 20 minutes after [NOW]. */
    fun order(
        id: String = "order-1",
        status: OrderStatus = OrderStatus.PENDING,
        customerName: String? = "Maria Murillo",
        customerPhone: String? = "987654321",
        orderCode: String? = "859653",
        itemCount: Int = 2,
        total: Double = 156.90,
        expiresAt: Instant? = NOW.plusSeconds(20 * 60),
        rejectionReason: String? = null,
        createdAt: Instant? = NOW,
    ) = Order(
        id = id,
        status = status,
        customerName = customerName,
        customerPhone = customerPhone,
        orderCode = orderCode,
        itemCount = itemCount,
        total = total,
        expiresAt = expiresAt,
        rejectionReason = rejectionReason,
        createdAt = createdAt,
    )

    /** An order in the last 3 minutes before expiry — the countdown's emphasized treatment. */
    fun expiringSoonOrder(
        id: String = "order-expiring",
        orderCode: String? = "859654",
    ) = order(id = id, orderCode = orderCode, expiresAt = NOW.plusSeconds(2 * 60))

    /**
     * An order predating backend V15 and created without a customer name — the two display
     * fallbacks the card has to cover.
     */
    fun unlabelledOrder(
        id: String = "order-unlabelled",
    ) = order(id = id, customerName = null, orderCode = null)

    fun orderItem(
        id: String = "item-1",
        productId: String = "product-1",
        productName: String = "Taladro Percutor Bosch GSB 13 RE",
        quantity: Int = 1,
        unitPrice: Double = 50.0,
        lineTotal: Double = 50.0,
        productStock: Int? = 12,
        productAvailability: Availability? = Availability.AVAILABLE,
        productImageUrl: String? = null,
        listPrice: Double? = null,
        productActive: Boolean = true,
        currentProductName: String? = null,
    ) = OrderItem(
        id = id,
        productId = productId,
        productName = productName,
        quantity = quantity,
        unitPrice = unitPrice,
        lineTotal = lineTotal,
        productStock = productStock,
        productAvailability = productAvailability,
        productImageUrl = productImageUrl,
        listPrice = listPrice,
        productActive = productActive,
        currentProductName = currentProductName,
    )

    fun orderDetail(
        order: Order = order(),
        items: List<OrderItem> = listOf(orderItem()),
    ) = OrderDetail(order, items)
}
