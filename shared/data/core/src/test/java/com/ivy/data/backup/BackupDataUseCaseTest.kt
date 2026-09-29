package com.ivy.data.backup

import com.ivy.base.TestDispatchersProvider
import com.ivy.base.di.KotlinxSerializationModule
import com.ivy.data.DataObserver
import com.ivy.data.db.dao.fake.FakeAccountDao
import com.ivy.data.db.dao.fake.FakeBudgetDao
import com.ivy.data.db.dao.fake.FakeCategoryDao
import com.ivy.data.db.dao.fake.FakeCreditCardDao
import com.ivy.data.db.dao.fake.FakeLoanDao
import com.ivy.data.db.dao.fake.FakeLoanRecordDao
import com.ivy.data.db.dao.fake.FakePlannedPaymentDao
import com.ivy.data.db.dao.fake.FakeSettingsDao
import com.ivy.data.db.dao.fake.FakeTagAssociationDao
import com.ivy.data.db.dao.fake.FakeTagDao
import com.ivy.data.db.dao.fake.FakeTransactionDao
import com.ivy.data.db.entity.AccountEntity
import com.ivy.data.db.entity.CreditCardEntity
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.CreditCardRepository
import com.ivy.data.repository.CurrencyRepository
import com.ivy.data.repository.fake.fakeRepositoryMemoFactory
import com.ivy.data.repository.mapper.AccountMapper
import com.ivy.data.repository.mapper.CreditCardMapper
import com.ivy.data.testResource
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.UUID

class BackupDataUseCaseTest {
    private fun newBackupDataUseCase(
        accountDao: FakeAccountDao = FakeAccountDao(),
        categoryDao: FakeCategoryDao = FakeCategoryDao(),
        transactionDao: FakeTransactionDao = FakeTransactionDao(),
        plannedPaymentDao: FakePlannedPaymentDao = FakePlannedPaymentDao(),
        budgetDao: FakeBudgetDao = FakeBudgetDao(),
        settingsDao: FakeSettingsDao = FakeSettingsDao(),
        loanDao: FakeLoanDao = FakeLoanDao(),
        loanRecordDao: FakeLoanRecordDao = FakeLoanRecordDao(),
        tagDao: FakeTagDao = FakeTagDao(),
        tagAssociationDao: FakeTagAssociationDao = FakeTagAssociationDao(),
        creditCardDao: FakeCreditCardDao = FakeCreditCardDao(),
    ): BackupDataUseCase {
        val creditCardMapper = CreditCardMapper()
        val accountMapper = AccountMapper(
            CurrencyRepository(
                settingsDao = settingsDao,
                writeSettingsDao = settingsDao,
                dispatchersProvider = TestDispatchersProvider,
            )
        )
        return BackupDataUseCase(
            accountDao = accountDao,
            accountMapper = accountMapper,
            accountRepository = AccountRepository(
                accountDao = accountDao,
                writeAccountDao = accountDao,
                mapper = accountMapper,
                dispatchersProvider = TestDispatchersProvider,
                memoFactory = fakeRepositoryMemoFactory(),
            ),
            budgetDao = budgetDao,
            categoryDao = categoryDao,
            loanRecordDao = loanRecordDao,
            loanDao = loanDao,
            plannedPaymentRuleDao = plannedPaymentDao,
            transactionDao = transactionDao,
            transactionWriter = transactionDao,
            settingsDao = settingsDao,
            categoryWriter = categoryDao,
            settingsWriter = settingsDao,
            budgetWriter = budgetDao,
            loanWriter = loanDao,
            loanRecordWriter = loanRecordDao,
            plannedPaymentRuleWriter = plannedPaymentDao,

            context = mockk(relaxed = true),
            sharedPrefs = mockk(relaxed = true),
            json = KotlinxSerializationModule.provideJson(),
            dispatchersProvider = TestDispatchersProvider,
            fileSystem = mockk(relaxed = true),
            dataObserver = DataObserver(),
            tagsReader = tagDao,
            tagsWriter = tagDao,
            tagAssociationReader = tagAssociationDao,
            tagAssociationWriter = tagAssociationDao,
            creditCardDao = creditCardDao,
            creditCardMapper = creditCardMapper,
            creditCardRepository = CreditCardRepository(
                mapper = creditCardMapper,
                creditCardDao = creditCardDao,
                writeCreditCardDao = creditCardDao,
                memoFactory = fakeRepositoryMemoFactory(),
            ),
        )
    }

    @Test
    fun `credit cards round trip without secrets`() = runTest {
        // given: a card backed by an account
        val cardId = UUID.randomUUID()
        val sourceAccounts = FakeAccountDao().apply {
            save(
                AccountEntity(
                    name = "HDFC Credit Card",
                    currency = "INR",
                    color = 1,
                    icon = "ic_vue_money_card",
                    orderNum = 1.0,
                    includeInBalance = false,
                    id = cardId,
                )
            )
        }
        val sourceCards = FakeCreditCardDao().apply {
            save(
                CreditCardEntity(
                    cardholderName = "Harshith",
                    issuer = "HDFC Bank",
                    network = "VISA",
                    last4 = "6304",
                    bin = "437551",
                    expiryMonth = 12,
                    expiryYear = 2030,
                    creditLimit = 36_000.0,
                    billingDay = 5,
                    dueDay = 25,
                    repaymentAccountId = null,
                    payeeVpa = null,
                    id = cardId,
                )
            )
        }
        val source = newBackupDataUseCase(accountDao = sourceAccounts, creditCardDao = sourceCards)

        // when
        val exportedJson = source.generateJsonBackup()

        // then: the JSON has the card but no secret fields
        exportedJson shouldNotContain "\"pan\""
        exportedJson shouldNotContain "\"cvv\""
        exportedJson shouldNotContain "panEncrypted"

        // and when: importing into a fresh instance
        val targetAccounts = FakeAccountDao()
        val targetCards = FakeCreditCardDao()
        val target = newBackupDataUseCase(accountDao = targetAccounts, creditCardDao = targetCards)
        target.importJson(exportedJson, onProgress = {})

        // then: the card and its account are restored with the same id
        targetCards.findById(cardId)?.last4 shouldBe "6304"
        targetAccounts.findById(cardId)?.name shouldBe "HDFC Credit Card"
    }

    private suspend fun backupTestCase(backupVersion: String) {
        // given
        val originalBackupUseCase = newBackupDataUseCase()
        val backupJsonData = testResource("backups/$backupVersion.json")
            .readText(Charsets.UTF_16)

        // when
        val importedDataRes = originalBackupUseCase.importJson(backupJsonData, onProgress = {})

        // then
        importedDataRes.accountsImported shouldBeGreaterThan 0
        importedDataRes.transactionsImported shouldBeGreaterThan 0
        importedDataRes.categoriesImported shouldBeGreaterThan 0
        importedDataRes.failedRows.size shouldBe 0

        // Also - exporting and re-importing the data should work
        // given
        val exportedJson = originalBackupUseCase.generateJsonBackup()

        // when
        val freshBackupUseCase = newBackupDataUseCase()
        val reImportedDataRes = freshBackupUseCase.importJson(exportedJson, onProgress = {})
        // then
        reImportedDataRes shouldBe importedDataRes

        // Finally, exporting again should yield the same result
        freshBackupUseCase.generateJsonBackup() shouldBe exportedJson
    }

    @Test
    fun `backup compatibility with 450 (150)`() = runTest {
        backupTestCase("450-150")
    }
}