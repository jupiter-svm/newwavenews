package ru.chikov.andrey.newwavenews;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateParser {
	public static long prepDate(String postdate) {
		int year;
		String day = "", month = "", hour = "", minute = "";

		Pattern pattern;
		Matcher matcher;

		Date d = new Date();
		GregorianCalendar gc = new GregorianCalendar();
		gc.setTime(d);

		year = gc.get(Calendar.YEAR);

		if (postdate.indexOf("�����") == -1
				&& postdate.indexOf("�������") == -1) {
			// ������� ���� ������
			day = postdate.substring(0, postdate.indexOf(" "));

			// if (day.length() == 1)
			// day = "0" + day;

			postdate = postdate.substring(postdate.indexOf(" ") + 1);

			// �����
			month = postdate.substring(0, postdate.indexOf(" "));
			postdate = postdate.substring(postdate.indexOf(" ") + 1);
			postdate = postdate.substring(postdate.indexOf(" ") + 1); // �������
																		// "� "

			month = getMonthNum(month);

			hour = postdate.substring(0, postdate.indexOf(":"));
			postdate = postdate.substring(postdate.indexOf(":") + 1);

			minute = postdate;

		} else if (postdate.indexOf("�������") != -1) {

			day = String.valueOf(gc.get(Calendar.DAY_OF_MONTH));
			month = String.valueOf(gc.get(Calendar.MONTH) + 1);

			if (month.length() == 1)
				month = "0" + month;

			pattern = Pattern.compile("\\d{1,2}:\\d{1,2}");
			matcher = pattern.matcher(postdate);

			if (matcher.find()) {
				String time = matcher.group(0);

				hour = time.substring(0, time.indexOf(":"));

				if (hour.length() == 1)
					hour = "0" + hour;

				minute = time.substring(time.indexOf(":") + 1);
			}
		} else if (postdate.indexOf("�����") != -1) {
			GregorianCalendar yesterday = new GregorianCalendar();

			Date today = new Date();
			yesterday.setTime(today);
			yesterday.add(Calendar.DAY_OF_YEAR, -1);

			day = String.valueOf(yesterday.get(Calendar.DAY_OF_MONTH));
			month = String.valueOf(yesterday.get(Calendar.MONTH) + 1);

			if (month.length() == 1)
				month = "0" + month;

			pattern = Pattern.compile("\\d{1,2}:\\d{1,2}");
			matcher = pattern.matcher(postdate);

			if (matcher.find()) {
				String time = matcher.group(0);

				hour = time.substring(0, time.indexOf(":"));

				if (hour.length() == 1)
					hour = "0" + hour;

				minute = time.substring(time.indexOf(":") + 1);
			}
		}

		GregorianCalendar resCal = new GregorianCalendar();

		resCal.set(Calendar.YEAR, year);
		resCal.set(Calendar.MONTH, Integer.valueOf(month));
		resCal.set(Calendar.DAY_OF_MONTH, Integer.valueOf(day));
		resCal.set(Calendar.HOUR_OF_DAY, Integer.valueOf(hour));
		resCal.set(Calendar.MINUTE, Integer.valueOf(minute));
		resCal.set(Calendar.MILLISECOND, 0);

		return resCal.getTimeInMillis();

	}

	// ������� ����� ������. � JDK 1.7 ����� ������������ ��� ����� switch
	private static String getMonthNum(String month) {

		if (month.equalsIgnoreCase("���"))
			month = "01";

		if (month.equalsIgnoreCase("���"))
			month = "02";

		if (month.equalsIgnoreCase("���"))
			month = "03";

		if (month.equalsIgnoreCase("���"))
			month = "04";

		if (month.equalsIgnoreCase("���"))
			month = "05";

		if (month.equalsIgnoreCase("���"))
			month = "06";

		if (month.equalsIgnoreCase("���"))
			month = "07";

		if (month.equalsIgnoreCase("���"))
			month = "08";

		if (month.equalsIgnoreCase("���"))
			month = "09";

		if (month.equalsIgnoreCase("���"))
			month = "10";

		if (month.equalsIgnoreCase("���"))
			month = "11";

		if (month.equalsIgnoreCase("���"))
			month = "12";

		return month;
	}

	// ���������� ���� � ������������� �� ���� � �����, ��� �� ����� � ��������
	public static String dateToPost(String millisec) {
		String date = "";
		String hour = "";
		String minute = "";

		// �������

		Calendar curDate = Calendar.getInstance();

		// ���� �����
		Calendar postDate = Calendar.getInstance();
		postDate.setTimeInMillis(Long.valueOf(millisec));

		// �����
		Calendar yesterday = Calendar.getInstance();
		yesterday.add(Calendar.DAY_OF_YEAR, -1);

		if (String.valueOf(postDate.get(Calendar.HOUR_OF_DAY)).length() == 1)
			hour = "0" + String.valueOf(postDate.get(Calendar.HOUR_OF_DAY));
		else
			hour = String.valueOf(postDate.get(Calendar.HOUR_OF_DAY));

		if (String.valueOf(postDate.get(Calendar.MINUTE)).length() == 1)
			minute = "0" + String.valueOf(postDate.get(Calendar.MINUTE));
		else
			minute = String.valueOf(postDate.get(Calendar.MINUTE));

		if (curDate.get(Calendar.DAY_OF_MONTH) == postDate
				.get(Calendar.DAY_OF_MONTH)) {

			date = "�������" + " � " + hour + ":" + minute;
		} else if (yesterday.get(Calendar.DAY_OF_MONTH) == postDate
				.get(Calendar.DAY_OF_MONTH)) {

			date = "�����" + " � " + hour + ":" + minute;
		} else {
			date = String.valueOf(postDate.get(Calendar.DAY_OF_MONTH)) + " "
					+ numMonthToName(postDate.get(Calendar.MONTH)) + " � "
					+ hour + ":" + minute;
		}

		return date;
	}

	// �������������� ������ ������ � ��������
	private static String numMonthToName(int number) {
		String name = "";

		switch (number) {
		case 1:
			name = "������";
			break;
		case 2:
			name = "�������";
			break;
		case 3:
			name = "�����";
			break;
		case 4:
			name = "������";
			break;
		case 5:
			name = "���";
			break;
		case 6:
			name = "����";
			break;
		case 7:
			name = "����";
			break;
		case 8:
			name = "�������";
			break;
		case 9:
			name = "��������";
			break;
		case 10:
			name = "�������";
			break;
		case 11:
			name = "������";
			break;
		case 12:
			name = "�������";
			break;
		}

		return name;

	}

	// ������� ��������� ������� ����
	public static String getCurDate() {
		Calendar calendar = Calendar.getInstance();

		String day = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
		String month = String.valueOf(calendar.get(Calendar.MONTH) + 1);
		String hour = String.valueOf(calendar.get(Calendar.HOUR_OF_DAY));
		String minute = String.valueOf(calendar.get(Calendar.MINUTE));
		
		//���������� ������� ����
		if(hour.length()==1)
			hour="0"+hour;
		
		if(minute.length()==1)
			minute="0"+minute;

		if (day.length() == 1)
			day = "0" + day;

		if (month.length() == 1)
			month = "0" + month;

		String updDate = day + "-" + month + "-"
				+ String.valueOf(calendar.get(Calendar.YEAR)) + " " + hour
				+ ":" + minute;
		return updDate;
	}
}
