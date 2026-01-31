package com.damianq1.playermaxai.parser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class M3UParser {

    public static class Channel {
        public final String name;
        public final String url;
        public final String group;
        public final String logo;
        public final String epgInfo;

        public Channel(String name, String url, String group, String logo, String epgInfo) {
            this.name = (name != null) ? name.trim() : "Bez nazwy";
            this.url = url;
            this.group = (group != null && !group.isEmpty()) ? group : "Inne";
            this.logo = logo;
            this.epgInfo = epgInfo;
        }
    }

    public static List<Channel> parse(String m3uUrl) throws Exception {
        List<Channel> channels = new ArrayList<>();
        URL urlObj = new URL(m3uUrl);
        BufferedReader reader = new BufferedReader(new InputStreamReader(urlObj.openStream()));

        String line;
        String name = null;
        String group = null;
        String logo = null;
        StringBuilder epgBuilder = new StringBuilder();

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("#EXTINF:")) {
                int comma = line.indexOf(',');
                if (comma > -1) {
                    String meta = line.substring(0, comma);
                    name = line.substring(comma + 1).trim();

                    if (meta.contains("group-title=\"")) {
                        group = meta.split("group-title=\"")[1].split("\"")[0];
                    }

                    if (meta.contains("tvg-logo=\"")) {
                        logo = meta.split("tvg-logo=\"")[1].split("\"")[0];
                    }

                    if (name.contains("|")) {
                        String[] parts = name.split("\\|", 2);
                        name = parts[0].trim();
                        if (parts.length > 1) {
                            epgBuilder.append(parts[1].trim());
                        }
                    }
                }
            } else if (!line.startsWith("#") && !line.isEmpty() && name != null) {
                channels.add(new Channel(name, line, group, logo, epgBuilder.toString()));
                name = null;
                group = null;
                logo = null;
                epgBuilder.setLength(0);
            }
        }

        reader.close();
        return channels;
    }
}
