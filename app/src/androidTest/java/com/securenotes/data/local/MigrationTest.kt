package com.securenotes.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Instrumented test for Room database migrations.
 * Verifies data integrity across schema changes.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    companion object {
        private const val TEST_DB = "migration_test_db"
    }

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        NoteDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    /**
     * Tests migration from V1 to V2.
     * V2 adds: isDeleted, deletedAt, isLocked columns
     */
    @Test
    @Throws(IOException::class)
    fun migrate1To2() {
        // Create V1 database with test data
        helper.createDatabase(TEST_DB, 1).apply {
            // Insert test note using V1 schema
            execSQL("""
                INSERT INTO notes (id, encryptedTitle, encryptedContent, createdAt, updatedAt, isFavorite, colorTag)
                VALUES (1, 'test_title', 'test_content', 1000, 2000, 0, NULL)
            """.trimIndent())
            close()
        }

        // Run migration
        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, NoteDatabase.MIGRATION_1_2)

        // Verify data integrity
        val cursor = db.query("SELECT * FROM notes WHERE id = 1")
        assertTrue(cursor.moveToFirst())

        // Verify existing columns preserved
        assertEquals("test_title", cursor.getString(cursor.getColumnIndex("encryptedTitle")))
        assertEquals("test_content", cursor.getString(cursor.getColumnIndex("encryptedContent")))
        assertEquals(1000L, cursor.getLong(cursor.getColumnIndex("createdAt")))
        assertEquals(2000L, cursor.getLong(cursor.getColumnIndex("updatedAt")))
        assertEquals(0, cursor.getInt(cursor.getColumnIndex("isFavorite")))

        // Verify new columns have correct default values
        assertEquals(0, cursor.getInt(cursor.getColumnIndex("isDeleted")))
        assertTrue(cursor.isNull(cursor.getColumnIndex("deletedAt")))
        assertEquals(0, cursor.getInt(cursor.getColumnIndex("isLocked")))

        cursor.close()
        db.close()
    }

    /**
     * Tests that multiple notes are preserved during migration.
     */
    @Test
    @Throws(IOException::class)
    fun migrate1To2_multipleNotes() {
        // Create V1 database with multiple notes
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL("""
                INSERT INTO notes (id, encryptedTitle, encryptedContent, createdAt, updatedAt, isFavorite, colorTag)
                VALUES (1, 'title1', 'content1', 1000, 2000, 0, NULL)
            """.trimIndent())
            execSQL("""
                INSERT INTO notes (id, encryptedTitle, encryptedContent, createdAt, updatedAt, isFavorite, colorTag)
                VALUES (2, 'title2', 'content2', 3000, 4000, 1, 1)
            """.trimIndent())
            execSQL("""
                INSERT INTO notes (id, encryptedTitle, encryptedContent, createdAt, updatedAt, isFavorite, colorTag)
                VALUES (3, 'title3', 'content3', 5000, 6000, 0, 2)
            """.trimIndent())
            close()
        }

        // Run migration
        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, NoteDatabase.MIGRATION_1_2)

        // Verify all notes preserved
        val cursor = db.query("SELECT COUNT(*) FROM notes")
        assertTrue(cursor.moveToFirst())
        assertEquals(3, cursor.getInt(0))
        cursor.close()

        // Verify each note has correct defaults
        val notesCursor = db.query("SELECT * FROM notes ORDER BY id")
        assertEquals(3, notesCursor.count)
        
        while (notesCursor.moveToNext()) {
            assertEquals(0, notesCursor.getInt(notesCursor.getColumnIndex("isDeleted")))
            assertTrue(notesCursor.isNull(notesCursor.getColumnIndex("deletedAt")))
            assertEquals(0, notesCursor.getInt(notesCursor.getColumnIndex("isLocked")))
        }
        
        notesCursor.close()
        db.close()
    }
}
