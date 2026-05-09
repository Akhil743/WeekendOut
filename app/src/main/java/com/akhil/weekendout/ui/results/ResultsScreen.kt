package com.akhil.weekendout.ui.results

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.akhil.weekendout.data.model.RecommendedCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    onOpenDetail: (String) -> Unit,
    onBack: () -> Unit,
    vm: ResultsViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Picks for you") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when (val s = state) {
                ResultsState.Idle, ResultsState.Loading -> CircularProgressIndicator()
                is ResultsState.Loaded -> ResultsList(s.cards, onOpenDetail)
                ResultsState.NoMatches -> EmptyOrError(
                    title = "Nothing matches yet",
                    body = "Loosen the vibe or crowd filters and try again.",
                    onAction = onBack, actionLabel = "Edit filters"
                )
                is ResultsState.Error -> EmptyOrError(
                    title = "Couldn't fetch picks",
                    body = s.message,
                    onAction = { vm.fetch() }, actionLabel = "Retry"
                )
            }
        }
    }
}

@Composable
private fun ResultsList(cards: List<RecommendedCard>, onOpen: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(cards, key = { it.place.id }) { card -> ResultCard(card) { onOpen(card.place.id) } }
    }
}

@Composable
private fun ResultCard(card: RecommendedCard, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            AsyncImage(
                model = card.place.photo_urls.firstOrNull(),
                contentDescription = card.place.name,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(20.dp, 20.dp, 0.dp, 0.dp)),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(card.place.name, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold))
                Text(
                    card.why,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (card.place.drive_time_from_blr_min > 0) {
                        AssistChip(
                            onClick = {},
                            label = { Text("${card.place.drive_time_from_blr_min}m drive") },
                            leadingIcon = {
                                Icon(Icons.Outlined.AccessTime, null, modifier = Modifier.size(AssistChipDefaults.IconSize))
                            }
                        )
                    }
                    val budget = "₹".repeat(card.place.budget.coerceIn(1, 4))
                    AssistChip(onClick = {}, label = { Text(budget) })
                    card.place.region?.takeIf { it.isNotBlank() }?.let {
                        AssistChip(onClick = {}, label = { Text(it) })
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyOrError(title: String, body: String, onAction: () -> Unit, actionLabel: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onAction) { Text(actionLabel) }
    }
}
