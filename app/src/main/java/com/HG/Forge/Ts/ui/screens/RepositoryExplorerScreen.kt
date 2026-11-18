package com.HG.Forge.Ts.ui.screens

import com.HG.Forge.Ts.ui.localization.forgeText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun RepositoryExplorerScreen(
    modifier: Modifier = Modifier,
    viewModel: RepositoryExplorerViewModel = viewModel()
) {
    var query by remember { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(forgeText("Repository Explorer"), style = MaterialTheme.typography.headlineMedium)
        Text(forgeText("Search public GitHub repositories and inspect basic metadata."),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(forgeText("Repository search")) },
            placeholder = { Text(forgeText("Kotlin Android networking")) },
            singleLine = true
        )
        Button(
            onClick = { viewModel.search(query) },
            enabled = query.trim().length >= 2 && !viewModel.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) { Text(forgeText("Search GitHub")) }

        if (viewModel.isLoading) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator()
                Text(forgeText("Searching public repositories…"))
            }
        }
        viewModel.error?.let {
            Card {
                Text(it, modifier = Modifier.padding(14.dp), color = MaterialTheme.colorScheme.error)
            }
        }
        viewModel.result?.let { result ->
            Text(forgeText("${result.totalCount} matching public repositories (showing ${result.repositories.size})"),
                style = MaterialTheme.typography.titleSmall
            )
            result.repositories.forEach { repository ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(repository.fullName, style = MaterialTheme.typography.titleMedium)
                        if (repository.description.isNotBlank()) {
                            Text(repository.description, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(forgeText("Stars") + ": ${repository.stars}  ·  " + forgeText("Language:") + " " + forgeText(repository.language.ifBlank { "Language not specified" }),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(forgeText("License:") + " ${repository.license}", style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = { uriHandler.openUri(repository.htmlUrl) }) {
                            Text(forgeText("Open on GitHub"))
                        }
                    }
                }
            }
        }
        Text(forgeText("Read-only access. Search terms are sent to GitHub. FORGE-TS does not request a GitHub token, access private repositories or modify repository data. GitHub applies unauthenticated API rate limits."),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
