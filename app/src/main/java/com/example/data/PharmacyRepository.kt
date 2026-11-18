package com.example.data

import android.content.Context
import com.example.R
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.example.model.BusinessSetting
import com.example.model.CustomerAddress
import com.example.model.CustomerProfile
import com.example.model.DeliveryPerson
import com.example.model.ManagerAccount
import com.example.model.UserAccount
import com.example.util.SecurityCrypto
import com.example.util.TimezoneUtils
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

class PharmacyRepository(
    val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun currentUid(): String? = auth.currentUser?.uid

    fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in with Google.")

    fun observeUserAccount(uid: String): Flow<UserAccount?> =
        db.collection("users").document(uid)
            .snapshots()
            .map { snap ->
                if (snap.exists()) snap.toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                else null
            }

    fun observeCustomerProfile(uid: String): Flow<CustomerProfile?> =
        db.collection("customers").document(uid)
            .snapshots()
            .map { snap ->
                if (snap.exists()) snap.toObject(CustomerProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                else null
            }

    fun observeCustomerAddresses(uid: String): Flow<List<CustomerAddress>> =
        db.collection("customers").document(uid).collection("addresses")
            .snapshots()
            .map { snap -> snap.toObjects(CustomerAddress::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }

    fun observeAllCustomers(): Flow<List<CustomerProfile>> =
        db.collection("customers")
            .snapshots()
            .map { snap -> snap.toObjects(CustomerProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }

    fun observeManagers(): Flow<List<ManagerAccount>> =
        db.collection("managers")
            .snapshots()
            .map { snap -> snap.toObjects(ManagerAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }

    fun observeDeliveryPersonnel(): Flow<List<DeliveryPerson>> =
        db.collection("deliveryPersonnel")
            .snapshots()
            .map { snap -> snap.toObjects(DeliveryPerson::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }

    fun observeShopPublicSettings(): Flow<BusinessSetting?> =
        db.collection("businessSettings").document("shop_public")
            .snapshots()
            .map { snap ->
                if (snap.exists()) snap.toObject(BusinessSetting::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                else BusinessSetting(id = "shop_public")
            }

    fun observeFinancialConfig(): Flow<BusinessSetting?> =
        db.collection("businessSettings").document("financial_config")
            .snapshots()
            .map { snap ->
                if (snap.exists()) snap.toObject(BusinessSetting::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                else BusinessSetting(id = "financial_config")
            }

    suspend fun ensureSeededAccountsIfFresh(): Result<Unit> {
        return try {
            requireUserId()
            val ownerDirDoc = db.collection("managerDirectory").document("owner").get().await()
            if (!ownerDirDoc.exists()) {
                val defaultAccounts = listOf(
                    Triple("owner", "Owner", "owner") to "1234",
                    Triple("father", "Father", "father") to "father1234",
                    Triple("me", "Me", "me") to "me1234",
                    Triple("elder_uncle", "Elder Uncle", "uncle") to "uncle1234",
                    Triple("brother", "Brother", "brother") to "brother1234"
                )
                val batch = db.batch()
                defaultAccounts.forEachIndexed { idx, (info, initialPass) ->
                    val (id, name, username) = info
                    val salt = SecurityCrypto.generateSalt()
                    val hash = SecurityCrypto.hashPassword(salt, initialPass)
                    val roleType = if (id == "owner") "owner" else "family_manager"
                    val mgrData = mapOf(
                        "name" to name,
                        "username" to username,
                        "mobile" to "987654321$idx",
                        "roleType" to roleType,
                        "passwordHash" to hash,
                        "passwordSalt" to salt,
                        "active" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    batch.set(db.collection("managers").document(id), mgrData)
                    batch.set(
                        db.collection("managerDirectory").document(username),
                        mapOf(
                            "accountId" to id,
                            "username" to username,
                            "salt" to salt,
                            "roleType" to roleType,
                            "active" to true
                        )
                    )
                }

                // Seed initial demo delivery person account if missing
                val demoDpDirDoc = db.collection("deliveryDirectory").document("rahul_dp").get().await()
                if (!demoDpDirDoc.exists()) {
                    val dpSalt = SecurityCrypto.generateSalt()
                    val dpHash = SecurityCrypto.hashPassword(dpSalt, "1234")
                    batch.set(
                        db.collection("deliveryPersonnel").document("demo_dp"),
                        mapOf(
                            "name" to "Rahul Kumar (Demo)",
                            "mobile" to "9111222333",
                            "username" to "rahul_dp",
                            "passwordHash" to dpHash,
                            "passwordSalt" to dpSalt,
                            "active" to true,
                            "onShift" to false,
                            "trackingConsentGranted" to false,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    batch.set(
                        db.collection("deliveryDirectory").document("rahul_dp"),
                        mapOf(
                            "accountId" to "demo_dp",
                            "username" to "rahul_dp",
                            "salt" to dpSalt,
                            "roleType" to "delivery",
                            "active" to true
                        )
                    )
                }

                // Seed shop_public if missing
                val shopDoc = db.collection("businessSettings").document("shop_public").get().await()
                if (!shopDoc.exists()) {
                    batch.set(
                        db.collection("businessSettings").document("shop_public"),
                        mapOf(
                            "shopName" to "PRAYAG DEEN & SONS",
                            "phone" to "+91 98765 43210",
                            "address" to "Main Medical Market, Sadar Bazar",
                            "hours" to "8:00 AM - 10:30 PM",
                            "baseDeliveryCharge" to 20.0,
                            "freeDeliveryAbove" to 500.0,
                            "shopLat" to 26.8467,
                            "shopLng" to 80.9462,
                            "accountingMethod" to "ACCRUAL",
                            "backupRetentionDays" to 365L,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                batch.commit().await()
            } else {
                // Ensure demo_dp exists if deliveryDirectory empty
                val demoDpDirDoc = db.collection("deliveryDirectory").document("rahul_dp").get().await()
                if (!demoDpDirDoc.exists()) {
                    val dpSalt = SecurityCrypto.generateSalt()
                    val dpHash = SecurityCrypto.hashPassword(dpSalt, "1234")
                    db.collection("deliveryPersonnel").document("demo_dp").set(
                        mapOf(
                            "name" to "Rahul Kumar (Demo)",
                            "mobile" to "9111222333",
                            "username" to "rahul_dp",
                            "passwordHash" to dpHash,
                            "passwordSalt" to dpSalt,
                            "active" to true,
                            "onShift" to false,
                            "trackingConsentGranted" to false,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                    db.collection("deliveryDirectory").document("rahul_dp").set(
                        mapOf(
                            "accountId" to "demo_dp",
                            "username" to "rahul_dp",
                            "salt" to dpSalt,
                            "roleType" to "delivery",
                            "active" to true
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "managers/seed")
            Result.failure(e)
        }
    }

    suspend fun registerCustomer(
        name: String,
        phone: String,
        phoneVerified: Boolean,
        addressLabel: String,
        houseNo: String,
        street: String,
        villageOrArea: String,
        landmark: String,
        lat: Double,
        lng: Double,
        accuracyMeters: Double,
        language: String
    ): Result<Unit> {
        return try {
            val uid = requireUserId()
            val email = auth.currentUser?.email.orEmpty()
            val fullAddress = listOf(houseNo, street, villageOrArea, landmark)
                .filter { it.isNotBlank() }
                .joinToString(", ")

            val addrRef = db.collection("customers").document(uid).collection("addresses").document()
            val batch = db.batch()
            batch.set(
                addrRef,
                mapOf(
                    "userId" to uid,
                    "label" to addressLabel.ifBlank { "Home" },
                    "houseNo" to houseNo,
                    "street" to street,
                    "villageOrArea" to villageOrArea.ifBlank { "Local Area" },
                    "landmark" to landmark,
                    "fullAddress" to fullAddress.ifBlank { villageOrArea },
                    "lat" to lat,
                    "lng" to lng,
                    "accuracyMeters" to accuracyMeters,
                    "primaryAddress" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            val existingCust = db.collection("customers").document(uid).get().await()
            if (!existingCust.exists()) {
                batch.set(
                    db.collection("customers").document(uid),
                    mapOf(
                        "userId" to uid,
                        "name" to name.trim(),
                        "phone" to phone.trim(),
                        "phoneVerified" to phoneVerified,
                        "defaultAddressId" to addrRef.id,
                        "defaultAddressText" to fullAddress.ifBlank { villageOrArea },
                        "defaultLandmark" to landmark,
                        "defaultArea" to villageOrArea.ifBlank { "Local Area" },
                        "defaultLat" to lat,
                        "defaultLng" to lng,
                        "totalOrders" to 0L,
                        "totalSpent" to 0.0,
                        "active" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.update(
                    db.collection("customers").document(uid),
                    mapOf(
                        "name" to name.trim(),
                        "phone" to phone.trim(),
                        "phoneVerified" to phoneVerified,
                        "defaultAddressId" to addrRef.id,
                        "defaultAddressText" to fullAddress.ifBlank { villageOrArea },
                        "defaultLandmark" to landmark,
                        "defaultArea" to villageOrArea.ifBlank { "Local Area" },
                        "defaultLat" to lat,
                        "defaultLng" to lng,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.set(
                db.collection("users").document(uid),
                mapOf(
                    "userId" to uid,
                    "role" to "customer",
                    "displayName" to name.trim(),
                    "phone" to phone.trim(),
                    "email" to email,
                    "active" to true,
                    "language" to language,
                    "notificationsEnabled" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "customers/${auth.currentUser?.uid}")
            Result.failure(e)
        }
    }

    suspend fun saveCustomerAddress(
        address: CustomerAddress,
        setAsDefault: Boolean
    ): Result<Unit> {
        return try {
            val uid = requireUserId()
            val ref = if (address.id.isBlank()) {
                db.collection("customers").document(uid).collection("addresses").document()
            } else {
                db.collection("customers").document(uid).collection("addresses").document(address.id)
            }
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "label" to address.label.ifBlank { "Address" },
                "houseNo" to address.houseNo,
                "street" to address.street,
                "villageOrArea" to address.villageOrArea.ifBlank { "Local Area" },
                "landmark" to address.landmark,
                "fullAddress" to address.fullAddress,
                "lat" to address.lat,
                "lng" to address.lng,
                "accuracyMeters" to address.accuracyMeters,
                "primaryAddress" to setAsDefault,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (address.id.isBlank()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
                ref.set(payload).await()
            } else {
                ref.update(payload).await()
            }
            if (setAsDefault) {
                db.collection("customers").document(uid).update(
                    mapOf(
                        "defaultAddressId" to ref.id,
                        "defaultAddressText" to address.fullAddress,
                        "defaultLandmark" to address.landmark,
                        "defaultArea" to address.villageOrArea.ifBlank { "Local Area" },
                        "defaultLat" to address.lat,
                        "defaultLng" to address.lng,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "customers/addresses")
            Result.failure(e)
        }
    }

    suspend fun setDefaultCustomerAddress(address: CustomerAddress): Result<Unit> {
        return try {
            val uid = requireUserId()
            val col = db.collection("customers").document(uid).collection("addresses")
            val existing = col.get().await()
            val batch = db.batch()
            existing.documents.forEach { doc ->
                batch.update(
                    doc.reference,
                    mapOf(
                        "primaryAddress" to (doc.id == address.id),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.update(
                db.collection("customers").document(uid),
                mapOf(
                    "defaultAddressId" to address.id,
                    "defaultAddressText" to address.fullAddress,
                    "defaultLandmark" to address.landmark,
                    "defaultArea" to address.villageOrArea.ifBlank { "Local Area" },
                    "defaultLat" to address.lat,
                    "defaultLng" to address.lng,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "customers/addresses/default")
            Result.failure(e)
        }
    }

    suspend fun deleteCustomerAddress(addressId: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            db.collection("customers").document(uid).collection("addresses").document(addressId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "customers/addresses/$addressId")
            Result.failure(e)
        }
    }

    suspend fun updateCustomerProfile(name: String, phone: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            val batch = db.batch()
            batch.update(
                db.collection("customers").document(uid),
                mapOf(
                    "name" to name.trim(),
                    "phone" to phone.trim(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                db.collection("users").document(uid),
                mapOf(
                    "displayName" to name.trim(),
                    "phone" to phone.trim(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "customers/${auth.currentUser?.uid}")
            Result.failure(e)
        }
    }

    suspend fun loginAsManager(
        usernameInput: String,
        passwordInput: String,
        language: String
    ): Result<ManagerAccount> {
        val cleanUser = usernameInput.trim().lowercase()
        return try {
            val uid = requireUserId()
            ensureSeededAccountsIfFresh()

            val dirSnap = db.collection("managerDirectory").document(cleanUser).get().await()
            if (!dirSnap.exists()) {
                return Result.failure(IllegalArgumentException("Manager username '$cleanUser' not found."))
            }
            val accountId = dirSnap.getString("accountId").orEmpty()
            val salt = dirSnap.getString("salt").orEmpty()
            val roleType = dirSnap.getString("roleType").orEmpty()
            val active = dirSnap.getBoolean("active") ?: false
            if (!active) {
                return Result.failure(IllegalStateException("This manager account has been deactivated by the Owner."))
            }

            val candidateHash = SecurityCrypto.hashPassword(salt, passwordInput)
            val batch = db.batch()
            batch.update(
                db.collection("managers").document(accountId),
                mapOf(
                    "linkedUid" to uid,
                    "authVerificationHash" to candidateHash,
                    "lastLoginAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val roleWire = if (roleType == "owner") "owner" else "manager"
            val userRef = db.collection("users").document(uid)
            val userSnap = userRef.get().await()
            val userPayload = mutableMapOf<String, Any>(
                "userId" to uid,
                "role" to roleWire,
                "displayName" to cleanUser.replaceFirstChar { it.uppercase() },
                "email" to auth.currentUser?.email.orEmpty(),
                "linkedManagerId" to accountId,
                "active" to true,
                "language" to language,
                "notificationsEnabled" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!userSnap.exists()) {
                userPayload["createdAt"] = FieldValue.serverTimestamp()
            }
            batch.set(userRef, userPayload, SetOptions.merge())
            batch.commit().await()

            val mgr = db.collection("managers").document(accountId).get().await()
                .toObject(ManagerAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: return Result.failure(IllegalStateException("Manager profile unavailable."))
            // Sync proper displayName from manager record
            userRef.update(mapOf("displayName" to mgr.name, "updatedAt" to FieldValue.serverTimestamp())).await()
            Result.success(mgr)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "managers/$cleanUser")
            val friendly = if (e.message?.contains("PERMISSION_DENIED") == true) {
                "Invalid password or deactivated manager account."
            } else {
                e.localizedMessage ?: "Manager login failed."
            }
            Result.failure(Exception(friendly, e))
        }
    }

    suspend fun loginAsDeliveryPerson(
        usernameInput: String,
        passwordInput: String,
        language: String
    ): Result<DeliveryPerson> {
        val cleanUser = usernameInput.trim().lowercase()
        return try {
            val uid = requireUserId()
            ensureSeededAccountsIfFresh()

            val dirSnap = db.collection("deliveryDirectory").document(cleanUser).get().await()
            if (!dirSnap.exists()) {
                return Result.failure(IllegalArgumentException("Delivery username '$cleanUser' not found."))
            }
            val dpId = dirSnap.getString("accountId").orEmpty()
            val salt = dirSnap.getString("salt").orEmpty()
            val active = dirSnap.getBoolean("active") ?: false
            if (!active) {
                return Result.failure(IllegalStateException("This delivery account is deactivated."))
            }

            val candidateHash = SecurityCrypto.hashPassword(salt, passwordInput)
            val batch = db.batch()
            batch.update(
                db.collection("deliveryPersonnel").document(dpId),
                mapOf(
                    "linkedUid" to uid,
                    "authVerificationHash" to candidateHash,
                    "lastLoginAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val userRef = db.collection("users").document(uid)
            val userSnap = userRef.get().await()
            val userPayload = mutableMapOf<String, Any>(
                "userId" to uid,
                "role" to "delivery",
                "displayName" to cleanUser,
                "email" to auth.currentUser?.email.orEmpty(),
                "linkedDeliveryPersonId" to dpId,
                "active" to true,
                "language" to language,
                "notificationsEnabled" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!userSnap.exists()) {
                userPayload["createdAt"] = FieldValue.serverTimestamp()
            }
            batch.set(userRef, userPayload, SetOptions.merge())
            batch.commit().await()

            val dp = db.collection("deliveryPersonnel").document(dpId).get().await()
                .toObject(DeliveryPerson::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: return Result.failure(IllegalStateException("Delivery profile unavailable."))
            userRef.update(mapOf("displayName" to dp.name, "updatedAt" to FieldValue.serverTimestamp())).await()
            Result.success(dp)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "deliveryPersonnel/$cleanUser")
            val friendly = if (e.message?.contains("PERMISSION_DENIED") == true) {
                "Invalid password or deactivated delivery account."
            } else {
                e.localizedMessage ?: "Delivery login failed."
            }
            Result.failure(Exception(friendly, e))
        }
    }

    suspend fun updateLanguagePreference(language: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid)
            if (ref.get().await().exists()) {
                ref.update(mapOf("language" to language, "updatedAt" to FieldValue.serverTimestamp())).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveNotificationSettings(enabled: Boolean): Result<Unit> {
        return try {
            val uid = requireUserId()
            db.collection("users").document(uid).update(
                mapOf("notificationsEnabled" to enabled, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncFcmToken(): Result<Unit> {
        return try {
            val uid = requireUserId()
            val token = FirebaseMessaging.getInstance().token.await()
            if (token.isNotBlank()) {
                val userRef = db.collection("users").document(uid)
                if (userRef.get().await().exists()) {
                    userRef.update(
                        mapOf(
                            "fcmToken" to token.take(500),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun buildBusinessBackupJson(): Result<String> {
        return try {
            requireUserId()
            val orders = db.collection("orders").get().await().documents.map { it.data?.plus("id" to it.id) }
            val financials = db.collection("orderFinancials").get().await().documents.map { it.data?.plus("id" to it.id) }
            val customers = db.collection("customers").get().await().documents.map { it.data?.plus("id" to it.id) }
            val audit = db.collection("auditLogs").get().await().documents.map { it.data?.plus("id" to it.id) }
            val root = JSONObject().apply {
                put("shop", "PRAYAG DEEN & SONS")
                put("exportedAtIst", TimezoneUtils.formatDateTimeDisplay(null))
                put("ordersCount", orders.size)
                put("financialsCount", financials.size)
                put("customersCount", customers.size)
                put("auditLogsCount", audit.size)
                put("orders", JSONArray(orders.map { JSONObject(it ?: emptyMap<String, Any>()) }))
                put("orderFinancials", JSONArray(financials.map { JSONObject(it ?: emptyMap<String, Any>()) }))
                put("customers", JSONArray(customers.map { JSONObject(it ?: emptyMap<String, Any>()) }))
            }
            Result.success(root.toString(2))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "backup")
            Result.failure(e)
        }
    }
}
