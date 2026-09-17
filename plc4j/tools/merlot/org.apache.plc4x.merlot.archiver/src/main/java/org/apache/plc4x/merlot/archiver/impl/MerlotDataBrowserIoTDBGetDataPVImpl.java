/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.plc4x.merlot.archiver.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.plc4x.merlot.archiver.api.MerlotHtc;
import org.apache.plc4x.merlot.archiver.core.MerlotPBRawSerializer;
import org.epics.vtype.VType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MerlotDataBrowserIoTDBGetDataPVImpl extends HttpServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(MerlotDataBrowserIoTDBGetDataPVImpl.class);

   
    private final Pattern opti_pattern = Pattern.compile("optimized_\\d+\\(([^)]+)\\)");
    private final Pattern ncount_pattern = Pattern.compile("ncount\\(([^)]+)\\)");
    private final Pattern count_pattern = Pattern.compile("count_\\d+\\(([^)]+)\\)");
    private final Pattern optimLast_pattern = Pattern.compile("optimLastSample_\\d+\\(([^)]+)\\)");

    private final MerlotHtc mhtc;

    public MerlotDataBrowserIoTDBGetDataPVImpl(MerlotHtc mhtc) {
        this.mhtc = mhtc;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String from = req.getParameter("from");
        String to = req.getParameter("to");
        String[] pvs = req.getParameterValues("pv");

        LOGGER.info("Inicio Servlet.");
        if (from == null || to == null || pvs == null || pvs.length == 0) {
            return;
        }

        resp.setContentType("application/octet-stream");
        OutputStream out = resp.getOutputStream();

        for (String pv : pvs) {
            Matcher opti_matcher = opti_pattern.matcher(pv);
            Matcher ncount_matcher = ncount_pattern.matcher(pv);
            Matcher count_matcher = count_pattern.matcher(pv);
            Matcher optimLast_matcher = optimLast_pattern.matcher(pv);

            if (opti_matcher.matches()) {
                LOGGER.info("optimized pattern not supported for PV: {}", pv);
            } else if (optimLast_matcher.matches()) {
                LOGGER.info("optimLastSample pattern not supported for PV: {}", pv);
            } else if (ncount_matcher.matches()) {
                LOGGER.info("NCount events");
            } else if (count_matcher.matches()) {
                LOGGER.info("count pattern not supported for PV: {}", pv);
            } else {
                createRawResponse(pv, from, to, out);
            }
        }
        out.flush();
    }

    private void createRawResponse(String pv, String from, String to, OutputStream out) throws IOException {
        List<VType> values = mhtc.getPVs(pv, from, to);
        
        System.out.println(mhtc.getPVs(pv, from, to).size());
        if (values == null || values.isEmpty()) {
            return;
        }

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        MerlotPBRawSerializer.serializeIoTDBToPBRaw(values, pv, bout);
        out.write(bout.toByteArray());
    }
}
