package com.example

import com.example.data.location.RealTimeLocationService
import com.example.data.model.AcceptanceRadarMetrics
import com.example.data.model.AccountRolePolicy
import com.example.data.model.BiddingSessionSummary
import com.example.data.model.CounterOfferSubmission
import com.example.data.model.FairPriceAssessment
import com.example.data.model.HirafiCategories
import com.example.data.model.HirafiQuickScenarios
import com.example.data.model.IdentityVerificationStatus
import com.example.data.model.IdentityVerificationValidator
import com.example.data.model.JobHandshakeSecurity
import com.example.data.model.JobPriceProposal
import com.example.data.model.LoginValidator
import com.example.data.model.MoroccanLocations
import com.example.data.model.OfferSortOption
import com.example.data.model.OfferStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.PhoneValidator
import com.example.data.model.PortfolioItem
import com.example.data.model.RequestStatus
import com.example.data.model.ServiceOffer
import com.example.data.model.ServiceRequest
import com.example.data.model.UserRole
import com.example.data.model.UserSession
import com.example.data.remote.MapRoutingService
import com.example.data.repository.HirafiRepository
import com.example.ui.viewmodel.RealtimeBiddingViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

  @Test
  fun `job price proposal evaluates market fairness and clamps counter offers`() {
    val low = JobPriceProposal(
      categoryId = "plumbing",
      proposedPriceDh = 70,
      recommendedMinDh = 100,
      recommendedMaxDh = 180
    )
    assertEquals(FairPriceAssessment.LOW, low.assessFairness())
    assertEquals(40, low.clampedPrice(10))
    assertEquals(5000, low.clampedPrice(9999))

    val fair = low.copy(proposedPriceDh = 140)
    assertEquals(FairPriceAssessment.FAIR, fair.assessFairness())

    val generous = low.copy(proposedPriceDh = 220)
    assertEquals(FairPriceAssessment.GENEROUS, generous.assessFairness())

    val counter = CounterOfferSubmission(
      requestId = "req_1",
      targetOfferId = "off_1",
      fromRole = UserRole.CRAFTSMAN,
      proposedPriceDh = 160,
      etaMinutes = 12,
      note = "شامل القطع"
    )
    assertEquals(160, counter.proposedPriceDh)
    assertEquals(UserRole.CRAFTSMAN, counter.fromRole)
  }

  @Test
  fun `bidding session summary exposes metrics to client and hides competitor bids from craftsman`() {
    val now = 200_000L
    val offers = listOf(
      ServiceOffer("o1", "r1", "c1", "كريم", "0600", 4.7, 50, "سباكة", 140, 8, 1.2, "", OfferStatus.PENDING, now - 1000, now + 30_000),
      ServiceOffer("o2", "r1", "c2", "عمر", "0601", 4.95, 120, "سباكة", 110, 15, 2.5, "", OfferStatus.PENDING, now - 1000, now + 30_000)
    )

    val clientSummary = BiddingSessionSummary.fromOffers(UserRole.CLIENT, offers, 120)
    assertEquals(2, clientSummary.activeOfferCount)
    assertEquals(110, clientSummary.lowestPriceDh)
    assertEquals(8, clientSummary.fastestEtaMinutes)
    assertEquals(4.95, clientSummary.highestRating!!, 0.001)

    val craftsmanSummary = BiddingSessionSummary.fromOffers(UserRole.CRAFTSMAN, offers, 120)
    assertEquals(0, craftsmanSummary.activeOfferCount)
    assertNull(craftsmanSummary.lowestPriceDh)
    assertNull(craftsmanSummary.fastestEtaMinutes)
    assertNull(craftsmanSummary.highestRating)
  }

  @Test
  fun `quick triage scenarios cover all categories and handshake security generates valid 4-digit pin and warranty`() {
    HirafiCategories.all.forEach { category ->
      val presets = HirafiQuickScenarios.forCategory(category.id)
      assertTrue("Expected presets for ${category.id}", presets.isNotEmpty())
      presets.forEach { preset ->
        assertTrue(preset.recommendedPriceDh >= 40)
        assertTrue(preset.estimatedMinutes > 0)
      }
    }

    val req = ServiceRequest(
      id = "req_security_test_1",
      clientId = "client_42",
      clientName = "أنس",
      categoryId = "plumbing",
      categoryName = "سباكة",
      title = "تسرب تحت الحوض",
      description = "",
      addressLabel = "المعاريف، الدار البيضاء",
      lat = 33.5822,
      lng = -7.6324,
      mapX = 0.5f,
      mapY = 0.5f,
      proposedPrice = 150,
      acceptedPrice = 150,
      paymentMethod = PaymentMethod.CASH,
      isUrgent = true,
      status = RequestStatus.COMPLETED,
      assignedCraftsmanId = "craft_7",
      assignedCraftsmanName = "كمال السباك",
      assignedCraftsmanPhone = "+212600112233",
      assignedCraftsmanRating = 4.9,
      photoUri = null,
      createdAtMillis = 1_700_000_000_000L,
      updatedAtMillis = 1_700_003_600_000L
    )

    val pin = JobHandshakeSecurity.deriveSecurityPin(req)
    assertEquals(4, pin.length)
    assertTrue(pin.all { it.isDigit() })
    assertEquals(pin, JobHandshakeSecurity.deriveSecurityPin(req))

    val warranty = JobHandshakeSecurity.deriveWarrantyCode(req)
    assertTrue(warranty.startsWith("HRF-") && warranty.endsWith("-30D"))

    val shareText = JobHandshakeSecurity.buildWarrantyShareText(req)
    assertTrue(shareText.contains(warranty))
    assertTrue(shareText.contains("كمال السباك"))

    val radarHigh = AcceptanceRadarMetrics.compute(
      JobPriceProposal(categoryId = "plumbing", proposedPriceDh = 210, recommendedMinDh = 100, recommendedMaxDh = 180, isUrgent = true)
    )
    val radarLow = AcceptanceRadarMetrics.compute(
      JobPriceProposal(categoryId = "plumbing", proposedPriceDh = 60, recommendedMinDh = 100, recommendedMaxDh = 180, isUrgent = false)
    )
    assertTrue(radarHigh.probabilityPercent > radarLow.probabilityPercent)
  }

  @Test
  fun `permanent role lock blocks client from craftsman mode and craftsman from client mode`() {
    val unregistered = UserSession(role = UserRole.CLIENT, isRoleLocked = false)
    assertFalse(AccountRolePolicy.canAccessMode(unregistered, UserRole.CLIENT))
    assertFalse(AccountRolePolicy.canAccessMode(unregistered, UserRole.CRAFTSMAN))
    assertTrue(AccountRolePolicy.canSwitchRole(unregistered, UserRole.CRAFTSMAN))

    val lockedClient = UserSession(role = UserRole.CLIENT, isRoleLocked = true)
    assertTrue(AccountRolePolicy.canAccessMode(lockedClient, UserRole.CLIENT))
    assertFalse(AccountRolePolicy.canAccessMode(lockedClient, UserRole.CRAFTSMAN))
    assertFalse(AccountRolePolicy.canSwitchRole(lockedClient, UserRole.CRAFTSMAN))
    assertTrue(AccountRolePolicy.canSwitchRole(lockedClient, UserRole.CLIENT))

    val lockedCraftsman = UserSession(
      role = UserRole.CRAFTSMAN,
      isRoleLocked = true,
      craftsmanSpecialtyId = "electricity",
      isCraftsmanSpecialtyLocked = true
    )
    assertTrue(AccountRolePolicy.canAccessMode(lockedCraftsman, UserRole.CRAFTSMAN))
    assertFalse(AccountRolePolicy.canAccessMode(lockedCraftsman, UserRole.CLIENT))
    assertFalse(AccountRolePolicy.canSwitchRole(lockedCraftsman, UserRole.CLIENT))
    assertTrue(AccountRolePolicy.canSwitchRole(lockedCraftsman, UserRole.CRAFTSMAN))
  }

  @Test
  fun `identity verification is restricted to craftsmen and validates moroccan CIN steps`() {
    val client = UserSession(role = UserRole.CLIENT, isRoleLocked = true)
    val unregisteredCraftsman = UserSession(role = UserRole.CRAFTSMAN, isRoleLocked = false)
    val craftsman = UserSession(
      role = UserRole.CRAFTSMAN,
      isRoleLocked = true,
      craftsmanSpecialtyId = "plumbing",
      isCraftsmanSpecialtyLocked = true,
      verificationStatus = IdentityVerificationStatus.VERIFIED
    )

    assertFalse(AccountRolePolicy.canAccessIdentityVerification(client))
    assertFalse(AccountRolePolicy.canAccessIdentityVerification(unregisteredCraftsman))
    assertTrue(AccountRolePolicy.canAccessIdentityVerification(craftsman))
    assertTrue(craftsman.isCraftsmanVerified)
    assertFalse(client.isCraftsmanVerified)

    assertTrue(IdentityVerificationValidator.isValidCin("bk 654321"))
    assertFalse(IdentityVerificationValidator.isValidCin("12"))
    assertEquals("BK••••21", IdentityVerificationValidator.maskCin("bk654321"))
    assertEquals(
      4,
      IdentityVerificationValidator.completionStepsCount(
        cin = "BK654321",
        frontUri = "content://front",
        backUri = "content://back",
        selfieUri = "content://selfie"
      )
    )
    assertEquals(
      2,
      IdentityVerificationValidator.completionStepsCount(
        cin = "BK654321",
        frontUri = "content://front",
        backUri = null,
        selfieUri = null
      )
    )
  }

  @Test
  fun `login validator checks full name email password and produces deterministic hash`() {
    assertTrue(LoginValidator.isValidFullName("يونس العلمي"))
    assertFalse(LoginValidator.isValidFullName("A"))

    assertTrue(LoginValidator.isValidEmail("Youness.User@gmail.com"))
    assertEquals("youness.user@gmail.com", LoginValidator.normalizeEmail("  Youness.User@Gmail.com "))
    assertFalse(LoginValidator.isValidEmail("invalid-email"))

    assertTrue(LoginValidator.isValidPassword("secret123"))
    assertFalse(LoginValidator.isValidPassword("12345"))

    val h1 = LoginValidator.hashPassword("User@Gmail.com", "secret123")
    val h2 = LoginValidator.hashPassword("user@gmail.com ", "secret123")
    assertEquals(64, h1.length)
    assertEquals(h1, h2)
  }

  @Test
  fun `phone validator normalizes moroccan numbers and portfolio items hold craftsman metadata`() {
    assertEquals("+212612345678", PhoneValidator.normalize("0612-345 678"))
    assertTrue(PhoneValidator.isValidMoroccanPhone("0612345678"))
    assertTrue(PhoneValidator.isValidMoroccanPhone("+212 661-234510"))
    assertFalse(PhoneValidator.isValidMoroccanPhone("123"))

    val work = PortfolioItem(
      id = "p1",
      craftsmanId = "uid_123",
      title = "تركيب سخان ماء",
      description = "تبديل كامل مع ضمان",
      categoryName = "سباكة",
      photoUri = "content://work_1",
      createdAtMillis = 12345L
    )
    assertEquals("uid_123", work.craftsmanId)
    assertEquals("تركيب سخان ماء", work.title)

    val session = UserSession(
      role = UserRole.CRAFTSMAN,
      isRoleLocked = true,
      experienceYears = 8,
      basePriceDh = 160,
      bio = "سباك محترف"
    )
    assertEquals(8, session.experienceYears)
    assertEquals(160, session.basePriceDh)
  }
}
