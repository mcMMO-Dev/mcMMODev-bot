package com.nuclearw.mcmmobot;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jibble.pircbot.PircBot;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Bot extends PircBot {
	public static String topic;
	private boolean started = false;

	private static final String googleKey = "";
	private static final String ghBase = "https://api.github.com/repos/mcMMO-Dev/mcMMO/";
	private static final String jenkinsBase = "http://ci.mcmmo.info/job/mcMMO/";

	public Bot() {
		this.setName(Config.nick);
	}

	@Override
	protected void onTopic(String channel, String topic, String setBy, long date, boolean changed) {
		if(channel.contains("mcmmodev")) {
			Bot.topic = topic;
			if(!started) {
				Main.daysTask.start();
				started = true;
			}
		}
	}

	@Override
	protected void onMessage(String channel, String sender, String login, String hostname, String message) {
		if(channel.contains("mcmmodev")) {
			if(message.equalsIgnoreCase(".reset")) {
				String topic = Bot.topic;

				System.out.println("Topic: " + topic);

				Matcher matcher = Pattern.compile("([0-9]+) days").matcher(topic);

				matcher.find();

				int newDays = 0;

				topic = matcher.replaceAll(newDays + " days");

				setTopic("#mcmmodev", topic);

				try {
					Main.writeReset(System.currentTimeMillis());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		if(channel.equalsIgnoreCase("#mcmmo") || channel.equalsIgnoreCase("#mcmmodev")) {
			try {
				Set<Matcher> matchers = new HashSet<Matcher>();
				matchers.add(Pattern.compile("![0-9]+").matcher(message));
				matchers.add(Pattern.compile("github.com/mcMMO-Dev/mcMMO/issues/[0-9]+").matcher(message));

				for(Matcher matcher : matchers) {
					while(matcher.find()) {
						try {
							String issue = message.substring(matcher.start(), matcher.end());
							String returnMessage = issueMessage(issue);

							sendMessage(channel, returnMessage);
						} catch (Exception ex) { }
					}
				}
			} catch (Exception ex) { }

			if(message.toLowerCase().equals("!issues")) {
				sendMessage(channel, "https://github.com/mcMMO-Dev/mcMMO/issues");
			}

			try {
				Set<Matcher> matchers = new HashSet<Matcher>();
				matchers.add(Pattern.compile("!ch ([0-9]+)((?: )?[0-9]*)").matcher(message));

				for(Matcher matcher : matchers) {
					while(matcher.find()) {
						try {
							String match = message.substring(matcher.start(), matcher.end());
							Matcher number = Pattern.compile("[0-9]+").matcher(match);
							number.find();
							Integer startBuild = Integer.parseInt(match.substring(number.start(), number.end()));
							Integer endBuild = null;
							if(number.find()) {
								endBuild = Integer.parseInt(match.substring(number.start(), number.end()));
							}

							// First round to try to get info from Jenkins
							if(!Main.builds.containsKey(startBuild)) {
								try {
									tryFetchBuild(startBuild);
								} catch(Exception e) { }
							}
							
							if(endBuild != null && !Main.builds.containsKey(endBuild)) {
								try {
									tryFetchBuild(endBuild);
								} catch(Exception e) { }
							}

							// Second round after we tried to get info from Jenkins
							if(!Main.builds.containsKey(startBuild)) {
								sendMessage(channel, "I don't have any information on build #" + startBuild);
								return;
							}

							if(endBuild != null && !Main.builds.containsKey(endBuild)) {
								sendMessage(channel, "I don't have any information on build #" + startBuild);
								return;
							}

							String base = "https://github.com/mcMMO-Dev/mcMMO/compare/";
							base += Main.builds.get(startBuild);
							base += "...";
							if(endBuild != null) {
								base += Main.builds.get(endBuild);
							} else {
								base += "master";
							}

							String shortUrl = shortenUrl(base);

							sendMessage(channel, "Changes: " + shortUrl);
							
						} catch (Exception ex) { }
					}
				}
			} catch (Exception ex) { }
		}
	}

	private static void tryFetchBuild(Integer build) throws IOException, JSONException {
		JSONObject main = readJsonFromUrl(jenkinsBase + build + "/api/json?tree=actions[lastBuiltRevision[SHA1]]");
		JSONArray array = main.getJSONArray("actions");
		String sha = "";
		for(int i = 0; i < array.length(); i++) {
			JSONObject iObject = array.getJSONObject(i);
			if(iObject.has("lastBuiltRevision")) {
				sha = iObject.getJSONObject("lastBuiltRevision").getString("SHA1");
				break;
			}
		}
		if(!sha.isEmpty()) {
			System.out.println(build + " " + sha);
			Main.addBuild(build, sha);
		}
	}

	private static String issueMessage(String issue) throws IOException, JSONException {
		Matcher matcher = Pattern.compile("[0-9]+").matcher(issue);
		if(matcher.find()) {
			issue = issue.substring(matcher.start(), matcher.end());
		}

		int issueNumber = Integer.valueOf(issue);
		if(issueNumber <= 0) return null;

		JSONObject main = readJsonFromUrl(ghBase + "issues/" + issueNumber);
		String url = main.getString("html_url");
		String title = main.getString("title");

		String shortUrl = shortenUrl(url);

		return "Issue #" + issueNumber + ": " + shortUrl + " - " + title;
	}

	private static String shortenUrl(String url) throws JSONException {
		JSONObject request = new JSONObject();
		request.append("longUrl", url);

		String responseStr = executePost("https://www.googleapis.com/urlshortener/v1/url?key=" + googleKey, request.toString());
		JSONObject response = new JSONObject(responseStr);

		return response.getString("id");
	}

	private static String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}

	public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
		InputStream is = new URL(url).openStream();
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
			String jsonText = readAll(rd);
			JSONObject json = new JSONObject(jsonText);
			return json;
		} finally {
			is.close();
		}
	}

	public static String executePost(String targetURL, String urlParameters)
	{
		URL url;
		HttpURLConnection connection = null;	
		try {
			//Create connection
			url = new URL(targetURL);
			connection = (HttpURLConnection)url.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/json");

			connection.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
			connection.setRequestProperty("Content-Language", "en-US");	

			connection.setUseCaches(false);
			connection.setDoInput(true);
			connection.setDoOutput(true);

			//Send request
			DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
			wr.writeBytes(urlParameters);
			wr.flush();
			wr.close();

			//Get Response	
			InputStream is = connection.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			String line;
			StringBuffer response = new StringBuffer(); 
			while((line = rd.readLine()) != null) {
				response.append(line);
				response.append('\r');
			}
			rd.close();
			return response.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			if(connection != null) {
				connection.disconnect(); 
			}
		}
	}
}
