package com.sirim.scanner.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SirimRecord::class,
        SkuRecord::class,
        DatabaseRecord::class,
        User::class,
        Session::class
    ],
    version = 4,
    exportSchema = true
)
abstract class SirimDatabase : RoomDatabase() {
    abstract fun sirimRecordDao(): SirimRecordDao
    abstract fun skuRecordDao(): SkuRecordDao
    abstract fun databaseRecordDao(): DatabaseRecordDao
    abstract fun userDao(): UserDao
    abstract fun sessionDao(): SessionDao

    companion object {
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("BEGIN TRANSACTION")
                try {
                    database.execSQL(
                        "CREATE TABLE IF NOT EXISTS sku_records (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "barcode TEXT NOT NULL, " +
                            "batch_no TEXT, " +
                            "brand_trademark TEXT, " +
                            "model TEXT, " +
                            "type TEXT, " +
                            "rating TEXT, " +
                            "size TEXT, " +
                            "image_path TEXT, " +
                            "created_at INTEGER NOT NULL, " +
                            "is_verified INTEGER NOT NULL, " +
                            "needs_sync INTEGER NOT NULL, " +
                            "server_id TEXT, " +
                            "last_synced INTEGER)"
                    )
                    database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sku_records_barcode ON sku_records(barcode)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_sku_records_created_at ON sku_records(created_at)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_sku_records_brand_trademark ON sku_records(brand_trademark)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_sku_records_is_verified ON sku_records(is_verified)")
                    database.execSQL("COMMIT")
                } catch (error: Exception) {
                    database.execSQL("ROLLBACK")
                    throw error
                }
            }
        }

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_sirim_records_brand_verified " +
                        "ON sirim_records(brand_trademark, is_verified)"
                )
            }
        }

        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("BEGIN TRANSACTION")
                try {
                    // Add product_name to sku_records
                    database.execSQL("ALTER TABLE sku_records ADD COLUMN product_name TEXT NOT NULL DEFAULT ''")
                    database.execSQL("ALTER TABLE sku_records ADD COLUMN category TEXT")
                    database.execSQL("ALTER TABLE sku_records ADD COLUMN manufacturer TEXT")
                    database.execSQL("ALTER TABLE sku_records ADD COLUMN description TEXT")
                    database.execSQL("ALTER TABLE sku_records ADD COLUMN custom_fields TEXT")
                    database.execSQL("ALTER TABLE sku_records ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0")
                    
                    // Update sirim_records with duplicate detection fields
                    database.execSQL("ALTER TABLE sirim_records ADD COLUMN sku_record_id INTEGER NOT NULL DEFAULT 0")
                    database.execSQL("ALTER TABLE sirim_records ADD COLUMN sirim_serial_normalized TEXT NOT NULL DEFAULT ''")
                    database.execSQL("ALTER TABLE sirim_records ADD COLUMN content_hash TEXT NOT NULL DEFAULT ''")
                    database.execSQL("ALTER TABLE sirim_records ADD COLUMN duplicate_status TEXT NOT NULL DEFAULT 'NEW'")
                    database.execSQL("ALTER TABLE sirim_records ADD COLUMN original_record_id INTEGER")
                    database.execSQL("ALTER TABLE sirim_records ADD COLUMN duplicate_checked_at INTEGER")
                    database.execSQL("ALTER TABLE sirim_records ADD COLUMN duplicate_confidence REAL")
                    database.execSQL("ALTER TABLE sirim_records ADD COLUMN duplicate_reason TEXT")
                    database.execSQL("ALTER TABLE sirim_records ADD COLUMN qr_payload TEXT")
                    database.execSQL("ALTER TABLE sirim_records ADD COLUMN ocr_confidence REAL")
                    database.execSQL("ALTER TABLE sirim_records ADD COLUMN verification_status TEXT")
                    database.execSQL("ALTER TABLE sirim_records ADD COLUMN custom_fields TEXT")
                    database.execSQL("ALTER TABLE sirim_records ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0")
                    
                    // Create new indexes for duplicate detection
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_sirim_records_sirim_serial_normalized ON sirim_records(sirim_serial_normalized)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_sirim_records_content_hash ON sirim_records(content_hash)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_sirim_records_batch_brand ON sirim_records(batch_no, brand_trademark)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_sirim_records_duplicate_status ON sirim_records(duplicate_status)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_sirim_records_sku_record_id ON sirim_records(sku_record_id)")
                    
                    // Create databases table
                    database.execSQL(
                        "CREATE TABLE IF NOT EXISTS databases (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "name TEXT NOT NULL, " +
                            "file_path TEXT NOT NULL, " +
                            "thumbnail_path TEXT, " +
                            "record_count INTEGER NOT NULL DEFAULT 0, " +
                            "unique_count INTEGER NOT NULL DEFAULT 0, " +
                            "duplicate_count INTEGER NOT NULL DEFAULT 0, " +
                            "variant_count INTEGER NOT NULL DEFAULT 0, " +
                            "file_size_bytes INTEGER NOT NULL DEFAULT 0, " +
                            "created_at INTEGER NOT NULL, " +
                            "updated_at INTEGER NOT NULL)"
                    )
                    database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_databases_name ON databases(name)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_databases_created_at ON databases(created_at)")
                    
                    // Create users table
                    database.execSQL(
                        "CREATE TABLE IF NOT EXISTS users (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "username TEXT NOT NULL, " +
                            "password_hash TEXT NOT NULL, " +
                            "role TEXT NOT NULL, " +
                            "created_at INTEGER NOT NULL)"
                    )
                    database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_users_username ON users(username)")
                    
                    // Create sessions table
                    database.execSQL(
                        "CREATE TABLE IF NOT EXISTS sessions (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "user_id INTEGER NOT NULL, " +
                            "last_database_id INTEGER, " +
                            "last_sku_record_id INTEGER, " +
                            "created_at INTEGER NOT NULL, " +
                            "updated_at INTEGER NOT NULL, " +
                            "FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE)"
                    )
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_sessions_user_id ON sessions(user_id)")
                    
                    // Populate normalized serials for existing records
                    database.execSQL("""
                        UPDATE sirim_records 
                        SET sirim_serial_normalized = UPPER(REPLACE(REPLACE(sirim_serial_no, ' ', ''), '-', ''))
                    """)
                    
                    database.execSQL("COMMIT")
                } catch (error: Exception) {
                    database.execSQL("ROLLBACK")
                    throw error
                }
            }
        }
    }
}
