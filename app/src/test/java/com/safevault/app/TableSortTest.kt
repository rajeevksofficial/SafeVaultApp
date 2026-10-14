package com.safevault.app

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Comparator

class TableSortTest {

    private fun createItem(id: Long, title: String, portal: String, username: String): VaultItem {
        return VaultItem(
            id = id,
            title = title,
            portal = portal,
            username = username,
            passwordEncrypted = "enc",
            passwordIv = "iv"
        )
    }

    @Test
    fun testDefaultSortByTitleCaseInsensitive() {
        val item1 = createItem(1, "zebra notes", "z.com", "user1")
        val item2 = createItem(2, "Apple ID", "apple.com", "user2")
        val item3 = createItem(3, "bank of america", "boa.com", "user3")
        val item4 = createItem(4, "Amazon", "amazon.com", "user4")

        val list = listOf(item1, item2, item3, item4)

        val comparator: Comparator<VaultItem> = compareBy(java.lang.String.CASE_INSENSITIVE_ORDER) { it.title }
        val sorted = list.sortedWith(comparator)

        assertEquals("Amazon", sorted[0].title)
        assertEquals("Apple ID", sorted[1].title)
        assertEquals("bank of america", sorted[2].title)
        assertEquals("zebra notes", sorted[3].title)
    }

    @Test
    fun testSortByTitleDescending() {
        val item1 = createItem(1, "alpha", "a.com", "user1")
        val item2 = createItem(2, "Beta", "b.com", "user2")
        val item3 = createItem(3, "gamma", "g.com", "user3")

        val list = listOf(item1, item2, item3)

        val comparator: Comparator<VaultItem> = compareBy(java.lang.String.CASE_INSENSITIVE_ORDER) { it.title }
        val sortedDesc = list.sortedWith(comparator.reversed())

        assertEquals("gamma", sortedDesc[0].title)
        assertEquals("Beta", sortedDesc[1].title)
        assertEquals("alpha", sortedDesc[2].title)
    }
}
