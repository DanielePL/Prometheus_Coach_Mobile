package com.prometheuscoach.mobile.ui.screens.coach

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prometheuscoach.mobile.data.model.CoachSetCard
import com.prometheuscoach.mobile.ui.components.GlowAvatarLarge
import com.prometheuscoach.mobile.ui.components.GradientBackground
import com.prometheuscoach.mobile.ui.theme.DarkSurface
import com.prometheuscoach.mobile.ui.theme.PrometheusOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachSetCardScreen(
    coachId: String,
    onNavigateBack: () -> Unit,
    viewModel: CoachSetCardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(coachId) {
        viewModel.loadCoachProfile(coachId)
    }

    GradientBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Coach Profile", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrometheusOrange)
                    }
                }
                state.error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = state.error ?: "Failed to load profile",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                state.profile != null -> {
                    CoachSetCardContent(
                        profile = state.profile!!,
                        modifier = Modifier.padding(paddingValues),
                        onSocialMediaClick = { url ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CoachSetCardContent(
    profile: CoachSetCard,
    modifier: Modifier = Modifier,
    onSocialMediaClick: (String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main SetCard
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = DarkSurface.copy(alpha = 0.8f)
            ),
            border = BorderStroke(2.dp, PrometheusOrange.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar with glow ring
                GlowAvatarLarge(
                    avatarUrl = profile.avatarUrl,
                    name = profile.displayName
                )

                // Verified badge
                if (profile.isVerified) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified",
                            tint = PrometheusOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Verified Coach",
                            style = MaterialTheme.typography.labelMedium,
                            color = PrometheusOrange
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Name
                Text(
                    text = profile.displayName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                // Coach Badge
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = PrometheusOrange.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, PrometheusOrange.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = PrometheusOrange,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PROMETHEUS COACH",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrometheusOrange,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Specialization
                if (!profile.specialization.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = profile.specialization,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }

                // Experience
                if (profile.yearsExperience != null && profile.yearsExperience > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${profile.yearsExperience} years experience",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                // Bio
                if (!profile.bio.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 32.dp),
                        color = Color.White.copy(alpha = 0.1f)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = profile.bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }

                // Social Media Links
                if (profile.hasSocialMedia) {
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 32.dp),
                        color = Color.White.copy(alpha = 0.1f)
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "CONNECT",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        profile.instagramUrl?.let { url ->
                            SocialMediaButton(
                                icon = Icons.Default.CameraAlt,
                                label = "Instagram",
                                onClick = { onSocialMediaClick(url) }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        profile.tiktokUrl?.let { url ->
                            SocialMediaButton(
                                icon = Icons.Default.MusicNote,
                                label = "TikTok",
                                onClick = { onSocialMediaClick(url) }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        profile.youtubeUrl?.let { url ->
                            SocialMediaButton(
                                icon = Icons.Default.PlayCircle,
                                label = "YouTube",
                                onClick = { onSocialMediaClick(url) }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        profile.twitterUrl?.let { url ->
                            SocialMediaButton(
                                icon = Icons.Default.Tag,
                                label = "X",
                                onClick = { onSocialMediaClick(url) }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        profile.websiteUrl?.let { url ->
                            SocialMediaButton(
                                icon = Icons.Default.Language,
                                label = "Web",
                                onClick = { onSocialMediaClick(url) }
                            )
                        }
                    }
                }

                // Certifications
                if (!profile.certifications.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 32.dp),
                        color = Color.White.copy(alpha = 0.1f)
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "CERTIFICATIONS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    profile.certifications.forEach { cert ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = PrometheusOrange,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = cert,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SocialMediaButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = PrometheusOrange.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, PrometheusOrange.copy(alpha = 0.3f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = PrometheusOrange,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}