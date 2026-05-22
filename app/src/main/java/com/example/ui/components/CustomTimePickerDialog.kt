package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun CustomTimePickerDialog(
    initialTime: String, // format "HH:mm"
    onDismiss: () -> Unit,
    onTimeSelected: (String) -> Unit
) {
    val initialParts = initialTime.split(":")
    val initialHour = initialParts.getOrNull(0) ?: "08"
    val initialMinute = initialParts.getOrNull(1) ?: "00"

    var hourStr by remember { mutableStateOf(initialHour) }
    var minuteStr by remember { mutableStateOf(initialMinute) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Nhập Thời Gian (HH:mm)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hour TextField
                    OutlinedTextField(
                        value = hourStr,
                        onValueChange = {
                            if (it.length <= 2 && it.all { char -> char.isDigit() }) {
                                hourStr = it
                            }
                        },
                        label = { Text("Giờ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(80.dp),
                        singleLine = true
                    )

                    Text(
                        text = ":",
                        style = MaterialTheme.typography.displaySmall,
                        modifier = Modifier.padding(horizontal = 12.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Minute TextField
                    OutlinedTextField(
                        value = minuteStr,
                        onValueChange = {
                            if (it.length <= 2 && it.all { char -> char.isDigit() }) {
                                minuteStr = it
                            }
                        },
                        label = { Text("Phút") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(80.dp),
                        singleLine = true
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("HỦY")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        onClick = {
                            val hStr = hourStr.padStart(2, '0')
                            val mStr = minuteStr.padStart(2, '0')
                            val h = hStr.toIntOrNull()
                            val m = mStr.toIntOrNull()
                            if (h == null || h !in 0..23) {
                                errorMessage = "Giờ hợp lệ từ 00 đến 23"
                                return@Button
                            }
                            if (m == null || m !in 0..59) {
                                errorMessage = "Phút hợp lệ từ 00 đến 59"
                                return@Button
                            }
                            val formatted = String.format("%02d:%02d", h, m)
                            onTimeSelected(formatted)
                        }
                    ) {
                        Text("XÁC NHẬN")
                    }
                }
            }
        }
    }
}
