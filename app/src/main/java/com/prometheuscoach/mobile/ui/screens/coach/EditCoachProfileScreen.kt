package com.prometheuscoach.mobile.ui.screens.coach

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prometheuscoach.mobile.ui.components.GradientBackground
import com.prometheuscoach.mobile.ui.theme.DarkSurface
import com.prometheuscoach.mobile.ui.theme.PrometheusOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCoachProfileScreen(
    onNavigateBack: () -> Unit,
    onPreviewSetCard: () -> Unit,
    viewModel: EditCoachProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // Show toast on save success
    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            Toast.makeText(context, "Profile saved!", Toast.LENGTH_SHORT).show()
            viewModel.clearSaveSuccess()
        }
    }

    // Show toast on error
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    GradientBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Edit Public Profile", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = onPreviewSetCard,
                            enabled = !state.isLoading
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = PrometheusOrange
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Preview", color = PrometheusOrange)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrometheusOrange)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Info card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = PrometheusOrange.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = PrometheusOrange
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "This is your public profile that users can see. Your private stats like client count are not shown here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Bio Section
                    SectionCard(
                        title = "ABOUT YOU",
                        icon = Icons.Default.Person
                    ) {
                        OutlinedTextField(
                            value = state.bio,
                            onValueChange = { viewModel.updateBio(it) },
                            label = { Text("Bio") },
                            placeholder = { Text("Tell clients about yourself...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            colors = coachTextFieldColors()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = state.specialization,
                            onValueChange = { viewModel.updateSpecialization(it) },
                            label = { Text("Specialization") },
                            placeholder = { Text("e.g., Strength Training, HIIT, Bodybuilding") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = coachTextFieldColors()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = state.yearsExperience,
                            onValueChange = { viewModel.updateYearsExperience(it) },
                            label = { Text("Years of Experience") },
                            placeholder = { Text("e.g., 5") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = coachTextFieldColors()
                        )
                    }

                    // Social Media Section
                    SectionCard(
                        title = "SOCIAL MEDIA",
                        icon = Icons.Default.Share
                    ) {
                        SocialMediaField(
                            value = state.instagramHandle,
                            onValueChange = { viewModel.updateInstagram(it) },
                            label = "Instagram",
                            placeholder = "@username",
                            icon = Icons.Default.CameraAlt
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        SocialMediaField(
                            value = state.tiktokHandle,
                            onValueChange = { viewModel.updateTikTok(it) },
                            label = "TikTok",
                            placeholder = "@username",
                            icon = Icons.Default.MusicNote
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        SocialMediaField(
                            value = state.youtubeHandle,
                            onValueChange = { viewModel.updateYouTube(it) },
                            label = "YouTube",
                            placeholder = "@channel",
                            icon = Icons.Default.PlayCircle
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        SocialMediaField(
                            value = state.twitterHandle,
                            onValueChange = { viewModel.updateTwitter(it) },
                            label = "X / Twitter",
                            placeholder = "@username",
                            icon = Icons.Default.Tag
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        SocialMediaField(
                            value = state.websiteUrl,
                            onValueChange = { viewModel.updateWebsite(it) },
                            label = "Website",
                            placeholder = "https://yoursite.com",
                            icon = Icons.Default.Language
                        )
                    }

                    // Save Button
                    Button(
                        onClick = { viewModel.saveProfile() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !state.isSaving,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrometheusOrange
                        )
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Save Profile",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface.copy(alpha = 0.6f)
        ),
        border = BorderStroke(1.dp, PrometheusOrange.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrometheusOrange,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrometheusOrange,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            content()
        }
    }
}

@Composable
private fun SocialMediaField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrometheusOrange.copy(alpha = 0.7f)
            )
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        colors = coachTextFieldColors()
    )
}

@Composable
private fun coachTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PrometheusOrange,
    focusedLabelColor = PrometheusOrange,
    cursorColor = PrometheusOrange,
    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
    unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
)