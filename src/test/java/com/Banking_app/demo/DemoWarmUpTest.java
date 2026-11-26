package com.Banking_app.demo;

import com.Banking_app.transaction.application.port.in.command.RecordForeignRemittanceCommand;
import com.Banking_app.transaction.application.port.in.command.TransferMoneyCommand;
import com.Banking_app.transaction.application.port.in.result.RemittanceResult;
import com.Banking_app.transaction.application.port.in.result.TransferResult;
import com.Banking_app.transaction.application.port.in.usecase.RecordForeignRemittanceUseCase;
import com.Banking_app.transaction.application.port.in.usecase.TransferMoneyUseCase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** What the warm-up asks the use cases for. The real use cases run in DemoDataSeederTest. */
class DemoWarmUpTest {

    private final TransferMoneyUseCase transfers = mock(TransferMoneyUseCase.class);
    private final RecordForeignRemittanceUseCase remittances = mock(RecordForeignRemittanceUseCase.class);
    private final DemoCast cast = DemoCast.standard(90);

    @Test
    void makesThreeTransfersAndOneRemittance_requestedByTheOwners() {
        TransferResult transferResult = mock(TransferResult.class);
        when(transferResult.transactionReference()).thenReturn("TXN-A", "TXN-B", "TXN-D");
        when(transfers.transfer(any())).thenReturn(transferResult);
        RemittanceResult remittanceResult = mock(RemittanceResult.class);
        when(remittanceResult.transactionReference()).thenReturn("RMT-C");
        when(remittances.receive(any())).thenReturn(remittanceResult);

        List<String> references = new DemoWarmUp(transfers, remittances).run(cast);

        assertThat(references).containsExactly("TXN-A", "TXN-B", "RMT-C", "TXN-D");

        ArgumentCaptor<TransferMoneyCommand> sent = ArgumentCaptor.forClass(TransferMoneyCommand.class);
        verify(transfers, times(3)).transfer(sent.capture());
        List<TransferMoneyCommand> commands = sent.getAllValues();

        assertThat(commands).extracting(TransferMoneyCommand::fromAccountNumber)
                .containsExactly(DemoCast.PRIYA_SAVINGS, DemoCast.MAIN_SAVINGS, DemoCast.AHMAD_CURRENT);
        assertThat(commands).extracting(TransferMoneyCommand::requesterUserId).containsExactly(
                cast.user("priya.n").id(), cast.user("weiming").id(), cast.user("ahmad.f").id());
        assertThat(commands).allSatisfy(c -> {
            assertThat(c.currency()).isEqualTo("MYR");
            assertThat(c.quoteId()).isNull();
            assertThat(UUID.fromString(c.idempotencyKey())).isNotNull();   // the format the web API accepts
        });

        TransferMoneyCommand large = commands.get(2);
        assertThat(large.toAccountNumber()).isEqualTo(DemoCast.PRIYA_SAVINGS);
        assertThat(large.amount()).isGreaterThanOrEqualTo(new BigDecimal("10000.00"));   // LARGE_TRANSFER limit

        ArgumentCaptor<RecordForeignRemittanceCommand> received =
                ArgumentCaptor.forClass(RecordForeignRemittanceCommand.class);
        verify(remittances).receive(received.capture());
        assertThat(received.getValue().toAccountNumber()).isEqualTo(DemoCast.MAIN_SAVINGS);
        assertThat(received.getValue().currency()).isEqualTo("USD");
        assertThat(received.getValue().senderName()).isEqualTo("Lee Chong");
    }

    @Test
    void idempotencyKeys_areFixedDistinctUuids() {
        assertThat(DemoWarmUp.idempotencyKey(1)).isEqualTo(DemoWarmUp.idempotencyKey(1))
                .isNotEqualTo(DemoWarmUp.idempotencyKey(2));
        assertThat(UUID.fromString(DemoWarmUp.idempotencyKey(4)).toString()).isEqualTo(DemoWarmUp.idempotencyKey(4));
    }
}
