package com.example.webviewdemo

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

object Utils {
//    @Composable
//    fun DownloadProgressDialog(progress: Int) {
//        AlertDialog(
//            onDismissRequest = {},
//            title = {
//                Text(
//                    text = "Downloading...",
//                    style = MaterialTheme.typography.headlineMedium,
//                    color = MaterialTheme.colorScheme.onSurface
//                )
//            },
//            text = {
//                if (progress >= 0) {
//                    Column(
//                        horizontalAlignment = Alignment.CenterHorizontally,
//                        modifier = Modifier.padding(horizontal = 24.dp)
//                    ) {
//                        Text(
//                            text = "Progress: $progress%",
//                            style = MaterialTheme.typography.bodySmall,
//                            color = MaterialTheme.colorScheme.onSurface,
//                            modifier = Modifier.padding(vertical = 8.dp)
//                        )
//                        LinearProgressIndicator(
//                            progress = { progress / 100f },
//                            modifier = Modifier.fillMaxWidth(),
//                            color = MaterialTheme.colorScheme.primary,
//                        )
//                    }
//                } else {
//                    Text(
//                        text = "Download failed.",
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = MaterialTheme.colorScheme.error,
//                        modifier = Modifier.padding(vertical = 8.dp)
//                    )
//                }
//            },
//            confirmButton = {
//                if (progress == 100) {
//                    Toast.makeText(LocalContext.current, "Download Complete", Toast.LENGTH_SHORT)
//                        .show()
//                } else if (progress == -1) {
//                    Toast.makeText(LocalContext.current, "Download Failed", Toast.LENGTH_SHORT)
//                        .show()
//                }
//            },
//            modifier = Modifier.padding(16.dp),
//            shape = MaterialTheme.shapes.medium
//        )
//    }

}