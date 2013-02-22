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

public class Main {
	private static Bot bot;
	public static Thread daysTask;
	public static boolean online = true; // Use this to kill the topic task

	public static HashMap<Integer, String> builds = new HashMap<Integer, String>();

	public static void main(String[] args) throws Exception{
		Config.load();

		Runnable daysRunnable = new Runnable(){
			@Override
			public void run(){
				while (online){
					System.out.println("Running runnable");

					long lastReset = 0;
					long now = System.currentTimeMillis();

					try{
						FileInputStream fis = new FileInputStream("lastReset");
						InputStreamReader in = new InputStreamReader(fis, "UTF-8");

						Scanner scan = new Scanner(in);
						lastReset = scan.nextLong();

						in.close();
						fis.close();
					}catch(Exception e){
						lastReset = now;
						try{
							Main.writeReset(now);
						}catch(IOException e1){
							e1.printStackTrace();
						}
					}

					long waitTime = Config.countInDays ? 86400000 : 1000;

					if(lastReset + waitTime <= now){
						try{
							String topic = Bot.topic;

							String trail = Config.countInDays ? "days" : "seconds";
							System.out.println("Topic: " + topic);

							Matcher matcher = Pattern.compile("([0-9]+) " + trail).matcher(topic);

							matcher.find();

							long newDays = Integer.valueOf(matcher.group(1)) + 1;

							topic = matcher.replaceAll(newDays + " " + trail);

							bot.setTopic("#mcmmodev", topic);
							//bot.setTopic("#turt2live", topic); // turt2live

							Main.writeReset(now);
						}catch(Exception e){
							e.printStackTrace();
							System.out.println("Could not change topic");
						}
					}else{
						waitTime = lastReset + waitTime - now + 1;
					}

					try{
						// Reduce spam, less than 10k we wait 1 second, else do math
						Thread.sleep(waitTime < 10000 ? 1000 : waitTime / 1000);
					}catch(InterruptedException e){
						e.printStackTrace();
					}
				}
			}
		};

		daysTask = new Thread(daysRunnable);

		bot = new Bot();
		bot.setVerbose(true);
		//bot.connect(Config.host, Config.port, Config.password, new TrustingSSLSocketFactory());
		bot.connect(Config.host, Config.port, Config.password, null);
		for(String channel : Config.channels){
			bot.joinChannel(channel);
		}

		BufferedReader br = new BufferedReader(new FileReader(new File("builds")));
		String line;
		while ((line = br.readLine()) != null){
			if(line.contains(","))
				;
			try{
				Integer build = Integer.parseInt(line.substring(line.indexOf(",") + 1));
				String sha = line.substring(0, line.indexOf(","));
				System.out.println(build + " " + sha);
				builds.put(build, sha);
			}catch(Exception e){}
		}
		br.close();
	}

	public static Bot getBot(){
		return bot;
	}

	public static void addBuild(Integer build, String sha) throws IOException{
		if(builds.containsKey(build))
			return;

		builds.put(build, sha);

		FileWriter fw = new FileWriter("builds", true); //the true will append the new data
		fw.write(sha + "," + build + "\n");//appends the string to the file
		fw.close();
	}

	public static void writeReset(long time) throws IOException{
		FileOutputStream fos = new FileOutputStream("lastReset");
		OutputStreamWriter out = new OutputStreamWriter(fos, "UTF-8");

		System.out.println("Wrote file with: " + time);

		out.write(Long.valueOf(time).toString());

		out.flush();
		out.close();
		fos.close();
	}
}
