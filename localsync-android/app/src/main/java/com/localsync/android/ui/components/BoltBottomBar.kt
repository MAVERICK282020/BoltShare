package com.localsync.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class BoltTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    FILES("File Browser", Icons.Default.Folder),
    TRANSFERS("Transfers", Icons.Default.SwapVert),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun BoltBottomBar(
    currentTab: BoltTab,
    onSelectTab: (BoltTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0F1320))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BoltTab.values().forEach { tab ->
            val isSelected = tab == currentTab
            val activeBg = if (isSelected) Color(0xFF232B42) else Color.Transparent
            val contentColor = if (isSelected) Color(0xFF818CF8) else Color(0xFF64748B)

            Column(
                modifier = Modifier
                    .background(activeBg, RoundedCornerShape(14.dp))
                    .clickable { onSelectTab(tab) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.label,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tab.label,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = contentColor
                )
            }
        }
    }
}
