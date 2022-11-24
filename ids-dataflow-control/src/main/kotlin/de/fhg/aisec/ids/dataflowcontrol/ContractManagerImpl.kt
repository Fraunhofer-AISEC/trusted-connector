package de.fhg.aisec.ids.dataflowcontrol

import de.fhg.aisec.ids.api.contracts.ContractConstants
import de.fhg.aisec.ids.api.contracts.ContractManager
import de.fhg.aisec.ids.api.contracts.ContractUtils
import de.fhg.aisec.ids.api.contracts.ContractUtils.SERIALIZER
import de.fhg.aisec.ids.api.contracts.copy
import de.fhg.aisec.ids.api.settings.Settings
import de.fraunhofer.iais.eis.AbstractConstraint
import de.fraunhofer.iais.eis.Action
import de.fraunhofer.iais.eis.BinaryOperator
import de.fraunhofer.iais.eis.ConstraintBuilder
import de.fraunhofer.iais.eis.ContractOffer
import de.fraunhofer.iais.eis.ContractOfferBuilder
import de.fraunhofer.iais.eis.LeftOperand
import de.fraunhofer.iais.eis.PermissionBuilder
import de.fraunhofer.iais.eis.util.TypedLiteral
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.net.URI

@Component("contractManager")
class ContractManagerImpl : ContractManager {

    @Autowired
    private lateinit var settings: Settings

    override fun makeContract(contractProperties: Map<String, Any>): ContractOffer {
        val requestedArtifact = contractProperties[ContractConstants.ARTIFACT_URI_PROPERTY] as URI?
            ?: throw RuntimeException("No artifact URI provided in contractProperties")
        val notAfterDateTime = contractProperties[ContractConstants.UC_NOT_AFTER_DATETIME] as String?
        val notBeforeDateTime = contractProperties[ContractConstants.UC_NOT_BEFORE_DATETIME] as String?

        @Suppress("UNCHECKED_CAST")
        val dockerImageUris: List<URI> =
            contractProperties[ContractConstants.UC_DOCKER_IMAGE_URIS] as List<URI>? ?: emptyList()

        val contractDate = ContractUtils.newGregorianCalendar()
        val timeConstraints = mutableListOf<AbstractConstraint>()

        // Add not after (BEFORE) usage constraint
        notAfterDateTime.let { dateTime ->
            timeConstraints += ConstraintBuilder()
                ._leftOperand_(LeftOperand.POLICY_EVALUATION_TIME)
                ._operator_(BinaryOperator.BEFORE)
                ._rightOperand_(
                    TypedLiteral(
                        ContractUtils.DATATYPE_FACTORY.newXMLGregorianCalendar(dateTime.toString()).toString(),
                        ContractUtils.TYPE_DATETIMESTAMP
                    )
                )
                .build()
        }
        // Add not before (AFTER) usage constraint
        notBeforeDateTime?.let { dateTime ->
            timeConstraints += ConstraintBuilder()
                ._leftOperand_(LeftOperand.POLICY_EVALUATION_TIME)
                ._operator_(BinaryOperator.AFTER)
                ._rightOperand_(
                    TypedLiteral(
                        ContractUtils.DATATYPE_FACTORY.newXMLGregorianCalendar(dateTime).toString(),
                        ContractUtils.TYPE_DATETIMESTAMP
                    )
                )
                .build()
        }
        return ContractOfferBuilder()
            ._contractDate_(contractDate)
            ._contractStart_(contractDate)
            // Contract end one year in the future
            ._contractEnd_(contractDate.copy().apply { year += 1 })
            ._permission_(
                if (dockerImageUris.isEmpty()) {
                    // If no Docker images have been specified, just use time constraints
                    listOf(
                        PermissionBuilder()
                            ._target_(requestedArtifact)
                            ._action_(listOf(Action.USE))
                            ._constraint_(
                                timeConstraints
                            )
                            .build()
                    )
                } else {
                    // If Docker images have been specified, combine each with the specified time constraints
                    dockerImageUris.map {
                        PermissionBuilder()
                            ._target_(requestedArtifact)
                            ._action_(listOf(Action.USE))
                            ._constraint_(
                                listOf(
                                    ConstraintBuilder()
                                        ._leftOperand_(LeftOperand.SYSTEM)
                                        ._operator_(BinaryOperator.SAME_AS)
                                        ._rightOperandReference_(it)
                                        .build()
                                ) + timeConstraints
                            )
                            .build()
                    }
                }
            )
            .build()
    }

    override fun storeContract(key: String, contract: ContractOffer) =
        settings.storeContract(key, SERIALIZER.serialize(contract))

    override fun loadContract(key: String) =
        settings.loadContract(key)?.let { SERIALIZER.deserialize(it, ContractOffer::class.java) }
}
