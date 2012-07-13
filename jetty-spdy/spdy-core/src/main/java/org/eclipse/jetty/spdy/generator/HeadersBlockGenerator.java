//========================================================================
//Copyright 2011-2012 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
//All rights reserved. This program and the accompanying materials
//are made available under the terms of the Eclipse Public License v1.0
//and Apache License v2.0 which accompanies this distribution.
//The Eclipse Public License is available at
//http://www.eclipse.org/legal/epl-v10.html
//The Apache License v2.0 is available at
//http://www.opensource.org/licenses/apache2.0.php
//You may elect to redistribute this code under either of these licenses.
//========================================================================

package org.eclipse.jetty.spdy.generator;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.eclipse.jetty.spdy.CompressionDictionary;
import org.eclipse.jetty.spdy.CompressionFactory;
import org.eclipse.jetty.spdy.api.Headers;
import org.eclipse.jetty.spdy.api.SPDY;

public class HeadersBlockGenerator
{
    private final CompressionFactory.Compressor compressor;
    private boolean needsDictionary = true;

    public HeadersBlockGenerator(CompressionFactory.Compressor compressor)
    {
        this.compressor = compressor;
    }

    public ByteBuffer generate(short version, Headers headers)
    {
        // TODO: ByteArrayOutputStream is quite inefficient, but grows on demand; optimize using ByteBuffer ?
        Charset iso1 = Charset.forName("ISO-8859-1");
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(headers.size() * 64);
        writeCount(version, buffer, headers.size());
        for (Headers.Header header : headers)
        {
            String name = header.name().toLowerCase();
            byte[] nameBytes = name.getBytes(iso1);
            writeNameLength(version, buffer, nameBytes.length);
            buffer.write(nameBytes, 0, nameBytes.length);

            // Most common path first
            String value = header.value();
            byte[] valueBytes = value.getBytes(iso1);
            if (header.hasMultipleValues())
            {
                String[] values = header.values();
                for (int i = 1; i < values.length; ++i)
                {
                    byte[] moreValueBytes = values[i].getBytes(iso1);
                    byte[] newValueBytes = new byte[valueBytes.length + 1 + moreValueBytes.length];
                    System.arraycopy(valueBytes, 0, newValueBytes, 0, valueBytes.length);
                    newValueBytes[valueBytes.length] = 0;
                    System.arraycopy(moreValueBytes, 0, newValueBytes, valueBytes.length + 1, moreValueBytes.length);
                    valueBytes = newValueBytes;
                }
            }

            writeValueLength(version, buffer, valueBytes.length);
            buffer.write(valueBytes, 0, valueBytes.length);
        }

        return compress(version, buffer.toByteArray());
    }

    private ByteBuffer compress(short version, byte[] bytes)
    {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(bytes.length);

        // The headers compression context is per-session, so we need to synchronize
        synchronized (compressor)
        {
            if (needsDictionary)
            {
                compressor.setDictionary(CompressionDictionary.get(version));
                needsDictionary = false;
            }

            compressor.setInput(bytes);

            // Compressed bytes may be bigger than input bytes, so we need to loop and accumulate them
            // Beware that the minimum amount of bytes generated by the compressor is few bytes, so we
            // need to use an output buffer that is big enough to exit the compress loop
            buffer.reset();
            int compressed;
            byte[] output = new byte[Math.max(256, bytes.length)];
            while (true)
            {
                // SPDY uses the SYNC_FLUSH mode
                compressed = compressor.compress(output);
                buffer.write(output, 0, compressed);
                if (compressed < output.length)
                    break;
            }
        }

        return ByteBuffer.wrap(buffer.toByteArray());
    }

    private void writeCount(short version, ByteArrayOutputStream buffer, int value)
    {
        switch (version)
        {
            case SPDY.V2:
            {
                buffer.write((value & 0xFF_00) >>> 8);
                buffer.write(value & 0x00_FF);
                break;
            }
            case SPDY.V3:
            {
                buffer.write((value & 0xFF_00_00_00) >>> 24);
                buffer.write((value & 0x00_FF_00_00) >>> 16);
                buffer.write((value & 0x00_00_FF_00) >>> 8);
                buffer.write(value & 0x00_00_00_FF);
                break;
            }
            default:
            {
                // Here the version is trusted to be correct; if it's not
                // then it's a bug rather than an application error
                throw new IllegalStateException();
            }
        }
    }

    private void writeNameLength(short version, ByteArrayOutputStream buffer, int length)
    {
        writeCount(version, buffer, length);
    }

    private void writeValueLength(short version, ByteArrayOutputStream buffer, int length)
    {
        writeCount(version, buffer, length);
    }
}
