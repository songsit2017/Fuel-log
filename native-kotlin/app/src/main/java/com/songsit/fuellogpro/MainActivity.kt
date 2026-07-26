package com.songsit.fuellogpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.songsit.fuellogpro.auth.GoogleAuthRepository
import com.songsit.fuellogpro.data.firebase.FirestoreVehicleRepository
import com.songsit.fuellogpro.ui.vehicles.VehiclesScreen
import com.songsit.fuellogpro.ui.vehicles.VehiclesViewModel
import com.songsit.fuellogpro.ui.vehicles.VehiclesViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val auth = GoogleAuthRepository()
        val vehicles = FirestoreVehicleRepository()
        setContent {
            MaterialTheme {
                Surface {
                    val viewModel: VehiclesViewModel = viewModel(
                        factory = VehiclesViewModelFactory(vehicles),
                    )
                    val state by viewModel.uiState.collectAsState()
                    LaunchedEffect(auth.currentUid) {
                        auth.currentUid?.let(viewModel::onSignedIn)
                    }
                    VehiclesScreen(state = state)
                }
            }
        }
    }
}

