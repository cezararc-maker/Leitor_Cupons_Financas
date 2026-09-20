package br.com.leitorcuponsfinancas.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProductEntity::class,
        ReceiptEntity::class,
        ReceiptItemEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun receiptDao(): ReceiptDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS receipts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        accessKey TEXT NOT NULL,
                        sourceUrl TEXT NOT NULL,
                        merchantName TEXT,
                        merchantCnpj TEXT,
                        merchantAddress TEXT,
                        number TEXT,
                        series TEXT,
                        issuedAt TEXT,
                        totalAmount TEXT,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_receipts_accessKey ON receipts(accessKey)",
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS receipt_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        receiptId INTEGER NOT NULL,
                        lineNumber INTEGER NOT NULL,
                        fiscalDescription TEXT NOT NULL,
                        itemCode TEXT,
                        quantity TEXT,
                        unit TEXT,
                        unitPrice TEXT,
                        totalAmount TEXT,
                        productId INTEGER
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_receipt_items_receiptId ON receipt_items(receiptId)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_receipt_items_productId ON receipt_items(productId)",
                )
            }
        }

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "leitor_cupons_financas.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
