package com.songsit.fuellogpro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage

data class CarMake(val name: String, val domain: String)

val carMakes = listOf(
    CarMake("Abarth", "abarth.com"),
    CarMake("AC Cars", "accars.eu"),
    CarMake("Acura", "acura.com"),
    CarMake("Alfa Romeo", "alfaromeo.com"),
    CarMake("Aston Martin", "astonmartin.com"),
    CarMake("Audi", "audi.com"),
    CarMake("Bentley", "bentleymotors.com"),
    CarMake("BMW", "bmw.com"),
    CarMake("Bugatti", "bugatti.com"),
    CarMake("Buick", "buick.com"),
    CarMake("BYD", "byd.com"),
    CarMake("Cadillac", "cadillac.com"),
    CarMake("Changan", "changan.com.cn"),
    CarMake("Chevrolet", "chevrolet.com"),
    CarMake("Chrysler", "chrysler.com"),
    CarMake("Citroën", "citroen.com"),
    CarMake("Dodge", "dodge.com"),
    CarMake("Ferrari", "ferrari.com"),
    CarMake("Fiat", "fiat.com"),
    CarMake("Ford", "ford.com"),
    CarMake("GWM", "gwm-global.com"),
    CarMake("GMC", "gmc.com"),
    CarMake("Haval", "haval-global.com"),
    CarMake("Honda", "honda.com"),
    CarMake("Hyundai", "hyundai.com"),
    CarMake("Infiniti", "infiniti.com"),
    CarMake("Isuzu", "isuzu.co.jp"),
    CarMake("Jaguar", "jaguar.com"),
    CarMake("Jeep", "jeep.com"),
    CarMake("Kia", "kia.com"),
    CarMake("Lamborghini", "lamborghini.com"),
    CarMake("Land Rover", "landrover.com"),
    CarMake("Lexus", "lexus.com"),
    CarMake("Lincoln", "lincoln.com"),
    CarMake("Lotus", "lotuscars.com"),
    CarMake("Maserati", "maserati.com"),
    CarMake("Mazda", "mazda.com"),
    CarMake("McLaren", "mclaren.com"),
    CarMake("Mercedes-Benz", "mercedes-benz.com"),
    CarMake("MG", "mgmotor.me"),
    CarMake("Mini", "mini.com"),
    CarMake("Mitsubishi", "mitsubishi-motors.com"),
    CarMake("Neta", "hozonauto.com"),
    CarMake("Nissan", "nissan-global.com"),
    CarMake("Ora", "gwm-global.com"),
    CarMake("Peugeot", "peugeot.com"),
    CarMake("Porsche", "porsche.com"),
    CarMake("Ram", "ramtrucks.com"),
    CarMake("Renault", "renault.com"),
    CarMake("Rolls-Royce", "rolls-roycemotorcars.com"),
    CarMake("Subaru", "subaru.com"),
    CarMake("Suzuki", "globalsuzuki.com"),
    CarMake("Tesla", "tesla.com"),
    CarMake("Toyota", "toyota.com"),
    CarMake("Volkswagen", "volkswagen.com"),
    CarMake("Volvo", "volvocars.com")
).sortedBy { it.name }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleMakeSelectionScreen(
    onDismiss: () -> Unit,
    onMakeSelected: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredMakes = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            carMakes
        } else {
            carMakes.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("เลือกยี่ห้อรถ") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "กลับ")
                        }
                    }
                )
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("ค้นหายี่ห้อรถ...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "ล้างการค้นหา")
                            }
                        }
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = MaterialTheme.shapes.medium
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            items(filteredMakes) { make ->
                ListItem(
                    headlineContent = { Text(make.name, fontWeight = FontWeight.SemiBold) },
                    leadingContent = {
                        SubcomposeAsyncImage(
                            model = "https://www.carlogos.org/logo/${make.name.replace(" ", "-")}-logo.png",
                            contentDescription = make.name,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .padding(4.dp),
                            contentScale = ContentScale.Fit,
                            loading = {
                                Surface(
                                    modifier = Modifier.size(40.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(make.name.take(1).uppercase())
                                    }
                                }
                            },
                            error = {
                                Surface(
                                    modifier = Modifier.size(40.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(make.name.take(1).uppercase())
                                    }
                                }
                            }
                        )
                    },
                    modifier = Modifier.clickable {
                        onMakeSelected(make.name)
                        onDismiss()
                    }
                )
            }
        }
    }
}
