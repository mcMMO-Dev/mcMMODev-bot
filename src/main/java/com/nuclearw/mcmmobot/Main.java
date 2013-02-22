package com.nuclearw.mcmmobot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jibble.pircbot.TrustingSSLSocketFactory;

public class Main {
	private static Bot bot;
	public static Thread daysTask;

	public static HashMap<Integer, String> builds = new HashMap<Integer, String>();

	public static void main(String[] args) throws Exception {
		Config.load();

		bot = new Bot();
		bot.setVerbose(true);
		bot.connect(Config.host, Config.port, Config.password, new TrustingSSLSocketFactory());
		for(String channel : Config.channels) {
			bot.joinChannel(channel);
		}

		BufferedReader br = new BufferedReader(new FileReader(new File("builds")));
		String line;
		while((line = br.readLine()) != null) {
			if(line.contains(","));
			try {
				Integer build = Integer.parseInt(line.substring(line.indexOf(",")+1));
				String sha = line.substring(0, line.indexOf(","));
				System.out.println(build + " " + sha);
				builds.put(build, sha);
			} catch(Exception e) { }
		}
		br.close();

		Runnable daysRunnable = new Runnable(){
			@Override
			public void run() {
				System.out.println("Running runnable");

				long lastReset = 0;
				long now = System.currentTimeMillis();

				try {
					FileInputStream fis = new FileInputStream("lastReset");
					InputStreamReader in = new InputStreamReader(fis, "UTF-8");

					Scanner scan = new Scanner(in);
					lastReset = scan.nextLong();

					in.close();
					fis.close();
				} catch(Exception e) {
					lastReset = now;
					try {
						Main.writeReset(now);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}

				long waitTime = 86400000;

				if(lastReset + 86400000 <= now) {
					try {
						String topic = Bot.topic;

						System.out.println("Topic: " + topic);

						Matcher matcher = Pattern.compile("([0-9]+) days").matcher(topic);

						matcher.find();

						long newDays = Integer.valueOf(matcher.group(1)) + 1;

						topic = matcher.replaceAll(newDays + " days");

						bot.setTopic("#mcmmodev", topic);

						Main.writeReset(now);
					} catch(Exception e) {
						e.printStackTrace();
						System.out.println("Could not change topic");
					}
				} else {
					waitTime = lastReset + 86400000 - now + 1;
				}

				try {
					Thread.sleep(waitTime/1000);
				} catch(InterruptedException e) {
					e.printStackTrace();
				}
			}
		};

		daysTask = new Thread(daysRunnable);
	}

	public static Bot getBot() {
		return bot;
	}

	public static void addBuild(Integer build, String sha) throws IOException {
		if(builds.containsKey(build)) return;

		builds.put(build, sha);

		FileWriter fw = new FileWriter("builds",true); //the true will append the new data
	    fw.write(sha + "," + build + "\n");//appends the string to the file
	    fw.close();
	}

	public static void writeReset(long time) throws IOException {
		FileOutputStream fos = new FileOutputStream("lastReset");
		OutputStreamWriter out = new OutputStreamWriter(fos, "UTF-8");

		System.out.println("Wrote file with: " + time);

		out.write(Long.valueOf(time).toString());

		out.flush();
		out.close();
		fos.close();
	}
}
