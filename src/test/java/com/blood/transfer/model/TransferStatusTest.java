package com.blood.transfer.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.EnumSet;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class TransferStatusTest {

    /** The full set of (from, to) pairs for which canTransitionTo must be true. */
    private static Stream<Arguments> allowedTransitions() {
        return Stream.of(
                Arguments.of(TransferStatus.PENDING, TransferStatus.APPROVED),
                Arguments.of(TransferStatus.PENDING, TransferStatus.REJECTED),
                Arguments.of(TransferStatus.PENDING, TransferStatus.CANCELLED),
                Arguments.of(TransferStatus.PENDING, TransferStatus.INSUFFICIENT_STOCK),
                Arguments.of(TransferStatus.APPROVED, TransferStatus.COMPLETED),
                Arguments.of(TransferStatus.APPROVED, TransferStatus.CANCELLED),
                Arguments.of(TransferStatus.APPROVED, TransferStatus.EXPIRED)
        );
    }

    @ParameterizedTest
    @MethodSource("allowedTransitions")
    void canTransitionTo_allowedPairs_returnsTrue(TransferStatus from, TransferStatus to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    /** Full 7x7 matrix: every pair not in allowedTransitions() must be false. */
    @ParameterizedTest
    @MethodSource("allPairs")
    void canTransitionTo_fullMatrix(TransferStatus from, TransferStatus to) {
        boolean expected = allowedTransitions()
                .anyMatch(args -> args.get()[0] == from && args.get()[1] == to);
        assertThat(from.canTransitionTo(to)).isEqualTo(expected);
    }

    private static Stream<Arguments> allPairs() {
        return EnumSet.allOf(TransferStatus.class).stream()
                .flatMap(from -> EnumSet.allOf(TransferStatus.class).stream()
                        .map(to -> Arguments.of(from, to)));
    }

    @Test
    void canTransitionTo_pendingToItself_isFalse() {
        assertThat(TransferStatus.PENDING.canTransitionTo(TransferStatus.PENDING)).isFalse();
    }

    @Test
    void canTransitionTo_pendingToCompleted_isFalse() {
        assertThat(TransferStatus.PENDING.canTransitionTo(TransferStatus.COMPLETED)).isFalse();
    }

    @Test
    void canTransitionTo_approvedToItself_isFalse() {
        assertThat(TransferStatus.APPROVED.canTransitionTo(TransferStatus.APPROVED)).isFalse();
    }

    @Test
    void canTransitionTo_approvedToRejected_isFalse() {
        assertThat(TransferStatus.APPROVED.canTransitionTo(TransferStatus.REJECTED)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = TransferStatus.class, names = {"REJECTED", "CANCELLED", "COMPLETED", "EXPIRED", "INSUFFICIENT_STOCK"})
    void canTransitionTo_terminalStatesNeverTransition(TransferStatus terminal) {
        for (TransferStatus target : TransferStatus.values()) {
            assertThat(terminal.canTransitionTo(target))
                    .as("%s -> %s should be false (terminal)", terminal, target)
                    .isFalse();
        }
    }

    @ParameterizedTest
    @EnumSource(value = TransferStatus.class, names = {"REJECTED", "CANCELLED", "COMPLETED", "EXPIRED", "INSUFFICIENT_STOCK"})
    void isTerminal_trueForTerminalStates(TransferStatus terminal) {
        assertThat(terminal.isTerminal()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = TransferStatus.class, names = {"PENDING", "APPROVED"})
    void isTerminal_falseForNonTerminalStates(TransferStatus nonTerminal) {
        assertThat(nonTerminal.isTerminal()).isFalse();
    }
}
