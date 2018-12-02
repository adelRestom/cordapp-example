package com.example.contract

import com.example.contract.IOUContract.Companion.IOU_CONTRACT_ID
import com.example.state.IOUState
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class IOUContractTests {
    private val ledgerServices = MockServices()
    private val megaCorp = TestIdentity(CordaX500Name("MegaCorp", "London", "GB"))
    private val miniCorp = TestIdentity(CordaX500Name("MiniCorp", "New York", "US"))
    private val iouValue = 1

    @Test
    fun `Destroy IOU transaction must have one input`() {
        ledgerServices.ledger {
            transaction {
                // Even though Destroy command doesn't create an output; we need to add the below output,
                // otherwise we'll get the follwoing exception:
                // "A transaction must contain at least one input or output state"
                output(IOU_CONTRACT_ID, IOUState(iouValue, miniCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), IOUContract.Commands.Destroy())
                `fails with`("Only one input state should be consumed when destroying an IOU.")
            }
        }
    }

    @Test
    fun `Destroy IOU transaction must have no outputs`() {
        ledgerServices.ledger {
            transaction {
                input(IOU_CONTRACT_ID, IOUState(iouValue, miniCorp.party, megaCorp.party))
                output(IOU_CONTRACT_ID, IOUState(iouValue, miniCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), IOUContract.Commands.Destroy())
                `fails with`("There should be no outputs.")
            }
        }
    }

    @Test
    fun `Destroy IOU transaction must have one input and no output`() {
        ledgerServices.ledger {
            transaction {
                input(IOU_CONTRACT_ID, IOUState(iouValue, miniCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), IOUContract.Commands.Destroy())
                verifies()
            }
        }
    }

    @Test
    fun `Destroy IOU transaction must be signed by borrower and lender`() {
        ledgerServices.ledger {
            transaction {
                input(IOU_CONTRACT_ID, IOUState(iouValue, miniCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey), IOUContract.Commands.Destroy())
                `fails with` ("All of the participants must be signers.")
            }
        }
    }

    /*@Test
    fun `transaction must include Create command`() {
        ledgerServices.ledger {
            transaction {
                output(IOU_CONTRACT_ID, IOUState(iouValue, miniCorp.party, megaCorp.party))
                fails()
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), IOUContract.Commands.Create())
                verifies()
            }
        }
    }

    @Test
    fun `transaction must have no inputs`() {
        ledgerServices.ledger {
            transaction {
                input(IOU_CONTRACT_ID, IOUState(iouValue, miniCorp.party, megaCorp.party))
                output(IOU_CONTRACT_ID, IOUState(iouValue, miniCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), IOUContract.Commands.Create())
                `fails with`("No inputs should be consumed when issuing an IOU.")
            }
        }
    }

    @Test
    fun `transaction must have one output`() {
        ledgerServices.ledger {
            transaction {
                output(IOU_CONTRACT_ID, IOUState(iouValue, miniCorp.party, megaCorp.party))
                output(IOU_CONTRACT_ID, IOUState(iouValue, miniCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), IOUContract.Commands.Create())
                `fails with`("Only one output state should be created.")
            }
        }
    }

    @Test
    fun `lender must sign transaction`() {
        ledgerServices.ledger {
            transaction {
                output(IOU_CONTRACT_ID, IOUState(iouValue, miniCorp.party, megaCorp.party))
                command(miniCorp.publicKey, IOUContract.Commands.Create())
                `fails with`("All of the participants must be signers.")
            }
        }
    }

    @Test
    fun `borrower must sign transaction`() {
        ledgerServices.ledger {
            transaction {
                output(IOU_CONTRACT_ID, IOUState(iouValue, miniCorp.party, megaCorp.party))
                command(megaCorp.publicKey, IOUContract.Commands.Create())
                `fails with`("All of the participants must be signers.")
            }
        }
    }

    @Test
    fun `lender is not borrower`() {
        ledgerServices.ledger {
            transaction {
                output(IOU_CONTRACT_ID, IOUState(iouValue, megaCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), IOUContract.Commands.Create())
                `fails with`("The lender and the borrower cannot be the same entity.")
            }
        }
    }

    @Test
    fun `cannot create negative-value IOUs`() {
        ledgerServices.ledger {
            transaction {
                output(IOU_CONTRACT_ID, IOUState(-1, miniCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), IOUContract.Commands.Create())
                `fails with`("The IOU's value must be non-negative.")
            }
        }
    }*/
}