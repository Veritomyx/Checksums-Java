/*
 * Copyright (c) 2018 Veritomyx, Inc.
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

package com.veritomyx.checksums.app;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.veritomyx.checksums.app.cat.CatApp;
import com.veritomyx.checksums.app.cat.CatSettings;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

class Checksum {

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        final MainSettings mainSettings = new MainSettings();
        final CatSettings catSettings = new CatSettings();
        final JCommander jCommander = JCommander.newBuilder()
                .addObject(mainSettings)
                .addCommand("cat", catSettings)
                .build();

        jCommander.parse(args);

        final String command = jCommander.getParsedCommand();
        if (command == null) {
            jCommander.usage();
            return;
        }

        switch (jCommander.getParsedCommand()) {
            case "cat":
                runCat(jCommander, catSettings);
                break;
            default:
                jCommander.usage();
        }
    }

    static class MainSettings {
        @Parameter(names = "--help", help = true)
        private boolean help;

        boolean getHelp() {
            return help;
        }
    }

    private static void runCat(JCommander jCommander, CatSettings catSettings) throws IOException, NoSuchAlgorithmException {
        if (catSettings.getHelp()) {
            jCommander.usage("cat");
            return;
        }

        CatApp.main(catSettings.getFiles().toArray(new String[0]));
    }
}
