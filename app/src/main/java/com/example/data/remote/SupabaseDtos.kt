package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SupabaseAuthRequest(
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class SupabaseAuthUserDto(
    val id: String? = null,
    val email: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseAuthResponse(
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "token_type") val tokenType: String? = null,
    @Json(name = "user") val user: SupabaseAuthUserDto? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseProfileDto(
    val id: String,
    @Json(name = "full_name") val fullName: String,
    val phone: String,
    val city: String,
    val role: String,
    val specialty: String,
    @Json(name = "is_online") val isOnline: Boolean
)

@JsonClass(generateAdapter = true)
data class SupabaseProfileLocationDto(
    val id: String,
    val lat: Double,
    val lng: Double
)

@JsonClass(generateAdapter = true)
data class SupabaseProfileLocationPatchDto(
    val lat: Double,
    val lng: Double
)

@JsonClass(generateAdapter = true)
data class SupabaseServiceRequestDto(
    val id: String,
    @Json(name = "client_id") val clientId: String? = null,
    @Json(name = "client_name") val clientName: String,
    @Json(name = "category_id") val categoryId: String,
    @Json(name = "category_name") val categoryName: String,
    val title: String,
    val description: String = "",
    @Json(name = "address_label") val addressLabel: String,
    val lat: Double,
    val lng: Double,
    @Json(name = "proposed_price") val proposedPrice: Int,
    @Json(name = "accepted_price") val acceptedPrice: Int? = null,
    @Json(name = "payment_method") val paymentMethod: String = "cash",
    @Json(name = "is_urgent") val isUrgent: Boolean = true,
    val status: String = "searching",
    @Json(name = "assigned_craftsman_id") val assignedCraftsmanId: String? = null,
    @Json(name = "assigned_craftsman_name") val assignedCraftsmanName: String? = null,
    @Json(name = "assigned_craftsman_phone") val assignedCraftsmanPhone: String? = null,
    @Json(name = "photo_uri") val photoUri: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseRequestPatchDto(
    @Json(name = "proposed_price") val proposedPrice: Int? = null,
    @Json(name = "accepted_price") val acceptedPrice: Int? = null,
    val status: String? = null,
    @Json(name = "assigned_craftsman_id") val assignedCraftsmanId: String? = null,
    @Json(name = "assigned_craftsman_name") val assignedCraftsmanName: String? = null,
    @Json(name = "assigned_craftsman_phone") val assignedCraftsmanPhone: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseServiceOfferDto(
    val id: String,
    @Json(name = "request_id") val requestId: String,
    @Json(name = "craftsman_id") val craftsmanId: String? = null,
    @Json(name = "craftsman_name") val craftsmanName: String,
    @Json(name = "craftsman_phone") val craftsmanPhone: String,
    @Json(name = "craftsman_rating") val craftsmanRating: Double,
    @Json(name = "completed_jobs") val completedJobs: Int,
    val specialty: String,
    @Json(name = "offered_price") val offeredPrice: Int,
    @Json(name = "eta_minutes") val etaMinutes: Int,
    @Json(name = "distance_km") val distanceKm: Double,
    val note: String = "",
    val status: String = "pending"
)

@JsonClass(generateAdapter = true)
data class SupabaseOfferPatchDto(
    val status: String,
    @Json(name = "offered_price") val offeredPrice: Int? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseChatMessageDto(
    val id: String,
    @Json(name = "request_id") val requestId: String,
    @Json(name = "sender_id") val senderId: String,
    @Json(name = "sender_name") val senderName: String,
    @Json(name = "sender_role") val senderRole: String,
    @Json(name = "message_text") val messageText: String
)

@JsonClass(generateAdapter = true)
data class SupabaseReviewDto(
    val id: String,
    @Json(name = "request_id") val requestId: String,
    @Json(name = "craftsman_id") val craftsmanId: String? = null,
    @Json(name = "client_id") val clientId: String? = null,
    val rating: Int,
    val tags: List<String> = emptyList(),
    val comment: String = ""
)
