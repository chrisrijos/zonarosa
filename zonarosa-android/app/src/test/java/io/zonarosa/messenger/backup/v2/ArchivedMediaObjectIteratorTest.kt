package io.zonarosa.messenger.backup.v2

import assertk.assertThat
import assertk.assertions.hasSize
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import org.junit.Before
import org.junit.Test
import io.zonarosa.core.models.backup.MediaRootBackupKey
import io.zonarosa.core.util.Base64
import io.zonarosa.core.util.Util
import io.zonarosa.messenger.MockCursor
import io.zonarosa.messenger.keyvalue.BackupValues
import io.zonarosa.messenger.keyvalue.ZonaRosaStore

class ArchivedMediaObjectIteratorTest {
  private val cursor = mockk<MockCursor>(relaxed = true) {
    every { getString(any()) } returns Base64.encodeWithPadding(Util.getSecretBytes(32))
    every { moveToPosition(any()) } answers { callOriginal() }
    every { moveToNext() } answers { callOriginal() }
    every { position } answers { callOriginal() }
    every { isLast } answers { callOriginal() }
    every { isAfterLast } answers { callOriginal() }
  }

  @Before
  fun setup() {
    val mockBackupValues = mockk<BackupValues>()
    every { mockBackupValues.mediaRootBackupKey } returns MediaRootBackupKey(ByteArray(32))

    mockkObject(ZonaRosaStore)
    every { ZonaRosaStore.backup } returns mockBackupValues
  }

  @Test
  fun `Given a cursor with 0 items, when I convert to a list, then I expect a size of 0`() {
    runTest(0)
  }

  @Test
  fun `Given a cursor with 100 items, when I convert to a list, then I expect a size of 100`() {
    runTest(100)
  }

  private fun runTest(size: Int) {
    every { cursor.count } returns size
    val iterator = ArchiveMediaItemIterator(cursor)

    val list = iterator.asSequence().toList()

    assertThat(list).hasSize(size)
  }
}
