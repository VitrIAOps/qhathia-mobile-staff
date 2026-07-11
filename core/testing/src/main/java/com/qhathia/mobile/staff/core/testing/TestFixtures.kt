package com.qhathia.mobile.staff.core.testing

import com.qhathia.mobile.staff.core.domain.model.AuthTokens
import com.qhathia.mobile.staff.core.domain.model.Availability
import com.qhathia.mobile.staff.core.domain.model.Category
import com.qhathia.mobile.staff.core.domain.model.Device
import com.qhathia.mobile.staff.core.domain.model.DeviceStatus
import com.qhathia.mobile.staff.core.domain.model.NewProduct
import com.qhathia.mobile.staff.core.domain.model.Product
import com.qhathia.mobile.staff.core.domain.model.SelectedImage
import com.qhathia.mobile.staff.core.domain.model.Session
import com.qhathia.mobile.staff.core.domain.model.Store
import com.qhathia.mobile.staff.core.domain.model.User
import com.qhathia.mobile.staff.core.domain.model.UserRole

/** Shared domain fixtures for tests. */
object TestFixtures {

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
    ) = Session(tokens, user, activeStore)

    fun product(
        id: String = "product-1",
        categoryId: String? = "category-1",
        categoryName: String? = "Electrónica",
        name: String = "Taladro Percutor Bosch GSB 13 RE",
        price: Double = 189.0,
        availability: Availability = Availability.AVAILABLE,
        primaryImageUrl: String? = null,
    ) = Product(id, categoryId, categoryName, name, price, availability, primaryImageUrl)

    fun category(
        id: String = "category-1",
        name: String = "Electrónica",
    ) = Category(id, name)

    fun device(
        id: String = "device-1",
        serialNumber: String? = "SN-001",
        status: DeviceStatus = DeviceStatus.ACTIVE,
        appVersion: String? = "1.0.0",
        linkedAt: String? = "2026-04-15T18:30:00Z",
    ) = Device(id, serialNumber, status, appVersion, linkedAt)

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
    ) = NewProduct(name, price, quantity, description, categoryId, tags, images)
}
