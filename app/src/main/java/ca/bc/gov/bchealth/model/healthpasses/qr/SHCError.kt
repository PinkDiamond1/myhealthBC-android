package ca.bc.gov.bchealth.model.healthpasses.qr

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * [SHCError] holds error code & message
 *
 * @author Pinakin Kansara
 */
@Parcelize
data class SHCError(
    val errorCode: Int,
    val message: String
) : Parcelable
