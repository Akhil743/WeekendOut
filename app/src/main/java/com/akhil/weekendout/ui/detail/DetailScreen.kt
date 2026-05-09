package com.akhil.weekendout.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkAdded
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.akhil.weekendout.data.model.Place
import com.akhil.weekendout.ui.common.SignInPromptSheet
import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    placeId: String,
    onBack: () -> Unit,
    vm: DetailViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(placeId) { vm.load(placeId) }

    val ctx = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    if (state.showSignInPrompt) {
        SignInPromptSheet(
            isLoading = state.signingIn,
            onSignIn = {
                (ctx as? ComponentActivity)?.let { vm.startGoogleSignIn(it) }
            },
            onSkip = { vm.skipSignIn() },
            onDismiss = { vm.dismissSignInPrompt() }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(state.place?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.toggleSave() }) {
                        val icon = if (state.isSaved) Icons.Outlined.BookmarkAdded else Icons.Outlined.Bookmark
                        Icon(icon, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        val place = state.place
        if (place == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            PhotoPager(place)
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(place.name, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold))
                place.address?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (place.short_summary.isNotBlank()) {
                    Text(place.short_summary, style = MaterialTheme.typography.bodyLarge)
                }
                TagRow(place)
                OutlinedButton(
                    onClick = {
                        val uri = Uri.parse("geo:0,0?q=${Uri.encode(place.maps_query.ifBlank { place.name })}")
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        ctx.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Map, contentDescription = null)
                    Text("  Open in Maps")
                }
                Button(
                    onClick = { vm.toggleSave() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.isSaved) "Saved" else "Save place")
                }
            }
        }
    }
}

@Composable
private fun PhotoPager(place: Place) {
    if (place.photo_urls.isEmpty()) return
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(place.photo_urls) { url ->
            AsyncImage(
                model = url,
                contentDescription = place.name,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun TagRow(place: Place) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        place.vibe.forEach { AssistChip(onClick = {}, label = { Text(it) }) }
        place.good_for.forEach { AssistChip(onClick = {}, label = { Text("for $it") }) }
        place.crowd?.let { AssistChip(onClick = {}, label = { Text("$it crowd") }) }
        place.temp_class?.let { AssistChip(onClick = {}, label = { Text(it) }) }
        if (place.has_stay) AssistChip(onClick = {}, label = { Text("stay available") })
    }
}
