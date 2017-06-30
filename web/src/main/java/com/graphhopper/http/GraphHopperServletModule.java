/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for 
 *  additional information regarding copyright ownership.
 * 
 *  GraphHopper GmbH licenses this file to you under the Apache License, 
 *  Version 2.0 (the "License"); you may not use this file except in 
 *  compliance with the License. You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.http;

import com.bedatadriven.jackson.datatype.jts.JtsModule;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.google.inject.Provides;
import com.google.inject.servlet.ServletModule;
import com.graphhopper.util.CmdArgs;

import javax.inject.Singleton;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Peter Karich
 */
public class GraphHopperServletModule extends ServletModule {
    protected final CmdArgs args;
    protected Map<String, String> params = new HashMap<String, String>();

    public GraphHopperServletModule(CmdArgs args) {
        this.args = args;
        params.put("mimeTypes", "text/html,"
                + "text/plain,"
                + "text/xml,"
                + "application/xhtml+xml,"
                + "application/gpx+xml,"
                + "application/xml,"
                + "text/css,"
                + "application/json,"
                + "application/javascript,"
                + "image/svg+xml");
    }

    @Override
    protected void configureServlets() {
        filter("*").through(HeadFilter.class);
        bind(HeadFilter.class).in(Singleton.class);

        filter("*").through(CORSFilter.class, params);
        bind(CORSFilter.class).in(Singleton.class);

        filter("*").through(IPFilter.class);
        bind(IPFilter.class).toInstance(new IPFilter(args.get("jetty.whiteips", ""), args.get("jetty.blackips", "")));

        serve("/i18n*").with(I18NServlet.class);
        bind(I18NServlet.class).in(Singleton.class);

        serve("/info*").with(InfoServlet.class);
        bind(InfoServlet.class).in(Singleton.class);

        serve("/route*").with(GraphHopperServlet.class);
        bind(GraphHopperServlet.class).in(Singleton.class);

        serve("/nearest*").with(NearestServlet.class);
        bind(NearestServlet.class).in(Singleton.class);

        if (args.getBool("web.change_graph.enabled", false)) {
            serve("/change*").with(ChangeGraphServlet.class);
            bind(ChangeGraphServlet.class).in(Singleton.class);
        }

        // Can't do this because otherwise we can't add more paths _after_ this module.
        // Instead, put this route explicitly into Jetty.
        // (We really need a web service framework.)
        // serve("/*").with(InvalidRequestServlet.class);
        bind(InvalidRequestServlet.class).in(Singleton.class);
    }
}
