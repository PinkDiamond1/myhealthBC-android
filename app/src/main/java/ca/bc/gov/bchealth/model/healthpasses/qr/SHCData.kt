package ca.bc.gov.bchealth.model.healthpasses.qr

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * [SHCData] holds data decoded from SMART HEALTH CARD.
 *
 * @author Pinakin Kansara
 */
@Parcelize
data class SHCData(
    val header: SHCHeader,
    val payload: SHCPayload,
    val signature: String
) : Parcelable