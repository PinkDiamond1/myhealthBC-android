package ca.bc.gov.bchealth.datasource

import ca.bc.gov.bchealth.data.local.BcVaccineCardDataBase
import ca.bc.gov.bchealth.data.local.entity.CovidTestResult
import ca.bc.gov.bchealth.data.local.entity.HealthCard
import javax.inject.Inject

/**
 * [LocalDataSource]
 *
 * @author Pinakin Kansara
 */
class LocalDataSource @Inject constructor(
    private val dataBase: BcVaccineCardDataBase
) {

    suspend fun insert(card: HealthCard) = dataBase.getHealthCardDao().insert(card)

    suspend fun update(card: HealthCard) = dataBase.getHealthCardDao().update(card)

    suspend fun unLink(card: HealthCard) = dataBase.getHealthCardDao().delete(card)

    fun getCards() = dataBase.getHealthCardDao().getCards()

    suspend fun rearrange(cards: List<HealthCard>) = dataBase.getHealthCardDao().rearrange(cards)

    suspend fun insertCovidTestResult(covidTestResult: CovidTestResult) =
        dataBase.getCovidTestResultDao().insert(covidTestResult)

    suspend fun deleteCovidTestResult(reportId: String) =
        dataBase.getCovidTestResultDao().delete(reportId)

    fun getCovidTestResults() = dataBase.getCovidTestResultDao().getCovidTestResults()
}
