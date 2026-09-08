package dev.dheirav.thirsttrap.feature.diagnose

import dev.dheirav.thirsttrap.ui.AppIcons
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.dheirav.thirsttrap.domain.DiagnosisNode
import dev.dheirav.thirsttrap.domain.DiagnosisTree
import dev.dheirav.thirsttrap.domain.diagnosisTreeById
import dev.dheirav.thirsttrap.domain.diagnosisTrees

/**
 * Requirements item 19.
 *
 * One question at a time, each answerable by looking at the plant rather than
 * by knowing something. Every path ends in what to do, and every tree can
 * reach "this is fine" - a checklist that always concludes something is wrong
 * would only train alarm.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnoseScreen(onBack: () -> Unit, onLogEvent: (() -> Unit)? = null) {
    // Saveable: walking a tree then rotating the phone should not start over.
    var treeId by rememberSaveable { mutableStateOf<String?>(null) }
    var path by rememberSaveable { mutableStateOf(listOf<Int>()) }

    val tree = treeId?.let { diagnosisTreeById(it) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tree?.title ?: "What's wrong?") },
                navigationIcon = {
                    IconButton(onClick = {
                        when {
                            path.isNotEmpty() -> path = path.dropLast(1)
                            treeId != null -> treeId = null
                            else -> onBack()
                        }
                    }) {
                        Icon(AppIcons.arrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (tree == null) {
                Text(
                    "Pick whichever is closest. These ask about things you can see and " +
                        "feel right now, not things you need to already know.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                diagnosisTrees.forEach { t ->
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 64.dp)
                            .clickable { treeId = t.id; path = emptyList() },
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(t.title, fontWeight = FontWeight.SemiBold)
                            Text(
                                t.opener,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
                return@Column
            }

            when (val node = walk(tree, path)) {
                is DiagnosisNode.Question -> {
                    Text(node.prompt, style = MaterialTheme.typography.titleMedium)
                    node.how?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    node.answers.forEachIndexed { index, answer ->
                        Card(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .clickable { path = path + index },
                        ) {
                            Text(answer.label, modifier = Modifier.padding(16.dp))
                        }
                    }
                }

                is DiagnosisNode.Outcome -> {
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (node.benign) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        ),
                    ) {
                        Text(
                            node.verdict,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                    node.whatToDo.forEach {
                        Text("· $it", style = MaterialTheme.typography.bodyMedium)
                    }

                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    onLogEvent?.let {
                        TextButton(onClick = it, modifier = Modifier.fillMaxWidth()) {
                            Text("Log what you found")
                        }
                    }
                    TextButton(
                        onClick = { path = emptyList() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Start this one again") }
                    TextButton(
                        onClick = { treeId = null; path = emptyList() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Something else is wrong") }
                }
            }
        }
    }
}

/** Follows the recorded answers down the tree. */
private fun walk(tree: DiagnosisTree, path: List<Int>): DiagnosisNode {
    var node: DiagnosisNode = tree.root
    for (index in path) {
        val q = node as? DiagnosisNode.Question ?: return node
        node = q.answers.getOrNull(index)?.next ?: return node
    }
    return node
}
