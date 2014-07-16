package ru.chikov.andrey.newwavenews;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class NWNews {
	private Document doc;
	private String login;
	private String pass;
	private int connTimeout;
	private boolean totalgroup;
	private boolean kickassgroup;
	private boolean knockoutgroup;

	private Map<String, String> cookies = new HashMap<String, String>();

	public NWNews(String login, String pass, int connTimeout,
			boolean totalgroup, boolean kickassgroup, boolean knockoutgroup) {
		this.login = login;
		this.pass = pass;
		this.connTimeout = connTimeout;
		this.totalgroup = totalgroup;
		this.kickassgroup = kickassgroup;
		this.knockoutgroup = knockoutgroup;
	}

	// ���������� 0 ����� �� ��, 1 ��� ������������ ������/������, 2 ��� ������
	// ����������
	public int Connect() {
		Response resp;

		try {
			resp = Jsoup.connect("https://login.vk.com/?act=login")
					.data("email", login).data("pass", pass)
					.method(Method.POST).timeout(connTimeout).execute();

			cookies = resp.cookies();

		} catch (IOException e) {
			e.printStackTrace();
			return 2;
		}

		// ���� ����������� �� ��������, �� ���������� �������� �� ��������
		// ������������
		if (resp.url().toString().indexOf("login.php") != -1)
			return 1;

		return 0;
	}

	// TODO ������� ��������� ���� � ��������� �����
	public ArrayList<PostData> getNews() {
		Elements wall;
		String link = "";
		ArrayList<PostData> postData = new ArrayList<PostData>();

		/**************** ������� �� �������� ������ **************/

		if (totalgroup) {
			// ���� �� ������� �������� �������, ��������� ������ ������
			try {
				doc = Jsoup.connect("http://m.vk.com/newwavefamily")
						.cookies(cookies).timeout(connTimeout).get();
			} catch (IOException e) {
				e.printStackTrace();
			}

			wall = doc.getElementsByClass("post");

			for (Element post : wall) {
				link = post.getElementsByClass("medias").html();
				link = getLink(link);

				PostData line = new PostData(String.valueOf(DateParser
						.prepDate(post.getElementsByClass("date").text())),
						post.getElementsByClass("author").text(),
						remFullView(post.getElementsByClass("text").text()),
						link, "�������� ������", "true");
				postData.add(line);
			}
		}

		/**************** ������� �� Kick Ass Crew **************/
		if (kickassgroup) {
			// ���� �� ������� �������� �������, ��������� ������ ������
			try {
				doc = Jsoup.connect("http://m.vk.com/kickasscrew")
						.cookies(cookies).timeout(connTimeout).get();
			} catch (IOException e) {
				e.printStackTrace();
			}

			link = "";

			wall = doc.getElementsByClass("post");

			for (Element post : wall) {
				link = post.getElementsByClass("medias").html();
				link = getLink(link);

				PostData line = new PostData(String.valueOf(DateParser
						.prepDate(post.getElementsByClass("date").text())),
						post.getElementsByClass("author").text(),
						remFullView(post.getElementsByClass("text").text()),
						link, "KickAssCrew", "true");
				postData.add(line);
			}
		}

		/**************** ������� �� KnockOut Crew **************/
		if (knockoutgroup) {			
			// ���� �� ������� �������� �������, ��������� ������ ������
			try {
				doc = Jsoup.connect("http://m.vk.com/knockoutcrew")
						.cookies(cookies).timeout(connTimeout).get();
			} catch (IOException e) {
				e.printStackTrace();
			}

			link = "";

			wall = doc.getElementsByClass("post");

			for (Element post : wall) {
				link = post.getElementsByClass("medias").html();
				link = getLink(link);

				PostData line = new PostData(String.valueOf(DateParser
						.prepDate(post.getElementsByClass("date").text())),
						post.getElementsByClass("author").text(),
						remFullView(post.getElementsByClass("text").text()),
						link, "KnockOutCrew", "true");
				postData.add(line);
			}
		}

		return postData;
	}

	// �������� ������ �� ����� � ����������� ����
	private String getLink(String link) {
		Pattern pattern = Pattern.compile("((?<==)http.*?(?=" + '"'
				+ "))|((?<=src=" + '"' + ")http.*?(?=" + '"' + "))");
		Matcher matcher = pattern.matcher(link);

		if (matcher.find()) {
			link = matcher.group(0);

			// TODO �������������� ����� ������ ��������� .replace
			link = link.replace("%3A%2F%2F", "://");
			link = link.replace("%2F", "/");
			link = link.replace("%3F", "?");
			link = link.replace("%3D", "=");
		} else
			link = "";

		return link;
	}

	// ������� ����� "�������� ���������"
	private String remFullView(String text) {
		return text = text.replace("�������� ���������..", "");
	}
}
