package ca.bc.gov.bchealth.model.healthpasses.decoder

/**
 * [JwksKey]
 *
 * @author Pinakin Kansara
 */
data class JwksKey(

    val key: String,

    val kid: String,

    val use: String,

    val alg: String,

    val crv: String,

    val x: String,

    val y: String,

    val x5c: List<String>
)
