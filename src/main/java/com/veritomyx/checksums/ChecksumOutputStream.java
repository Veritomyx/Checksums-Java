/*
 * Copyright (c) 2017 Veritomyx, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 *  Neither the name of PeakInvestigator-Java-SDK nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package com.veritomyx.checksums;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Optional;

public class ChecksumOutputStream extends FilterOutputStream {

    private enum State { OUT, BUFFER, CHECKSUM, FINISHED, CLOSED };

    private final static Logger LOGGER = LoggerFactory.getLogger(ChecksumOutputStream.class);
    private final static int CHECKSUM_SIZE = 51;
    private final static byte[] CHECKSUM_BYTES = { '#', ' ', 'c', 'h', 'e', 'c', 'k', 's', 'u', 'm', ':'};

    private final MessageDigest digest;
    private final byte[] buffer;

    private State state;
    private int index;
    private Optional<String> found = Optional.empty();

    public ChecksumOutputStream(OutputStream out) throws NoSuchAlgorithmException {
        super(out);
        this.digest = MessageDigest.getInstance("SHA-1");
        this.state = State.OUT;
        this.buffer = new byte[CHECKSUM_SIZE];
        this.index = 0;
    }

    /**
     * Convenience method to return a ChecksumOutputStream that wraps a
     * BufferedOutputStream around one returned from
     * {@link java.nio.file.Files}.
     *
     * @param path Path used for output file
     * @return A new instance of a ChecksumInputStream
     * @throws NoSuchAlgorithmException if SHA-1 is not found
     * @throws IOException              if an I/O error occurs opening the file
     */
    public static OutputStream create(Path path) throws NoSuchAlgorithmException, IOException {
        return new ChecksumOutputStream(new BufferedOutputStream(Files.newOutputStream(path)));
    }

    @Override
    public void write(int b) throws IOException {
        // ignore everything after encountering first checksum line
        if (state == State.FINISHED) {
            return;
        }

        if (state == State.BUFFER || state == State.CHECKSUM) {
            writeBuffer(b);
        } else if (state == State.OUT && (char) b == '#') {
            state = State.BUFFER;
            writeBuffer(b);
        } else {
            digest.update((byte) b);
            out.write(b);
        }
    }

    @Override
    public void close() throws IOException {
        if (state == State.CLOSED) {
            return;
        } else if (state == State.BUFFER) {
            digest.update(buffer, 0, index);
            out.write(buffer, 0, index);
        } else if (state == State.CHECKSUM) {
            LOGGER.error("Partial checksum line detected: {}", new String(Arrays.copyOf(buffer, index)));
            throw new InvalidChecksumException();
        }

        final String checksum = "# checksum:" + Hex.encodeHexString(digest.digest());

        if (found.isPresent()) {
            if (!checksum.equals(found.get())) {
                LOGGER.error("Calculated: {}, Found: {}", checksum.substring(11), found.get().substring(11));
                throw new InvalidChecksumException();
            }
        }

        out.write(checksum.getBytes());
        out.write('\n');
        out.close();
        state = State.CLOSED;
    }

    private void writeBuffer(int value) throws IOException {
        // if "# checksum:" is in buffer, set state to CHECKSUM
        if (index == CHECKSUM_BYTES.length) {
            state = State.CHECKSUM;
        }

        final byte b = (byte) value;

        if (state == State.CHECKSUM || b == CHECKSUM_BYTES[index]) {
            buffer[index] = b;
            index++;
        } else {
            digest.update(buffer, 0, index);
            out.write(buffer, 0, index);

            // if '#' is encountered, need to store it in buffer; else write/digest
            if ((char) value == '#') {
                buffer[0] = b;
                index = 1;
                state = State.BUFFER;
            } else {
                digest.update(b);
                out.write(value);
                index = 0;
                state = State.OUT;
            }
        }

        // haven't read all of checksum
        if (index < CHECKSUM_SIZE) {
            return;
        }

        final String line = new String(buffer);
        state = State.FINISHED;
        found = Optional.of(line);

    }
}

