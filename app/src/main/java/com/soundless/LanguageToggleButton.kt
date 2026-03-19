package com.soundless

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LanguageToggleButton(
    currentLanguage: Language,
    onLanguageSelected: (Language) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF2A2A2A))
                .clickable { expanded = true }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                currentLanguage.shortLabel,
                color = Color(0xFF8E8E93),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Color(0xFF2A2A2A),
        ) {
            Language.entries.forEach { lang ->
                DropdownMenuItem(
                    text = {
                        Text(
                            lang.label,
                            color = if (lang == currentLanguage) Color(0xFF4A90D9) else Color.White,
                            fontSize = 15.sp,
                        )
                    },
                    onClick = {
                        onLanguageSelected(lang)
                        expanded = false
                    },
                )
            }
        }
    }
}
