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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Optional;

public class ChecksumInputStream extends InputStream {

    private final static Logger LOGGER = LoggerFactory.getLogger(ChecksumInputStream.class);

    private final PushbackInputStream in;
    private final MessageDigest digest;
    private final byte[] buffer = new byte[51];
    private boolean isClosed;
    private Optional<String> foundChecksum = Optional.empty();

    public ChecksumInputStream(InputStream in) throws NoSuchAlgorithmException {
        this.in = new PushbackInputStream(in, buffer.length);
        this.digest = MessageDigest.getInstance("SHA-1");
        isClosed = false;
    }

    /**
     * Convenience method to return a ChecksumInputStream that wraps a
     * BufferedInputStream around one returned from
     * {@link java.nio.file.Files#newInputStream()}.
     *
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public static InputStream create(Path path) throws NoSuchAlgorithmException, IOException {
        return new ChecksumInputStream(new BufferedInputStream(Files.newInputStream(path)));
    }

    @Override
    public int read() throws IOException {
        int value = in.read();
        LOGGER.trace("Read: '{}'", (char) value);

        if (value == -1) {
            LOGGER.trace("At end of stream");
            return value;
        } else if (value == '#') {
            if (hasFoundChecksum()) {
                return -1;
            }
        }

        digest.update((byte) value);
        return value;
    }

    @Override
    public void close() throws IOException {
        if (isClosed) {
            return;
        }

        LOGGER.debug("Closing...");
        in.close();
        isClosed = true;
        LOGGER.debug("Closed.");

        String calculated = getChecksum();
        LOGGER.debug(".. Calculated checksum: {}", calculated);

        if (!foundChecksum.isPresent()) {
            LOGGER.debug(".. Found checksum: none");
            throw new MissingChecksumException();
        }

        String found = foundChecksum.get();
        LOGGER.debug(".. Found checksum: {}", found);

        if (!calculated.equals(found)) {
            throw new InvalidChecksumException();
        }
    }

    private boolean hasFoundChecksum() throws IOException {
        LOGGER.trace("Looking for checksum");
        int len = in.read(buffer);
        if (len < buffer.length) {
            in.unread(buffer, 0, len);
            return false;
        }

        String text = new String(buffer);
        if (!text.startsWith(" checksum")) {
            LOGGER.trace("Unreading {}", Arrays.toString(buffer));
            in.unread(buffer);
            return false;
        }

        String checksum = text.substring(10, buffer.length - 1);
        LOGGER.trace("Found checksum: '{}'", checksum);
        foundChecksum = Optional.of(checksum);
        return true;
    }

    String getChecksum() {
        byte[] hash = digest.digest();
        return Hex.encodeHexString(hash);
    }

}
