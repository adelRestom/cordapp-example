package com.example.flow

import com.example.state.IOUState
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.node.services.queryBy
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria


class IOUFlowTests {
    lateinit var network: MockNetwork
    lateinit var a: StartedMockNode
    lateinit var b: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(listOf("com.example.contract", "com.example.schema"))
        a = network.createPartyNode()
        b = network.createPartyNode()
        // For real nodes this happens automatically, but we have to manually register the flow for tests.
        listOf(a, b).forEach { it.registerInitiatedFlow(ExampleFlow.Acceptor::class.java) }
        listOf(a, b).forEach { it.registerInitiatedFlow(ExampleFlow.DestroyIOUAcceptor::class.java) }
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `recorded "Destroy IOU" transaction has no outputs and a single input, the input IOU`() {
        // Create the IOU
        val iouValue = 1
        val flow = ExampleFlow.Initiator(iouValue, b.info.singleIdentity())
        val future = a.startFlow(flow)
        network.runNetwork()
        val ious = a.transaction {
            a.services.vaultService.queryBy<IOUState>().states
        }
        val ref = ious.single().ref
        val txHash = ref.txhash.toString()
        val txIndex = ref.index

        // Destroy the IOU
        val destroyFlow = ExampleFlow.DestroyIOUInitiator(txHash, txIndex)
        val destroyFuture = a.startFlow(destroyFlow)
        network.runNetwork()
        val destroySignedTx = destroyFuture.getOrThrow()

        // We check the recorded "Destroy" transaction in both vaults.
        for (node in listOf(a, b)) {
            val recordedTx = node.services.validatedTransactions.getTransaction(destroySignedTx.id)
            val txInputs = recordedTx!!.tx.inputs
            assert(txInputs.size == 1)
            val txOutputs = recordedTx!!.tx.outputs
            assert(txOutputs.isEmpty())

            val stateRef = txInputs[0]
            val iouStateAndRef = node.services.toStateAndRef<IOUState>(stateRef)
            val recordedState = iouStateAndRef.state.data
            assertEquals(recordedState.value, iouValue)
            assertEquals(recordedState.lender, a.info.singleIdentity())
            assertEquals(recordedState.borrower, b.info.singleIdentity())
        }
    }

    @Test
    fun `flow removes the IOU from both parties' vaults`() {
        // Create the IOU
        val iouValue = 1
        val flow = ExampleFlow.Initiator(iouValue, b.info.singleIdentity())
        val future = a.startFlow(flow)
        network.runNetwork()
        val ious = b.transaction {
            b.services.vaultService.queryBy<IOUState>().states
        }
        val ref = ious.single().ref
        val txHash = ref.txhash.toString()
        val txIndex = ref.index

        // Destroy the IOU
        val destroyFlow = ExampleFlow.DestroyIOUInitiator(txHash, txIndex)
        val destroyFuture = b.startFlow(destroyFlow)
        network.runNetwork()

        // IOU is now marked as consumed in participating nodes
        for (node in listOf(a, b)) {
            node.transaction {
                val unconsumedCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
                val ious = node.services.vaultService.queryBy<IOUState>(unconsumedCriteria).states
                assert(ious.isEmpty())
            }
        }
    }

    /*@Test
    fun `flow rejects invalid IOUs`() {
        val flow = ExampleFlow.Initiator(-1, b.info.singleIdentity())
        val future = a.startFlow(flow)
        network.runNetwork()

        // The IOUContract specifies that IOUs cannot have negative values.
        assertFailsWith<TransactionVerificationException> { future.getOrThrow() }
    }

    @Test
    fun `SignedTransaction returned by the flow is signed by the initiator`() {
        val flow = ExampleFlow.Initiator(1, b.info.singleIdentity())
        val future = a.startFlow(flow)
        network.runNetwork()

        val signedTx = future.getOrThrow()
        signedTx.verifySignaturesExcept(b.info.singleIdentity().owningKey)
    }

    @Test
    fun `SignedTransaction returned by the flow is signed by the acceptor`() {
        val flow = ExampleFlow.Initiator(1, b.info.singleIdentity())
        val future = a.startFlow(flow)
        network.runNetwork()

        val signedTx = future.getOrThrow()
        signedTx.verifySignaturesExcept(a.info.singleIdentity().owningKey)
    }

    @Test
    fun `flow records a transaction in both parties' transaction storages`() {
        val flow = ExampleFlow.Initiator(1, b.info.singleIdentity())
        val future = a.startFlow(flow)
        network.runNetwork()
        val signedTx = future.getOrThrow()

        // We check the recorded transaction in both transaction storages.
        for (node in listOf(a, b)) {
            assertEquals(signedTx, node.services.validatedTransactions.getTransaction(signedTx.id))
        }
    }

    @Test
    fun `recorded transaction has no inputs and a single output, the input IOU`() {
        val iouValue = 1
        val flow = ExampleFlow.Initiator(iouValue, b.info.singleIdentity())
        val future = a.startFlow(flow)
        network.runNetwork()
        val signedTx = future.getOrThrow()

        // We check the recorded transaction in both vaults.
        for (node in listOf(a, b)) {
            val recordedTx = node.services.validatedTransactions.getTransaction(signedTx.id)
            val txOutputs = recordedTx!!.tx.outputs
            assert(txOutputs.size == 1)

            val recordedState = txOutputs[0].data as IOUState
            assertEquals(recordedState.value, iouValue)
            assertEquals(recordedState.lender, a.info.singleIdentity())
            assertEquals(recordedState.borrower, b.info.singleIdentity())
        }
    }

    @Test
    fun `flow records the correct IOU in both parties' vaults`() {
        val iouValue = 1
        val flow = ExampleFlow.Initiator(1, b.info.singleIdentity())
        val future = a.startFlow(flow)
        network.runNetwork()
        future.getOrThrow()

        // We check the recorded IOU in both vaults.
        for (node in listOf(a, b)) {
            node.transaction {
                val ious = node.services.vaultService.queryBy<IOUState>().states
                assertEquals(1, ious.size)
                val recordedState = ious.single().state.data
                assertEquals(recordedState.value, iouValue)
                assertEquals(recordedState.lender, a.info.singleIdentity())
                assertEquals(recordedState.borrower, b.info.singleIdentity())
            }
        }
    }*/
}