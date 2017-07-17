package timer.lib;

import java.util.Calendar;
import java.util.Date;

public final class Dates {
	private Dates() {}

	public static Date toDate(Date date) {
		final Calendar dateCalendar = Calendar.getInstance();
		dateCalendar.setTime(date);
		dateCalendar.set(Calendar.HOUR_OF_DAY, 0);
		dateCalendar.set(Calendar.MINUTE, 0);
		dateCalendar.set(Calendar.SECOND, 0);
		dateCalendar.set(Calendar.MILLISECOND, 0);

		return dateCalendar.getTime();
	}
}
