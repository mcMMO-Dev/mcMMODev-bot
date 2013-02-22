package com.nuclearw.mcmmobot;

import java.util.ArrayList;
import java.util.List;

public class Config {
	public static String prefix;

	public static String nick, host, password;

	public static List<String> channels;

	public static int port;

	public static boolean ssl;

	public static void load() {
		// TODO: Load
		/**/
		prefix = "`";

		ssl = true;
		nick = "mcMMO";
		host = "nuclearw.com";
		port = 6666;
		password = "";

		channels = new ArrayList<String>();
		channels.add("#mcmmodev");
	}
}
