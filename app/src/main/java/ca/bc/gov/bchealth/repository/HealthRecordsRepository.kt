package ca.bc.gov.bchealth.repository

import ca.bc.gov.bchealth.data.local.entity.CovidTestResult
import ca.bc.gov.bchealth.data.local.entity.HealthCard
import ca.bc.gov.bchealth.datasource.LocalDataSource
import ca.bc.gov.bchealth.model.ImmunizationRecord
import ca.bc.gov.bchealth.model.healthrecords.HealthRecord
import ca.bc.gov.bchealth.model.healthrecords.VaccineData
import ca.bc.gov.bchealth.utils.Response
import ca.bc.gov.bchealth.utils.SHCDecoder
import ca.bc.gov.bchealth.utils.getIssueDate
import java.sql.Date
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine

/*
* Created by amit_metri on 26,November,2021
*/
class HealthRecordsRepository @Inject constructor(
    private val dataSource: LocalDataSource,
    private val shcDecoder: SHCDecoder,
) {

    /*
    * Used to manage Success, Error and Loading status in the UI
    * */
    private val responseMutableSharedFlow = MutableSharedFlow<Response<String>>()
    val responseSharedFlow: SharedFlow<Response<String>>
        get() = responseMutableSharedFlow.asSharedFlow()

    /*
    * Health Records
    * */
    val healthRecords: Flow<List<HealthRecord>> =

        dataSource.getCards()
            .combine(dataSource.getCovidTestResults()) { healthPasses, covidTestResults ->

                val healthRecordList: MutableList<HealthRecord> = mutableListOf()

                healthPasses.forEach { healthPass ->

                    try {
                        val data = shcDecoder.getImmunizationStatus(healthPass.uri)

                        /*
                        * Add common data, vaccination data and covid test results data to health record of a member
                        * */
                        healthRecordList.add(
                            HealthRecord(
                                data.name,
                                data.status,
                                data.issueDate.getIssueDate(),
                                getIndividualVaccinationData(data),
                                covidTestResults.filter {
                                    it.patientDisplayName.lowercase() == data.name.lowercase()
                                }
                            )
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                /*
                * There is possibility that a member may not get vaccinated but can have covid test results.
                * Below method prepares the health records for such members
                * */
                covidTestResults.forEach { covidTestResult ->
                    var isHealthRecordAlreadyPresent = false
                    healthRecordList.forEach innerLoop@{ healthRecord ->
                        if (covidTestResult.patientDisplayName.lowercase()
                            == healthRecord.name.lowercase()
                        ) {
                            isHealthRecordAlreadyPresent = true
                            return@innerLoop
                        }
                    }

                    if (!isHealthRecordAlreadyPresent) {
                        healthRecordList.add(
                            HealthRecord(
                                covidTestResult.patientDisplayName,
                                null,
                                "",
                                mutableListOf(),
                                covidTestResults.filter {
                                    covidTestResult.patientDisplayName.lowercase() ==
                                        it.patientDisplayName.lowercase()
                                }
                            )
                        )
                    }
                }

                healthRecordList
            }

    val vaccineInfo: HashMap<String, String> = mapOf(
        "28581000087106" to "PFIZER-BIONTECH COMIRNATY COVID-19",
        "28571000087109" to "MODERNA SPIKEVAX",
        "28761000087108" to "ASTRAZENECA VAXZEVRIA",
        "28961000087105" to "COVISHIELD",
        "28951000087107" to "JANSSEN (JOHNSON & JOHNSON)",
        "29171000087106" to "NOVAVAX",
        "31431000087100" to "CANSINOBIO",
        "31341000087103" to "SPUTNIK",
        "31311000087104" to "SINOVAC-CORONAVAC ",
        "31301000087101" to "SINOPHARM",
        "NON-WHO" to "UNSPECIFIED COVID-19 VACCINE"
    ) as HashMap<String, String>

    /*
    * Prepare individual member vaccination record
    * */
    private fun getIndividualVaccinationData(data: ImmunizationRecord): MutableList<VaccineData> {
        val vaccineDataList: MutableList<VaccineData> = mutableListOf()

        data.immunizationEntries?.forEachIndexed { index, entry ->

            var value = ""

            entry.resource.vaccineCode?.coding?.forEach { coding ->
                if (vaccineInfo.keys.contains(coding.code))
                    value = vaccineInfo.getValue(coding.code)
            }

            val productInfo = if (value.isEmpty())
                "Not Available"
            else
                value

            vaccineDataList.add(
                VaccineData(
                    data.name,
                    (index + 1).toString(),
                    entry.resource.occurrenceDateTime,
                    productInfo,
                    "Not Available",
                    entry.resource.performer?.last()?.actor?.display,
                    entry.resource.lotNumber
                )
            )
        }

        return vaccineDataList
    }

    /*
    * Fetch the covid test result
    * */
    suspend fun getCovidTestResult(phn: String, dob: String, dot: Any) {

        responseMutableSharedFlow.emit(Response.Loading())

        // TODO: 26/11/21 Network call to be implemented

        saveCovidTestResult(
            CovidTestResult(
                Random.nextInt(1000000).toString(),
                "Amit Metri",
                "Freshworks lab",
                Date.valueOf("2021-10-10"),
                Date.valueOf("2021-9-12"),
                "Covid Test",
                "COVID-19 Corona Virus RNA (PCR/NAAT)",
                "Final",
                "Positive",
                "Tested Positive",
                "Tested Positive description",
                "link",
                ""
            )
        )
    }

    /*
    * Save covid test results in DB
    * */
    private suspend fun saveCovidTestResult(covidTestResult: CovidTestResult) {

        dataSource.insertCovidTestResult(
            covidTestResult
        )

        responseMutableSharedFlow.emit(Response.Success(covidTestResult.patientDisplayName))
    }

    fun fetchHealthRecordFromHealthCard(healthCard: HealthCard): ImmunizationRecord? {

        try {
            return shcDecoder.getImmunizationStatus(healthCard.uri)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}