package com.example.githubsearch.core.model

/**
 * Display order for the merged list: case-insensitive by name, then users before repositories,
 * then id. The last two keys only exist so that equal names never reorder between two identical
 * responses — without them the merged order would depend on which request happened to return first.
 */
val SearchItemOrder: Comparator<SearchItem> =
    compareBy(String.CASE_INSENSITIVE_ORDER, SearchItem::sortKey)
        .thenBy { item -> if (item is SearchItem.User) 0 else 1 }
        .thenBy(SearchItem::id)
