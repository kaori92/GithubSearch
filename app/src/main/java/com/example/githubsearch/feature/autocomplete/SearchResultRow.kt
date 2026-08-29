package com.example.githubsearch.feature.autocomplete

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.githubsearch.R
import com.example.githubsearch.core.model.SearchItem

/**
 * One autocomplete result. Public so another screen can reuse the row on its own, and so
 * [GithubAutocomplete] callers can replace it through `itemContent` without touching internals.
 */
@Composable
fun SearchResultRow(
    item: SearchItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = when (item) {
        is SearchItem.User -> item.login
        is SearchItem.Repository -> item.name
    }
    val subtitle = when (item) {
        is SearchItem.User -> null
        is SearchItem.Repository ->
            item.description?.takeIf(String::isNotBlank)
                ?: item.ownerLogin?.takeIf(String::isNotBlank)
    }
    val typeLabel = when (item) {
        is SearchItem.User -> stringResource(R.string.autocomplete_type_user)
        is SearchItem.Repository -> stringResource(R.string.autocomplete_type_repository)
    }

    ListItem(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(onClickLabel = title, onClick = onClick)
            .semantics(mergeDescendants = true) {},
        leadingContent = {
            Avatar(
                url = item.avatarUrl,
                contentDescription = stringResource(R.string.autocomplete_avatar_of, title),
                shape = if (item is SearchItem.User) CircleShape else RoundedCornerShape(8.dp),
                fallback = if (item is SearchItem.User) {
                    Icons.Outlined.Person
                } else {
                    Icons.Outlined.Folder
                },
                fallbackDescription = typeLabel,
            )
        },
        headlineContent = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = if (subtitle != null) {
            { Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        } else {
            null
        },
        trailingContent = { Text(typeLabel, style = MaterialTheme.typography.labelMedium) },
    )
}

@Composable
private fun Avatar(
    url: String?,
    contentDescription: String,
    shape: Shape,
    fallback: ImageVector,
    fallbackDescription: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (url != null) {
            // AsyncImage rather than SubcomposeAsyncImage: subcomposing every row costs an extra
            // measure pass in a LazyColumn.
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(40.dp),
            )
        } else {
            Icon(
                imageVector = fallback,
                contentDescription = fallbackDescription,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
