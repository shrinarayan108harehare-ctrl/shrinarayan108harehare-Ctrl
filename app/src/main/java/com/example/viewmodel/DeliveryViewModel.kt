package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.OrderAndChatRepository
import com.example.model.DeliveryLocation
import com.example.model.DeliveryPerson
import com.example.model.OrderStatus
import com.example.model.PharmacyOrder
import com.example.model.RoutePlan
import com.example.model.UiState
import com.example.util.LocationUtils
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DeliveryViewModel(
    private val appContext: Context,
    private val deliveryPerson: DeliveryPerson,
    private val deliveryUid: String,
    private val orderRepo: OrderAndChatRepository
) : ViewModel() {

    val assignedOrdersState: StateFlow<UiState<List<PharmacyOrder>>> =
        orderRepo.observeDeliveryAssignedOrders(deliveryUid)
            .map<List<PharmacyOrder>, UiState<List<PharmacyOrder>>> { UiState.Success(it) }
            .catch { error ->
                val ffe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (ffe == null && error is CancellationException) throw error
                emit(UiState.Error((ffe ?: error).message ?: "Failed to load assigned orders"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val routePlans: StateFlow<List<RoutePlan>> = orderRepo.observeDeliveryRoutePlans(deliveryPerson.id)
        .catch { if (it is CancellationException) throw it; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val ownLocation: StateFlow<DeliveryLocation?> = orderRepo.observeOwnDeliveryLocation(deliveryPerson.id)
        .catch { if (it is CancellationException) throw it; emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val _feedback = MutableStateFlow<String?>(null)
    val feedback: StateFlow<String?> = _feedback.asStateFlow()

    private var trackingJob: Job? = null

    fun clearFeedback() { _feedback.value = null }

    fun startShiftTracking() {
        if (_isTracking.value) return
        _isTracking.value = true
        trackingJob = viewModelScope.launch {
            while (isActive && _isTracking.value) {
                val gps = LocationUtils.fetchCurrentLocation(appContext)
                val activeOrders = (assignedOrdersState.value as? UiState.Success)?.data
                    ?.count { it.status in listOf("ASSIGNED", "PICKED_UP", "OUT_FOR_DELIVERY") } ?: 0
                if (gps != null) {
                    orderRepo.updateDeliveryShiftLocation(
                        dpId = deliveryPerson.id,
                        dpName = deliveryPerson.name,
                        lat = gps.lat,
                        lng = gps.lng,
                        accuracyMeters = gps.accuracyMeters.toDouble(),
                        trackingActive = true,
                        statusLabel = if (activeOrders > 0) "On Active Delivery ($activeOrders)" else "On Shift (Available)",
                        activeOrderCount = activeOrders.toLong()
                    )
                }
                delay(20_000L) // Battery-conscious 20s periodic update during active shift only
            }
        }
    }

    fun stopShiftTracking() {
        _isTracking.value = false
        trackingJob?.cancel()
        trackingJob = null
        viewModelScope.launch {
            val last = ownLocation.value
            orderRepo.updateDeliveryShiftLocation(
                dpId = deliveryPerson.id,
                dpName = deliveryPerson.name,
                lat = last?.lat ?: 26.8467,
                lng = last?.lng ?: 80.9462,
                accuracyMeters = last?.accuracyMeters ?: 20.0,
                trackingActive = false,
                statusLabel = "Shift Ended",
                activeOrderCount = 0L
            )
            _feedback.value = "Shift ended. GPS location tracking stopped."
        }
    }

    fun updateOrderStatus(order: PharmacyOrder, newStatus: OrderStatus, issueReport: String = "") {
        viewModelScope.launch {
            val dist = if (newStatus == OrderStatus.DELIVERED && order.estimatedDistanceKm > 0.0) {
                order.estimatedDistanceKm
            } else null
            orderRepo.updateOrderStatusByDelivery(order, newStatus, issueReport, dist)
                .onSuccess { _feedback.value = "${order.orderNumber} updated to ${newStatus.labelEn}." }
                .onFailure { _feedback.value = it.localizedMessage }
        }
    }

    override fun onCleared() {
        super.onCleared()
        trackingJob?.cancel()
    }

    companion object {
        fun provideFactory(
            appContext: Context,
            deliveryPerson: DeliveryPerson,
            deliveryUid: String,
            orderRepo: OrderAndChatRepository
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DeliveryViewModel(
                    appContext = appContext.applicationContext,
                    deliveryPerson = deliveryPerson,
                    deliveryUid = deliveryUid,
                    orderRepo = orderRepo
                )
            }
        }
    }
}
