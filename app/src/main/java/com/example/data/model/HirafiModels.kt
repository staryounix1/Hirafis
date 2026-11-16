package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Carpenter
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.FormatPaint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Plumbing
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class UserRole(val dbValue: String, val labelAr: String) {
    CLIENT("client", "زبون"),
    CRAFTSMAN("craftsman", "حرفي")
}

enum class PaymentMethod(val dbValue: String, val labelAr: String) {
    CASH("cash", "نقداً"),
    TRANSFER("transfer", "تحويل بنكي"),
    CARD("card", "بطاقة")
}

enum class RequestStatus(val dbValue: String, val labelAr: String) {
    SEARCHING("searching", "جاري البحث عن حرفي"),
    ACCEPTED("accepted", "تم قبول العرض"),
    EN_ROUTE("en_route", "الحرفي في الطريق"),
    ARRIVED("arrived", "وصل الحرفي للموقع"),
    IN_PROGRESS("in_progress", "جاري إنجاز الخدمة"),
    COMPLETED("completed", "تمت الخدمة"),
    CANCELLED("cancelled", "ملغاة");

    companion object {
        fun fromDb(value: String): RequestStatus =
            entries.firstOrNull { it.dbValue == value } ?: SEARCHING
    }
}

enum class OfferStatus(val dbValue: String, val labelAr: String) {
    PENDING("pending", "قيد الانتظار"),
    ACCEPTED("accepted", "مقبول"),
    DECLINED("declined", "مرفوض"),
    COUNTERED("countered", "مفاوضة"),
    EXPIRED("expired", "منتهي");

    companion object {
        fun fromDb(value: String): OfferStatus =
            entries.firstOrNull { it.dbValue == value } ?: PENDING
    }
}

data class ServiceCategory(
    val id: String,
    val nameAr: String,
    val icon: ImageVector,
    val minRecommendedDh: Int,
    val maxRecommendedDh: Int,
    val defaultPriceDh: Int,
    val accentColor: Color
)

object HirafiCategories {
    val all = listOf(
        ServiceCategory("plumbing", "سباكة", Icons.Rounded.Plumbing, 100, 180, 120, Color(0xFF00A8E8)),
        ServiceCategory("electricity", "كهرباء", Icons.Rounded.ElectricBolt, 120, 220, 150, Color(0xFFFFB300)),
        ServiceCategory("ac", "تبريد وتكييف", Icons.Rounded.AcUnit, 150, 300, 200, Color(0xFF00B894)),
        ServiceCategory("carpentry", "نجارة", Icons.Rounded.Carpenter, 130, 250, 160, Color(0xFFD35400)),
        ServiceCategory("painting", "صباغة", Icons.Rounded.FormatPaint, 180, 400, 250, Color(0xFF8E44AD)),
        ServiceCategory("locksmith", "أقفال ومفاتيح", Icons.Rounded.Lock, 80, 160, 100, Color(0xFFE74C3C)),
        ServiceCategory("masonry", "بناء وزليج", Icons.Rounded.Build, 160, 350, 220, Color(0xFF5D6D7E)),
        ServiceCategory("cleaning", "تنظيف", Icons.Rounded.CleaningServices, 90, 200, 130, Color(0xFF16A085))
    )

    fun byId(id: String): ServiceCategory = all.firstOrNull { it.id == id } ?: all.first()
}

enum class CraftsmanSortOption(val labelAr: String) {
    NEAREST("الأقرب"),
    TOP_RATED("الأعلى تقييماً"),
    LOWEST_PRICE("الأقل سعراً")
}

enum class OfferSortOption(val labelAr: String) {
    BEST_PRICE("الأقل سعراً"),
    FASTEST_ETA("الأسرع وصولاً"),
    TOP_RATED("الأعلى تقييماً")
}

data class CraftsmanPin(
    val id: String,
    val name: String,
    val specialtyId: String,
    val specialtyName: String,
    val rating: Double,
    val completedJobs: Int,
    val lat: Double,
    val lng: Double,
    val basePriceDh: Int,
    val distanceKm: Double,
    val etaMinutes: Int,
    val phone: String,
    val isVerified: Boolean = true,
    val experienceYears: Int = 6,
    val bio: String = "حرفي معتمد ومجهز بأحدث الأدوات الاحترافية"
)

data class LiveRouteState(
    val routePoints: List<Pair<Double, Double>> = emptyList(),
    val craftsmanLat: Double? = null,
    val craftsmanLng: Double? = null,
    val remainingDistanceKm: Double = 0.0,
    val etaMinutes: Int = 0
)

data class ServiceRequest(
    val id: String,
    val clientId: String,
    val clientName: String,
    val categoryId: String,
    val categoryName: String,
    val title: String,
    val description: String,
    val addressLabel: String,
    val lat: Double,
    val lng: Double,
    val mapX: Float,
    val mapY: Float,
    val proposedPrice: Int,
    val acceptedPrice: Int?,
    val paymentMethod: PaymentMethod,
    val isUrgent: Boolean,
    val status: RequestStatus,
    val assignedCraftsmanId: String?,
    val assignedCraftsmanName: String?,
    val assignedCraftsmanPhone: String?,
    val assignedCraftsmanRating: Double?,
    val photoUri: String?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

data class ServiceOffer(
    val id: String,
    val requestId: String,
    val craftsmanId: String,
    val craftsmanName: String,
    val craftsmanPhone: String,
    val craftsmanRating: Double,
    val completedJobs: Int,
    val specialty: String,
    val offeredPrice: Int,
    val etaMinutes: Int,
    val distanceKm: Double,
    val note: String,
    val status: OfferStatus,
    val createdAtMillis: Long,
    val expiresAtMillis: Long
)

data class ChatMessage(
    val id: String,
    val requestId: String,
    val senderId: String,
    val senderName: String,
    val senderRole: UserRole,
    val text: String,
    val createdAtMillis: Long
)

data class ReviewRecord(
    val id: String,
    val requestId: String,
    val craftsmanId: String,
    val rating: Int,
    val tags: List<String>,
    val comment: String,
    val createdAtMillis: Long
)

data class UserSession(
    val userId: String = "local_user",
    val fullName: String = "يونس العلمي",
    val phone: String = "+212 612-345678",
    val city: String = "الدار البيضاء",
    val role: UserRole = UserRole.CLIENT,
    val craftsmanSpecialtyId: String = "plumbing",
    val isCraftsmanSpecialtyLocked: Boolean = false,
    val isCraftsmanOnline: Boolean = true,
    val supabaseEmail: String? = null,
    val supabaseAccessToken: String? = null
) {
    val isSupabaseAuthenticated: Boolean
        get() = !supabaseAccessToken.isNullOrBlank()
}

data class CityCoord(
    val nameAr: String,
    val lat: Double,
    val lng: Double
)

data class CityNeighborhood(
    val nameAr: String,
    val lat: Double,
    val lng: Double
)

data class SearchPlaceResult(
    val displayName: String,
    val lat: Double,
    val lng: Double
)

object MoroccanLocations {
    val cityCoords = listOf(
        CityCoord("الدار البيضاء", 33.5822, -7.6324),
        CityCoord("الرباط", 34.0209, -6.8416),
        CityCoord("مراكش", 31.6340, -8.0103),
        CityCoord("طنجة", 35.7595, -5.8340),
        CityCoord("فاس", 34.0181, -5.0078),
        CityCoord("أكادير", 30.4278, -9.5981)
    )

    val cities: List<String> = cityCoords.map { it.nameAr }

    fun coordForCity(city: String): CityCoord =
        cityCoords.firstOrNull { it.nameAr == city } ?: cityCoords.first()

    val neighborhoods = listOf(
        CityNeighborhood("المعاريف، شارع الزرقطوني، الدار البيضاء", 33.5822, -7.6324),
        CityNeighborhood("أنفا، قرب موروكو مول، الدار البيضاء", 33.5898, -7.6852),
        CityNeighborhood("سيدي معروف، التكنوبارك، الدار البيضاء", 33.5398, -7.6481),
        CityNeighborhood("عين السبع، الدار البيضاء", 33.6061, -7.5419),
        CityNeighborhood("أكدال، الرباط", 34.0011, -6.8477),
        CityNeighborhood("حي حسان، الرباط", 34.0209, -6.8310),
        CityNeighborhood("جليز، مراكش", 31.6340, -8.0103),
        CityNeighborhood("مالاباطا، طنجة", 35.7745, -5.7768)
    )
}
