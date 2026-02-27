package io.zonarosa.messenger.search

import io.zonarosa.messenger.database.model.ThreadRecord

data class ThreadSearchResult(val results: List<ThreadRecord>, val query: String)
