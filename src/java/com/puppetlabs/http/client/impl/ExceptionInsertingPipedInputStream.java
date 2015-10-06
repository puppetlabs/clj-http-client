package com.puppetlabs.http.client.impl;

import java.io.IOException;
import java.io.PipedInputStream;

public class ExceptionInsertingPipedInputStream extends PipedInputStream {

    private final Promise<IOException> ioExceptionPromise;

    public ExceptionInsertingPipedInputStream(Promise<IOException> ioExceptionPromise) {
        this.ioExceptionPromise = ioExceptionPromise;
    }

    private void checkFinalResult() throws IOException {
        try {
            IOException ioException = ioExceptionPromise.deref();
            if (ioException != null) {
                throw ioException;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized int read() throws IOException {
        int read = super.read();
        if (read == -1) {
            checkFinalResult();
        }
        return read;
    }

    @Override
    public synchronized int read(byte[] b, int off, int len) throws IOException {
        int read = super.read(b, off, len);
        if (read == -1) {
            checkFinalResult();
        }
        return read;
    }

    @Override
    public void close() throws IOException {
        super.close();
        checkFinalResult();
    }

}
