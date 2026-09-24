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
        MerchantProductLinkEntity::class,
        MerchantEntity::class,
        TaxonomyNodeEntity::class,
        TaxonomyProductLinkEntity::class,
    ],
    version = AppDatabase.VERSION,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun receiptDao(): ReceiptDao
    abstract fun merchantProductLinkDao(): MerchantProductLinkDao
    abstract fun merchantDao(): MerchantDao
    abstract fun taxonomyDao(): TaxonomyDao

    companion object {
        const val VERSION = 7

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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE receipts ADD COLUMN issuedDate TEXT",
                )
                db.execSQL(
                    """
                    UPDATE receipts
                    SET issuedDate =
                        substr(issuedAt, 7, 4) || '-' ||
                        substr(issuedAt, 4, 2) || '-' ||
                        substr(issuedAt, 1, 2)
                    WHERE issuedAt IS NOT NULL
                      AND length(issuedAt) >= 10
                    """.trimIndent(),
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS merchant_product_links (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        merchantCnpj TEXT NOT NULL,
                        itemCode TEXT,
                        fiscalDescription TEXT NOT NULL,
                        fiscalSearchKey TEXT NOT NULL,
                        productId INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        lastUsedAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS
                    index_merchant_product_links_merchantCnpj_itemCode
                    ON merchant_product_links(merchantCnpj, itemCode)
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS
                    index_merchant_product_links_merchantCnpj_fiscalSearchKey
                    ON merchant_product_links(merchantCnpj, fiscalSearchKey)
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    index_merchant_product_links_productId
                    ON merchant_product_links(productId)
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE receipt_items ADD COLUMN correctedDescription TEXT")
                db.execSQL("ALTER TABLE receipt_items ADD COLUMN correctedQuantity TEXT")
                db.execSQL("ALTER TABLE receipt_items ADD COLUMN correctedUnit TEXT")
                db.execSQL("ALTER TABLE receipt_items ADD COLUMN correctedUnitPrice TEXT")
                db.execSQL("ALTER TABLE receipt_items ADD COLUMN correctedTotalAmount TEXT")
                db.execSQL("ALTER TABLE receipt_items ADD COLUMN correctedAt INTEGER")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE receipts ADD COLUMN sourceType TEXT NOT NULL DEFAULT 'NFCE'")
                db.execSQL("ALTER TABLE receipts ADD COLUMN createdById TEXT")
                db.execSQL("ALTER TABLE receipts ADD COLUMN createdByName TEXT")
                db.execSQL("ALTER TABLE receipt_items ADD COLUMN correctedById TEXT")
                db.execSQL("ALTER TABLE receipt_items ADD COLUMN correctedByName TEXT")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS merchants (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        displayName TEXT NOT NULL,
                        cnpjDigits TEXT,
                        searchKey TEXT NOT NULL,
                        active INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_merchants_cnpjDigits ON merchants(cnpjDigits)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_merchants_searchKey ON merchants(searchKey)",
                )

                db.execSQL("ALTER TABLE receipts ADD COLUMN merchantId INTEGER")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_receipts_merchantId ON receipts(merchantId)",
                )

                db.execSQL(
                    """
                    INSERT OR IGNORE INTO merchants(
                        displayName,
                        cnpjDigits,
                        searchKey,
                        active,
                        createdAt,
                        updatedAt
                    )
                    SELECT
                        COALESCE(NULLIF(TRIM(merchantName), ''), 'Estabelecimento'),
                        REPLACE(REPLACE(REPLACE(COALESCE(merchantCnpj, ''), '.', ''), '/', ''), '-', ''),
                        UPPER(TRIM(COALESCE(NULLIF(merchantName, ''), 'Estabelecimento'))),
                        1,
                        MIN(createdAt),
                        MAX(createdAt)
                    FROM receipts
                    WHERE length(
                        REPLACE(REPLACE(REPLACE(COALESCE(merchantCnpj, ''), '.', ''), '/', ''), '-', '')
                    ) = 14
                    GROUP BY REPLACE(REPLACE(REPLACE(COALESCE(merchantCnpj, ''), '.', ''), '/', ''), '-', '')
                    """.trimIndent(),
                )

                db.execSQL(
                    """
                    UPDATE receipts
                    SET merchantId = (
                        SELECT m.id
                        FROM merchants m
                        WHERE m.cnpjDigits = REPLACE(
                            REPLACE(
                                REPLACE(COALESCE(receipts.merchantCnpj, ''), '.', ''),
                                '/',
                                ''
                            ),
                            '-',
                            ''
                        )
                        LIMIT 1
                    )
                    WHERE merchantId IS NULL
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE merchants ADD COLUMN segmentNodeId INTEGER")
                db.execSQL("ALTER TABLE merchants ADD COLUMN cnaeMain TEXT")
                db.execSQL("ALTER TABLE merchants ADD COLUMN segmentSource TEXT")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_merchants_segmentNodeId ON merchants(segmentNodeId)",
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS taxonomy_nodes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        stableKey TEXT NOT NULL,
                        parentId INTEGER,
                        level TEXT NOT NULL,
                        name TEXT NOT NULL,
                        searchKey TEXT NOT NULL,
                        builtIn INTEGER NOT NULL,
                        active INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_taxonomy_nodes_stableKey ON taxonomy_nodes(stableKey)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_taxonomy_nodes_parentId ON taxonomy_nodes(parentId)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_taxonomy_nodes_level ON taxonomy_nodes(level)",
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS taxonomy_product_links (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        taxonomyNodeId INTEGER NOT NULL,
                        productId INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS
                    index_taxonomy_product_links_taxonomyNodeId_productId
                    ON taxonomy_product_links(taxonomyNodeId, productId)
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_taxonomy_product_links_productId ON taxonomy_product_links(productId)",
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .build()
                    .also { instance = it }
            }

        @Synchronized
        fun closeForRestore() {
            instance?.close()
            instance = null
        }
    }
}
