package com.example.oniongrade.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.oniongrade.ui.theme.HealthyGreen
import com.example.oniongrade.ui.theme.OnionPurple
import com.example.oniongrade.ui.theme.OnionPurpleLight

@Composable
fun BuyerDashboard(
    onNavigateToCapture: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToReports: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "🛒 Buyer Quality Verification",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardCard(
                title = "Check Quality",
                description = "Upload onion sample image to verify",
                icon = Icons.Default.Search,
                accentColor = OnionPurple,
                backgroundColor = OnionPurpleLight,
                onClick = onNavigateToCapture,
                modifier = Modifier.weight(1f)
            )

            DashboardCard(
                title = "Quality Results",
                description = "Inspect healthy % & defect analysis",
                icon = Icons.Default.Assessment,
                accentColor = HealthyGreen,
                backgroundColor = Color(0xFFE8F5E9),
                onClick = onNavigateToHistory,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardCard(
                title = "AI Reports",
                description = "Download/Share verification PDF",
                icon = Icons.Default.Description,
                accentColor = Color(0xFF1976D2),
                backgroundColor = Color(0xFFE3F2FD),
                onClick = onNavigateToReports,
                modifier = Modifier.weight(1f)
            )

            DashboardCard(
                title = "Scan History",
                description = "Review past seller batch verifications",
                icon = Icons.Default.History,
                accentColor = Color(0xFFF57C00),
                backgroundColor = Color(0xFFFFF3E0),
                onClick = onNavigateToHistory,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
