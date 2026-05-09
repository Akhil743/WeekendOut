package com.akhil.weekendout.ui.planner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.akhil.weekendout.R
import com.akhil.weekendout.data.model.Crowd
import com.akhil.weekendout.data.model.DurationClass
import com.akhil.weekendout.data.model.GroupType
import com.akhil.weekendout.data.model.TempPref
import com.akhil.weekendout.ui.common.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    onSubmitted: () -> Unit,
    onOpenSaved: () -> Unit,
    vm: PlannerViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.planner_title)) },
                actions = {
                    IconButton(onClick = onOpenSaved) {
                        Icon(Icons.Outlined.Bookmark, contentDescription = "Saved")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                stringResource(R.string.planner_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ── Duration ──
            SectionHeader(stringResource(R.string.section_when))
            DurationRow(state.durationClass) { vm.setDuration(it) }

            // ── Group ──
            SectionHeader(stringResource(R.string.section_who))
            ChipFlow(
                options = GroupType.entries.map { it to it.wire.replaceFirstChar(Char::titlecase) },
                isSelected = { it == state.groupType },
                onToggle = { vm.setGroup(it) }
            )

            // ── Vibe ──
            SectionHeader(stringResource(R.string.section_vibe))
            ChipFlow(
                options = PlannerViewModel.ALL_VIBES.map { it to it.replaceFirstChar(Char::titlecase) },
                isSelected = { it in state.vibe },
                onToggle = { vm.toggleVibe(it) }
            )

            // ── Crowd ──
            SectionHeader(stringResource(R.string.section_crowd))
            CrowdRow(state.crowd) { vm.setCrowd(it) }

            // ── Temp ──
            SectionHeader(stringResource(R.string.section_temp))
            TempRow(state.tempPref) { vm.setTemp(it) }

            // ── Stay ──
            SectionHeader(stringResource(R.string.section_stay))
            StayRow(state.stayRequired) { vm.setStay(it) }

            // ── Budget ──
            SectionHeader(stringResource(R.string.section_budget) + ": ${"₹".repeat(state.budget)}")
            Slider(
                value = state.budget.toFloat(),
                onValueChange = { vm.setBudget(it.toInt()) },
                valueRange = 1f..4f,
                steps = 2
            )

            // ── Include nearby getaways ──
            StayRow(state.includeNearbyGetaways, label = "Include nearby getaways (Coorg, Ooty…)") {
                vm.setIncludeGetaways(it)
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { vm.submit(onSubmitted) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.cta_recommend))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DurationRow(selected: DurationClass, onSelect: (DurationClass) -> Unit) {
    val items = DurationClass.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        items.forEachIndexed { i, d ->
            SegmentedButton(
                selected = d == selected,
                onClick = { onSelect(d) },
                shape = SegmentedButtonDefaults.itemShape(index = i, count = items.size),
                label = { Text(d.wire) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CrowdRow(selected: Crowd, onSelect: (Crowd) -> Unit) {
    val items = Crowd.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        items.forEachIndexed { i, c ->
            SegmentedButton(
                selected = c == selected,
                onClick = { onSelect(c) },
                shape = SegmentedButtonDefaults.itemShape(index = i, count = items.size),
                label = { Text(c.wire.replaceFirstChar(Char::titlecase)) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TempRow(selected: TempPref, onSelect: (TempPref) -> Unit) {
    val items = TempPref.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        items.forEachIndexed { i, t ->
            SegmentedButton(
                selected = t == selected,
                onClick = { onSelect(t) },
                shape = SegmentedButtonDefaults.itemShape(index = i, count = items.size),
                label = { Text(t.wire.replaceFirstChar(Char::titlecase)) }
            )
        }
    }
}

@Composable
private fun StayRow(checked: Boolean, label: String = "I need accommodation", onChange: (Boolean) -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipFlow(
    options: List<Pair<T, String>>,
    isSelected: (T) -> Boolean,
    onToggle: (T) -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = isSelected(value),
                onClick = { onToggle(value) },
                label = { Text(label) }
            )
        }
    }
}
