package com.puppetlabs.http.client.impl;

import org.apache.http.HttpResponse;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.client.methods.AsyncByteConsumer;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;

public class StreamingAsyncResponseConsumer extends AsyncByteConsumer<HttpResponse> {

    private volatile HttpResponse response;
    private volatile PipedOutputStream pos;
    private volatile Deliverable<HttpResponse> promise;
    private volatile Promise<IOException> ioExceptionPromise = new Promise<>();

    public void setFinalResult(IOException ioException) {
        ioExceptionPromise.deliver(ioException);
    }

    public StreamingAsyncResponseConsumer(Deliverable<HttpResponse> promise) {
        this.promise = promise;
    }

    @Override
    protected void onResponseReceived(final HttpResponse response) throws IOException {
        PipedInputStream pis = new ExceptionInsertingPipedInputStream(ioExceptionPromise);
        pos = new PipedOutputStream();
        pos.connect(pis);
        ((BasicHttpEntity) response.getEntity()).setContent(pis);
        this.response = response;
        promise.deliver(response);
    }

    @Override
    protected void onByteReceived(final ByteBuffer buf, final IOControl ioctrl) throws IOException {
        while (buf.hasRemaining()) {
            byte[] bs = new byte[buf.remaining()];
            buf.get(bs);
            pos.write(bs);
        }
    }

    @Override
    protected void releaseResources() {
        this.response = null;
        this.promise = null;
        try {
            if (pos != null) {
                this.pos.close();
                this.pos = null;
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected HttpResponse buildResult(final HttpContext context) {
        return response;
    }

}