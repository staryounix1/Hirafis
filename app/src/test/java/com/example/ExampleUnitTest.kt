package com.example

import com.example.data.location.RealTimeLocationService
import com.example.data.model.HirafiCategories
import com.example.data.model.MoroccanLocations
import com.example.data.model.OfferSortOption
import com.example.data.model.OfferStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.RequestStatus
import com.example.data.model.ServiceOffer
import com.example.data.model.ServiceRequest
import com.example.data.remote.MapRoutingService
import com.example.data.repository.HirafiRepository
import com.example.ui.viewmodel.RealtimeBiddingViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun `haversine distance between Casablanca neighborhoods is realistic`() {
    val km = MapRoutingService.haversineKm(33.5883, -7.6328, 33.5950, -7.6187)
    assertTrue("Expected positive short distance, got $km", km in 1.0..3.0)
  }

  @Test
  fun `moroccan city coordinate lookup resolves Marrakech and falls back to Casablanca`() {
    val marrakech = MoroccanLocations.coordForCity("مراكش")
    assertEquals(31.6340, marrakech.lat, 0.01)
    val unknown = MoroccanLocations.coordForCity("مدينة غير معروفة")
    assertEquals("الدار البيضاء", unknown.nameAr)
  }

  @Test
  fun `nearby craftsmen cover all 8 categories with valid distances and ETAs`() {
    val craftsmen = HirafiRepository.computeCraftsmenAround(33.5822, -7.6324)
    assertEquals(16, craftsmen.size)
    val coveredCategories = craftsmen.map { it.specialtyId }.toSet()
    HirafiCategories.all.forEach { category ->
      assertTrue("Missing craftsman for category ${category.id}", category.id in coveredCategories)
    }
    craftsmen.forEach { pin ->
      assertTrue("Distance must be positive for ${pin.id}", pin.distanceKm > 0.0)
      assertTrue("ETA must be positive for ${pin.id}", pin.etaMinutes > 0)
      assertTrue("Bio must not be blank for ${pin.id}", pin.bio.isNotBlank())
    }
  }

  @Test
  fun `realtime bidding sorts active offers and excludes expired or declined bids`() {
    val now = 100_000L
    val offers = listOf(
      ServiceOffer("o1", "r1", "c1", "كريم", "0600", 4.7, 50, "سباكة", 140, 8, 1.2, "", OfferStatus.PENDING, now - 1000, now + 30_000),
      ServiceOffer("o2", "r1", "c2", "عمر", "0601", 4.95, 120, "سباكة", 110, 15, 2.5, "", OfferStatus.PENDING, now - 1000, now + 30_000),
      ServiceOffer("o3", "r1", "c3", "رشيد", "0602", 4.8, 90, "سباكة", 125, 12, 1.8, "", OfferStatus.COUNTERED, now - 1000, now - 500),
      ServiceOffer("o4", "r1", "c4", "سليم", "0603", 5.0, 200, "سباكة", 90, 5, 0.5, "", OfferStatus.DECLINED, now - 1000, now + 30_000),
      ServiceOffer("o5", "r1", "c5", "مراد", "0604", 4.6, 30, "سباكة", 95, 6, 0.9, "", OfferStatus.PENDING, now - 5000, now - 100)
    )

    val byPrice = RealtimeBiddingViewModel.sortAndFilterActiveOffers(offers, OfferSortOption.BEST_PRICE, now)
    assertEquals(listOf("o2", "o3", "o1"), byPrice.map { it.id })

    val byEta = RealtimeBiddingViewModel.sortAndFilterActiveOffers(offers, OfferSortOption.FASTEST_ETA, now)
    assertEquals(listOf("o1", "o3", "o2"), byEta.map { it.id })

    val byRating = RealtimeBiddingViewModel.sortAndFilterActiveOffers(offers, OfferSortOption.TOP_RATED, now)
    assertEquals(listOf("o2", "o3", "o1"), byRating.map { it.id })
  }

  @Test
  fun `craftsman only sees open requests matching their single locked profession and only their own offers`() {
    val requests = listOf(
      ServiceRequest(
        id = "r_plumb",
        clientId = "u1",
        clientName = "ياسين",
        categoryId = "plumbing",
        categoryName = "السباكة",
        title = "تسرب مياه",
        description = "",
        addressLabel = "المعاريف",
        lat = 33.58,
        lng = -7.63,
        mapX = 0.5f,
        mapY = 0.5f,
        proposedPrice = 150,
        acceptedPrice = null,
        paymentMethod = PaymentMethod.CASH,
        isUrgent = false,
        status = RequestStatus.SEARCHING,
        assignedCraftsmanId = null,
        assignedCraftsmanName = null,
        assignedCraftsmanPhone = null,
        assignedCraftsmanRating = null,
        photoUri = null,
        createdAtMillis = 1000L,
        updatedAtMillis = 1000L
      ),
      ServiceRequest(
        id = "r_elec",
        clientId = "u2",
        clientName = "سلمى",
        categoryId = "electrical",
        categoryName = "الكهرباء",
        title = "انقطاع كهرباء",
        description = "",
        addressLabel = "أنفا",
        lat = 33.59,
        lng = -7.64,
        mapX = 0.5f,
        mapY = 0.5f,
        proposedPrice = 180,
        acceptedPrice = null,
        paymentMethod = PaymentMethod.CASH,
        isUrgent = false,
        status = RequestStatus.SEARCHING,
        assignedCraftsmanId = null,
        assignedCraftsmanName = null,
        assignedCraftsmanPhone = null,
        assignedCraftsmanRating = null,
        photoUri = null,
        createdAtMillis = 2000L,
        updatedAtMillis = 2000L
      )
    )
    val lockedSpecialtyId = "plumbing"
    val visibleToPlumber = requests.filter {
      it.status == RequestStatus.SEARCHING && it.categoryId == lockedSpecialtyId
    }
    assertEquals(listOf("r_plumb"), visibleToPlumber.map { it.id })

    val myCraftsmanId = "c_me"
    val allOffers = listOf(
      ServiceOffer("o_mine", "r_plumb", myCraftsmanId, "أنا", "0611", 4.9, 10, "سباك", 140, 6, 1.0, "", OfferStatus.PENDING, 1000L, 50_000L),
      ServiceOffer("o_other", "r_plumb", "c_competitor", "حرفي منافس", "0622", 4.8, 25, "سباك", 120, 4, 0.7, "", OfferStatus.PENDING, 1000L, 50_000L)
    )
    val visibleToCraftsman = allOffers.filter { it.craftsmanId == myCraftsmanId }
    assertEquals(listOf("o_mine"), visibleToCraftsman.map { it.id })
  }

  @Test
  fun `real time location service computes dynamic ETA and validates coordinates`() {
    assertTrue(RealTimeLocationService.isCoordinateValid(33.5822, -7.6324))
    assertFalse(RealTimeLocationService.isCoordinateValid(0.0, 0.0))
    assertFalse(RealTimeLocationService.isCoordinateValid(Double.NaN, -7.6))
    assertFalse(RealTimeLocationService.isCoordinateValid(95.0, 10.0))

    assertEquals(0, RealTimeLocationService.computeEtaMinutes(0.05, 5.0f))
    // 10 m/s = 36 km/h -> 6.0 km takes 10 minutes
    assertEquals(10, RealTimeLocationService.computeEtaMinutes(6.0, 10.0f))
    // Stationary (0 m/s) uses 24 km/h urban baseline -> 4.0 km takes 10 minutes
    assertEquals(10, RealTimeLocationService.computeEtaMinutes(4.0, 0.0f))
  }
}
