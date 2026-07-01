package com.minio.storage.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class TxCleanup {

    private static final Logger log = LoggerFactory.getLogger(TxCleanup.class);

    private TxCleanup() {
    }

    public static void onRollback(Runnable action) {

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_ROLLED_BACK) {
                            try {
                                action.run();
                            } catch (Exception e) {
                                log.error("Čišćenje nakon rollback-a nije uspjelo", e);
                            }
                        }
                    }
                }
        );
    }

}